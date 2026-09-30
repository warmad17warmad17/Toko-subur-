package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TopSellingProduct
import com.example.ui.components.PromoExclusionManagerDialog
import com.example.ui.viewmodel.TokoViewModel
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: TokoViewModel,
    modifier: Modifier = Modifier
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allProductsList by viewModel.allProducts.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val totalInventoryValuation by viewModel.totalInventoryValuation.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockProductCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedProductCategoryFilter.collectAsStateWithLifecycle()
    val lowStockFilterActive by viewModel.lowStockFilterActive.collectAsStateWithLifecycle()

    val topSellingProducts by viewModel.topSellingProductsInOneWeek.collectAsStateWithLifecycle()
    val topSellingLimit by viewModel.topSellingLimit.collectAsStateWithLifecycle()
    val excludedPromoProducts by viewModel.excludedPromoProducts.collectAsStateWithLifecycle()

    var selectedCatalogTab by remember { mutableStateOf(0) } // 0 = Semua Produk, 1 = Terlaris
    var showPromoExclusionManagerDialog by remember { mutableStateOf(false) }
    var showEditTopSellingLimitDialog by remember { mutableStateOf(false) }

    var showAddEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    var showCategoryManagementDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Inventory Valuation & Summary Header
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
                        Column {
                            Text(
                                text = "Katalog & Stok Barang",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kelola harga beli, harga jual, stok & kode QR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showCategoryManagementDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("manage_categories_btn")
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Katalog (${allCategories.size})", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inventory Valuation Banner (Berdasarkan Harga Beli)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Total Nilai Aset Stok (Harga Beli):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalInventoryValuation),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (lowStockCount > 0) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.clickable { viewModel.toggleLowStockFilter() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$lowStockCount Menipis",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Segmented Tab: Katalog Semua Produk vs 10 Produk Terlaris Minggu Ini
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                // Tab 0: Semua Produk
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedCatalogTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selectedCatalogTab = 0 }
                        .padding(vertical = 10.dp)
                        .testTag("tab_all_products"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedCatalogTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Semua Produk (${filteredProducts.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedCatalogTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCatalogTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: Terlaris Minggu Ini
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedCatalogTab == 1) Color(0xFFFEF3C7) else Color.Transparent)
                        .clickable { selectedCatalogTab = 1 }
                        .padding(vertical = 10.dp)
                        .testTag("tab_top_selling_products"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedCatalogTab == 1) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔥 $topSellingLimit Terlaris (1 Minggu)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedCatalogTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedCatalogTab == 1) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val excludedIds = remember(excludedPromoProducts) { excludedPromoProducts.map { it.productId }.toSet() }
            val topRankMap = remember(topSellingProducts) { topSellingProducts.associate { it.productId to it.rank } }

            if (selectedCatalogTab == 0) {
                // Filters & Search Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari berdasarkan nama atau kode QR...",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setProductSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setProductSearchQuery("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Category Filter Chips + Low Stock Warning Filter
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null && !lowStockFilterActive,
                                onClick = {
                                    viewModel.setSelectedCategoryFilter(null)
                                    if (lowStockFilterActive) viewModel.toggleLowStockFilter()
                                },
                                label = { Text("Semua Katalog") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        // Low Stock Alert Chip
                        item {
                            FilterChip(
                                selected = lowStockFilterActive,
                                onClick = { viewModel.toggleLowStockFilter() },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (lowStockFilterActive) Color.White else Color(0xFFD97706)
                                    )
                                },
                                label = {
                                    Text("Peringatan Stok Menipis (${lowStockCount})")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD97706),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        items(allCategories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.id,
                                onClick = {
                                    viewModel.setSelectedCategoryFilter(
                                        if (selectedCategoryFilter == cat.id) null else cat.id
                                    )
                                },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Products List
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada produk yang ditemukan",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gunakan tombol + di bawah untuk menambah produk baru",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                isPromoExcluded = product.id in excludedIds,
                                topRank = topRankMap[product.id],
                                onEdit = {
                                    productToEdit = product
                                    showAddEditProductDialog = true
                                },
                                onDelete = {
                                    productToDelete = product
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            } else {
                // KATALOG 10 PRODUK TERLARIS DALAM SATU MINGGU
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header Banner 10 Terlaris
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("banner_top_selling_header"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF59E0B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Katalog $topSellingLimit Produk Terlaris",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Periode 1 Minggu Terakhir (7 Hari) • Kuota: $topSellingLimit produk",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Info Pengecualian Promo & Tombol Atur Kuota
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (excludedPromoProducts.isNotEmpty()) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (excludedPromoProducts.isNotEmpty()) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { showPromoExclusionManagerDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (excludedPromoProducts.isNotEmpty())
                                                "${excludedPromoProducts.size} Promo Dikecualikan"
                                            else "0 Promo Dikecualikan",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { showEditTopSellingLimitDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF92400E)),
                                        border = BorderStroke(1.dp, Color(0xFFD97706)),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("btn_edit_top_selling_limit")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = Color(0xFFB45309)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Atur Kuota ($topSellingLimit)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showPromoExclusionManagerDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp).testTag("btn_manage_promo_from_top_selling")
                                    ) {
                                        Text("Kelola Promo", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // Daftar 10 Produk Terlaris
                    if (topSellingProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Belum Ada Penjualan dalam 1 Minggu",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lakukan transaksi kasir di Toko Subur. Produk yang paling laris terjual dalam 7 hari terakhir akan otomatis tampil di sini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(topSellingProducts, key = { it.productId }) { item ->
                                TopSellingProductItemCard(
                                    item = item,
                                    isPromoExcluded = item.productId in excludedIds,
                                    onEdit = {
                                        productToEdit = item.product ?: allProductsList.firstOrNull { it.id == item.productId }
                                        if (productToEdit != null) {
                                            showAddEditProductDialog = true
                                        }
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

        // Floating Action Button to Add Product (hidden when typing)
        @OptIn(ExperimentalLayoutApi::class)
        val isImeVisible = WindowInsets.isImeVisible
        if (!isImeVisible) {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddEditProductDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk Baru")
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditProductDialog) {
        AddEditProductDialog(
            categories = allCategories,
            product = productToEdit,
            onSave = { name, catId, catName, qr, beli, jual, stok, minAlert ->
                if (productToEdit == null) {
                    viewModel.addProduct(name, catId, catName, qr, beli, jual, stok, minAlert)
                } else {
                    viewModel.updateProduct(
                        productToEdit!!.copy(
                            name = name,
                            categoryId = catId,
                            categoryName = catName,
                            qrCode = qr,
                            hargaBeli = beli,
                            hargaJual = jual,
                            stok = stok,
                            minimumStokAlert = minAlert
                        )
                    )
                }
                showAddEditProductDialog = false
                productToEdit = null
            },
            onDismiss = {
                showAddEditProductDialog = false
                productToEdit = null
            }
        )
    }

    // Delete Product Confirmation
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus '${productToDelete?.name}' dari daftar produk?") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { viewModel.deleteProduct(it) }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Manage Categories Dialog
    if (showCategoryManagementDialog) {
        CategoryManagementDialog(
            categories = allCategories,
            products = filteredProducts,
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showCategoryManagementDialog = false }
        )
    }

    // Promo Exclusion Dialog for Terlaris
    if (showPromoExclusionManagerDialog) {
        PromoExclusionManagerDialog(
            excludedList = excludedPromoProducts,
            allProducts = allProductsList,
            onAddExclusion = { prod, reason ->
                viewModel.addExcludedPromoProduct(prod, reason)
            },
            onRemoveExclusion = { pId ->
                viewModel.removeExcludedPromoProduct(pId)
            },
            onDismiss = { showPromoExclusionManagerDialog = false }
        )
    }

    // Dialog Sesuaikan Jumlah Produk Terlaris (Misal dari 10 menjadi 20)
    if (showEditTopSellingLimitDialog) {
        EditTopSellingLimitDialog(
            currentLimit = topSellingLimit,
            onSaveLimit = { newLimit ->
                viewModel.setTopSellingLimit(newLimit)
                showEditTopSellingLimitDialog = false
            },
            onDismiss = { showEditTopSellingLimitDialog = false }
        )
    }
}

@Composable
private fun TopSellingProductItemCard(
    item: TopSellingProduct,
    isPromoExcluded: Boolean,
    onEdit: () -> Unit
) {
    val rankBadgeBg = when (item.rank) {
        1 -> Color(0xFFFEF3C7) // Gold
        2 -> Color(0xFFF1F5F9) // Silver
        3 -> Color(0xFFFFEDD5) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val rankBadgeBorder = when (item.rank) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFEA580C) // Bronze
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val rankBadgeTextColor = when (item.rank) {
        1 -> Color(0xFF92400E)
        2 -> Color(0xFF475569)
        3 -> Color(0xFF9A3412)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val rankLabel = when (item.rank) {
        1 -> "🥇 #1"
        2 -> "🥈 #2"
        3 -> "🥉 #3"
        else -> "#${item.rank}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_selling_card_${item.rank}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, rankBadgeBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Rank Badge, Name, Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(rankBadgeBg)
                            .border(1.5.dp, rankBadgeBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rankLabel,
                            fontSize = if (item.rank <= 3) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankBadgeTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.categoryName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.qrCode.isNotBlank()) {
                                Text(
                                    text = " • ${item.qrCode}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("edit_top_selling_${item.productId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Produk",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Highlight Statistik Penjualan: JUMLAH TOTAL TERJUAL & TOTAL OMZET
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jumlah Total Terjual (Highlighted in blue/primary container)
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Terjual: ${item.totalSoldQuantity} pcs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

                // Total Omzet
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Omzet (1 Minggu):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(item.totalRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Harga Jual & Sisa Stok
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Harga Jual: ${CurrencyFormatter.formatRupiah(item.currentPrice)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val stockColor = if (item.currentStock <= 5) Color(0xFFD97706) else Color(0xFF16A34A)
                Text(
                    text = "Sisa Stok: ${item.currentStock} pcs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stockColor
                )
            }
        }
    }
}

@Composable
private fun ProductItemCard(
    product: ProductEntity,
    isPromoExcluded: Boolean = false,
    topRank: Int? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stok <= product.minimumStokAlert
    val isOutOfStock = product.stok <= 0
    val margin = product.hargaJual - product.hargaBeli

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOutOfStock) MaterialTheme.colorScheme.error
            else if (isLowStock) Color(0xFFF59E0B)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.categoryName,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (topRank != null) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🔥 #$topRank Terlaris",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isPromoExcluded) {
                            Surface(
                                color = Color(0xFFF3E8FF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🏷️ Promo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = product.qrCode,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_product_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Produk",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_product_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Produk",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Prices & Stock details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Harga Beli & Harga Jual
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Jual: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(product.hargaJual),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Beli: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(product.hargaBeli),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Laba +${CurrencyFormatter.formatRupiah(margin)})",
                            fontSize = 11.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Stock Badge: "pada stok produk terdapat fitur peringatan untuk jumlah stok produk yang tinggal sedikit"
                Surface(
                    color = when {
                        isOutOfStock -> Color(0xFFFEE2E2)
                        isLowStock -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLowStock) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOutOfStock) Color(0xFFDC2626) else Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when {
                                isOutOfStock -> "Stok Habis (0)"
                                isLowStock -> "Stok Menipis (${product.stok})"
                                else -> "Stok: ${product.stok}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isOutOfStock -> Color(0xFF991B1B)
                                isLowStock -> Color(0xFF92400E)
                                else -> Color(0xFF166534)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditProductDialog(
    categories: List<CategoryEntity>,
    product: ProductEntity?,
    onSave: (
        name: String,
        categoryId: Long,
        categoryName: String,
        qrCode: String,
        hargaBeli: Double,
        hargaJual: Double,
        stok: Int,
        minimumStokAlert: Int
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == product?.categoryId } ?: categories.firstOrNull()
        )
    }
    var qrCode by remember { mutableStateOf(product?.qrCode ?: "") }
    var hargaBeliStr by remember {
        mutableStateOf(
            if (product != null && product.hargaBeli > 0) CurrencyFormatter.formatThousand(product.hargaBeli.toLong()) else ""
        )
    }
    var hargaJualStr by remember {
        mutableStateOf(
            if (product != null && product.hargaJual > 0) CurrencyFormatter.formatThousand(product.hargaJual.toLong()) else ""
        )
    }

    val isEditMode = product != null
    val existingStock = product?.stok ?: 0
    // Pada menu edit stok produk: jumlah stok produk yang ada akan di tambah dengan stok produk baru datang
    // Misal stok ada 5 pcs, diinput 10 pcs -> total jadi 15 pcs!
    var isIncomingAdditionMode by remember { mutableStateOf(isEditMode) }
    var incomingStockStr by remember { mutableStateOf("") }
    var manualTotalStockStr by remember { mutableStateOf(product?.stok?.toString() ?: "10") }
    var minStokAlertStr by remember { mutableStateOf(product?.minimumStokAlert?.toString() ?: "5") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .imePadding()
                .testTag("add_edit_product_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (product == null) "Tambah Produk Baru" else "Edit Produk & Stok",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    placeholder = { Text("Contoh: Beras Rojolele 5kg") },
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Pilih Katalog...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Katalog Produk") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("category_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        for (cat in categories) {
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // QR Code
                OutlinedTextField(
                    value = qrCode,
                    onValueChange = { qrCode = it },
                    label = { Text("Kode QR / Barcode") },
                    placeholder = { Text("Contoh: 8991001") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("product_qr_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Prices: Harga Beli & Harga Jual (Format Otomatis Titik Pemisah Ribuan)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hargaBeliStr,
                        onValueChange = { hargaBeliStr = CurrencyFormatter.formatInputNominal(it, hargaBeliStr) },
                        label = { Text("Harga Beli (Rp)") },
                        placeholder = { Text("Contoh: 10.000") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_harga_beli_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hargaJualStr,
                        onValueChange = { hargaJualStr = CurrencyFormatter.formatInputNominal(it, hargaJualStr) },
                        label = { Text("Harga Jual (Rp)") },
                        placeholder = { Text("Contoh: 12.500") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_harga_jual_input"),
                        singleLine = true
                    )
                }

                // Live Margin Preview
                val currentBeli = CurrencyFormatter.parseAmount(hargaBeliStr)
                val currentJual = CurrencyFormatter.parseAmount(hargaJualStr)
                if (currentJual > 0) {
                    val margin = currentJual - currentBeli
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (margin >= 0) "Estimasi Laba: " else "Estimasi Rugi: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(margin),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (margin >= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stock Management Section
                if (isEditMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kelola Stok Produk",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = if (existingStock <= 5) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Stok Saat Ini: $existingStock pcs",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (existingStock <= 5) Color(0xFF92400E) else Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Pilihan Mode: Tambah Stok Masuk vs Ubah Total Manual
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isIncomingAdditionMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { isIncomingAdditionMode = true }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Tambah Stok Baru",
                                        fontSize = 11.sp,
                                        fontWeight = if (isIncomingAdditionMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isIncomingAdditionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (!isIncomingAdditionMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { isIncomingAdditionMode = false }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "= Ubah Total Manual",
                                        fontSize = 11.sp,
                                        fontWeight = if (!isIncomingAdditionMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isIncomingAdditionMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isIncomingAdditionMode) {
                                val incoming = incomingStockStr.toIntOrNull() ?: 0
                                val computedFinalStock = existingStock + incoming

                                OutlinedTextField(
                                    value = incomingStockStr,
                                    onValueChange = { incomingStockStr = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Jumlah Stok Baru Datang") },
                                    placeholder = { Text("Misal: 10 (otomatis jadi ${existingStock + 10} pcs)") },
                                    prefix = { Text("+ ", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)) },
                                    suffix = { Text("pcs") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product_incoming_stock_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Tombol Cepat Tambah Stok Masuk (+5, +10, +20, +50, +100)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(5, 10, 20, 50, 100).forEach { addQty ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val cur = incomingStockStr.toIntOrNull() ?: 0
                                                    incomingStockStr = (cur + addQty).toString()
                                                }
                                        ) {
                                            Text(
                                                text = "+$addQty",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Kartu Perhitungan Real-Time
                                Surface(
                                    color = if (incoming > 0) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (incoming > 0) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (incoming > 0) {
                                            Text(
                                                text = "Perhitungan Penjumlahan Stok Otomatis:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$existingStock pcs (stok lama) + $incoming pcs (baru datang) = $computedFinalStock pcs",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF065F46)
                                            )
                                            Text(
                                                text = "Total Stok yang disimpan: $computedFinalStock pcs",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF16A34A)
                                            )
                                        } else {
                                            Text(
                                                text = "💡 Masukkan jumlah stok produk yang baru datang. Jumlah akan otomatis ditambahkan ke stok yang sudah ada ($existingStock pcs).",
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                            Text(
                                                text = "Total stok saat ini: $existingStock pcs (tidak ada penambahan)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = manualTotalStockStr,
                                    onValueChange = { manualTotalStockStr = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Koreksi Total Stok Manual") },
                                    placeholder = { Text("Contoh: 15") },
                                    suffix = { Text("pcs") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product_stok_input"),
                                    singleLine = true,
                                    supportingText = {
                                        Text("Ubah langsung total stok jika ada penyesuaian fisik (stok opname)")
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Produk Baru: Input Stok Awal
                    OutlinedTextField(
                        value = manualTotalStockStr,
                        onValueChange = { manualTotalStockStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Jumlah Stok Awal") },
                        placeholder = { Text("Contoh: 10") },
                        suffix = { Text("pcs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_stok_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Peringatan Batas Menipis
                OutlinedTextField(
                    value = minStokAlertStr,
                    onValueChange = { minStokAlertStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Peringatan Batas Stok Menipis") },
                    placeholder = { Text("Contoh: 5") },
                    suffix = { Text("pcs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("product_min_alert_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val beli = CurrencyFormatter.parseAmount(hargaBeliStr)
                            val jual = CurrencyFormatter.parseAmount(hargaJualStr)
                            val finalStok = if (isEditMode) {
                                if (isIncomingAdditionMode) {
                                    val inc = incomingStockStr.toIntOrNull() ?: 0
                                    existingStock + inc
                                } else {
                                    manualTotalStockStr.toIntOrNull() ?: existingStock
                                }
                            } else {
                                manualTotalStockStr.toIntOrNull() ?: 10
                            }
                            val minAlert = minStokAlertStr.toIntOrNull() ?: 5
                            val catId = selectedCategory?.id ?: 0L
                            val catName = selectedCategory?.name ?: "Umum"

                            onSave(name, catId, catName, qrCode, beli, jual, finalStok, minAlert)
                        },
                        enabled = name.isNotBlank() && selectedCategory != null && qrCode.isNotBlank(),
                        modifier = Modifier.testTag("save_product_btn")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryManagementDialog(
    categories: List<CategoryEntity>,
    products: List<ProductEntity>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .imePadding()
                .testTag("category_management_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kelola Katalog",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Text(
                    text = "Bisa menambah katalog baru atau menghapus katalog yang sudah tidak memiliki produk di dalamnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Add Category Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = {
                            newCategoryName = it
                            errorMessage = null
                        },
                        label = { Text("Nama Katalog Baru") },
                        placeholder = { Text("Contoh: Bumbu Masak") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        modifier = Modifier.testTag("add_category_btn")
                    ) {
                        Text("Tambah")
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Daftar Katalog Tersedia:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (cat in categories) {
                        val productCountInCat = products.count { it.categoryId == cat.id }
                        val canDelete = productCountInCat == 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$productCountInCat produk di katalog ini",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (canDelete) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (canDelete) {
                                    IconButton(
                                        onClick = { onDeleteCategory(cat) },
                                        modifier = Modifier.testTag("delete_category_${cat.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Katalog Kosong",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Ada Produk",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditTopSellingLimitDialog(
    currentLimit: Int,
    onSaveLimit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var limitInput by remember { mutableStateOf(currentLimit.toString()) }
    val presets = listOf(5, 10, 15, 20, 25, 30, 50)
    val parsed = limitInput.toIntOrNull() ?: currentLimit

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .imePadding()
                .testTag("edit_top_selling_limit_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sesuaikan Jumlah Terlaris",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Katalog Produk Terlaris 1 Minggu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tentukan berapa banyak produk terlaris dalam 1 minggu yang ingin ditampilkan pada katalog (misal dari 10 menjadi 20 produk terlaris):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pilihan Cepat (Presets)
                Text(
                    text = "Pilihan Cepat:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(4).forEach { p ->
                        val isSelected = parsed == p
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { limitInput = p.toString() },
                            color = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFFD97706)) else null
                        ) {
                            Text(
                                text = "$p",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.drop(4).forEach { p ->
                        val isSelected = parsed == p
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { limitInput = p.toString() },
                            color = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFFD97706)) else null
                        ) {
                            Text(
                                text = "$p",
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Input Field
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jumlah Produk Terlaris Ditampilkan") },
                    placeholder = { Text("Contoh: 20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_top_selling_limit_input"),
                    singleLine = true,
                    supportingText = {
                        Text("Bisa diatur dari 1 sampai 100 produk terlaris")
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Preview Card
                val validNumber = (limitInput.toIntOrNull() ?: currentLimit).coerceIn(1, 100)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Katalog akan menampilkan peringkat #1 sampai #$validNumber produk terlaris.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newLimit = (limitInput.toIntOrNull() ?: currentLimit).coerceIn(1, 100)
                            onSaveLimit(newLimit)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.testTag("save_top_selling_limit_btn")
                    ) {
                        Text("Terapkan ($validNumber)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
