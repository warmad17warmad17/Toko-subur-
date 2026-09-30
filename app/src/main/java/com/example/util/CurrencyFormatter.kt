package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indonesianLocale = Locale.forLanguageTag("id-ID")
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private val symbols = DecimalFormatSymbols(indonesianLocale).apply {
        groupingSeparator = '.'
    }

    private val thousandDecimalFormat = DecimalFormat("#,###", symbols)

    fun formatRupiah(amount: Double): String {
        return try {
            formatter.format(amount).replace("Rp", "Rp ")
        } catch (e: Exception) {
            "Rp ${formatThousand(amount.toLong())}"
        }
    }

    fun formatRupiah(amount: Long): String {
        return formatRupiah(amount.toDouble())
    }

    /**
     * Formats a Long number with dot (.) thousands separator (e.g. 10000 -> "10.000").
     */
    fun formatThousand(amount: Long): String {
        return try {
            thousandDecimalFormat.format(amount)
        } catch (e: Exception) {
            amount.toString()
        }
    }

    fun formatThousand(amount: Double): String {
        return formatThousand(amount.toLong())
    }

    /**
     * Formats user typing input into thousands-separated string (e.g. "10000" -> "10.000").
     * Supports typing digits, pasting, backspacing, and deleting dots gracefully.
     */
    fun formatInputNominal(newInput: String, oldInput: String = ""): String {
        val newDigits = newInput.filter { it.isDigit() }
        val oldDigits = oldInput.filter { it.isDigit() }

        // If user pressed backspace on a '.' separator, drop the previous digit
        val digitsToFormat = if (newInput.length < oldInput.length && newDigits == oldDigits && newDigits.isNotEmpty()) {
            newDigits.dropLast(1)
        } else {
            newDigits
        }

        if (digitsToFormat.isEmpty()) return ""
        val num = digitsToFormat.toLongOrNull() ?: return digitsToFormat
        return formatThousand(num)
    }

    fun parseAmount(input: String): Double {
        val cleaned = input.replace(Regex("[^0-9]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
