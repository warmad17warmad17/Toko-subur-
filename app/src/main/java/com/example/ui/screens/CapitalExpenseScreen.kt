package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
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
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

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
                        Text(
                            text = "Daftar Pengeluaran & Biaya",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                expenseToEdit = null
                                showAddEditExpenseDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("add_expense_top_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tambah",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Total Pengeluaran: ${CurrencyFormatter.formatRupiah(allExpenses.sumOf { it.amount })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (allExpenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MoneyOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum Ada Catatan Pengeluaran",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Catat pengeluaran seperti listrik, sewa, kresek, gaji di sini",
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
                        items(allExpenses, key = { it.id }) { expense ->
                            ExpenseItemCard(
                                expense = expense,
                                onEdit = {
                                    expenseToEdit = expense
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
            onSave = { title, category, amount, notes ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(title, category, amount, notes)
                } else {
                    viewModel.updateExpense(
                        expenseToEdit!!.copy(
                            title = title,
                            category = category,
                            amount = amount,
                            notes = notes
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = expense.category,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateFormatter.formatShortDate(expense.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.formatRupiah(expense.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
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
    onSave: (title: String, category: String, amount: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: "Operasional") }
    var amountText by remember {
        mutableStateOf(
            if (expense != null && expense.amount > 0) CurrencyFormatter.formatThousand(expense.amount.toLong()) else ""
        )
    }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }

    val defaultCategories = listOf("Operasional", "Listrik & Air", "Sewa & Tempat", "Plastik & Kresek", "Gaji Karyawan", "Lain-lain")

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
                    text = if (expense == null) "Tambah Pengeluaran" else "Edit Pengeluaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Pengeluaran") },
                    placeholder = { Text("Contoh: Bayar Tagihan Listrik") },
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori Pengeluaran") },
                    placeholder = { Text("Operasional / Listrik / Sewa dll") },
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
                            onSave(title, category, amount, notes)
                        },
                        enabled = title.isNotBlank() && amountText.isNotBlank(),
                        modifier = Modifier.testTag("save_expense_btn")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
