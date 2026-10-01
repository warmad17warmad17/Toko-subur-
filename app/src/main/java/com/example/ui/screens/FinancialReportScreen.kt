package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import com.example.data.auth.UserRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyReportSummary
import com.example.data.model.WeeklyReportSummary
import com.example.data.model.TransactionEntity
import com.example.ui.components.ReceiptDialog
import com.example.ui.viewmodel.PeriodFilter
import com.example.ui.viewmodel.TokoViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter

@Composable
fun FinancialReportScreen(
    viewModel: TokoViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val periodExpenses by viewModel.periodExpenses.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val weeklyDailyBreakdown by viewModel.weeklyDailyBreakdown.collectAsStateWithLifecycle()
    val monthlyWeeklyBreakdown by viewModel.monthlyWeeklyBreakdown.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val activeCashierCount by viewModel.activeCashierCount.collectAsStateWithLifecycle()
    val activeCashierLabel by viewModel.activeCashierLabel.collectAsStateWithLifecycle()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var expandAllDays by remember { mutableStateOf<Boolean?>(null) }
    var expandAllWeeks by remember { mutableStateOf<Boolean?>(null) }

    val showReceiptDialog by viewModel.showReceiptDialog.collectAsStateWithLifecycle()
    val receiptTx by viewModel.currentReceiptTransaction.collectAsStateWithLifecycle()
    val receiptItems by viewModel.currentReceiptItems.collectAsStateWithLifecycle()

    // Financial calculations
    val pendapatanKotor = periodTransactions.sumOf { it.totalAmount }
    val totalHpp = periodTransactions.sumOf { it.totalCost }
    val totalPengeluaran = periodExpenses.sumOf { it.amount }
    val pengeluaranOperasional = periodExpenses.filter { !it.isRestock }.sumOf { it.amount }
    val pengeluaranStokUlang = periodExpenses.filter { it.isRestock }.sumOf { it.amount }
    val labaKotor = pendapatanKotor - totalHpp
    val pendapatanBersih = labaKotor - pengeluaranOperasional // Pengeluaran stok produk TIDAK memotong pendapatan bersih

    // Cash reconciliation calculations (pengeluaran stok ulang tetap memotong kas fisik laci kasir)
    val penjualanTunai = periodTransactions.filter { it.paymentType == "TUNAI" }.sumOf { it.totalAmount }
    val penjualanNonTunai = periodTransactions.filter { it.paymentType == "NON_TUNAI" }.sumOf { it.totalAmount }
    val modalKasAwal = storeSettings.initialCashCapital
    val estimasiUangKasFisik = modalKasAwal + penjualanTunai - totalPengeluaran

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Laporan Keuangan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Analisis omzet, HPP modal produk, laba bersih & kas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.syncHppManually() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sync_hpp_button")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sinkronkan HPP",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sinkron HPP", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Period Tabs: Hari Ini, Minggu Ini, Bulan Ini, Semua
                TabRow(
                    selectedTabIndex = selectedPeriod.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("report_period_tabs")
                ) {
                    PeriodFilter.values().forEach { period ->
                        Tab(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.setSelectedPeriod(period) },
                            text = {
                                Text(
                                    text = period.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("period_tab_${period.name}")
                        )
                    }
                }
            }
        }

        // Scrollable Report Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role & Cashier Presence Status Card (Informasi Real-Time untuk Akun Pemilik & Kasir)
            Surface(
                color = if (currentUserRole == UserRole.PEMILIK) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (currentUserRole == UserRole.PEMILIK) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openActiveCashiersDialog() }
                    .testTag("report_role_presence_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (currentUserRole == UserRole.PEMILIK) Icons.Default.Shield else Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = if (currentUserRole == UserRole.PEMILIK) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentUserRole == UserRole.PEMILIK) "Akun Pemilik (Akses Hapus Transaksi)" else "Akun Kasir",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUserRole == UserRole.PEMILIK) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUserRole == UserRole.PEMILIK) "Ketuk untuk melihat detail semua perangkat kasir" else "Ketuk untuk beralih akun login",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$activeCashierLabel Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // Main Revenue & Profit Summary Cards (Row 1: Omzet & HPP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pendapatan Kotor Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pendapatan Kotor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(pendapatanKotor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${periodTransactions.size} Transaksi Penjualan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Modal Pokok Terjual (HPP) Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Modal Terjual (HPP)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(totalHpp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Total Harga Beli Produk",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Summary Cards (Row 2: Laba Kotor & Laba Bersih)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Laba Kotor Card (Omzet - HPP)
                val isLabaKotorPositive = labaKotor >= 0
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLabaKotorPositive) Color(0xFFE0F2FE) else Color(0xFFFEE2E2)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isLabaKotorPositive) Color(0xFF0369A1) else Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Laba Kotor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLabaKotorPositive) Color(0xFF0369A1) else Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(labaKotor),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLabaKotorPositive) Color(0xFF0369A1) else Color(0xFF991B1B)
                        )
                        Text(
                            text = "Omzet - Modal HPP",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLabaKotorPositive) Color(0xFF0369A1).copy(alpha = 0.8f) else Color(0xFF991B1B).copy(alpha = 0.8f)
                        )
                    }
                }

                // Pendapatan Bersih Card (Laba Kotor - Biaya Operasional)
                val isProfitPositive = pendapatanBersih >= 0
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfitPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = null,
                                tint = if (isProfitPositive) Color(0xFF166534) else Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pendapatan Bersih",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfitPositive) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.formatRupiah(pendapatanBersih),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProfitPositive) Color(0xFF166534) else Color(0xFF991B1B)
                        )
                        Text(
                            text = "Laba Bersih Akhir",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProfitPositive) Color(0xFF166534).copy(alpha = 0.8f) else Color(0xFF991B1B).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Summary Card Row 3: Ringkasan Uang Fisik di Laci Kasir vs Pendapatan Bersih
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_cash_drawer_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fisik Uang Tunai di Laci Kasir",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatRupiah(estimasiUangKasFisik),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Penjualan Tunai", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
                                Text(
                                    CurrencyFormatter.formatRupiah(penjualanTunai),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Stok Ulang Produk", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
                                Text(
                                    CurrencyFormatter.formatRupiah(pengeluaranStokUlang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4338CA),
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Biaya Operasional", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
                                Text(
                                    CurrencyFormatter.formatRupiah(pengeluaranOperasional),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (pengeluaranStokUlang > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 Pengeluaran stok produk (${CurrencyFormatter.formatRupiah(pengeluaranStokUlang)}) memotong kas laci, namun pendapatan bersih toko tetap utuh (${CurrencyFormatter.formatRupiah(pendapatanBersih)}) karena dicatat sebagai aset inventaris.",
                            fontSize = 10.sp,
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // SECTION KHUSUS: LAPORAN RINCI 7 HARI KE BELAKANG (PER HARI)
            // Menampilkan laporan per hari secara terperinci untuk evaluasi mingguan
            if (selectedPeriod == PeriodFilter.MINGGU_INI) {
                val totalOmzet7Days = weeklyDailyBreakdown.sumOf { it.totalOmzet }
                val totalHpp7Days = weeklyDailyBreakdown.sumOf { it.totalHpp }
                val totalLabaBersih7Days = weeklyDailyBreakdown.sumOf { it.labaBersih }
                val avgOmzetPerDay = if (weeklyDailyBreakdown.isNotEmpty()) totalOmzet7Days / weeklyDailyBreakdown.size else 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_7days_breakdown_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Laporan 7 Hari Terakhir",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Rincian omzet, HPP, laba & kas per hari",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "7 Hari Terakhir",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ringkasan 7 Hari (Akumulasi & Rata-rata per hari)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Total Omzet 7 Hari",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalOmzet7Days),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Rata-rata / Hari",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(avgOmzetPerDay),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1),
                                        maxLines = 1
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Laba Bersih 7 Hari",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalLabaBersih7Days),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (totalLabaBersih7Days >= 0) Color(0xFF166534) else Color(0xFFDC2626),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Kontrol Buka / Tutup Semua Rincian Hari
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    expandAllDays = if (expandAllDays == true) false else true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (expandAllDays == true) "Tutup Semua Hari" else "Buka Semua Hari",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Daftar kartu laporan per hari secara rinci
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            weeklyDailyBreakdown.forEach { daySummary ->
                                DailyReportCard(
                                    summary = daySummary,
                                    forceExpanded = expandAllDays,
                                    currentUserRole = currentUserRole,
                                    onViewReceipt = { tx -> viewModel.showReceiptForTransaction(tx) },
                                    onDeleteTransaction = { tx -> transactionToDelete = tx }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION KHUSUS: LAPORAN RINCI PER MINGGU PADA BULAN INI
            // Menampilkan evaluasi keuangan toko pada setiap minggunya di bulan ini
            if (selectedPeriod == PeriodFilter.BULAN_INI) {
                val totalOmzetMonth = monthlyWeeklyBreakdown.sumOf { it.totalOmzet }
                val totalHppMonth = monthlyWeeklyBreakdown.sumOf { it.totalHpp }
                val totalPengeluaranMonth = monthlyWeeklyBreakdown.sumOf { it.totalPengeluaran }
                val totalLabaBersihMonth = monthlyWeeklyBreakdown.sumOf { it.labaBersih }
                val avgOmzetPerWeek = if (monthlyWeeklyBreakdown.isNotEmpty()) totalOmzetMonth / monthlyWeeklyBreakdown.size else 0.0
                val bestWeek = monthlyWeeklyBreakdown.filter { it.totalOmzet > 0 }.maxByOrNull { it.totalOmzet }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monthly_weekly_breakdown_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Laporan Mingguan",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Rincian keuangan per minggu di bulan ini",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Surface(
                                color = Color(0xFFEEF2FF),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = "Bulan Ini",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4338CA),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ringkasan Akumulasi Mingguan Bulan Ini
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Omzet Bulan Ini", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(
                                            CurrencyFormatter.formatRupiah(totalOmzetMonth),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF4338CA)
                                        )
                                    }
                                    Column {
                                        Text("Rata-rata/Minggu", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(
                                            CurrencyFormatter.formatRupiah(avgOmzetPerWeek),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Laba Bersih Bulan Ini", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(
                                            CurrencyFormatter.formatRupiah(totalLabaBersihMonth),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (totalLabaBersihMonth >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                    }
                                }

                                if (bestWeek != null && bestWeek.totalOmzet > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "⭐ Penjualan Tertinggi:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${bestWeek.weekTitle} (${bestWeek.dateRangeLabel})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4338CA)
                                            )
                                        }
                                        Text(
                                            text = CurrencyFormatter.formatRupiah(bestWeek.totalOmzet),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Kontrol Buka / Tutup Semua Rincian Minggu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Setiap Minggu (${monthlyWeeklyBreakdown.size} Minggu):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedButton(
                                onClick = {
                                    expandAllWeeks = if (expandAllWeeks == true) false else true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (expandAllWeeks == true) "Tutup Semua Minggu" else "Buka Semua Minggu",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Daftar kartu laporan per minggu pada bulan ini secara rinci
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            monthlyWeeklyBreakdown.forEach { weekSummary ->
                                WeeklyReportCard(
                                    summary = weekSummary,
                                    totalMonthOmzet = totalOmzetMonth,
                                    forceExpanded = expandAllWeeks,
                                    currentUserRole = currentUserRole,
                                    onViewReceipt = { tx -> viewModel.showReceiptForTransaction(tx) },
                                    onDeleteTransaction = { tx -> transactionToDelete = tx }
                                )
                            }
                        }
                    }
                }
            }

            // Breakdown Table Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rincian Laba Rugi (${selectedPeriod.label})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ReportDetailRow("1. Total Penjualan (Omzet Kotor)", CurrencyFormatter.formatRupiah(pendapatanKotor), isPositive = true)
                    ReportDetailRow("2. Modal Pokok Produk Terjual (HPP)", "- ${CurrencyFormatter.formatRupiah(totalHpp)}", isNegative = true)
                    Text(
                        text = "* Sinkron otomatis dari harga beli produk terjual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ReportDetailRow(
                        label = "Laba Kotor Penjualan (Omzet - HPP)",
                        value = CurrencyFormatter.formatRupiah(labaKotor),
                        isBold = true,
                        valueColor = if (labaKotor >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    ReportDetailRow("3. Biaya Pengeluaran Operasional", "- ${CurrencyFormatter.formatRupiah(pengeluaranOperasional)}", isNegative = true)
                    if (pengeluaranStokUlang > 0) {
                        ReportDetailRow("4. Pengeluaran Stok Ulang Produk", CurrencyFormatter.formatRupiah(pengeluaranStokUlang), valueColor = Color(0xFF4338CA))
                        Text(
                            text = "* Masuk ke aset inventaris produk: tidak memotong pendapatan bersih, hanya memotong uang tunai laci kasir.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4338CA),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    ReportDetailRow(
                        "PENDAPATAN BERSIH AKHIR",
                        CurrencyFormatter.formatRupiah(pendapatanBersih),
                        isBold = true,
                        fontSize = 14.sp,
                        valueColor = if (pendapatanBersih >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }

            // SPECIAL NON-TUNAI & CASH RECONCILIATION REPORT
            // "Dan untuk pembayaran Non Tunai terdapat laporan khusus supaya uang tunai yang ada sesuai dengan laporan penjualan"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Laporan Khusus Uang Kas vs Non-Tunai",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Pencocokan uang tunai fisik di laci kasir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Split Cash vs Non-Cash
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tunai Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF16A34A))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Penjualan Tunai", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupiah(penjualanTunai),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        // Non-Tunai Box
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Non-Tunai (QRIS)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupiah(penjualanNonTunai),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reconciliation calculation box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Rekonsiliasi Uang Fisik di Laci Kasir:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ReportDetailRow("Modal Kas Awal Laci", "+ ${CurrencyFormatter.formatRupiah(modalKasAwal)}")
                            ReportDetailRow("Pemasukan Penjualan Tunai", "+ ${CurrencyFormatter.formatRupiah(penjualanTunai)}")
                            if (pengeluaranStokUlang > 0 && pengeluaranOperasional > 0) {
                                ReportDetailRow("Pengeluaran Operasional (Kas)", "- ${CurrencyFormatter.formatRupiah(pengeluaranOperasional)}")
                                ReportDetailRow("Pengeluaran Stok Ulang (Kas)", "- ${CurrencyFormatter.formatRupiah(pengeluaranStokUlang)}")
                            } else if (pengeluaranStokUlang > 0) {
                                ReportDetailRow("Pengeluaran Stok Ulang (Kas)", "- ${CurrencyFormatter.formatRupiah(pengeluaranStokUlang)}")
                            } else {
                                ReportDetailRow("Pengeluaran Kas Tunai", "- ${CurrencyFormatter.formatRupiah(totalPengeluaran)}")
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFCBD5E1))
                            ReportDetailRow(
                                "Fisik Uang Tunai di Laci Kasir",
                                CurrencyFormatter.formatRupiah(estimasiUangKasFisik),
                                isBold = true,
                                valueColor = Color(0xFF0F766E),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Catatan: Uang non-tunai (QRIS/Transfer) tidak dimasukkan ke dalam laci kasir melainkan masuk langsung ke rekening bank toko.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showReceiptDialog && receiptTx != null) {
        ReceiptDialog(
            settings = storeSettings,
            transaction = receiptTx!!,
            items = receiptItems,
            onDismiss = { viewModel.dismissReceiptDialog() },
            onDeleteTransaction = if (currentUserRole == UserRole.PEMILIK) {
                {
                    val toDelete = receiptTx
                    viewModel.dismissReceiptDialog()
                    transactionToDelete = toDelete
                }
            } else null
        )
    }

    // Dialog Konfirmasi Hapus Transaksi (Khusus Akun Pemilik)
    if (transactionToDelete != null) {
        val tx = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan & Hapus Transaksi?")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda ingin menghapus transaksi ${tx.invoiceNumber}?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total Transaksi: ${CurrencyFormatter.formatRupiah(tx.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Fitur Akun Pemilik: Stok barang pada transaksi ini akan otomatis dikembalikan ke inventaris toko, dan data akan disinkronkan ke seluruh kasir.",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val txToDelete = tx
                        transactionToDelete = null
                        viewModel.deleteTransaction(txToDelete, restoreStock = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_tx_report_btn")
                ) {
                    Text("Ya, Hapus & Kembalikan Stok", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { transactionToDelete = null }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun DailyReportCard(
    summary: DailyReportSummary,
    onViewReceipt: (TransactionEntity) -> Unit,
    forceExpanded: Boolean? = null,
    currentUserRole: UserRole = UserRole.KASIR,
    onDeleteTransaction: ((TransactionEntity) -> Unit)? = null
) {
    var internalExpanded by remember { mutableStateOf(summary.dayOffset == 0) }
    val expanded = forceExpanded ?: internalExpanded

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.dayOffset == 0) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (summary.dayOffset == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_card_${summary.dayOffset}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row (Dapat diklik untuk membuka rincian lengkap transaksi & beban hari tersebut)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { internalExpanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        color = if (summary.dayOffset == 0) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = summary.dayTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.dayOffset == 0) Color.White else Color(0xFF1E293B),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.formattedDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatRupiah(summary.totalOmzet),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.totalOmzet > 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.transactionCount} Transaksi",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick 4 metrics row (HPP, Laba Kotor, Pengeluaran, Laba Bersih)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("HPP Modal", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(
                        CurrencyFormatter.formatRupiah(summary.totalHpp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Laba Kotor", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(
                        CurrencyFormatter.formatRupiah(summary.labaKotor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Beban Biaya", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(
                        CurrencyFormatter.formatRupiah(summary.pengeluaranOperasional),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Laba Bersih", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1, softWrap = false)
                    Text(
                        CurrencyFormatter.formatRupiah(summary.labaBersih),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.labaBersih >= 0) Color(0xFF166534) else Color(0xFFDC2626),
                        maxLines = 1
                    )
                }
            }

            // Expanded Breakdown Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pembagian Tunai vs Non-Tunai pada hari ini
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Money,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFF166534)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tunai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                }
                                Text(
                                    CurrencyFormatter.formatRupiah(summary.penjualanTunai),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0E7FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFF3730A3)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Non-Tunai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3730A3))
                                }
                                Text(
                                    CurrencyFormatter.formatRupiah(summary.penjualanNonTunai),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3730A3)
                                )
                            }
                        }
                    }

                    // Rincian Transaksi yang terjadi pada hari ini
                    if (summary.transactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rincian Transaksi Hari Ini (${summary.transactions.size} transaksi):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.transactions.forEach { tx ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewReceipt(tx) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = DateFormatter.formatTimeOnly(tx.timestamp),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tx.invoiceNumber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (tx.paymentType == "TUNAI") Color(0xFFDCFCE7) else Color(0xFFE0E7FF),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (tx.paymentType == "TUNAI") "Tunai" else "QRIS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tx.paymentType == "TUNAI") Color(0xFF166534) else Color(0xFF3730A3),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = CurrencyFormatter.formatRupiah(tx.totalAmount),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (currentUserRole == UserRole.PEMILIK && onDeleteTransaction != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { onDeleteTransaction(tx) },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .testTag("delete_tx_btn_${tx.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Transaksi",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Rincian Pengeluaran pada hari ini
                    if (summary.expenses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rincian Pengeluaran (${summary.expenses.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.expenses.forEach { exp ->
                                val isRestock = exp.isRestock
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isRestock) Color(0xFFEEF2FF) else Color(0xFFFEF2F2),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isRestock) Color(0xFFC7D2FE) else Color(0xFFFECACA))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    exp.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isRestock) Color(0xFF3730A3) else Color(0xFF991B1B)
                                                )
                                                if (isRestock) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFF4338CA),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Stok Ulang",
                                                            fontSize = 9.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (isRestock) "${exp.category} • Potong kas laci (Aset stok)" else exp.category,
                                                fontSize = 10.sp,
                                                color = if (isRestock) Color(0xFF4338CA) else Color(0xFFB91C1C)
                                            )
                                        }
                                        Text(
                                            text = "- ${CurrencyFormatter.formatRupiah(exp.amount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRestock) Color(0xFF4338CA) else Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (summary.transactions.isEmpty() && summary.expenses.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tidak ada aktivitas transaksi atau pengeluaran pada tanggal ini.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    valueColor: Color = Color(0xFF0F172A),
    isPositive: Boolean = false,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF334155)
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                isPositive -> Color(0xFF16A34A)
                isNegative -> Color(0xFFDC2626)
                else -> valueColor
            }
        )
    }
}

