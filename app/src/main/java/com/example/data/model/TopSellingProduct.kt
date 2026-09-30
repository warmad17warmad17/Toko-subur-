package com.example.data.model

data class TopSellingProduct(
    val rank: Int,
    val productId: Long,
    val productName: String,
    val qrCode: String,
    val categoryName: String,
    val totalSoldQuantity: Int,
    val totalRevenue: Double,
    val currentStock: Int,
    val currentPrice: Double,
    val product: ProductEntity? = null
)
