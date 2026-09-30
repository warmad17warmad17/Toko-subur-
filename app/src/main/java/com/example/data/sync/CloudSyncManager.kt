package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
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
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.data.auth.CashierDeviceInfo
import com.example.util.TransactionItemHelper

enum class SyncStatus {
    OFFLINE,
    SYNCING,
    SYNCED,
    ERROR
}

/**
 * CloudSyncManager handles bidirectional real-time synchronization between the local Room SQLite
 * database and Firebase Realtime Database across multiple devices (up to 10+ HP POS terminals).
 *
 * Changes made on any terminal (product edits, stock deductions upon checkout, new sales, expenses)
 * are instantly broadcasted via Realtime Database WebSockets and reflected reactively in Jetpack
 * Compose without requiring any manual page refresh.
 */
class CloudSyncManager(
    private val context: Context,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val expenseDao: ExpenseDao,
    private val storeSettingsDao: StoreSettingsDao
) {
    companion object {
        const val DEFAULT_DATABASE_URL = "https://toko-subur-50bde-default-rtdb.asia-southeast1.firebasedatabase.app"
        const val DEFAULT_PROJECT_ID = "toko-subur-50bde"
        const val DEFAULT_API_KEY = "AIzaSyB4ZQh7Era0DmniaAtyVaAqeddj0Jq98VM"
        const val DEFAULT_STORE_CODE = "toko_subur"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cloud_sync_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_sync_timestamp", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncMessage = MutableStateFlow("Menghubungkan ke Firebase Realtime Database...")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _isRealtimeSyncEnabled = MutableStateFlow(prefs.getBoolean("realtime_sync_enabled", true))
    val isRealtimeSyncEnabled: StateFlow<Boolean> = _isRealtimeSyncEnabled.asStateFlow()

    private val _customDatabaseUrl = MutableStateFlow(
        prefs.getString("custom_database_url", DEFAULT_DATABASE_URL) ?: DEFAULT_DATABASE_URL
    )
    val customDatabaseUrl: StateFlow<String> = _customDatabaseUrl.asStateFlow()

    private val _customStoreCode = MutableStateFlow(
        prefs.getString("custom_store_code", DEFAULT_STORE_CODE) ?: DEFAULT_STORE_CODE
    )
    val customStoreCode: StateFlow<String> = _customStoreCode.asStateFlow()

    private val _activeCashiers = MutableStateFlow<List<CashierDeviceInfo>>(emptyList())
    val activeCashiers: StateFlow<List<CashierDeviceInfo>> = _activeCashiers.asStateFlow()

    private val _activeCashierCount = MutableStateFlow(1)
    val activeCashierCount: StateFlow<Int> = _activeCashierCount.asStateFlow()

    private var currentDeviceInfo: CashierDeviceInfo? = null

    private var activeStoreId: String? = null
    private val activeListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    init {
        val savedStoreCode = prefs.getString("custom_store_code", DEFAULT_STORE_CODE) ?: DEFAULT_STORE_CODE
        val savedEmail = prefs.getString("user_email", "") ?: ""
        if (savedStoreCode.isNotBlank()) {
            activeStoreId = "store_${sanitizeKey(savedStoreCode)}"
        } else if (savedEmail.isNotBlank()) {
            activeStoreId = "store_${sanitizeKey(savedEmail)}"
        } else {
            activeStoreId = "store_${sanitizeKey(DEFAULT_STORE_CODE)}"
        }

        // Start real-time sync automatically
        if (_isRealtimeSyncEnabled.value && activeStoreId != null) {
            startRealtimeSync(activeStoreId!!)
        }
    }

    private fun sanitizeKey(raw: String): String {
        return raw.trim().lowercase()
            .replace("@", "_at_")
            .replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
            .replace(" ", "_")
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun ensureFirebaseInitialized(): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val url = _customDatabaseUrl.value.ifBlank { DEFAULT_DATABASE_URL }.trim()
                val customProjectId = prefs.getString("custom_project_id", null) ?: DEFAULT_PROJECT_ID
                val defaultKey = com.example.BuildConfig.FIREBASE_API_KEY.ifEmpty { DEFAULT_API_KEY }
                val customApiKey = prefs.getString("custom_api_key", null) ?: defaultKey

                val builder = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId(customProjectId)
                    .setApiKey(customApiKey)
                    .setDatabaseUrl(url)

                FirebaseApp.initializeApp(context, builder.build())
            }
            true
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "Firebase initialization: ${e.message}")
            false
        }
    }

    private fun getDatabase(): FirebaseDatabase? {
        if (!ensureFirebaseInitialized()) return null
        return try {
            val url = _customDatabaseUrl.value.ifBlank { DEFAULT_DATABASE_URL }.trim()
            val db = FirebaseDatabase.getInstance(url)
            try {
                db.setPersistenceEnabled(true)
            } catch (_: Exception) {}
            db
        } catch (e: Exception) {
            Log.w("CloudSyncManager", "getDatabase error: ${e.message}")
            null
        }
    }

    fun getActiveStoreId(): String? = activeStoreId

    fun getCustomStoreCode(): String = _customStoreCode.value

    fun getCustomDatabaseUrl(): String = _customDatabaseUrl.value

    fun setCustomDatabaseUrl(url: String) {
        val clean = url.trim()
        _customDatabaseUrl.value = clean
        prefs.edit().putString("custom_database_url", clean).apply()
        activeStoreId?.let {
            if (_isRealtimeSyncEnabled.value) {
                startRealtimeSync(it)
            }
        }
    }

    fun setCustomStoreCode(storeCode: String) {
        val clean = storeCode.trim()
        _customStoreCode.value = clean
        prefs.edit().putString("custom_store_code", clean).apply()
        if (clean.isNotBlank()) {
            val storeId = "store_${sanitizeKey(clean)}"
            activeStoreId = storeId
            if (_isRealtimeSyncEnabled.value) {
                startRealtimeSync(storeId)
            }
        }
    }

    fun setRealtimeSyncEnabled(enabled: Boolean) {
        _isRealtimeSyncEnabled.value = enabled
        prefs.edit().putBoolean("realtime_sync_enabled", enabled).apply()
        if (enabled) {
            activeStoreId?.let { startRealtimeSync(it) }
        } else {
            stopRealtimeSync()
        }
    }

    fun onUserLoggedIn(user: GoogleUser) {
        val sanitizedEmail = sanitizeKey(user.email)
        val storeId = "store_$sanitizedEmail"
        activeStoreId = storeId
        prefs.edit().putString("user_email", user.email).apply()

        if (_isRealtimeSyncEnabled.value) {
            startRealtimeSync(storeId)
        }
    }

    fun onUserLoggedOut() {
        stopRealtimeSync()
        val customCode = _customStoreCode.value
        if (customCode.isNotBlank()) {
            activeStoreId = "store_${sanitizeKey(customCode)}"
            if (_isRealtimeSyncEnabled.value) {
                startRealtimeSync(activeStoreId!!)
            }
        } else {
            activeStoreId = null
            _syncStatus.value = SyncStatus.OFFLINE
            _syncMessage.value = "Akun keluar. Sinkronisasi dihentikan."
        }
    }

    /**
     * Starts active WebSocket real-time synchronization with Firebase Realtime Database.
     * Listens for changes to products, categories, transactions (including items), expenses, and settings.
     */
    fun startRealtimeSync(storeId: String) {
        stopRealtimeSync()
        activeStoreId = storeId

        val db = getDatabase()
        if (db == null) {
            _syncStatus.value = SyncStatus.OFFLINE
            _syncMessage.value = "Firebase Realtime Database belum siap. Masukkan URL database di Pengaturan."
            return
        }

        _syncStatus.value = SyncStatus.SYNCING
        _syncMessage.value = "Menghubungkan ke Firebase Realtime Database..."

        try {
            val storeRef = db.getReference("stores").child(storeId)
            try {
                storeRef.keepSynced(true)
            } catch (_: Exception) {}

            // 0. Connection state listener
            val connectedRef = db.getReference(".info/connected")
            val connectionListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        _syncStatus.value = SyncStatus.SYNCED
                        _syncMessage.value = "🟢 Real-Time Online Terhubung (Multi-HP Aktif)"
                        currentDeviceInfo?.let { updateDevicePresence(it) }
                    } else {
                        if (!isOnline()) {
                            _syncStatus.value = SyncStatus.OFFLINE
                            _syncMessage.value = "🟡 Mode Offline (Data tersimpan lokal di HP)"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _syncStatus.value = SyncStatus.ERROR
                    _syncMessage.value = "Kendala koneksi Firebase: ${error.message}"
                }
            }
            connectedRef.addValueEventListener(connectionListener)
            activeListeners.add(connectedRef to connectionListener)

            // 1. Listen for Products & Stock changes from other devices in real-time
            val productsRef = storeRef.child("products")
            val productListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        for (child in snapshot.children) {
                            try {
                                val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                                val id = child.child("id").value.toLongSafe(child.key?.toLongOrNull() ?: 0L)
                                if (id == 0L) continue

                                if (isDeleted) {
                                    productDao.deleteProductById(id)
                                } else {
                                    val name = child.child("name").getValue(String::class.java) ?: continue
                                    val catId = child.child("categoryId").value.toLongSafe(1L)
                                    val catName = child.child("categoryName").getValue(String::class.java) ?: "Umum"
                                    val qrCode = child.child("qrCode").getValue(String::class.java) ?: ""
                                    val beli = child.child("hargaBeli").value.toDoubleSafe(0.0)
                                    val jual = child.child("hargaJual").value.toDoubleSafe(0.0)
                                    val stok = child.child("stok").value.toIntSafe(0)
                                    val minAlert = child.child("minimumStokAlert").value.toIntSafe(5)

                                    val product = ProductEntity(
                                        id = id,
                                        name = name,
                                        categoryId = catId,
                                        categoryName = catName,
                                        qrCode = qrCode,
                                        hargaBeli = beli,
                                        hargaJual = jual,
                                        stok = stok,
                                        minimumStokAlert = minAlert
                                    )
                                    productDao.insertProduct(product)
                                }
                            } catch (e: Exception) {
                                Log.e("CloudSync", "Error parsing product snapshot", e)
                            }
                        }
                        updateSyncTimestamp("Stok & Produk tersinkronisasi real-time antar perangkat")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CloudSync", "Product listen cancelled: ${error.message}")
                    _syncStatus.value = SyncStatus.ERROR
                    _syncMessage.value = "Gagal menyinkronkan produk: ${error.message}"
                }
            }
            productsRef.addValueEventListener(productListener)
            activeListeners.add(productsRef to productListener)

            // 2. Listen for Categories changes
            val categoriesRef = storeRef.child("categories")
            val categoryListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        for (child in snapshot.children) {
                            try {
                                val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                                val id = child.child("id").value.toLongSafe(child.key?.toLongOrNull() ?: 0L)
                                if (id == 0L) continue

                                if (isDeleted) {
                                    categoryDao.deleteCategoryById(id)
                                } else {
                                    val name = child.child("name").getValue(String::class.java) ?: continue
                                    categoryDao.insertCategory(CategoryEntity(id = id, name = name))
                                }
                            } catch (e: Exception) {
                                Log.e("CloudSync", "Error parsing category snapshot", e)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("CloudSync", "Category listen error: ${error.message}")
                }
            }
            categoriesRef.addValueEventListener(categoryListener)
            activeListeners.add(categoriesRef to categoryListener)

            // 3. Listen for Transactions & Items from all cashier devices
            val txRef = storeRef.child("transactions")
            val txListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        for (child in snapshot.children) {
                            try {
                                val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                                val id = child.child("id").value.toLongSafe(child.key?.toLongOrNull() ?: 0L)
                                if (id == 0L) continue

                                if (isDeleted) {
                                    transactionDao.deleteTransactionItemsByTransactionId(id)
                                    transactionDao.deleteTransactionById(id)
                                    continue
                                }

                                val invoice = child.child("invoiceNumber").getValue(String::class.java) ?: "TM-$id"
                                val timestamp = child.child("timestamp").value.toLongSafe(System.currentTimeMillis())
                                val total = child.child("totalAmount").value.toDoubleSafe(0.0)
                                val cost = child.child("totalCost").value.toDoubleSafe(0.0)
                                val paymentType = child.child("paymentType").getValue(String::class.java) ?: "TUNAI"
                                val paid = child.child("paidAmount").value.toDoubleSafe(total)
                                val change = child.child("changeAmount").value.toDoubleSafe(0.0)
                                val notes = child.child("notes").getValue(String::class.java) ?: ""

                                val tx = TransactionEntity(
                                    id = id,
                                    invoiceNumber = invoice,
                                    timestamp = timestamp,
                                    totalAmount = total,
                                    totalCost = cost,
                                    paymentType = paymentType,
                                    paidAmount = paid,
                                    changeAmount = change,
                                    notes = notes
                                )
                                transactionDao.insertTransaction(tx)

                                // Parse nested items
                                val itemsSnap = child.child("items")
                                if (itemsSnap.exists()) {
                                    val itemsList = mutableListOf<TransactionItemEntity>()
                                    for (itemChild in itemsSnap.children) {
                                        val itemId = itemChild.child("id").value.toLongSafe(itemChild.key?.toLongOrNull() ?: 0L)
                                        val prodId = itemChild.child("productId").value.toLongSafe(0L)
                                        val prodName = itemChild.child("productName").getValue(String::class.java) ?: ""
                                        val qrCode = itemChild.child("qrCode").getValue(String::class.java) ?: ""
                                        val qty = itemChild.child("quantity").value.toIntSafe(1)
                                        val uPrice = itemChild.child("unitPrice").value.toDoubleSafe(0.0)
                                        val uCost = itemChild.child("unitCost").value.toDoubleSafe(0.0)
                                        val subtotal = itemChild.child("subtotal").value.toDoubleSafe(uPrice * qty)

                                        itemsList.add(
                                            TransactionItemEntity(
                                                id = itemId,
                                                transactionId = id,
                                                productId = prodId,
                                                productName = prodName,
                                                qrCode = qrCode,
                                                quantity = qty,
                                                unitPrice = uPrice,
                                                unitCost = uCost,
                                                subtotal = subtotal
                                            )
                                        )
                                    }
                                    if (itemsList.isNotEmpty()) {
                                        val cleanItems = TransactionItemHelper.deduplicateItems(itemsList, total)
                                        transactionDao.deleteTransactionItemsByTransactionId(id)
                                        transactionDao.insertTransactionItems(cleanItems)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CloudSync", "Error parsing transaction", e)
                            }
                        }
                        updateSyncTimestamp("Transaksi kasir tersinkronisasi real-time")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("CloudSync", "Transaction listen error: ${error.message}")
                }
            }
            txRef.addValueEventListener(txListener)
            activeListeners.add(txRef to txListener)

            // 4. Listen for Expenses changes
            val expensesRef = storeRef.child("expenses")
            val expenseListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        for (child in snapshot.children) {
                            try {
                                val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                                val id = child.child("id").value.toLongSafe(child.key?.toLongOrNull() ?: 0L)
                                if (id == 0L) continue

                                if (isDeleted) {
                                    expenseDao.deleteExpenseById(id)
                                } else {
                                    val title = child.child("title").getValue(String::class.java) ?: continue
                                    val category = child.child("category").getValue(String::class.java) ?: "Operasional"
                                    val amount = child.child("amount").value.toDoubleSafe(0.0)
                                    val timestamp = child.child("timestamp").value.toLongSafe(System.currentTimeMillis())
                                    val notes = child.child("notes").getValue(String::class.java) ?: ""

                                    val expense = ExpenseEntity(
                                        id = id,
                                        title = title,
                                        category = category,
                                        amount = amount,
                                        timestamp = timestamp,
                                        notes = notes
                                    )
                                    expenseDao.insertExpense(expense)
                                }
                            } catch (e: Exception) {
                                Log.e("CloudSync", "Error parsing expense", e)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("CloudSync", "Expense listen error: ${error.message}")
                }
            }
            expensesRef.addValueEventListener(expenseListener)
            activeListeners.add(expensesRef to expenseListener)

            // 5. Listen for Settings changes (Store Profile & Modal Kas)
            val settingsRef = storeRef.child("settings").child("profile")
            val settingsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        scope.launch {
                            val current = storeSettingsDao.getSettingsSync() ?: StoreSettingsEntity()
                            val storeName = snapshot.child("storeName").getValue(String::class.java) ?: current.storeName
                            val address = snapshot.child("storeAddress").getValue(String::class.java) ?: current.storeAddress
                            val phone = snapshot.child("storePhone").getValue(String::class.java) ?: current.storePhone
                            val footer = snapshot.child("receiptFooter").getValue(String::class.java) ?: current.receiptFooter
                            val capital = snapshot.child("initialCashCapital").value.toDoubleSafe(current.initialCashCapital)
                            val nominals = snapshot.child("quickNominals").getValue(String::class.java) ?: current.quickNominals

                            val updated = current.copy(
                                storeName = storeName,
                                storeAddress = address,
                                storePhone = phone,
                                receiptFooter = footer,
                                initialCashCapital = capital,
                                quickNominals = nominals
                            )
                            storeSettingsDao.insertOrUpdateSettings(updated)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("CloudSync", "Settings listen error: ${error.message}")
                }
            }
            settingsRef.addValueEventListener(settingsListener)
            activeListeners.add(settingsRef to settingsListener)

            // 6. Presence listener: Melacak kasir-kasir aktif secara real-time
            val presenceRef = storeRef.child("presence")
            val presenceListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<CashierDeviceInfo>()
                    for (child in snapshot.children) {
                        try {
                            val role = child.child("role").getValue(String::class.java) ?: "KASIR"
                            if (role == "KASIR") {
                                val devId = child.child("deviceId").getValue(String::class.java) ?: child.key ?: ""
                                val devName = child.child("deviceName").getValue(String::class.java) ?: "Perangkat Kasir"
                                val cNum = child.child("cashierNumber").value.toIntSafe(list.size + 1)
                                val lastSeen = child.child("lastSeen").value.toLongSafe(System.currentTimeMillis())
                                val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: true
                                list.add(
                                    CashierDeviceInfo(
                                        deviceId = devId,
                                        deviceName = devName,
                                        cashierNumber = cNum,
                                        role = role,
                                        lastSeen = lastSeen,
                                        isOnline = isOnline
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w("CloudSync", "presence parse error: ${e.message}")
                        }
                    }
                    _activeCashiers.value = list
                    val currentRoleIsKasir = currentDeviceInfo?.role == "KASIR"
                    _activeCashierCount.value = if (list.isNotEmpty()) list.size else if (currentRoleIsKasir) 1 else 1
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("CloudSync", "presence listen error: ${error.message}")
                }
            }
            presenceRef.addValueEventListener(presenceListener)
            activeListeners.add(presenceRef to presenceListener)

            // Segera kirim presence perangkat ini jika sudah ada info
            currentDeviceInfo?.let { updateDevicePresence(it) }

            _syncStatus.value = SyncStatus.SYNCED
            _syncMessage.value = "🟢 Sinkronisasi Real-Time Aktif (Multi-Ponsel Terhubung)"
        } catch (e: Exception) {
            Log.e("CloudSync", "Error setting up listeners: ${e.message}", e)
            _syncStatus.value = SyncStatus.ERROR
            _syncMessage.value = "Kendala koneksi Firebase Realtime Database: ${e.message}"
        }
    }

    fun stopRealtimeSync() {
        for ((ref, listener) in activeListeners) {
            try {
                ref.removeEventListener(listener)
            } catch (e: Exception) {
                Log.w("CloudSync", "Error removing listener: ${e.message}")
            }
        }
        activeListeners.clear()
    }

    private fun updateSyncTimestamp(message: String) {
        val now = System.currentTimeMillis()
        _lastSyncTime.value = now
        _syncStatus.value = SyncStatus.SYNCED
        _syncMessage.value = message
        prefs.edit().putLong("last_sync_timestamp", now).apply()
    }

    fun updateDevicePresence(device: CashierDeviceInfo) {
        currentDeviceInfo = device
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val presenceRef = db.getReference("stores").child(storeId)
                    .child("presence").child(device.deviceId)

                presenceRef.onDisconnect().removeValue()

                val data = mapOf(
                    "deviceId" to device.deviceId,
                    "deviceName" to device.deviceName,
                    "cashierNumber" to device.cashierNumber,
                    "role" to device.role,
                    "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "isOnline" to true
                )
                presenceRef.setValue(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "updateDevicePresence error: ${e.message}")
            }
        }
    }

    fun pushDeleteTransaction(txId: Long) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "isDeleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.getReference("stores").child(storeId)
                    .child("transactions").child(txId.toString())
                    .updateChildren(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushDeleteTransaction failed: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // PUSH LOCAL CHANGES TO REALTIME DATABASE
    // -------------------------------------------------------------
    fun pushProduct(product: ProductEntity) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = hashMapOf(
                    "id" to product.id,
                    "name" to product.name,
                    "categoryId" to product.categoryId,
                    "categoryName" to product.categoryName,
                    "qrCode" to product.qrCode,
                    "hargaBeli" to product.hargaBeli,
                    "hargaJual" to product.hargaJual,
                    "stok" to product.stok,
                    "minimumStokAlert" to product.minimumStokAlert,
                    "updatedAt" to System.currentTimeMillis(),
                    "isDeleted" to false
                )
                db.getReference("stores").child(storeId)
                    .child("products").child(product.id.toString())
                    .updateChildren(data as Map<String, Any>)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushProduct failed: ${e.message}")
            }
        }
    }

    fun pushDeleteProduct(productId: Long) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "isDeleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.getReference("stores").child(storeId)
                    .child("products").child(productId.toString())
                    .updateChildren(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushDeleteProduct failed: ${e.message}")
            }
        }
    }

    fun pushCategory(category: CategoryEntity) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "id" to category.id,
                    "name" to category.name,
                    "updatedAt" to System.currentTimeMillis(),
                    "isDeleted" to false
                )
                db.getReference("stores").child(storeId)
                    .child("categories").child(category.id.toString())
                    .setValue(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushCategory failed: ${e.message}")
            }
        }
    }

    fun pushDeleteCategory(categoryId: Long) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "isDeleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.getReference("stores").child(storeId)
                    .child("categories").child(categoryId.toString())
                    .updateChildren(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushDeleteCategory failed: ${e.message}")
            }
        }
    }

    fun pushTransaction(tx: TransactionEntity, items: List<TransactionItemEntity>) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val cleanItems = TransactionItemHelper.deduplicateItems(items, tx.totalAmount)

                val itemsMap = cleanItems.mapIndexed { index, item ->
                    val stableId = if (item.id > 0) item.id else (index + 1).toLong()
                    val itemKey = "item_${item.productId}_$stableId"
                    itemKey to mapOf(
                        "id" to stableId,
                        "transactionId" to tx.id,
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "qrCode" to item.qrCode,
                        "quantity" to item.quantity,
                        "unitPrice" to item.unitPrice,
                        "unitCost" to item.unitCost,
                        "subtotal" to item.subtotal
                    )
                }.toMap()

                val txData = mapOf(
                    "id" to tx.id,
                    "invoiceNumber" to tx.invoiceNumber,
                    "timestamp" to tx.timestamp,
                    "totalAmount" to tx.totalAmount,
                    "totalCost" to tx.totalCost,
                    "paymentType" to tx.paymentType,
                    "paidAmount" to tx.paidAmount,
                    "changeAmount" to tx.changeAmount,
                    "notes" to tx.notes,
                    "syncedAt" to System.currentTimeMillis(),
                    "items" to itemsMap
                )

                db.getReference("stores").child(storeId)
                    .child("transactions").child(tx.id.toString())
                    .setValue(txData)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushTransaction failed: ${e.message}")
            }
        }
    }

    fun pushExpense(expense: ExpenseEntity) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "id" to expense.id,
                    "title" to expense.title,
                    "category" to expense.category,
                    "amount" to expense.amount,
                    "timestamp" to expense.timestamp,
                    "notes" to expense.notes,
                    "updatedAt" to System.currentTimeMillis(),
                    "isDeleted" to false
                )
                db.getReference("stores").child(storeId)
                    .child("expenses").child(expense.id.toString())
                    .setValue(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushExpense failed: ${e.message}")
            }
        }
    }

    fun pushDeleteExpense(expenseId: Long) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "isDeleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.getReference("stores").child(storeId)
                    .child("expenses").child(expenseId.toString())
                    .updateChildren(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushDeleteExpense failed: ${e.message}")
            }
        }
    }

    fun pushSettings(settings: StoreSettingsEntity) {
        val storeId = activeStoreId ?: return
        if (!_isRealtimeSyncEnabled.value) return

        scope.launch {
            try {
                val db = getDatabase() ?: return@launch
                val data = mapOf(
                    "storeName" to settings.storeName,
                    "storeAddress" to settings.storeAddress,
                    "storePhone" to settings.storePhone,
                    "receiptFooter" to settings.receiptFooter,
                    "initialCashCapital" to settings.initialCashCapital,
                    "quickNominals" to settings.quickNominals,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.getReference("stores").child(storeId)
                    .child("settings").child("profile")
                    .setValue(data)
            } catch (e: Exception) {
                Log.w("CloudSync", "pushSettings failed: ${e.message}")
            }
        }
    }

    /**
     * Uploads ALL current local data to Firebase Realtime Database.
     * Used when first seeding the database or performing a full cloud sync.
     */
    suspend fun syncAllLocalToCloud(): Result<String> = withContext(Dispatchers.IO) {
        val storeId = activeStoreId
            ?: return@withContext Result.failure(IllegalStateException("Silakan masukkan Kode Toko atau Masuk dengan Akun Google terlebih dahulu."))

        val db = getDatabase()
            ?: return@withContext Result.failure(IllegalStateException("Layanan Firebase Realtime Database belum siap. Periksa URL Database di Pengaturan."))

        _syncStatus.value = SyncStatus.SYNCING
        _syncMessage.value = "Mengunggah seluruh data lokal ke Firebase Realtime Database..."

        try {
            val storeRef = db.getReference("stores").child(storeId)

            val categories = categoryDao.getAllCategoriesSync()
            for (cat in categories) {
                storeRef.child("categories").child(cat.id.toString()).setValue(
                    mapOf("id" to cat.id, "name" to cat.name, "updatedAt" to System.currentTimeMillis(), "isDeleted" to false)
                ).await()
            }

            val products = productDao.getAllProductsSync()
            for (p in products) {
                storeRef.child("products").child(p.id.toString()).setValue(
                    mapOf(
                        "id" to p.id,
                        "name" to p.name,
                        "categoryId" to p.categoryId,
                        "categoryName" to p.categoryName,
                        "qrCode" to p.qrCode,
                        "hargaBeli" to p.hargaBeli,
                        "hargaJual" to p.hargaJual,
                        "stok" to p.stok,
                        "minimumStokAlert" to p.minimumStokAlert,
                        "updatedAt" to System.currentTimeMillis(),
                        "isDeleted" to false
                    )
                ).await()
            }

            val transactions = transactionDao.getAllTransactionsSync()
            for (t in transactions) {
                val rawItems = transactionDao.getItemsForTransactionSync(t.id)
                val items = TransactionItemHelper.deduplicateItems(rawItems, t.totalAmount)
                val itemsMap = items.mapIndexed { index, item ->
                    val stableId = if (item.id > 0) item.id else (index + 1).toLong()
                    val itemKey = "item_${item.productId}_$stableId"
                    itemKey to mapOf(
                        "id" to stableId,
                        "transactionId" to t.id,
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "qrCode" to item.qrCode,
                        "quantity" to item.quantity,
                        "unitPrice" to item.unitPrice,
                        "unitCost" to item.unitCost,
                        "subtotal" to item.subtotal
                    )
                }.toMap()

                storeRef.child("transactions").child(t.id.toString()).setValue(
                    mapOf(
                        "id" to t.id,
                        "invoiceNumber" to t.invoiceNumber,
                        "timestamp" to t.timestamp,
                        "totalAmount" to t.totalAmount,
                        "totalCost" to t.totalCost,
                        "paymentType" to t.paymentType,
                        "paidAmount" to t.paidAmount,
                        "changeAmount" to t.changeAmount,
                        "notes" to t.notes,
                        "syncedAt" to System.currentTimeMillis(),
                        "items" to itemsMap
                    )
                ).await()
            }

            val expenses = expenseDao.getAllExpensesSync()
            for (e in expenses) {
                storeRef.child("expenses").child(e.id.toString()).setValue(
                    mapOf(
                        "id" to e.id,
                        "title" to e.title,
                        "category" to e.category,
                        "amount" to e.amount,
                        "timestamp" to e.timestamp,
                        "notes" to e.notes,
                        "updatedAt" to System.currentTimeMillis(),
                        "isDeleted" to false
                    )
                ).await()
            }

            val settings = storeSettingsDao.getSettingsSync()
            if (settings != null) {
                storeRef.child("settings").child("profile").setValue(
                    mapOf(
                        "storeName" to settings.storeName,
                        "storeAddress" to settings.storeAddress,
                        "storePhone" to settings.storePhone,
                        "receiptFooter" to settings.receiptFooter,
                        "initialCashCapital" to settings.initialCashCapital,
                        "quickNominals" to settings.quickNominals,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            updateSyncTimestamp("Berhasil mengunggah ${products.size} produk & ${transactions.size} transaksi ke Firebase Realtime Database.")
            Result.success("Sinkronisasi unggah ke Firebase Realtime Database berhasil!")
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            _syncMessage.value = "Gagal mengunggah data: ${e.message}"
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // TYPE CASTING HELPERS
    // -------------------------------------------------------------
    private fun Any?.toDoubleSafe(default: Double = 0.0): Double = when (this) {
        is Number -> this.toDouble()
        is String -> this.toDoubleOrNull() ?: default
        else -> default
    }

    private fun Any?.toLongSafe(default: Long = 0L): Long = when (this) {
        is Number -> this.toLong()
        is String -> this.toLongOrNull() ?: default
        else -> default
    }

    private fun Any?.toIntSafe(default: Int = 0): Int = when (this) {
        is Number -> this.toInt()
        is String -> this.toIntOrNull() ?: default
        else -> default
    }
}
