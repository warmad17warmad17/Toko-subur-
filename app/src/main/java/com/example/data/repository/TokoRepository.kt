package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.ProductDao
import com.example.data.dao.StoreSettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StoreSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TokoRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao,
    private val storeSettingsDao: StoreSettingsDao
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

    fun getItemsForTransaction(txId: Long): Flow<List<TransactionItemEntity>> =
        transactionDao.getItemsForTransaction(txId)

    suspend fun getItemsForTransactionSync(txId: Long): List<TransactionItemEntity> =
        transactionDao.getItemsForTransactionSync(txId)

    suspend fun processSale(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val txId = transactionDao.insertTransaction(transaction)
        val itemsWithId = items.map { it.copy(transactionId = txId) }
        transactionDao.insertTransactionItems(itemsWithId)

        // Decrement product stock
        for (item in items) {
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
        val items = transactionDao.getItemsForTransactionSync(txId)
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

    suspend fun syncTransactionsHpp(): Int {
        val allTx = transactionDao.getAllTransactionsSync()
        val allProducts = productDao.getAllProducts().firstOrNull() ?: emptyList()
        val productMap = allProducts.associateBy { it.id }
        var syncedCount = 0

        for (tx in allTx) {
            val items = transactionDao.getItemsForTransactionSync(tx.id)
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

            if (updatedItems) {
                transactionDao.updateTransactionItems(updatedItemList)
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
        expenses: List<ExpenseEntity>
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
    }
}
