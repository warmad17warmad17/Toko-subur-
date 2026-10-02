package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `test nominal formatting with thousands separator dot`() {
    assertEquals("10.000", com.example.util.CurrencyFormatter.formatInputNominal("10000"))
    assertEquals("100.000", com.example.util.CurrencyFormatter.formatInputNominal("100000"))
    assertEquals("1.000.000", com.example.util.CurrencyFormatter.formatInputNominal("1000000"))
    assertEquals("500", com.example.util.CurrencyFormatter.formatInputNominal("500"))
    assertEquals("", com.example.util.CurrencyFormatter.formatInputNominal(""))
  }

  @Test
  fun `test nominal backspace gracefully deletes digit`() {
    // Backspacing on dot separator from "1.000" -> user removed dot -> drops digit before dot
    assertEquals("100", com.example.util.CurrencyFormatter.formatInputNominal("1000", "1.000"))
  }

  @Test
  fun `test parseAmount parses dot separated nominal correctly`() {
    assertEquals(10000.0, com.example.util.CurrencyFormatter.parseAmount("10.000"), 0.001)
    assertEquals(1250000.0, com.example.util.CurrencyFormatter.parseAmount("1.250.000"), 0.001)
    assertEquals(0.0, com.example.util.CurrencyFormatter.parseAmount(""), 0.001)
  }

  @Test
  fun `test monthly weekly intervals logic`() {
    val cal = java.util.Calendar.getInstance()
    val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    var startDay = 1
    var weekNum = 1
    val weeks = mutableListOf<Pair<Int, Int>>()
    while (startDay <= maxDays) {
      val endDay = minOf(startDay + 6, maxDays)
      weeks.add(startDay to endDay)
      startDay = endDay + 1
      weekNum++
    }
    // Should have 4 or 5 weeks depending on month length
    assertTrue(weeks.size in 4..5)
    assertEquals(1, weeks.first().first)
    assertEquals(maxDays, weeks.last().second)
  }

  @Test
  fun `test deduplicate cloned duplicate items when sum exceeds transaction total`() {
    val items = listOf(
        com.example.data.model.TransactionItemEntity(
            id = 1,
            transactionId = 10,
            productId = 101,
            productName = "Beras Rojolele 5kg",
            qrCode = "8991001",
            quantity = 1,
            unitPrice = 72000.0,
            unitCost = 65000.0,
            subtotal = 72000.0
        ),
        com.example.data.model.TransactionItemEntity(
            id = 2,
            transactionId = 10,
            productId = 101,
            productName = "Beras Rojolele 5kg",
            qrCode = "8991001",
            quantity = 1,
            unitPrice = 72000.0,
            unitCost = 65000.0,
            subtotal = 72000.0
        )
    )

    // Transaction total was 72.000 (only 1 product), but items had duplicate row
    val clean = com.example.util.TransactionItemHelper.deduplicateItems(items, 72000.0)
    assertEquals(1, clean.size)
    assertEquals("Beras Rojolele 5kg", clean[0].productName)
    assertEquals(1, clean[0].quantity)
    assertEquals(72000.0, clean[0].subtotal, 0.001)
  }

  @Test
  fun `test merge legitimate multiple quantities of same product`() {
    val items = listOf(
        com.example.data.model.TransactionItemEntity(
            id = 1,
            transactionId = 11,
            productId = 102,
            productName = "Indomie Goreng",
            qrCode = "8991007",
            quantity = 2,
            unitPrice = 3500.0,
            unitCost = 2800.0,
            subtotal = 7000.0
        ),
        com.example.data.model.TransactionItemEntity(
            id = 2,
            transactionId = 11,
            productId = 102,
            productName = "Indomie Goreng",
            qrCode = "8991007",
            quantity = 3,
            unitPrice = 3500.0,
            unitCost = 2800.0,
            subtotal = 10500.0
        )
    )

    // Transaction total is 17.500 (5 items total)
    val clean = com.example.util.TransactionItemHelper.deduplicateItems(items, 17500.0)
    assertEquals(1, clean.size)
    assertEquals(5, clean[0].quantity)
    assertEquals(17500.0, clean[0].subtotal, 0.001)
  }

  @Test
  fun `test promo product exclusion filters promo products from top sellers ranking`() {
    val items = listOf(
        com.example.data.model.TransactionItemEntity(
            id = 1,
            transactionId = 1,
            productId = 1,
            productName = "Indomie Goreng",
            qrCode = "001",
            quantity = 50,
            unitPrice = 3500.0,
            unitCost = 2800.0,
            subtotal = 175000.0
        ),
        com.example.data.model.TransactionItemEntity(
            id = 2,
            transactionId = 1,
            productId = 2,
            productName = "Gula Pasir 1kg (Promo)",
            qrCode = "002",
            quantity = 100, // Highest quantity, BUT ON PROMO!
            unitPrice = 14000.0,
            unitCost = 13000.0,
            subtotal = 1400000.0
        ),
        com.example.data.model.TransactionItemEntity(
            id = 3,
            transactionId = 1,
            productId = 3,
            productName = "Beras 5kg",
            qrCode = "003",
            quantity = 30,
            unitPrice = 72000.0,
            unitCost = 65000.0,
            subtotal = 2160000.0
        )
    )

    val excludedPromoIds = setOf(2L) // Gula Pasir is on promo!

    // Filter out promo products
    val organicItems = items.filter { it.productId !in excludedPromoIds }
    val sortedByQty = organicItems.sortedByDescending { it.quantity }

    // Indomie (50 pcs) should be rank 1, Beras (30 pcs) rank 2, Gula Pasir (promo) excluded!
    assertEquals(2, sortedByQty.size)
    assertEquals("Indomie Goreng", sortedByQty[0].productName)
    assertEquals(50, sortedByQty[0].quantity)
    assertEquals("Beras 5kg", sortedByQty[1].productName)
    assertEquals(30, sortedByQty[1].quantity)
  }

  @Test
  fun `test top selling products custom limit adjustment from 10 to 20`() {
    val items = (1..25).map { i ->
      com.example.data.model.TopSellingProduct(
        rank = i,
        productId = i.toLong(),
        productName = "Produk #$i",
        qrCode = "QR$i",
        categoryName = "Kategori",
        totalSoldQuantity = 100 - i,
        totalRevenue = (100 - i) * 1000.0,
        currentStock = 20,
        currentPrice = 1000.0
      )
    }

    // Default limit 10
    val top10 = items.take(10)
    assertEquals(10, top10.size)
    assertEquals(1, top10.first().rank)
    assertEquals(10, top10.last().rank)

    // Adjusted limit 20
    val customLimit = 20
    val top20 = items.take(customLimit)
    assertEquals(20, top20.size)
    assertEquals("Produk #1", top20.first().productName)
    assertEquals("Produk #20", top20.last().productName)
  }

  @Test
  fun `test incoming stock addition logic (existing 5 pcs + incoming 10 pcs = 15 pcs)`() {
    val existingStock = 5
    val incomingNewStock = 10

    val calculatedFinalStock = existingStock + incomingNewStock
    assertEquals(15, calculatedFinalStock)

    // If no incoming stock entered (blank / 0), keep existing stock
    val zeroIncoming = 0
    val stockUnchanged = existingStock + zeroIncoming
    assertEquals(5, stockUnchanged)
  }

  @Test
  fun `test low stock detection only triggers when there is new low stock information`() {
    fun hasNewLowStockAlert(
      lowStockItems: List<Pair<Long, Int>>, // id to stock
      lastSignature: String
    ): Boolean {
      if (lowStockItems.isEmpty()) return false
      if (lastSignature.isBlank()) return true

      val previousStockMap = try {
        lastSignature.split(";").mapNotNull { entry ->
          val parts = entry.split(":")
          if (parts.size == 2) {
            val id = parts[0].toLongOrNull()
            val stok = parts[1].toIntOrNull()
            if (id != null && stok != null) id to stok else null
          } else null
        }.toMap()
      } catch (e: Exception) {
        emptyMap()
      }

      if (previousStockMap.isEmpty()) return true

      val hasNewLowStockProduct = lowStockItems.any { it.first !in previousStockMap }
      if (hasNewLowStockProduct) return true

      val hasDecreasedStock = lowStockItems.any { item ->
        val prevStok = previousStockMap[item.first]
        prevStok != null && item.second < prevStok
      }
      return hasDecreasedStock
    }

    // 1. Initial detection when lastSignature is empty -> must trigger
    val initialLow = listOf(1L to 3)
    val sig1 = "1:3"
    assertTrue(hasNewLowStockAlert(initialLow, ""))

    // 2. Re-checking when stock has not changed -> must NOT trigger
    assertFalse(hasNewLowStockAlert(initialLow, sig1))

    // 3. Stock decreases from 3 to 2 -> must trigger
    val decreasedLow = listOf(1L to 2)
    val sig2 = "1:2"
    assertTrue(hasNewLowStockAlert(decreasedLow, sig1))

    // 4. Same state again -> must NOT trigger
    assertFalse(hasNewLowStockAlert(decreasedLow, sig2))

    // 5. Another product drops to low stock -> must trigger
    val additionalLow = listOf(1L to 2, 2L to 4)
    val sig3 = "1:2;2:4"
    assertTrue(hasNewLowStockAlert(additionalLow, sig2))

    // 6. Restocking product 1 (now only product 2 is low) -> must NOT trigger sound alert
    val restockedLow = listOf(2L to 4)
    assertFalse(hasNewLowStockAlert(restockedLow, sig3))

    // 7. All products safe (empty list) -> must NOT trigger alert
    assertFalse(hasNewLowStockAlert(emptyList(), sig3))
  }
}
