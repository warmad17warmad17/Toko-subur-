package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Devices
import com.example.data.auth.UserRole
import com.example.ui.components.LoginRoleDialog
import com.example.ui.components.ActiveCashiersDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ProductEntity
import com.example.data.sync.SyncStatus
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.GoogleSyncDialog
import com.example.ui.components.NoKeyboardScannerInputField
import com.example.ui.components.ReceiptDialog
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.TokoViewModel
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CashierScreen(
    viewModel: TokoViewModel,
    modifier: Modifier = Modifier
) {
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val qrInputText by viewModel.qrInputText.collectAsStateWithLifecycle()
    val qrClearTrigger by viewModel.qrClearTrigger.collectAsStateWithLifecycle()

    val currentGoogleUser by viewModel.currentGoogleUser.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val isRealtimeSyncEnabled by viewModel.isRealtimeSyncEnabled.collectAsStateWithLifecycle()
    val customDatabaseUrl by viewModel.customDatabaseUrl.collectAsStateWithLifecycle()
    val customStoreCode by viewModel.customStoreCode.collectAsStateWithLifecycle()
    val showGoogleSyncDialog by viewModel.showGoogleSyncDialog.collectAsStateWithLifecycle()

    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val activeCashierCount by viewModel.activeCashierCount.collectAsStateWithLifecycle()
    val activeCashiers by viewModel.activeCashiers.collectAsStateWithLifecycle()
    val activeCashierLabel by viewModel.activeCashierLabel.collectAsStateWithLifecycle()
    val showLoginRoleDialog by viewModel.showLoginRoleDialog.collectAsStateWithLifecycle()
    val showActiveCashiersDialog by viewModel.showActiveCashiersDialog.collectAsStateWithLifecycle()

    val showReceiptDialog by viewModel.showReceiptDialog.collectAsStateWithLifecycle()
    val receiptTx by viewModel.currentReceiptTransaction.collectAsStateWithLifecycle()
    val receiptItems by viewModel.currentReceiptItems.collectAsStateWithLifecycle()

    val paymentType by viewModel.paymentType.collectAsStateWithLifecycle()
    val paidAmountText by viewModel.paidAmountText.collectAsStateWithLifecycle()

    var showScannerDialog by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<Long?>(null) }
    var productSearchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Store Info Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_toko_subur_logo_1790711721794),
                            contentDescription = "Logo Toko Subur",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                                .testTag("cashier_store_logo"),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = storeSettings.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("cashier_store_name_text")
                            )
                            Text(
                                text = storeSettings.storeAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User Role Chip (Pemilik vs Kasir)
                        Surface(
                            onClick = { viewModel.openLoginRoleDialog() },
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentUserRole == UserRole.PEMILIK) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (currentUserRole == UserRole.PEMILIK) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("top_user_role_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentUserRole == UserRole.PEMILIK) Icons.Default.Shield else Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = if (currentUserRole == UserRole.PEMILIK) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentUserRole.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentUserRole == UserRole.PEMILIK) Color(0xFF92400E) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Real-Time Online Active Cashiers Status Chip (Mengubah "10 HP Online" menjadi Kasir 1, Kasir 2, ... Kasir 9)
                        val isOnlineSyncActive = isRealtimeSyncEnabled && (syncStatus == SyncStatus.SYNCED || syncStatus == SyncStatus.SYNCING || currentGoogleUser != null)

                        Surface(
                            onClick = { viewModel.openActiveCashiersDialog() },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isOnlineSyncActive) {
                                Color(0xFFDCFCE7)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isOnlineSyncActive) Color(0xFF16A34A).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("top_active_cashier_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOnlineSyncActive) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (isOnlineSyncActive) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                if (isOnlineSyncActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF16A34A))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = activeCashierLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnlineSyncActive) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("active_cashier_label_text")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Cart item badge
                        BadgedBox(
                            badge = {
                                if (cart.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.testTag("cart_badge")
                                    ) {
                                        Text("${cart.sumOf { it.quantity }}")
                                    }
                                }
                            }
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (cart.isNotEmpty()) {
                                        showCheckoutSheet = true
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("top_cart_checkout_button")
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupiah(cartTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fast QR Input Field
                // "Pada saat scan QR input, apabila kode QR yang di input cocok,
                // maka tambahkan produk tersebut secara otomatis ke keranjang belanja
                // kode QR input teks akan terhapus otomatis setelah produk di tambahkan ke keranjang"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoKeyboardScannerInputField(
                        value = qrInputText,
                        clearTrigger = qrClearTrigger,
                        onValueChange = { viewModel.updateQrInputText(it) },
                        onDone = { code -> viewModel.submitQrInputManual(code) },
                        onClear = { viewModel.clearQrInput() },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showScannerDialog = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("open_qr_lookup_btn")
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = "Scan / Input Kode QR",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Main Content Area: Split between Cart & Quick Catalog Picker
        Row(modifier = Modifier.weight(1f)) {
            // Left Column: Active Cart (always visible for fast checkout)
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxSize()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Cart Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keranjang Belanja (${cart.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (cart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Keranjang Kosong",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Scan kode QR atau ketuk produk untuk menambah",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Cart Items List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            items(cart, key = { it.product.id }) { item ->
                                CartItemRow(
                                    item = item,
                                    onIncrease = { viewModel.addToCart(item.product) },
                                    onDecrease = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                                    onRemove = { viewModel.removeFromCart(item.product.id) }
                                )
                            }
                        }
                    }

                    // Cart Bottom Action
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Total Belanja",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatRupiah(cartTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showCheckoutSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_button"),
                                enabled = cart.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
                            }

                            if (cart.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.clearCart() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("clear_cart_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kosongkan Keranjang",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Column: Quick Product Catalog Grid & Search
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxSize()
                    .padding(top = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                // Category Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Semua", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(allCategories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat.id,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat.id) null else cat.id
                            },
                            label = { Text(cat.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (productSearchQuery.isEmpty()) {
                                Text(
                                    text = "Cari barang...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = productSearchQuery,
                                onValueChange = { productSearchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cashier_product_search")
                            )
                        }
                        if (productSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { productSearchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Product items
                val selectableProducts = allProducts.filter { prod ->
                    val matchCat = selectedCategoryFilter == null || prod.categoryId == selectedCategoryFilter
                    val matchQuery = productSearchQuery.isBlank() ||
                            prod.name.contains(productSearchQuery, ignoreCase = true) ||
                            prod.qrCode.contains(productSearchQuery, ignoreCase = true)
                    matchCat && matchQuery
                }

                if (selectableProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada produk yang sesuai",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(selectableProducts, key = { it.id }) { product ->
                            ProductQuickCard(
                                product = product,
                                onAdd = {
                                    viewModel.addToCart(product)
                                    viewModel.clearQrInput()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Payment Checkout
    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.imePadding()
        ) {
            CheckoutSheetContent(
                cart = cart,
                cartTotal = cartTotal,
                paymentType = paymentType,
                paidAmountText = paidAmountText,
                quickNominals = storeSettings.quickNominals,
                onPaymentTypeChange = { viewModel.setPaymentType(it) },
                onPaidAmountChange = { viewModel.updatePaidAmountText(it) },
                onQuickNominalClick = { viewModel.onQuickNominalClick(it) },
                onRemoveItem = { viewModel.removeFromCart(it) },
                onCompleteCheckout = { notes ->
                    viewModel.completeCheckout(notes)
                    coroutineScope.launch {
                        sheetState.hide()
                        showCheckoutSheet = false
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showCheckoutSheet = false
                    }
                }
            )
        }
    }

    // Barcode Quick Scanner / Lookup Modal
    if (showScannerDialog) {
        BarcodeScannerDialog(
            products = allProducts,
            onCodeScanned = { code ->
                viewModel.checkAndAutoAddQr(code)
                viewModel.clearQrInput()
            },
            onDismiss = {
                showScannerDialog = false
                viewModel.clearQrInput()
                keyboardController?.hide()
            }
        )
    }

    // Receipt Dialog
    if (showReceiptDialog && receiptTx != null) {
        ReceiptDialog(
            settings = storeSettings,
            transaction = receiptTx!!,
            items = receiptItems,
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }

    // Google Account & Real-Time Sync Dialog
    if (showGoogleSyncDialog) {
        GoogleSyncDialog(
            currentUser = currentGoogleUser,
            syncStatus = syncStatus,
            syncMessage = syncMessage,
            lastSyncTime = lastSyncTime,
            isRealtimeSyncEnabled = isRealtimeSyncEnabled,
            onToggleRealtimeSync = { viewModel.setRealtimeSyncEnabled(it) },
            onSignInGoogle = { viewModel.signInWithGoogle() },
            onSignInManual = { email, name -> viewModel.signInWithEmailDirect(email, name) },
            onSignOut = { viewModel.signOutGoogle() },
            onForceSyncAll = { viewModel.syncAllLocalToCloud() },
            onDismiss = { viewModel.dismissGoogleSyncDialog() },
            customDatabaseUrl = customDatabaseUrl,
            customStoreCode = customStoreCode,
            onUpdateCustomDatabaseUrl = { viewModel.updateCustomDatabaseUrl(it) },
            onUpdateCustomStoreCode = { viewModel.updateCustomStoreCode(it) }
        )
    }

    // Dialog Pemilihan Peran (Pemilik vs Kasir)
    if (showLoginRoleDialog) {
        LoginRoleDialog(
            currentRole = currentUserRole,
            onLoginOwner = { password -> viewModel.loginAsOwner(password) },
            onLoginCashier = { viewModel.loginAsCashier() },
            onDismiss = { viewModel.dismissLoginRoleDialog() }
        )
    }

    // Dialog Status Kasir-Kasir yang Sedang Aktif Real-Time
    if (showActiveCashiersDialog) {
        ActiveCashiersDialog(
            activeCount = activeCashierCount,
            activeCashiers = activeCashiers,
            currentUserRole = currentUserRole,
            currentDeviceId = viewModel.authRoleManager.deviceId,
            onDismiss = { viewModel.dismissActiveCashiersDialog() }
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("cart_item_${item.product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Delete item button: "Pada rincian pembayaran juga bisa diedit untuk menghapus pesanan barang jika ada pesanan yang batal"
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("remove_item_btn_${item.product.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus Barang",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CurrencyFormatter.formatRupiah(item.subtotal),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Stepper: - [Qty] +
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp)
                    )
                ) {
                    IconButton(
                        onClick = onDecrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Kurang",
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = onIncrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Tambah",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductQuickCard(
    product: ProductEntity,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.stok > 0, onClick = onAdd)
            .testTag("catalog_product_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Top Row: Product Name & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = CurrencyFormatter.formatRupiah(product.hargaJual),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Row: Horizontal QR Code badge, Category, and Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // QR Code horizontal badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.qrCode,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = product.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (product.stok <= 0) {
                    Text(
                        text = "Habis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                } else if (product.stok <= product.minimumStokAlert) {
                    Text(
                        text = "Sisa ${product.stok}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Stok: ${product.stok}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckoutSheetContent(
    cart: List<CartItem>,
    cartTotal: Double,
    paymentType: String,
    paidAmountText: String,
    quickNominals: String,
    onPaymentTypeChange: (String) -> Unit,
    onPaidAmountChange: (String) -> Unit,
    onQuickNominalClick: (String) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCompleteCheckout: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isNonTunai = paymentType == "NON_TUNAI"
    val paidAmount = if (isNonTunai) cartTotal else CurrencyFormatter.parseAmount(paidAmountText)
    val changeAmount = if (isNonTunai) 0.0 else (paidAmount - cartTotal).coerceAtLeast(0.0)
    val underpaidAmount = (cartTotal - paidAmount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .imePadding()
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pembayaran Kasir",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Total Bill Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL BELANJA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(cartTotal),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Method Selector: Tunai vs Non-Tunai
        Text(
            text = "Pilih Metode Pembayaran:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tunai Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPaymentTypeChange("TUNAI") }
                    .testTag("pay_method_tunai"),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isNonTunai) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Money,
                        contentDescription = null,
                        tint = if (!isNonTunai) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tunai (Cash)",
                        fontWeight = FontWeight.Bold,
                        color = if (!isNonTunai) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Non Tunai Button
            // "Serta tambahkan pembayaran Non Tunai dimana pembayaran Non Tunai Nominal yang dibayarkan Sesuai atau sama dengan harga produk yang harus di bayar"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPaymentTypeChange("NON_TUNAI") }
                    .testTag("pay_method_non_tunai"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNonTunai) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = if (isNonTunai) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Non-Tunai",
                        fontWeight = FontWeight.Bold,
                        color = if (isNonTunai) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isNonTunai) {
            // Non-Tunai Explanation Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pembayaran Non-Tunai (QRIS / EDC / Transfer)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Nominal otomatis pas sesuai total: ${CurrencyFormatter.formatRupiah(cartTotal)}. Tidak ada kembalian.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            // Tunai Fast Chips & Input
            Text(
                text = "Pilihan Nominal Cepat (Klik untuk menambah '000'):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fast Buttons: "Uang Pas 10 20 30 50 100 dimana nominal tersebut ketika di klik akan di tambahkan 000 pada saat pembayaran Contoh klik 10 maka akan menjadi 10000"
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Uang Pas
                OutlinedButton(
                    onClick = { onQuickNominalClick("PAS") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("quick_nominal_pas")
                ) {
                    Text("Uang Pas", fontWeight = FontWeight.Bold)
                }

                val nominalsList = quickNominals.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (code in nominalsList) {
                    OutlinedButton(
                        onClick = { onQuickNominalClick(code) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("quick_nominal_$code")
                    ) {
                        Text(code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Editable Nominal Input: "Nominal yang ditampilkan bisa diedit angkanya sebelum pembayaran"
            OutlinedTextField(
                value = paidAmountText,
                onValueChange = onPaidAmountChange,
                label = { Text("Nominal Uang Tunai Diterima (Rp)") },
                placeholder = { Text("Contoh: 50.000") },
                prefix = { Text("Rp ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paid_amount_text_field"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Change / Kembalian or Underpaid Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (underpaidAmount > 0) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (underpaidAmount > 0) {
                        Text(
                            text = "Uang Kurang:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(underpaidAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        Text(
                            text = "Kembalian:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(changeAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Order Summary items (can remove item if cancelled)
        Text(
            text = "Rincian Barang Belanja (Ketuk hapus jika batal):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            for (cartItem in cart) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cartItem.product.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${cartItem.quantity}x @ ${CurrencyFormatter.formatRupiah(cartItem.product.hargaJual)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyFormatter.formatRupiah(cartItem.subtotal),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onRemoveItem(cartItem.product.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Batalkan Barang",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Complete Payment Button
        val canPay = isNonTunai || (paidAmount >= cartTotal && cartTotal > 0)
        Button(
            onClick = {
                onCompleteCheckout("")
            },
            enabled = canPay,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_payment_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Simpan & Cetak Struk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
