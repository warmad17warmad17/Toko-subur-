package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.ui.viewmodel.TokoViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateFormatter

@Composable
fun CapitalExpenseScreen(
    viewModel: TokoViewModel,
    modifier: Modifier = Modifier
) {
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val totalInventoryValuation by viewModel.totalInventoryValuation.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    val modalKasAwal = storeSettings.initialCashCapital
    val totalModalToko = modalKasAwal + totalInventoryValuation

    var showEditCapitalDialog by remember { mutableStateOf(false) }
    var showAddEditExpenseDialog by remember { mutableStateOf(false) }
    var defaultIsRestock by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseFilter by remember { mutableStateOf("ALL") } // "ALL", "OPERASIONAL", "STOK"

    val operationalExpenses = remember(allExpenses) { allExpenses.filter { !it.isRestock } }
    val restockExpenses = remember(allExpenses) { allExpenses.filter { it.isRestock } }
    val totalExpenseSum = remember(allExpenses) { allExpenses.sumOf { it.amount } }
    val totalOperationalSum = remember(operationalExpenses) { operationalExpenses.sumOf { it.amount } }
    val totalRestockSum = remember(restockExpenses) { restockExpenses.sumOf { it.amount } }

    val displayedExpenses = remember(allExpenses, expenseFilter) {
        when (expenseFilter) {
            "OPERASIONAL" -> allExpenses.filter { !it.isRestock }
            "STOK" -> allExpenses.filter { it.isRestock }
            else -> allExpenses
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Modal Usaha & Pengeluaran Toko",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total modal usaha mencakup kas toko dan nilai stok produk berdasarkan harga beli",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Modal Usaha Card: "didalam modal tersebut terdapat total nilai produk yang ada berdasarkan harga beli produk"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TOTAL MODAL USAHA TOKO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showEditCapitalDialog = true },
                                    modifier = Modifier.testTag("edit_capital_btn")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Modal Kas", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = CurrencyFormatter.formatRupiah(totalModalToko),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Breakdown inside modal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Modal Kas Toko (Tunai):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(modalKasAwal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Nilai Aset Stok (Harga Beli):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalInventoryValuation),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expenses Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Daftar Pengeluaran & Biaya",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Biaya operasional & belanja stok ulang produk",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Action Buttons: "+ Stok Produk" (Fitur Pengeluaran Stok Ulang) and "+ Biaya"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    expenseToEdit = null
                                    defaultIsRestock = true
                                    showAddEditExpenseDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4338CA),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                modifier = Modifier.testTag("add_restock_expense_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Stok Produk",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    expenseToEdit = null
                                    defaultIsRestock = false
                                    showAddEditExpenseDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                                modifier = Modifier.testTag("add_expense_top_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Biaya",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3-Column Summary Cards: Total Pengeluaran, Biaya Operasional, Stok Ulang Produk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Total Keluar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text(
                                    CurrencyFormatter.formatRupiah(totalExpenseSum),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1
                                )
                                Text("${allExpenses.size} Catatan", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Operasional", fontSize = 10.sp, color = Color(0xFF991B1B), maxLines = 1)
                                Text(
                                    CurrencyFormatter.formatRupiah(totalOperationalSum),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    maxLines = 1
                                )
                                Text("Potong Laba & Kas", fontSize = 9.sp, color = Color(0xFFB91C1C), maxLines = 1)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Stok Produk", fontSize = 10.sp, color = Color(0xFF3730A3), maxLines = 1)
                                Text(
                                    CurrencyFormatter.formatRupiah(totalRestockSum),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4338CA),
                                    maxLines = 1
                                )
                                Text("Potong Kas Laci", fontSize = 9.sp, color = Color(0xFF4338CA), maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Accounting Notice Card explaining rule
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengeluaran untuk stok ulang produk memotong fisik uang tunai di laci kasir, namun TIDAK mengurangi pendapatan bersih toko karena dicatat sebagai penambahan aset stok barang.",
                                fontSize = 11.sp,
                                color = Color(0xFF166534),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips: Semua, Operasional, Stok Ulang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("ALL", "Semua (${allExpenses.size})", "filter_all_expenses"),
                            Triple("OPERASIONAL", "🏢 Operasional (${operationalExpenses.size})", "filter_operational_expenses"),
                            Triple("STOK", "📦 Stok Produk (${restockExpenses.size})", "filter_restock_expenses")
                        ).forEach { (key, label, tag) ->
                            val isSelected = expenseFilter == key
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) {
                                    if (key == "STOK") Color(0xFF4338CA) else MaterialTheme.colorScheme.primary
                                } else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                modifier = Modifier
                                    .clickable { expenseFilter = key }
                                    .testTag(tag)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (displayedExpenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (expenseFilter == "STOK") Icons.Default.Inventory2 else Icons.Default.MoneyOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (expenseFilter) {
                                    "STOK" -> "Belum Ada Pengeluaran Stok Produk"
                                    "OPERASIONAL" -> "Belum Ada Biaya Operasional"
                                    else -> "Belum Ada Catatan Pengeluaran"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (expenseFilter) {
                                    "STOK" -> "Tekan tombol '+ Stok Produk' untuk mencatat kulakan barang dagangan"
                                    "OPERASIONAL" -> "Tekan '+ Biaya' untuk mencatat listrik, sewa, gaji, kantong kresek"
                                    else -> "Catat biaya operasional toko atau belanja stok ulang produk"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedExpenses, key = { it.id }) { expense ->
                            ExpenseItemCard(
                                expense = expense,
                                onEdit = {
                                    expenseToEdit = expense
                                    defaultIsRestock = expense.isRestock
                                    showAddEditExpenseDialog = true
                                },
                                onDelete = {
                                    expenseToDelete = expense
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    // Edit Capital Dialog
    if (showEditCapitalDialog) {
        EditCapitalDialog(
            currentCapital = modalKasAwal,
            onSave = { newAmount ->
                viewModel.updateInitialCashCapital(newAmount)
                showEditCapitalDialog = false
            },
            onDismiss = { showEditCapitalDialog = false }
        )
    }

    // Add / Edit Expense Dialog
    if (showAddEditExpenseDialog) {
        AddEditExpenseDialog(
            expense = expenseToEdit,
            initialIsRestock = defaultIsRestock,
            onSave = { title, category, amount, notes, isRestock ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(title, category, amount, notes, isRestock)
                } else {
                    viewModel.updateExpense(
                        expenseToEdit!!.copy(
                            title = title,
                            category = category,
                            amount = amount,
                            notes = notes,
                            isRestock = isRestock
                        )
                    )
                }
                showAddEditExpenseDialog = false
                expenseToEdit = null
            },
            onDismiss = {
                showAddEditExpenseDialog = false
                expenseToEdit = null
            }
        )
    }

    // Delete Expense Confirmation
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Hapus Pengeluaran") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan '${expenseToDelete?.title}' sebesar ${CurrencyFormatter.formatRupiah(expenseToDelete?.amount ?: 0.0)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isRestock = expense.isRestock
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isRestock) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isRestock) 1.5.dp else 1.dp,
            if (isRestock) Color(0xFF818CF8) else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRestock) Icons.Default.Inventory2 else Icons.Default.Receipt,
                        contentDescription = null,
                        tint = if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRestock) Color(0xFF312E81) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isRestock) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isRestock) "📦 Stok Ulang Produk" else expense.category,
                            fontSize = 11.sp,
                            fontWeight = if (isRestock) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateFormatter.formatShortDate(expense.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isRestock)
                        "• Memotong kas laci • Laba bersih tidak terpotong (Aset Stok)"
                    else
                        "• Memotong laba bersih toko & kas laci",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isRestock) Color(0xFF4338CA) else Color(0xFF64748B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (expense.notes.isNotBlank()) {
                    Text(
                        text = "Catatan: ${expense.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatRupiah(expense.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.error
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_expense_${expense.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Pengeluaran",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_expense_${expense.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Pengeluaran",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditCapitalDialog(
    currentCapital: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var capitalText by remember {
        mutableStateOf(
            if (currentCapital > 0) CurrencyFormatter.formatThousand(currentCapital.toLong()) else ""
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .imePadding()
                .testTag("edit_capital_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Modal Kas Awal Toko",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Masukkan jumlah uang kas/tunai yang disiapkan di toko sebagai modal awal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = capitalText,
                    onValueChange = { capitalText = CurrencyFormatter.formatInputNominal(it, capitalText) },
                    label = { Text("Modal Kas Toko (Rp)") },
                    placeholder = { Text("Contoh: 500.000") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("capital_input_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = CurrencyFormatter.parseAmount(capitalText)
                            onSave(amount)
                        },
                        modifier = Modifier.testTag("save_capital_btn")
                    ) {
                        Text("Simpan Modal")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditExpenseDialog(
    expense: ExpenseEntity?,
    initialIsRestock: Boolean = false,
    onSave: (title: String, category: String, amount: Double, notes: String, isRestock: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isRestock by remember { mutableStateOf(expense?.isRestock ?: initialIsRestock) }
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var category by remember {
        mutableStateOf(
            expense?.category ?: if (isRestock) "Stok Ulang Produk" else "Operasional"
        )
    }
    var amountText by remember {
        mutableStateOf(
            if (expense != null && expense.amount > 0) CurrencyFormatter.formatThousand(expense.amount.toLong()) else ""
        )
    }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }

    val restockPresets = listOf(
        "Kulakan Barang Dagang",
        "Stok Ulang Minuman Dus",
        "Stok Ulang Sembako",
        "Kulakan Snack / Camilan",
        "Stok Ulang Rokok",
        "Pembelian Grosir Produk"
    )

    val operationalPresets = listOf(
        "Bayar Tagihan Listrik",
        "Beli Kantong Plastik & Kresek",
        "Bayar Sewa Toko / Kios",
        "Gaji Karyawan Toko",
        "Air Galon & Kebersihan",
        "Operasional Umum Toko"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .imePadding()
                .testTag("add_edit_expense_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (expense == null) {
                        if (isRestock) "Tambah Stok Ulang Produk" else "Tambah Biaya Operasional"
                    } else {
                        "Edit Catatan Pengeluaran"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selector: Stok Ulang Produk vs Biaya Operasional
                Text(
                    text = "Pilih Jenis Pengeluaran:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Stok Ulang Produk (Kulakan)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isRestock = true
                                if (category == "Operasional" || category.isBlank()) {
                                    category = "Stok Ulang Produk"
                                }
                            }
                            .testTag("type_restock_btn"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRestock) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            if (isRestock) 2.dp else 1.dp,
                            if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Stok Produk",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRestock) Color(0xFF312E81) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kulakan / Belanja Stok",
                                fontSize = 10.sp,
                                color = if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Potong Kas • Laba Tetap",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isRestock) Color(0xFF16A34A) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Option 2: Biaya Operasional
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isRestock = false
                                if (category == "Stok Ulang Produk" || category.isBlank()) {
                                    category = "Operasional"
                                }
                            }
                            .testTag("type_operational_btn"),
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isRestock) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            if (!isRestock) 2.dp else 1.dp,
                            if (!isRestock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (!isRestock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Operasional",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isRestock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Listrik, sewa, gaji, kresek",
                                fontSize = 10.sp,
                                color = if (!isRestock) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Potong Kas & Laba",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isRestock) MaterialTheme.colorScheme.error else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Informative Accounting Impact Callout
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRestock) Color(0xFFEEF2FF) else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, if (isRestock) Color(0xFFC7D2FE) else Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isRestock) Color(0xFF4338CA) else Color(0xFFB45309),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRestock)
                                "Pengeluaran stok produk memotong uang tunai fisik di laci kasir, namun TIDAK mengurangi pendapatan bersih toko (menjadi aset persediaan barang dagangan)."
                            else
                                "Pengeluaran biaya operasional memotong uang tunai fisik di laci kasir dan langsung mengurangi pendapatan bersih toko.",
                            fontSize = 10.sp,
                            color = if (isRestock) Color(0xFF312E81) else Color(0xFF92400E),
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Presets
                Text(
                    text = "Pilihan Cepat:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (if (isRestock) restockPresets else operationalPresets).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clickable {
                                title = preset
                                if (isRestock) {
                                    category = "Stok Ulang Produk"
                                } else {
                                    category = when {
                                        preset.contains("Listrik", true) -> "Listrik & Air"
                                        preset.contains("Plastik", true) -> "Plastik & Kresek"
                                        preset.contains("Sewa", true) -> "Sewa & Tempat"
                                        preset.contains("Gaji", true) -> "Gaji Karyawan"
                                        else -> "Operasional"
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "+ $preset",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isRestock) "Nama Pembelian / Kulakan Produk" else "Nama Pengeluaran") },
                    placeholder = { Text(if (isRestock) "Contoh: Kulakan Minuman & Snack Dus" else "Contoh: Bayar Tagihan Listrik") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori Pengeluaran") },
                    placeholder = { Text("Stok Ulang Produk / Operasional / Listrik dll") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_category_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = CurrencyFormatter.formatInputNominal(it, amountText) },
                    label = { Text("Jumlah Nominal (Rp)") },
                    placeholder = { Text("Contoh: 50.000") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_notes_input"),
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = CurrencyFormatter.parseAmount(amountText)
                            onSave(title, category, amount, notes, isRestock)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRestock) Color(0xFF4338CA) else MaterialTheme.colorScheme.primary
                        ),
                        enabled = title.isNotBlank() && amountText.isNotBlank(),
                        modifier = Modifier.testTag("save_expense_btn")
                    ) {
                        Text(if (isRestock) "Simpan Stok Ulang" else "Simpan")
                    }
                }
            }
        }
    }
}
