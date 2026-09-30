package com.example.data.model

data class WeeklyReportSummary(
    val weekNumber: Int, // 1, 2, 3, 4, 5
    val weekTitle: String, // e.g., "Minggu 1", "Minggu 2"
    val dateRangeLabel: String, // e.g., "01 - 07 Sep 2026"
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isCurrentWeek: Boolean,
    val transactions: List<TransactionEntity>,
    val expenses: List<ExpenseEntity>,
    val totalOmzet: Double,
    val totalHpp: Double,
    val labaKotor: Double,
    val totalPengeluaran: Double,
    val labaBersih: Double,
    val penjualanTunai: Double,
    val penjualanNonTunai: Double,
    val transactionCount: Int
)
