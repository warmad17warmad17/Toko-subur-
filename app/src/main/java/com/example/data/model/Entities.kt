package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val qrCode: String,
    val hargaBeli: Double,
    val hargaJual: Double,
    val stok: Int,
    val minimumStokAlert: Int = 5
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val totalCost: Double, // HPP (Harga Pokok Penjualan)
    val paymentType: String, // "TUNAI" or "NON_TUNAI"
    val paidAmount: Double,
    val changeAmount: Double,
    val notes: String = ""
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val qrCode: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "TOKO SUBUR",
    val storeAddress: String = "Jl. Kembang Kuning No.17, Surabaya",
    val storePhone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima kasih telah berbelanja di TOKO SUBUR!\nBarang yang sudah dibeli tidak dapat ditukar.",
    val initialCashCapital: Double = 500000.0, // Modal Kas Toko awal
    val quickNominals: String = "10,20,30,50,100"
)
