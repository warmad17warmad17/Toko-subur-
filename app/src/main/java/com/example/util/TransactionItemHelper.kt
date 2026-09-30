package com.example.util

import com.example.data.model.TransactionItemEntity
import kotlin.math.abs

object TransactionItemHelper {

    /**
     * Deduplicates and aggregates transaction items.
     * Prevents duplicate products in payment receipts and ensures sales reports (HPP & revenue)
     * are completely accurate.
     *
     * Handles two scenarios:
     * 1. Duplicate clones created by Firebase sync re-insertion (where sum(subtotal) > totalAmount):
     *    Keeps only the original unique item.
     * 2. Multiple purchases of the same product:
     *    Merges them into a single clean line item with sum of quantities and correct subtotal.
     */
    fun deduplicateItems(
        items: List<TransactionItemEntity>,
        transactionTotal: Double
    ): List<TransactionItemEntity> {
        if (items.isEmpty()) return emptyList()

        // Group by product identity: productId (if > 0) or qrCode or productName + unitPrice
        val grouped = items.groupBy { item ->
            when {
                item.productId > 0L -> "id_${item.productId}"
                item.qrCode.isNotBlank() -> "qr_${item.qrCode.trim().lowercase()}"
                else -> "name_${item.productName.trim().lowercase()}_${item.unitPrice}"
            }
        }

        val totalItemsSubtotal = items.sumOf { it.subtotal }
        val hasClonedDuplicates = transactionTotal > 0.0 && totalItemsSubtotal > (transactionTotal + 0.01)

        return grouped.values.map { group ->
            if (group.size == 1) {
                group.first()
            } else {
                val first = group.first()
                val allIdentical = group.all { 
                    it.quantity == first.quantity && 
                    it.unitPrice == first.unitPrice &&
                    abs(it.subtotal - first.subtotal) < 0.01
                }

                if (hasClonedDuplicates && allIdentical) {
                    // Cloned duplicate rows created by sync re-insertion: keep only single original
                    first
                } else {
                    // Multiple purchases of the same product: merge into single item with combined quantity
                    val totalQty = group.sumOf { it.quantity }
                    first.copy(
                        quantity = totalQty,
                        subtotal = totalQty * first.unitPrice
                    )
                }
            }
        }
    }
}
