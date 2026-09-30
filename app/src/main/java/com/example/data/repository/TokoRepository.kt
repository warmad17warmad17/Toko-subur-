package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExcludedPromoProductDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ProductDao
import com.example.data.dao.StoreSettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExcludedPromoProductEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StoreSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.util.TransactionItemHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class TokoRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao,
    private val storeSettingsDao: StoreSettingsDao,
    private val excludedPromoProductDao: ExcludedPromoProductDao
) {
    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun addCategory(name: String): Long {
        return categoryDao.insertCategory(CategoryEntity(name = name.trim()))
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun canDeleteCategory(categoryId: Long): Boolean {
        val count = productDao.getProductCountByCategoryId(categoryId)
        return count == 0
    }

    suspend fun deleteCategory(category: CategoryEntity): Result<Unit> {
        val count = productDao.getProductCountByCategoryId(category.id)
        return if (count == 0) {
            categoryDao.deleteCategory(category)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Katalog tidak bisa dihapus karena masih memiliki $count produk di dalamnya."))
        }
    }

    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    suspend fun getProductByQrCode(qrCode: String): ProductEntity? = productDao.getProductByQrCode(qrCode.trim())

    suspend fun addProduct(product: ProductEntity): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allTransactionItems: Flow<List<TransactionItemEntity>> = transactionDao.getAllTransactionItems()

    // Excluded Promo Products for Top 10 Best Sellers
    val excludedPromoProducts: Flow<List<ExcludedPromoProductEntity>> = excludedPromoProductDao.getAllExcluded()

    suspend fun addExcludedPromoProduct(productId: Long, productName: String, reason: String = "Sedang Promo Toko") {
        excludedPromoProductDao.insertExcluded(
            ExcludedPromoProductEntity(
                productId = productId,
                productName = productName.trim(),
                reason = reason.trim(),
                excludedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeExcludedPromoProduct(productId: Long) {
        excludedPromoProductDao.deleteByProductId(productId)
    }

    suspend fun getExcludedPromoProductsSync(): List<ExcludedPromoProductEntity> =
        excludedPromoProductDao.getAllExcludedSync()

    suspend fun getTransactionById(txId: Long): TransactionEntity? = transactionDao.getTransactionById(txId)

    fun getItemsForTransaction(txId: Long): Flow<List<TransactionItemEntity>> =
        transactionDao.getItemsForTransaction(txId).map { rawItems ->
            val tx = transactionDao.getTransactionById(txId)
            TransactionItemHelper.deduplicateItems(rawItems, tx?.totalAmount ?: 0.0)
        }

    suspend fun getItemsForTransactionSync(txId: Long): List<TransactionItemEntity> {
        val rawItems = transactionDao.getItemsForTransactionSync(txId)
        val tx = transactionDao.getTransactionById(txId)
        return TransactionItemHelper.deduplicateItems(rawItems, tx?.totalAmount ?: 0.0)
    }

    suspend fun processSale(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val cleanItems = TransactionItemHelper.deduplicateItems(items, transaction.totalAmount)
        val cleanCost = cleanItems.sumOf { it.quantity * it.unitCost }
        val finalTx = if (cleanCost > 0.0 && transaction.totalCost <= 0.0) {
            transaction.copy(totalCost = cleanCost)
        } else transaction

        val txId = transactionDao.insertTransaction(finalTx)
        val itemsWithId = cleanItems.map { it.copy(id = 0, transactionId = txId) }
        transactionDao.deleteTransactionItemsByTransactionId(txId)
        transactionDao.insertTransactionItems(itemsWithId)

        // Decrement product stock
        for (item in cleanItems) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val updatedStock = (product.stok - item.quantity).coerceAtLeast(0)
                productDao.updateStock(product.id, updatedStock)
            }
        }
        return txId
    }

    /**
     * Menghapus transaksi yang sudah berhasil (misalnya ketika transaksi dibatalkan pelanggan).
     * Jika restoreStock bernilai true, stok produk yang terjual akan dikembalikan ke data inventaris toko.
     * Mengembalikan daftar ProductEntity yang stoknya telah diperbarui untuk disinkronkan ke cloud.
     */
    suspend fun deleteTransaction(txId: Long, restoreStock: Boolean = true): List<ProductEntity> {
        val restoredProducts = mutableListOf<ProductEntity>()
        val rawItems = transactionDao.getItemsForTransactionSync(txId)
        val tx = transactionDao.getTransactionById(txId)
        val items = if (tx != null) TransactionItemHelper.deduplicateItems(rawItems, tx.totalAmount) else rawItems
        if (restoreStock) {
            for (item in items) {
                val product = productDao.getProductById(item.productId)
                if (product != null) {
                    val updatedStock = product.stok + item.quantity
                    productDao.updateStock(product.id, updatedStock)
                    restoredProducts.add(product.copy(stok = updatedStock))
                }
            }
        }
        transactionDao.deleteTransactionItemsByTransactionId(txId)
        transactionDao.deleteTransactionById(txId)
        return restoredProducts
    }

    /**
     * Membersihkan dan merapikan struk transaksi di database lokal yang memiliki produk ganda/double,
     * serta memastikan tidak ada duplikasi data akibat sinkronisasi berulang.
     */
    suspend fun cleanupDuplicateTransactionItems(): Int {
        val allTx = transactionDao.getAllTransactionsSync()
        var fixedCount = 0

        for (tx in allTx) {
            val rawItems = transactionDao.getItemsForTransactionSync(tx.id)
            if (rawItems.isEmpty()) continue

            val cleanItems = TransactionItemHelper.deduplicateItems(rawItems, tx.totalAmount)
            val hasChanged = cleanItems.size != rawItems.size ||
                    cleanItems.sumOf { it.quantity } != rawItems.sumOf { it.quantity } ||
                    cleanItems.sumOf { it.subtotal } != rawItems.sumOf { it.subtotal }

            if (hasChanged) {
                transactionDao.deleteTransactionItemsByTransactionId(tx.id)
                val itemsWithId = cleanItems.map { it.copy(id = 0, transactionId = tx.id) }
                transactionDao.insertTransactionItems(itemsWithId)
                fixedCount++
            }
        }
        return fixedCount
    }

    suspend fun syncTransactionsHpp(): Int {
        cleanupDuplicateTransactionItems()
        val allTx = transactionDao.getAllTransactionsSync()
        val allProducts = productDao.getAllProducts().firstOrNull() ?: emptyList()
        val productMap = allProducts.associateBy { it.id }
        var syncedCount = 0

        for (tx in allTx) {
            val rawItems = transactionDao.getItemsForTransactionSync(tx.id)
            val items = TransactionItemHelper.deduplicateItems(rawItems, tx.totalAmount)
            var updatedItems = false

            val updatedItemList = items.map { item ->
                var unitCost = item.unitCost
                if (unitCost <= 0.0) {
                    val prod = productMap[item.productId] ?: allProducts.firstOrNull { 
                        it.qrCode.equals(item.qrCode, ignoreCase = true) || it.name.equals(item.productName, ignoreCase = true) 
                    }
                    if (prod != null && prod.hargaBeli > 0.0) {
                        unitCost = prod.hargaBeli
                        updatedItems = true
                    }
                }
                item.copy(unitCost = unitCost)
            }

            if (updatedItems || updatedItemList.size != rawItems.size) {
                transactionDao.deleteTransactionItemsByTransactionId(tx.id)
                val toInsert = updatedItemList.map { it.copy(id = 0, transactionId = tx.id) }
                transactionDao.insertTransactionItems(toInsert)
            }

            val totalCostFromItems = updatedItemList.sumOf { it.quantity * it.unitCost }
            if (totalCostFromItems > 0.0 && (tx.totalCost <= 0.0 || tx.totalCost != totalCostFromItems)) {
                transactionDao.updateTransaction(tx.copy(totalCost = totalCostFromItems))
                syncedCount++
            }
        }
        return syncedCount
    }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun addExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    // Store Settings
    val storeSettings: Flow<StoreSettingsEntity?> = storeSettingsDao.getSettings()

    suspend fun getStoreSettingsSync(): StoreSettingsEntity {
        return storeSettingsDao.getSettingsSync() ?: StoreSettingsEntity()
    }

    suspend fun updateStoreSettings(settings: StoreSettingsEntity) {
        storeSettingsDao.insertOrUpdateSettings(settings)
    }

    // Data Export & Restore Accessors
    suspend fun getAllProductsSync(): List<ProductEntity> =
        allProducts.firstOrNull() ?: emptyList()

    suspend fun getAllCategoriesSync(): List<CategoryEntity> =
        allCategories.firstOrNull() ?: emptyList()

    suspend fun getAllTransactionsSync(): List<TransactionEntity> =
        allTransactions.firstOrNull() ?: emptyList()

    suspend fun getAllExpensesSync(): List<ExpenseEntity> =
        allExpenses.firstOrNull() ?: emptyList()

    suspend fun restoreDatabase(
        settings: StoreSettingsEntity?,
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        transactionItems: List<TransactionItemEntity>,
        expenses: List<ExpenseEntity>,
        excludedPromoProducts: List<ExcludedPromoProductEntity> = emptyList()
    ) {
        if (settings != null) {
            storeSettingsDao.insertOrUpdateSettings(settings)
        }
        for (cat in categories) {
            categoryDao.insertCategory(cat)
        }
        for (prod in products) {
            productDao.insertProduct(prod)
        }
        for (tx in transactions) {
            transactionDao.insertTransaction(tx)
        }
        if (transactionItems.isNotEmpty()) {
            transactionDao.insertTransactionItems(transactionItems)
        }
        for (exp in expenses) {
            expenseDao.insertExpense(exp)
        }
        for (excluded in excludedPromoProducts) {
            excludedPromoProductDao.insertExcluded(excluded)
        }
    }
}
