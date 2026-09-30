package com.example.util

import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StoreSettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val storeSettings: StoreSettingsEntity?,
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val transactions: List<TransactionEntity>,
    val transactionItems: List<TransactionItemEntity>,
    val expenses: List<ExpenseEntity>
)

object BackupManager {

    fun exportToJson(
        settings: StoreSettingsEntity,
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        transactionItems: List<TransactionItemEntity>,
        expenses: List<ExpenseEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "TOKO MAKMUR")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // Settings
        val settingsObj = JSONObject().apply {
            put("storeName", settings.storeName)
            put("storeAddress", settings.storeAddress)
            put("storePhone", settings.storePhone)
            put("receiptFooter", settings.receiptFooter)
            put("initialCashCapital", settings.initialCashCapital)
            put("quickNominals", settings.quickNominals)
        }
        root.put("settings", settingsObj)

        // Categories
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
            }
            categoriesArray.put(catObj)
        }
        root.put("categories", categoriesArray)

        // Products
        val productsArray = JSONArray()
        for (prod in products) {
            val prodObj = JSONObject().apply {
                put("id", prod.id)
                put("name", prod.name)
                put("categoryId", prod.categoryId)
                put("categoryName", prod.categoryName)
                put("qrCode", prod.qrCode)
                put("hargaBeli", prod.hargaBeli)
                put("hargaJual", prod.hargaJual)
                put("stok", prod.stok)
                put("minimumStokAlert", prod.minimumStokAlert)
            }
            productsArray.put(prodObj)
        }
        root.put("products", productsArray)

        // Transactions
        val transactionsArray = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject().apply {
                put("id", tx.id)
                put("invoiceNumber", tx.invoiceNumber)
                put("timestamp", tx.timestamp)
                put("totalAmount", tx.totalAmount)
                put("totalCost", tx.totalCost)
                put("paymentType", tx.paymentType)
                put("paidAmount", tx.paidAmount)
                put("changeAmount", tx.changeAmount)
                put("notes", tx.notes)
            }
            transactionsArray.put(txObj)
        }
        root.put("transactions", transactionsArray)

        // Transaction Items
        val itemsArray = JSONArray()
        for (item in transactionItems) {
            val itemObj = JSONObject().apply {
                put("id", item.id)
                put("transactionId", item.transactionId)
                put("productId", item.productId)
                put("productName", item.productName)
                put("qrCode", item.qrCode)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("unitCost", item.unitCost)
                put("subtotal", item.subtotal)
            }
            itemsArray.put(itemObj)
        }
        root.put("transactionItems", itemsArray)

        // Expenses
        val expensesArray = JSONArray()
        for (exp in expenses) {
            val expObj = JSONObject().apply {
                put("id", exp.id)
                put("title", exp.title)
                put("category", exp.category)
                put("amount", exp.amount)
                put("timestamp", exp.timestamp)
                put("notes", exp.notes)
            }
            expensesArray.put(expObj)
        }
        root.put("expenses", expensesArray)

        return root.toString(2)
    }

    fun parseJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        val settings: StoreSettingsEntity? = if (root.has("settings")) {
            val sObj = root.getJSONObject("settings")
            StoreSettingsEntity(
                id = 1,
                storeName = sObj.optString("storeName", "TOKO SUBUR"),
                storeAddress = sObj.optString("storeAddress", "Jl. Kembang Kuning No.17, Surabaya"),
                storePhone = sObj.optString("storePhone", "0812-3456-7890"),
                receiptFooter = sObj.optString("receiptFooter", "Terima kasih telah berbelanja di TOKO SUBUR!\nBarang yang sudah dibeli tidak dapat ditukar."),
                initialCashCapital = sObj.optDouble("initialCashCapital", 1000000.0),
                quickNominals = sObj.optString("quickNominals", "10,20,30,50,100")
            )
        } else null

        val categories = mutableListOf<CategoryEntity>()
        if (root.has("categories")) {
            val cArray = root.getJSONArray("categories")
            for (i in 0 until cArray.length()) {
                val obj = cArray.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name")
                    )
                )
            }
        }

        val products = mutableListOf<ProductEntity>()
        if (root.has("products")) {
            val pArray = root.getJSONArray("products")
            for (i in 0 until pArray.length()) {
                val obj = pArray.getJSONObject(i)
                products.add(
                    ProductEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        categoryId = obj.optLong("categoryId", 0L),
                        categoryName = obj.optString("categoryName", ""),
                        qrCode = obj.optString("qrCode", ""),
                        hargaBeli = obj.optDouble("hargaBeli", 0.0),
                        hargaJual = obj.optDouble("hargaJual", 0.0),
                        stok = obj.optInt("stok", 0),
                        minimumStokAlert = obj.optInt("minimumStokAlert", 5)
                    )
                )
            }
        }

        val transactions = mutableListOf<TransactionEntity>()
        if (root.has("transactions")) {
            val tArray = root.getJSONArray("transactions")
            for (i in 0 until tArray.length()) {
                val obj = tArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0L),
                        invoiceNumber = obj.optString("invoiceNumber", "TRX-${System.currentTimeMillis()}"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        paymentType = obj.optString("paymentType", "TUNAI"),
                        paidAmount = obj.optDouble("paidAmount", 0.0),
                        changeAmount = obj.optDouble("changeAmount", 0.0),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val items = mutableListOf<TransactionItemEntity>()
        if (root.has("transactionItems")) {
            val iArray = root.getJSONArray("transactionItems")
            for (i in 0 until iArray.length()) {
                val obj = iArray.getJSONObject(i)
                items.add(
                    TransactionItemEntity(
                        id = obj.optLong("id", 0L),
                        transactionId = obj.optLong("transactionId", 0L),
                        productId = obj.optLong("productId", 0L),
                        productName = obj.optString("productName", ""),
                        qrCode = obj.optString("qrCode", ""),
                        quantity = obj.optInt("quantity", 1),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        unitCost = obj.optDouble("unitCost", 0.0),
                        subtotal = obj.optDouble("subtotal", 0.0)
                    )
                )
            }
        }

        val expenses = mutableListOf<ExpenseEntity>()
        if (root.has("expenses")) {
            val eArray = root.getJSONArray("expenses")
            for (i in 0 until eArray.length()) {
                val obj = eArray.getJSONObject(i)
                expenses.add(
                    ExpenseEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        category = obj.optString("category", "Umum"),
                        amount = obj.optDouble("amount", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        return BackupData(
            storeSettings = settings,
            categories = categories,
            products = products,
            transactions = transactions,
            transactionItems = items,
            expenses = expenses
        )
    }
}