@Composable
fun WeeklyReportCard(
    summary: WeeklyReportSummary,
    totalMonthOmzet: Double,
    onViewReceipt: (TransactionEntity) -> Unit,
    forceExpanded: Boolean? = null,
    currentUserRole: UserRole = UserRole.KASIR,
    onDeleteTransaction: ((TransactionEntity) -> Unit)? = null
) {
    var internalExpanded by remember { mutableStateOf(summary.isCurrentWeek) }
    val expanded = forceExpanded ?: internalExpanded

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isCurrentWeek) Color(0xFFF5F3FF) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.2.dp,
            if (summary.isCurrentWeek) Color(0xFF818CF8) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_card_${summary.weekNumber}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row (Dapat diklik untuk membuka rincian lengkap transaksi & beban minggu tersebut)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { internalExpanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (summary.isCurrentWeek) Color(0xFF6366F1) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = summary.weekTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isCurrentWeek) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = summary.dateRangeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summary.isCurrentWeek) {
                            Text(
                                text = "Minggu Berjalan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatRupiah(summary.totalOmzet),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.totalOmzet > 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.transactionCount} Transaksi",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick 4 metrics row (HPP, Laba Kotor, Pengeluaran, Laba Bersih)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("HPP Modal", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        CurrencyFormatter.formatRupiah(summary.totalHpp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
                Column {
                    Text("Laba Kotor", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        CurrencyFormatter.formatRupiah(summary.labaKotor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                }
                Column {
                    Text("Beban Biaya", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        CurrencyFormatter.formatRupiah(summary.totalPengeluaran),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Laba Bersih", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        CurrencyFormatter.formatRupiah(summary.labaBersih),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.labaBersih >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            }

            // Expanded Breakdown Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pembagian Tunai vs Non-Tunai pada minggu ini & Kontribusi Omzet
                    val percentageContribution = if (totalMonthOmzet > 0) {
                        (summary.totalOmzet / totalMonthOmzet * 100).toInt()
                    } else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kontribusi terhadap Omzet Bulan Ini:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = Color(0xFFEEF2FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$percentageContribution% dari total bulan ini",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Money,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFF166534)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tunai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                }
                                Text(
                                    CurrencyFormatter.formatRupiah(summary.penjualanTunai),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE0E7FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFF3730A3)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Non-Tunai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3730A3))
                                }
                                Text(
                                    CurrencyFormatter.formatRupiah(summary.penjualanNonTunai),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3730A3)
                                )
                            }
                        }
                    }

                    // Rincian Transaksi yang terjadi pada minggu ini
                    if (summary.transactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rincian Transaksi (${summary.transactions.size} transaksi):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.transactions.forEach { tx ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewReceipt(tx) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = DateFormatter.formatFullDateTime(tx.timestamp),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tx.invoiceNumber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (tx.paymentType == "TUNAI") Color(0xFFDCFCE7) else Color(0xFFE0E7FF),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (tx.paymentType == "TUNAI") "Tunai" else "QRIS",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tx.paymentType == "TUNAI") Color(0xFF166534) else Color(0xFF3730A3),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = CurrencyFormatter.formatRupiah(tx.totalAmount),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (currentUserRole == UserRole.PEMILIK && onDeleteTransaction != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { onDeleteTransaction(tx) },
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .testTag("delete_tx_week_${tx.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus Transaksi",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Rincian Pengeluaran pada minggu ini
                    if (summary.expenses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Pengeluaran Operasional (${summary.expenses.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            summary.expenses.forEach { exp ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                exp.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF991B1B)
                                            )
                                            Text(
                                                "${exp.category} • ${DateFormatter.formatShortDate(exp.timestamp)}",
                                                fontSize = 10.sp,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }
                                        Text(
                                            text = "- ${CurrencyFormatter.formatRupiah(exp.amount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (summary.transactions.isEmpty() && summary.expenses.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Belum ada aktivitas transaksi atau pengeluaran pada minggu ini.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
