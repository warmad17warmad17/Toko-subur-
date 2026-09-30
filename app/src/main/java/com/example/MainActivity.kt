package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.CapitalExpenseScreen
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.FinancialReportScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SettingsBackupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TokoViewModel
import kotlinx.coroutines.flow.collectLatest

enum class MainNavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    KASIR("Kasir", Icons.Default.PointOfSale, "nav_kasir"),
    PRODUK("Produk", Icons.Default.Inventory, "nav_produk"),
    RIWAYAT("Riwayat", Icons.AutoMirrored.Filled.ReceiptLong, "nav_riwayat"),
    LAPORAN("Laporan", Icons.Default.Assessment, "nav_laporan"),
    MODAL("Modal", Icons.Default.AccountBalanceWallet, "nav_modal"),
    PENGATURAN("Pengaturan", Icons.Default.Settings, "nav_pengaturan")
}

class MainActivity : ComponentActivity() {

    private val viewModel: TokoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sembunyikan gambar aplikasi dari Galeri HP (tempatkan .nomedia di direktori aplikasi)
        com.example.util.NoMediaHelper.hideAppImagesFromGallery(this)

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var currentDestination by remember { mutableStateOf(MainNavDestination.KASIR) }

                // Permintaan izin notifikasi runtime untuk Android 13+ (API 33+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.setNotificationEnabled(true)
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!isGranted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Periksa apakah aplikasi dibuka dari klik notifikasi stok menipis
                LaunchedEffect(intent) {
                    if (intent?.getStringExtra("navigate_to") == "PRODUK") {
                        currentDestination = MainNavDestination.PRODUK
                        if (intent.getBooleanExtra("filter_low_stock", false)) {
                            viewModel.setLowStockFilter(true)
                        }
                    }
                }

                // Listen for messages from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collectLatest { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                // If not in Kasir, Back button returns to Kasir
                if (currentDestination != MainNavDestination.KASIR) {
                    BackHandler {
                        currentDestination = MainNavDestination.KASIR
                    }
                }

                @OptIn(ExperimentalLayoutApi::class)
                val isImeVisible = WindowInsets.isImeVisible

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isImeVisible) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                MainNavDestination.values().forEach { destination ->
                                    val selected = currentDestination == destination
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentDestination = destination },
                                        icon = {
                                            Icon(
                                                destination.icon,
                                                contentDescription = destination.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = destination.title,
                                                fontSize = 10.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(destination.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val screenModifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                    when (currentDestination) {
                        MainNavDestination.KASIR -> CashierScreen(viewModel = viewModel, modifier = screenModifier)
                        MainNavDestination.PRODUK -> ProductsScreen(viewModel = viewModel, modifier = screenModifier)
                        MainNavDestination.RIWAYAT -> HistoryScreen(viewModel = viewModel, modifier = screenModifier)
                        MainNavDestination.LAPORAN -> FinancialReportScreen(viewModel = viewModel, modifier = screenModifier)
                        MainNavDestination.MODAL -> CapitalExpenseScreen(viewModel = viewModel, modifier = screenModifier)
                        MainNavDestination.PENGATURAN -> SettingsBackupScreen(viewModel = viewModel, modifier = screenModifier)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getStringExtra("navigate_to") == "PRODUK") {
            viewModel.setLowStockFilter(intent.getBooleanExtra("filter_low_stock", false))
        }
    }
}
