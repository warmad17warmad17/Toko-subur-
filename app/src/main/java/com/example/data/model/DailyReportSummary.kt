package com.example.data.model

data class DailyReportSummary(
    val dayOffset: Int, // 0 = Hari Ini, 1 = Kemarin, ... 6 = 6 hari lalu
    val startTimestamp: Long,
    val endTimestamp: Long,
    val dayTitle: String, // e.g., "Hari Ini (Rabu)", "Kemarin (Selasa)", "Senin"
    val formattedDate: String, // e.g., "30 Sep 2026"
    val transactions: List<TransactionEntity>,
    val expenses: List<ExpenseEntity>,
    val totalOmzet: Double,
    val totalHpp: Double,
    val labaKotor: Double,
    val totalPengeluaran: Double,
    val labaBersih: Double,
    val penjualanTunai: Double,
    val penjualanNonTunai: Double,
    val transactionCount: Int,
    val pengeluaranOperasional: Double = expenses.filter { !it.isRestock }.sumOf { it.amount },
    val pengeluaranStok: Double = expenses.filter { it.isRestock }.sumOf { it.amount }
)
