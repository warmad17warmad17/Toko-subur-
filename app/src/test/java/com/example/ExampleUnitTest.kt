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
}
