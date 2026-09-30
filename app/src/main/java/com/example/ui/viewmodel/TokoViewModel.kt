package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StoreSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.data.repository.TokoRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.GoogleAuthManager
import com.example.data.sync.GoogleUser
import com.example.data.sync.SyncStatus
import com.example.util.BackupData
import com.example.util.BackupManager
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter
import com.example.util.NotificationHelper
import com.example.util.NotificationPreferences
import com.example.util.NotificationSoundItem
import com.example.data.auth.AppAuthManager
import com.example.data.auth.CashierDeviceInfo
import com.example.data.auth.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.data.model.DailyReportSummary
import com.example.data.model.ExcludedPromoProductEntity
import com.example.data.model.TopSellingProduct
import com.example.data.model.WeeklyReportSummary
import com.example.util.TransactionItemHelper

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val subtotal: Double get() = product.hargaJual * quantity
    val subtotalCost: Double get() = product.hargaBeli * quantity
}

enum class PeriodFilter(val label: String) {
    HARI_INI("Hari Ini"),
    MINGGU_INI("Minggu Ini"),
    BULAN_INI("Bulan Ini"),
    SEMUA("Semua")
}

class TokoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TokoRepository
    val authManager = GoogleAuthManager(application)
    val authRoleManager = AppAuthManager(application)
    val cloudSyncManager: CloudSyncManager
    val notificationPrefs = NotificationPreferences(application)

    val currentUserRole: StateFlow<UserRole> = authRoleManager.currentUserRole
    val activeCashierCount: StateFlow<Int> get() = cloudSyncManager.activeCashierCount
    val activeCashiers: StateFlow<List<CashierDeviceInfo>> get() = cloudSyncManager.activeCashiers

    val activeCashierLabel: StateFlow<String> by lazy {
        cloudSyncManager.activeCashierCount.map { count ->
            "Kasir ${count.coerceIn(1, 9)}"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Kasir 1")
    }

    private val _showLoginRoleDialog = MutableStateFlow(false)
    val showLoginRoleDialog: StateFlow<Boolean> = _showLoginRoleDialog.asStateFlow()

    private val _showActiveCashiersDialog = MutableStateFlow(false)
    val showActiveCashiersDialog: StateFlow<Boolean> = _showActiveCashiersDialog.asStateFlow()

    private val _isNotificationEnabled = MutableStateFlow(notificationPrefs.isNotificationEnabled)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled.asStateFlow()

    private val _selectedNotificationSoundTitle = MutableStateFlow(notificationPrefs.soundTitle)
    val selectedNotificationSoundTitle: StateFlow<String> = _selectedNotificationSoundTitle.asStateFlow()

    private val _selectedNotificationSoundUri = MutableStateFlow(notificationPrefs.soundUri)
    val selectedNotificationSoundUri: StateFlow<String> = _selectedNotificationSoundUri.asStateFlow()

    private val _availableNotificationSounds = MutableStateFlow<List<NotificationSoundItem>>(emptyList())
    val availableNotificationSounds: StateFlow<List<NotificationSoundItem>> = _availableNotificationSounds.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = TokoRepository(
            db.categoryDao(),
            db.productDao(),
            db.transactionDao(),
            db.expenseDao(),
            db.storeSettingsDao(),
            db.excludedPromoProductDao()
        )
        cloudSyncManager = CloudSyncManager(
            application,
            db.productDao(),
            db.categoryDao(),
            db.transactionDao(),
            db.expenseDao(),
            db.storeSettingsDao()
        )

        // Observe logged-in Google user and connect real-time sync
        viewModelScope.launch {
            authManager.currentUser.collectLatest { user ->
                if (user != null) {
                    cloudSyncManager.onUserLoggedIn(user)
                } else {
                    cloudSyncManager.onUserLoggedOut()
                }
            }
        }

        // Automatically cleanup duplicate items and sync HPP for any existing transactions on startup
        viewModelScope.launch {
            repository.cleanupDuplicateTransactionItems()
            repository.syncTransactionsHpp()
        }

        // Muat daftar suara notifikasi internal ponsel
        viewModelScope.launch(Dispatchers.IO) {
            val sounds = NotificationHelper.getDeviceNotificationSounds(application)
            _availableNotificationSounds.value = sounds
        }

        // Sembunyikan gambar dan file media aplikasi dari Galeri ponsel
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.NoMediaHelper.hideAppImagesFromGallery(application)
        }

        // Sinkronkan presence perangkat berdasarkan peran saat ini
        viewModelScope.launch {
            authRoleManager.currentUserRole.collectLatest { role ->
                syncCurrentDevicePresence(role)
            }
        }
    }

    // Settings
    val storeSettings: StateFlow<StoreSettingsEntity> = repository.storeSettings
        .map { it ?: StoreSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreSettingsEntity()
        )

    // Categories
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Transactions
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expenses
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Excluded Promo Products (Produk promo yang dikecualikan dari 10 produk terlaris)
    val excludedPromoProducts: StateFlow<List<ExcludedPromoProductEntity>> = repository.excludedPromoProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExcludedPromoProduct(product: ProductEntity, reason: String = "Sedang Promo Toko") {
        viewModelScope.launch {
            repository.addExcludedPromoProduct(product.id, product.name, reason)
            emitMessage("Produk '${product.name}' ditambahkan ke daftar pengecualian promo.")
        }
    }

    fun removeExcludedPromoProduct(productId: Long) {
        viewModelScope.launch {
            repository.removeExcludedPromoProduct(productId)
            emitMessage("Produk dihapus dari daftar pengecualian promo.")
        }
    }

    // -------------------------------------------------------------
    // KATALOG PRODUK TERLARIS DALAM SATU MINGGU (7 HARI TERAKHIR)
    // -------------------------------------------------------------
    private val appPrefs: android.content.SharedPreferences =
        application.getSharedPreferences("toko_app_prefs", Context.MODE_PRIVATE)

    private val _topSellingLimit = MutableStateFlow(appPrefs.getInt("top_selling_limit", 10))
    val topSellingLimit: StateFlow<Int> = _topSellingLimit.asStateFlow()

    fun setTopSellingLimit(limit: Int) {
        val safeLimit = limit.coerceIn(1, 100)
        _topSellingLimit.value = safeLimit
        appPrefs.edit().putInt("top_selling_limit", safeLimit).apply()
        emitMessage("Katalog menampilkan $safeLimit produk terlaris dalam 1 minggu.")
    }

    val topSellingProductsInOneWeek: StateFlow<List<TopSellingProduct>> = combine(
        allTransactions,
        repository.allTransactionItems,
        allProducts,
        excludedPromoProducts,
        _topSellingLimit
    ) { transactions, allItems, products, excludedList, limit ->
        val oneWeekAgo = DateFormatter.getStartOfLast7Days()
        val weekTransactions = transactions.filter { it.timestamp >= oneWeekAgo }
        if (weekTransactions.isEmpty() || allItems.isEmpty()) {
            return@combine emptyList<TopSellingProduct>()
        }

        val weekTxMap = weekTransactions.associateBy { it.id }
        val excludedProductIds = excludedList.map { it.productId }.toSet()
        val excludedNames = excludedList.map { it.productName.trim().lowercase() }.toSet()

        // Ambil item transaksi 7 hari terakhir yang dikelompokkan per transaksi untuk dideduplikasi
        val itemsByTx = allItems.filter { it.transactionId in weekTxMap }.groupBy { it.transactionId }

        val deduplicatedItems = mutableListOf<TransactionItemEntity>()
        for ((txId, items) in itemsByTx) {
            val tx = weekTxMap[txId]
            val cleanItems = TransactionItemHelper.deduplicateItems(items, tx?.totalAmount ?: 0.0)
            deduplicatedItems.addAll(cleanItems)
        }

        val productMap = products.associateBy { it.id }
        val groupedByProduct = deduplicatedItems.groupBy { item ->
            if (item.productId > 0L) "id_${item.productId}" else "name_${item.productName.trim().lowercase()}"
        }

        val summaryList = mutableListOf<TopSellingProduct>()
        for ((_, items) in groupedByProduct) {
            val first = items.first()
            val prod = productMap[first.productId] ?: products.firstOrNull {
                it.name.equals(first.productName, ignoreCase = true) ||
                (first.qrCode.isNotBlank() && it.qrCode.equals(first.qrCode, ignoreCase = true))
            }

            val pId = prod?.id ?: first.productId
            val pName = prod?.name ?: first.productName

            // Pengecualian promo toko: produk promo TIDAK termasuk terlaris
            if (pId in excludedProductIds || pName.trim().lowercase() in excludedNames) {
                continue
            }

            val totalSold = items.sumOf { it.quantity }
            val totalRevenue = items.sumOf { it.subtotal }

            if (totalSold > 0) {
                summaryList.add(
                    TopSellingProduct(
                        rank = 0,
                        productId = pId,
                        productName = pName,
                        qrCode = prod?.qrCode ?: first.qrCode,
                        categoryName = prod?.categoryName ?: "Umum",
                        totalSoldQuantity = totalSold,
                        totalRevenue = totalRevenue,
                        currentStock = prod?.stok ?: 0,
                        currentPrice = prod?.hargaJual ?: first.unitPrice,
                        product = prod
                    )
                )
            }
        }

        summaryList.sortedWith(
            compareByDescending<TopSellingProduct> { it.totalSoldQuantity }
                .thenByDescending { it.totalRevenue }
        ).take(limit).mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Events & Toasts
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // ONLINE REAL-TIME SYNC & GOOGLE ACCOUNT
    // -------------------------------------------------------------
    val currentGoogleUser: StateFlow<GoogleUser?> = authManager.currentUser
    val syncStatus: StateFlow<SyncStatus> = cloudSyncManager.syncStatus
    val lastSyncTime: StateFlow<Long> = cloudSyncManager.lastSyncTime
    val syncMessage: StateFlow<String> = cloudSyncManager.syncMessage
    val isRealtimeSyncEnabled: StateFlow<Boolean> = cloudSyncManager.isRealtimeSyncEnabled
    val customDatabaseUrl: StateFlow<String> = cloudSyncManager.customDatabaseUrl
    val customStoreCode: StateFlow<String> = cloudSyncManager.customStoreCode

    private val _showGoogleSyncDialog = MutableStateFlow(false)
    val showGoogleSyncDialog: StateFlow<Boolean> = _showGoogleSyncDialog.asStateFlow()

    fun openGoogleSyncDialog() {
        _showGoogleSyncDialog.value = true
    }

    fun dismissGoogleSyncDialog() {
        _showGoogleSyncDialog.value = false
    }

    fun updateCustomDatabaseUrl(url: String) {
        cloudSyncManager.setCustomDatabaseUrl(url)
        emitMessage("URL Firebase Realtime Database disimpan.")
    }

    fun updateCustomStoreCode(code: String) {
        cloudSyncManager.setCustomStoreCode(code)
        emitMessage("Kode Toko diperbarui: $code")
    }

    fun setRealtimeSyncEnabled(enabled: Boolean) {
        cloudSyncManager.setRealtimeSyncEnabled(enabled)
        emitMessage(if (enabled) "Sinkronisasi real-time multi-ponsel diaktifkan." else "Sinkronisasi real-time dinonaktifkan.")
    }

    fun signInWithGoogle(webClientId: String = "") {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(webClientId)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                emitMessage("✓ Berhasil masuk sebagai ${user.displayName}")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Gagal masuk dengan akun Google"
                emitMessage(err)
            }
        }
    }

    fun signInWithEmailDirect(email: String, name: String? = null) {
        val result = authManager.signInWithEmailDirect(email, name)
        if (result.isSuccess) {
            val user = result.getOrNull()!!
            emitMessage("✓ Akun ${user.email} berhasil dihubungkan ke sinkronisasi online.")
        } else {
            emitMessage(result.exceptionOrNull()?.message ?: "Gagal menghubungkan email.")
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            authManager.signOut()
            emitMessage("Akun Google telah keluar.")
        }
    }

    fun syncAllLocalToCloud() {
        viewModelScope.launch {
            val result = cloudSyncManager.syncAllLocalToCloud()
            if (result.isSuccess) {
                emitMessage(result.getOrNull() ?: "Sinkronisasi berhasil!")
            } else {
                emitMessage(result.exceptionOrNull()?.message ?: "Gagal menyinkronkan data ke cloud.")
            }
        }
    }

    // -------------------------------------------------------------
    // CASHIER & CART STATE
    // -------------------------------------------------------------
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Double> = _cart.map { cartList ->
        cartList.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val cartTotalCost: StateFlow<Double> = _cart.map { cartList ->
        cartList.sumOf { it.subtotalCost }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Fast QR Input
    private val _qrInputText = MutableStateFlow("")
    val qrInputText: StateFlow<String> = _qrInputText.asStateFlow()

    private val _qrClearTrigger = MutableStateFlow(0L)
    val qrClearTrigger: StateFlow<Long> = _qrClearTrigger.asStateFlow()

    fun clearQrInput() {
        _qrInputText.value = ""
        _qrClearTrigger.value = System.currentTimeMillis()
    }

    fun updateQrInputText(text: String) {
        _qrInputText.value = text
        // Fast instant barcode detection
        if (text.isNotBlank()) {
            val success = checkAndAutoAddQr(text.trim())
            if (success) {
                clearQrInput()
            }
        }
    }

    /**
     * Checks if the scanned/typed QR matches any product in stock.
     * If matched, automatically adds to cart and clears input text!
     */
    fun checkAndAutoAddQr(code: String): Boolean {
        if (code.isBlank()) return false
        val cleanCode = code.trim()
        val products = allProducts.value
        val match = products.firstOrNull { it.qrCode.equals(cleanCode, ignoreCase = true) }
        if (match != null) {
            if (match.stok <= 0) {
                emitMessage("Stok '${match.name}' habis!")
                clearQrInput()
                return false
            }
            addToCart(match)
            emitMessage("✓ '${match.name}' ditambahkan")
            clearQrInput() // Auto-clear so cashier can immediately scan the next product
            return true
        }
        return false
    }

    fun submitQrInputManual(rawCode: String? = null) {
        val code = (rawCode ?: _qrInputText.value).trim()
        if (code.isBlank()) return
        val success = checkAndAutoAddQr(code)
        if (!success) {
            emitMessage("Produk dengan kode '$code' tidak ditemukan!")
            clearQrInput()
        }
    }

    fun addToCart(product: ProductEntity) {
        val currentCart = _cart.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst {
            it.product.id == product.id ||
            (it.product.qrCode.isNotBlank() && it.product.qrCode.equals(product.qrCode, ignoreCase = true))
        }
        if (existingIndex >= 0) {
            val existing = currentCart[existingIndex]
            if (existing.quantity >= product.stok) {
                emitMessage("Jumlah melebihi stok yang tersedia (${product.stok})")
                return
            }
            currentCart[existingIndex] = existing.copy(quantity = existing.quantity + 1)
        } else {
            if (product.stok <= 0) {
                emitMessage("Stok '${product.name}' habis!")
                return
            }
            currentCart.add(CartItem(product = product, quantity = 1))
        }
        _cart.value = currentCart
    }

    fun updateCartQuantity(productId: Long, newQuantity: Int) {
        val currentCart = _cart.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else {
                val item = currentCart[index]
                if (newQuantity > item.product.stok) {
                    emitMessage("Maksimal stok tersedia: ${item.product.stok}")
                    currentCart[index] = item.copy(quantity = item.product.stok)
                } else {
                    currentCart[index] = item.copy(quantity = newQuantity)
                }
            }
            _cart.value = currentCart
        }
    }

    fun removeFromCart(productId: Long) {
        val currentCart = _cart.value.toMutableList()
        currentCart.removeAll { it.product.id == productId }
        _cart.value = currentCart
        emitMessage("Pesanan produk telah dihapus dari keranjang")
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // -------------------------------------------------------------
    // PAYMENT & CHECKOUT STATE
    // -------------------------------------------------------------
    private val _paymentType = MutableStateFlow("TUNAI") // "TUNAI" or "NON_TUNAI"
    val paymentType: StateFlow<String> = _paymentType.asStateFlow()

    private val _paidAmountText = MutableStateFlow("")
    val paidAmountText: StateFlow<String> = _paidAmountText.asStateFlow()

    fun setPaymentType(type: String) {
        _paymentType.value = type
        if (type == "NON_TUNAI") {
            // Non tunai is exactly equal to the total bill
            val total = cartTotal.value
            _paidAmountText.value = CurrencyFormatter.formatThousand(total.toLong())
        }
    }

    fun updatePaidAmountText(text: String) {
        _paidAmountText.value = CurrencyFormatter.formatInputNominal(text, _paidAmountText.value)
    }

    /**
     * Fast nominal buttons:
     * Clicking 10 sets 10.000, 20 sets 20.000, 30 sets 30.000, 50 sets 50.000, 100 sets 100.000
     * As specified: "Uang Pas 10 20 30 50 100 dimana nominal tersebut ketika di klik akan di tambahkan 000"
     */
    fun onQuickNominalClick(nominalCode: String) {
        if (nominalCode.equals("PAS", ignoreCase = true)) {
            val total = cartTotal.value
            _paidAmountText.value = CurrencyFormatter.formatThousand(total.toLong())
        } else {
            // e.g. "10" -> "10.000", "20" -> "20.000", etc.
            val digits = nominalCode.filter { it.isDigit() }
            if (digits.isNotEmpty()) {
                val fullAmount = "${digits}000"
                val num = fullAmount.toLongOrNull() ?: 0L
                _paidAmountText.value = CurrencyFormatter.formatThousand(num)
            }
        }
    }

    // Receipt Dialog state after transaction
    private val _showReceiptDialog = MutableStateFlow(false)
    val showReceiptDialog: StateFlow<Boolean> = _showReceiptDialog.asStateFlow()

    private val _currentReceiptTransaction = MutableStateFlow<TransactionEntity?>(null)
    val currentReceiptTransaction: StateFlow<TransactionEntity?> = _currentReceiptTransaction.asStateFlow()

    private val _currentReceiptItems = MutableStateFlow<List<TransactionItemEntity>>(emptyList())
    val currentReceiptItems: StateFlow<List<TransactionItemEntity>> = _currentReceiptItems.asStateFlow()

    fun dismissReceiptDialog() {
        _showReceiptDialog.value = false
        _currentReceiptTransaction.value = null
        _currentReceiptItems.value = emptyList()
    }

    fun showReceiptForTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val items = repository.getItemsForTransactionSync(transaction.id)
            val updatedTx = repository.getTransactionById(transaction.id) ?: transaction
            _currentReceiptTransaction.value = updatedTx
            _currentReceiptItems.value = items
            _showReceiptDialog.value = true
        }
    }

    fun completeCheckout(notes: String = "") {
        val cartList = _cart.value
        if (cartList.isEmpty()) {
            emitMessage("Keranjang belanja masih kosong!")
            return
        }

        val total = cartList.sumOf { it.subtotal }
        val totalCost = cartList.sumOf { it.subtotalCost }
        val isNonTunai = _paymentType.value == "NON_TUNAI"

        val paid = if (isNonTunai) {
            total
        } else {
            CurrencyFormatter.parseAmount(_paidAmountText.value)
        }

        if (!isNonTunai && paid < total) {
            emitMessage("Uang pembayaran kurang ${CurrencyFormatter.formatRupiah(total - paid)}")
            return
        }

        val change = if (isNonTunai) 0.0 else (paid - total).coerceAtLeast(0.0)

        viewModelScope.launch {
            val invoiceNo = "TM-${SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())}"
            val tx = TransactionEntity(
                invoiceNumber = invoiceNo,
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                totalCost = totalCost,
                paymentType = _paymentType.value,
                paidAmount = paid,
                changeAmount = change,
                notes = notes
            )

            val txItems = cartList.map { cartItem ->
                TransactionItemEntity(
                    transactionId = 0, // will be assigned by repository
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    qrCode = cartItem.product.qrCode,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.product.hargaJual,
                    unitCost = cartItem.product.hargaBeli,
                    subtotal = cartItem.subtotal
                )
            }

            val txId = repository.processSale(tx, txItems)
            val savedTx = repository.getTransactionById(txId) ?: tx.copy(id = txId)
            val savedItems = repository.getItemsForTransactionSync(txId)

            // Push to cloud in real time for other phones
            cloudSyncManager.pushTransaction(savedTx, savedItems)
            for (cartItem in cartList) {
                repository.getProductById(cartItem.product.id)?.let { cloudSyncManager.pushProduct(it) }
            }

            _cart.value = emptyList()
            _paidAmountText.value = ""
            _paymentType.value = "TUNAI"

            // Show receipt immediately
            _currentReceiptTransaction.value = savedTx
            _currentReceiptItems.value = savedItems
            _showReceiptDialog.value = true

            // Periksa stok terbaru setelah transaksi berhasil dikurangi
            checkAndNotifyLowStock(force = true)

            emitMessage("Transaksi berhasil disimpan!")
        }
    }

    // -------------------------------------------------------------
    // PRODUCT & CATALOG MANAGEMENT
    // -------------------------------------------------------------
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedProductCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedProductCategoryFilter: StateFlow<Long?> = _selectedProductCategoryFilter.asStateFlow()

    private val _lowStockFilterActive = MutableStateFlow(false)
    val lowStockFilterActive: StateFlow<Boolean> = _lowStockFilterActive.asStateFlow()

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setSelectedCategoryFilter(catId: Long?) {
        _selectedProductCategoryFilter.value = catId
    }

    fun toggleLowStockFilter() {
        _lowStockFilterActive.value = !_lowStockFilterActive.value
    }

    fun setLowStockFilter(active: Boolean) {
        _lowStockFilterActive.value = active
    }

    // Filtered Products
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _productSearchQuery,
        _selectedProductCategoryFilter,
        _lowStockFilterActive
    ) { products, query, catFilter, lowStockOnly ->
        products.filter { prod ->
            val matchQuery = query.isBlank() ||
                    prod.name.contains(query, ignoreCase = true) ||
                    prod.qrCode.contains(query, ignoreCase = true) ||
                    prod.categoryName.contains(query, ignoreCase = true)

            val matchCategory = catFilter == null || prod.categoryId == catFilter
            val matchLowStock = !lowStockOnly || prod.stok <= prod.minimumStokAlert

            matchQuery && matchCategory && matchLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product valuation based on buy price: Sum(stok * hargaBeli)
    val totalInventoryValuation: StateFlow<Double> = allProducts.combine(allProducts) { prods, _ ->
        prods.sumOf { it.stok * it.hargaBeli }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lowStockProductCount: StateFlow<Int> = allProducts.combine(allProducts) { prods, _ ->
        prods.count { it.stok <= it.minimumStokAlert }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Pantau perubahan data stok produk terbaru dan beri notifikasi jika ada stok menipis
        viewModelScope.launch {
            allProducts.collectLatest { products ->
                if (products.isNotEmpty()) {
                    checkAndNotifyLowStock(products = products, force = false)
                }
            }
        }
    }

    fun addProduct(
        name: String,
        categoryId: Long,
        categoryName: String,
        qrCode: String,
        hargaBeli: Double,
        hargaJual: Double,
        stok: Int,
        minimumStokAlert: Int
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                name = name.trim(),
                categoryId = categoryId,
                categoryName = categoryName,
                qrCode = qrCode.trim(),
                hargaBeli = hargaBeli,
                hargaJual = hargaJual,
                stok = stok,
                minimumStokAlert = minimumStokAlert
            )
            val newId = repository.addProduct(product)
            cloudSyncManager.pushProduct(product.copy(id = newId))
            emitMessage("Produk '${product.name}' berhasil ditambahkan")
            checkAndNotifyLowStock(force = true)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            cloudSyncManager.pushProduct(product)
            emitMessage("Produk '${product.name}' berhasil diperbarui")
            checkAndNotifyLowStock(force = true)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            cloudSyncManager.pushDeleteProduct(product.id)
            emitMessage("Produk '${product.name}' dihapus")
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repository.addCategory(name.trim())
            cloudSyncManager.pushCategory(CategoryEntity(id = newId, name = name.trim()))
            emitMessage("Katalog '$name' berhasil ditambahkan")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val result = repository.deleteCategory(category)
            if (result.isSuccess) {
                cloudSyncManager.pushDeleteCategory(category.id)
                emitMessage("Katalog '${category.name}' berhasil dihapus")
            } else {
                emitMessage(result.exceptionOrNull()?.message ?: "Gagal menghapus katalog")
            }
        }
    }

    // -------------------------------------------------------------
    // EXPENSES & CAPITAL (MODAL & PENGELUARAN)
    // -------------------------------------------------------------
    fun addExpense(title: String, category: String, amount: Double, notes: String = "") {
        viewModelScope.launch {
            val exp = ExpenseEntity(
                title = title.trim(),
                category = category.trim(),
                amount = amount,
                timestamp = System.currentTimeMillis(),
                notes = notes.trim()
            )
            val newId = repository.addExpense(exp)
            cloudSyncManager.pushExpense(exp.copy(id = newId))
            emitMessage("Pengeluaran sebesar ${CurrencyFormatter.formatRupiah(amount)} disimpan")
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            cloudSyncManager.pushExpense(expense)
            emitMessage("Pengeluaran diperbarui")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            cloudSyncManager.pushDeleteExpense(expense.id)
            emitMessage("Pengeluaran dihapus")
        }
    }

    fun updateInitialCashCapital(amount: Double) {
        viewModelScope.launch {
            val current = storeSettings.value
            val updated = current.copy(initialCashCapital = amount)
            repository.updateStoreSettings(updated)
            cloudSyncManager.pushSettings(updated)
            emitMessage("Modal kas awal toko diperbarui: ${CurrencyFormatter.formatRupiah(amount)}")
        }
    }

    // -------------------------------------------------------------
    // FINANCIAL REPORTS & RECONCILIATION
    // -------------------------------------------------------------
    private val _selectedPeriod = MutableStateFlow(PeriodFilter.HARI_INI)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    fun setSelectedPeriod(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    val periodTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedPeriod
    ) { transactions, period ->
        val startTime = when (period) {
            PeriodFilter.HARI_INI -> DateFormatter.getStartOfToday()
            PeriodFilter.MINGGU_INI -> DateFormatter.getStartOfLast7Days()
            PeriodFilter.BULAN_INI -> DateFormatter.getStartOfMonth()
            PeriodFilter.SEMUA -> 0L
        }
        transactions.filter { it.timestamp >= startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periodExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        _selectedPeriod
    ) { expenses, period ->
        val startTime = when (period) {
            PeriodFilter.HARI_INI -> DateFormatter.getStartOfToday()
            PeriodFilter.MINGGU_INI -> DateFormatter.getStartOfLast7Days()
            PeriodFilter.BULAN_INI -> DateFormatter.getStartOfMonth()
            PeriodFilter.SEMUA -> 0L
        }
        expenses.filter { it.timestamp >= startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Rincian laporan harian 7 hari ke belakang (Hari Ini, Kemarin, hingga 6 hari lalu).
     * Menyajikan analisis per hari secara mendalam dan transparan.
     */
    val weeklyDailyBreakdown: StateFlow<List<DailyReportSummary>> = combine(
        allTransactions,
        allExpenses
    ) { transactions, expenses ->
        val result = mutableListOf<DailyReportSummary>()
        val dayNameFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

        for (offset in 0..6) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = dayCal.timeInMillis

            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            dayCal.set(Calendar.MILLISECOND, 999)
            val endOfDay = dayCal.timeInMillis

            val dayTransactions = transactions.filter { it.timestamp in startOfDay..endOfDay }
            val dayExpenses = expenses.filter { it.timestamp in startOfDay..endOfDay }

            val rawDayName = dayNameFormat.format(Date(startOfDay))
            val title = when (offset) {
                0 -> "Hari Ini ($rawDayName)"
                1 -> "Kemarin ($rawDayName)"
                else -> rawDayName
            }
            val dateStr = dateFormat.format(Date(startOfDay))

            val omzet = dayTransactions.sumOf { it.totalAmount }
            val hpp = dayTransactions.sumOf { it.totalCost }
            val labaKotor = omzet - hpp
            val pengeluaran = dayExpenses.sumOf { it.amount }
            val labaBersih = labaKotor - pengeluaran
            val tunai = dayTransactions.filter { it.paymentType == "TUNAI" }.sumOf { it.totalAmount }
            val nonTunai = dayTransactions.filter { it.paymentType == "NON_TUNAI" }.sumOf { it.totalAmount }

            result.add(
                DailyReportSummary(
                    dayOffset = offset,
                    startTimestamp = startOfDay,
                    endTimestamp = endOfDay,
                    dayTitle = title,
                    formattedDate = dateStr,
                    transactions = dayTransactions,
                    expenses = dayExpenses,
                    totalOmzet = omzet,
                    totalHpp = hpp,
                    labaKotor = labaKotor,
                    totalPengeluaran = pengeluaran,
                    labaBersih = labaBersih,
                    penjualanTunai = tunai,
                    penjualanNonTunai = nonTunai,
                    transactionCount = dayTransactions.size
                )
            )
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Rincian laporan keuangan per Minggu pada Bulan Ini.
     * Mengelompokkan transaksi dan pengeluaran bulan ini ke dalam setiap minggunya
     * (Minggu 1: tgl 1-7, Minggu 2: tgl 8-14, Minggu 3: tgl 15-21, Minggu 4: tgl 22-28, Minggu 5: tgl 29-akhir bulan).
     */
    val monthlyWeeklyBreakdown: StateFlow<List<WeeklyReportSummary>> = combine(
        allTransactions,
        allExpenses
    ) { transactions, expenses ->
        val result = mutableListOf<WeeklyReportSummary>()
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

        // Hitung jumlah hari maksimal dalam bulan berjalan
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthNameFormat = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
        val monthYearStr = monthNameFormat.format(cal.time)

        var startDay = 1
        var weekNum = 1

        while (startDay <= maxDays) {
            val endDay = minOf(startDay + 6, maxDays)

            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, startDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTimestamp = startCal.timeInMillis

            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, endDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endTimestamp = endCal.timeInMillis

            val isCurrentWeek = todayDayOfMonth in startDay..endDay
            val weekTransactions = transactions.filter { it.timestamp in startTimestamp..endTimestamp }
            val weekExpenses = expenses.filter { it.timestamp in startTimestamp..endTimestamp }

            val omzet = weekTransactions.sumOf { it.totalAmount }
            val hpp = weekTransactions.sumOf { it.totalCost }
            val labaKotor = omzet - hpp
            val pengeluaran = weekExpenses.sumOf { it.amount }
            val labaBersih = labaKotor - pengeluaran
            val tunai = weekTransactions.filter { it.paymentType == "TUNAI" }.sumOf { it.totalAmount }
            val nonTunai = weekTransactions.filter { it.paymentType == "NON_TUNAI" }.sumOf { it.totalAmount }

            val dateRangeStr = String.format(Locale("id", "ID"), "%02d - %02d %s", startDay, endDay, monthYearStr)

            result.add(
                WeeklyReportSummary(
                    weekNumber = weekNum,
                    weekTitle = "Minggu $weekNum",
                    dateRangeLabel = dateRangeStr,
                    startTimestamp = startTimestamp,
                    endTimestamp = endTimestamp,
                    isCurrentWeek = isCurrentWeek,
                    transactions = weekTransactions,
                    expenses = weekExpenses,
                    totalOmzet = omzet,
                    totalHpp = hpp,
                    labaKotor = labaKotor,
                    totalPengeluaran = pengeluaran,
                    labaBersih = labaBersih,
                    penjualanTunai = tunai,
                    penjualanNonTunai = nonTunai,
                    transactionCount = weekTransactions.size
                )
            )

            startDay = endDay + 1
            weekNum++
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncHppManually() {
        viewModelScope.launch {
            val cleaned = repository.cleanupDuplicateTransactionItems()
            val count = repository.syncTransactionsHpp()
            if (cleaned > 0 || count > 0) {
                emitMessage("Berhasil merapikan struk & menyinkronkan HPP untuk ${maxOf(cleaned, count)} transaksi!")
            } else {
                emitMessage("Seluruh struk transaksi dan HPP sudah sinkron dan akurat")
            }
        }
    }

    // -------------------------------------------------------------
    // STORE & RECEIPT SETTINGS
    // -------------------------------------------------------------
    fun updateStoreName(newName: String) {
        val trimmed = newName.trim().ifBlank { "TOKO SUBUR" }
        viewModelScope.launch {
            val current = storeSettings.value
            val updated = current.copy(storeName = trimmed)
            repository.updateStoreSettings(updated)
            emitMessage("Nama toko berhasil diubah menjadi: $trimmed")
        }
    }

    fun updateReceiptSettings(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        receiptFooter: String
    ) {
        viewModelScope.launch {
            val current = storeSettings.value
            val cleanName = storeName.trim().ifBlank { "TOKO SUBUR" }
            val updated = current.copy(
                storeName = cleanName,
                storeAddress = storeAddress.trim(),
                storePhone = storePhone.trim(),
                receiptFooter = receiptFooter.trim()
            )
            repository.updateStoreSettings(updated)
            cloudSyncManager.pushSettings(updated)
            emitMessage("Nama toko dan pengaturan struk berhasil disimpan!")
        }
    }

    fun enforceHideImagesFromGallery() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = com.example.util.NoMediaHelper.hideAppImagesFromGallery(getApplication())
            if (success) {
                emitMessage("Gambar aplikasi berhasil disembunyikan dari galeri ponsel!")
            } else {
                emitMessage("Perlindungan .nomedia telah diperbarui di folder penyimpanan.")
            }
        }
    }

    fun addQuickNominal(nominal: String) {
        val clean = nominal.filter { it.isDigit() }
        if (clean.isBlank()) return
        viewModelScope.launch {
            val current = storeSettings.value
            val existing = current.quickNominals.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!existing.contains(clean)) {
                existing.add(clean)
                val sorted = existing.sortedBy { it.toLongOrNull() ?: 0L }
                val updated = current.copy(quickNominals = sorted.joinToString(","))
                repository.updateStoreSettings(updated)
                emitMessage("Nominal '$clean' (${clean}.000) ditambahkan")
            } else {
                emitMessage("Nominal '$clean' sudah ada")
            }
        }
    }

    fun removeQuickNominal(nominal: String) {
        viewModelScope.launch {
            val current = storeSettings.value
            val existing = current.quickNominals.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (existing.remove(nominal)) {
                val updated = current.copy(quickNominals = existing.joinToString(","))
                repository.updateStoreSettings(updated)
                emitMessage("Nominal '$nominal' dihapus")
            }
        }
    }

    // -------------------------------------------------------------
    // BACKUP & RESTORE (.JSON TO LOCAL PHONE STORAGE)
    // -------------------------------------------------------------
    fun exportBackupToJsonUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val settings = storeSettings.value
                val categories = repository.getAllCategoriesSync()
                val products = repository.getAllProductsSync()
                val transactions = repository.getAllTransactionsSync()
                val transactionItems = repository.getItemsForTransactionSync(0) // or all items
                val allTxItems = mutableListOf<TransactionItemEntity>()
                for (tx in transactions) {
                    allTxItems.addAll(repository.getItemsForTransactionSync(tx.id))
                }
                val expenses = repository.getAllExpensesSync()
                val excludedPromoProducts = repository.getExcludedPromoProductsSync()

                val jsonContent = BackupManager.exportToJson(
                    settings = settings,
                    categories = categories,
                    products = products,
                    transactions = transactions,
                    transactionItems = allTxItems,
                    expenses = expenses,
                    excludedPromoProducts = excludedPromoProducts
                )

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonContent)
                        writer.flush()
                    }
                }
                emitMessage("✓ Berhasil mencadangkan data ke file JSON!")
            } catch (e: Exception) {
                emitMessage("Gagal mencadangkan data: ${e.localizedMessage}")
            }
        }
    }

    fun exportAppApkToUri(context: Context, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceApkPath = context.applicationInfo.sourceDir
                val sourceFile = java.io.File(sourceApkPath)
                if (sourceFile.exists()) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    emitMessage("✓ Berkas TokoSubur.apk berhasil disimpan ke penyimpanan ponsel!")
                } else {
                    emitMessage("Gagal menemukan berkas APK aplikasi di sistem.")
                }
            } catch (e: Exception) {
                emitMessage("Gagal mengekspor APK: ${e.localizedMessage}")
            }
        }
    }

    fun importBackupFromJsonUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }

                val backupData = BackupManager.parseJson(stringBuilder.toString())

                repository.restoreDatabase(
                    settings = backupData.storeSettings,
                    categories = backupData.categories,
                    products = backupData.products,
                    transactions = backupData.transactions,
                    transactionItems = backupData.transactionItems,
                    expenses = backupData.expenses,
                    excludedPromoProducts = backupData.excludedPromoProducts
                )

                emitMessage("✓ Berhasil memulihkan ${backupData.products.size} produk dan data transaksi!")
            } catch (e: Exception) {
                emitMessage("Gagal memulihkan data: Format file tidak sesuai!")
            }
        }
    }

    // -------------------------------------------------------------
    // NOTIFIKASI SUARA STOK MENIPIS & PENYIMPANAN INTERNAL SUARA
    // -------------------------------------------------------------
    fun setNotificationEnabled(enabled: Boolean) {
        notificationPrefs.isNotificationEnabled = enabled
        _isNotificationEnabled.value = enabled
        if (!enabled) {
            NotificationHelper.cancelLowStockNotification(getApplication())
            emitMessage("Notifikasi stok menipis dinonaktifkan")
        } else {
            notificationPrefs.resetSignature()
            checkAndNotifyLowStock(force = true)
            emitMessage("Notifikasi stok menipis diaktifkan")
        }
    }

    fun setSelectedNotificationSound(title: String, uriString: String) {
        notificationPrefs.soundTitle = title
        notificationPrefs.soundUri = uriString
        _selectedNotificationSoundTitle.value = title
        _selectedNotificationSoundUri.value = uriString
        notificationPrefs.resetSignature()
        emitMessage("Suara notifikasi dipilih: $title")
    }

    fun refreshDeviceNotificationSounds() {
        viewModelScope.launch(Dispatchers.IO) {
            val sounds = NotificationHelper.getDeviceNotificationSounds(getApplication())
            _availableNotificationSounds.value = sounds
        }
    }

    fun playPreviewSound(uriString: String) {
        NotificationHelper.playPreviewSound(getApplication(), uriString)
    }

    fun stopPreviewSound() {
        NotificationHelper.stopPreviewSound()
    }

    fun triggerManualLowStockCheck() {
        checkAndNotifyLowStock(force = true)
        val lowStockItems = allProducts.value.filter { it.stok <= it.minimumStokAlert }
        if (lowStockItems.isNotEmpty()) {
            emitMessage("Notifikasi suara dikirim untuk ${lowStockItems.size} produk dengan stok menipis")
        } else {
            emitMessage("Semua produk masih memiliki stok aman!")
        }
    }

    fun checkAndNotifyLowStock(products: List<ProductEntity> = allProducts.value, force: Boolean = false) {
        if (!notificationPrefs.isNotificationEnabled) return

        val lowStockItems = products.filter { it.stok <= it.minimumStokAlert }
        if (lowStockItems.isEmpty()) {
            NotificationHelper.cancelLowStockNotification(getApplication())
            return
        }

        // Tanda tangan unik berdasarkan ID dan sisa stok untuk menghindari spam berulang tanpa perubahan
        val currentSignature = lowStockItems.sortedBy { it.id }.joinToString(";") { "${it.id}:${it.stok}" }
        if (!force && currentSignature == notificationPrefs.lastNotifiedSignature) {
            return
        }

        notificationPrefs.lastNotifiedSignature = currentSignature
        NotificationHelper.showLowStockNotification(
            context = getApplication(),
            lowStockProducts = lowStockItems,
            soundUriString = notificationPrefs.soundUri
        )
    }

    fun testSoundNotification() {
        val currentLowStock = allProducts.value.filter { it.stok <= it.minimumStokAlert }
        val testItems = if (currentLowStock.isNotEmpty()) {
            currentLowStock
        } else {
            listOf(
                ProductEntity(
                    name = "Contoh Produk Menipis",
                    categoryId = 1,
                    categoryName = "Sembako",
                    qrCode = "0000",
                    hargaBeli = 10000.0,
                    hargaJual = 12000.0,
                    stok = 2,
                    minimumStokAlert = 5
                )
            )
        }

        NotificationHelper.showLowStockNotification(
            context = getApplication(),
            lowStockProducts = testItems,
            soundUriString = notificationPrefs.soundUri
        )
        emitMessage("🔔 Notifikasi suara berhasil dikirim!")
    }

    override fun onCleared() {
        super.onCleared()
        NotificationHelper.stopPreviewSound()
    }

    // -------------------------------------------------------------
    // ROLE AUTH & PRESENCE MANAGEMENT
    // -------------------------------------------------------------
    fun syncCurrentDevicePresence(role: UserRole = authRoleManager.currentUserRole.value) {
        val info = CashierDeviceInfo(
            deviceId = authRoleManager.deviceId,
            deviceName = authRoleManager.deviceName,
            cashierNumber = authRoleManager.assignedCashierNumber.value,
            role = role.name,
            isOnline = true
        )
        cloudSyncManager.updateDevicePresence(info)
    }

    fun openLoginRoleDialog() {
        _showLoginRoleDialog.value = true
    }

    fun dismissLoginRoleDialog() {
        _showLoginRoleDialog.value = false
    }

    fun openActiveCashiersDialog() {
        _showActiveCashiersDialog.value = true
    }

    fun dismissActiveCashiersDialog() {
        _showActiveCashiersDialog.value = false
    }

    fun loginAsOwner(password: String): Result<Unit> {
        val result = authRoleManager.loginAsOwner(password)
        if (result.isSuccess) {
            syncCurrentDevicePresence(UserRole.PEMILIK)
            emitMessage("Berhasil masuk sebagai Akun Pemilik")
            dismissLoginRoleDialog()
        }
        return result
    }

    fun loginAsCashier(): Result<Unit> {
        val result = authRoleManager.loginAsCashier()
        if (result.isSuccess) {
            syncCurrentDevicePresence(UserRole.KASIR)
            emitMessage("Masuk sebagai Akun Kasir")
            dismissLoginRoleDialog()
        }
        return result
    }

    fun changeOwnerPassword(oldPass: String, newPass: String): Result<Unit> {
        val result = authRoleManager.changeOwnerPassword(oldPass, newPass)
        if (result.isSuccess) {
            emitMessage("Kata sandi pemilik berhasil diperbarui!")
        }
        return result
    }

    fun resetOwnerPasswordToDefault(): Result<Unit> {
        val result = authRoleManager.resetOwnerPasswordToDefault()
        if (result.isSuccess) {
            emitMessage("Kata sandi pemilik direset ke default (1234567890)")
        }
        return result
    }

    /**
     * Menghapus transaksi yang sudah berhasil (khusus akun Pemilik).
     * Jika restoreStock bernilai true, stok produk yang dibatalkan akan otomatis dikembalikan ke inventaris toko.
     */
    fun deleteTransaction(transaction: TransactionEntity, restoreStock: Boolean = true) {
        if (currentUserRole.value != UserRole.PEMILIK) {
            emitMessage("Akses Ditolak: Hanya Akun Pemilik yang dapat menghapus transaksi!")
            return
        }

        viewModelScope.launch {
            try {
                val restoredProducts = repository.deleteTransaction(transaction.id, restoreStock)
                for (p in restoredProducts) {
                    cloudSyncManager.pushProduct(p)
                }
                cloudSyncManager.pushDeleteTransaction(transaction.id)

                if (currentReceiptTransaction.value?.id == transaction.id) {
                    _showReceiptDialog.value = false
                    _currentReceiptTransaction.value = null
                }

                val stockMsg = if (restoreStock && restoredProducts.isNotEmpty()) {
                    " dan ${restoredProducts.size} stok produk dikembalikan ke inventaris"
                } else ""
                emitMessage("Transaksi ${transaction.invoiceNumber} berhasil dihapus$stockMsg.")
            } catch (e: Exception) {
                emitMessage("Gagal menghapus transaksi: ${e.message}")
            }
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }
}
