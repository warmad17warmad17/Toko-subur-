package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.auth.UserRole
import com.example.ui.components.LoginRoleDialog
import com.example.ui.components.ActiveCashiersDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.sync.SyncStatus
import com.example.ui.components.GoogleSyncDialog
import com.example.ui.components.NotificationSoundPickerDialog
import com.example.ui.components.PromoExclusionManagerDialog
import com.example.ui.components.AddPromoExclusionPickerDialog
import com.example.ui.viewmodel.TokoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsBackupScreen(
    viewModel: TokoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()

    var storeName by remember(storeSettings) { mutableStateOf(storeSettings.storeName) }
    var storeAddress by remember(storeSettings) { mutableStateOf(storeSettings.storeAddress) }
    var storePhone by remember(storeSettings) { mutableStateOf(storeSettings.storePhone) }
    var receiptFooter by remember(storeSettings) { mutableStateOf(storeSettings.receiptFooter) }
    var newNominalInput by remember { mutableStateOf("") }

    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsStateWithLifecycle()
    val soundTitle by viewModel.selectedNotificationSoundTitle.collectAsStateWithLifecycle()
    val soundUri by viewModel.selectedNotificationSoundUri.collectAsStateWithLifecycle()
    val availableSounds by viewModel.availableNotificationSounds.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockProductCount.collectAsStateWithLifecycle()

    val currentGoogleUser by viewModel.currentGoogleUser.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val isRealtimeSyncEnabled by viewModel.isRealtimeSyncEnabled.collectAsStateWithLifecycle()
    val customDatabaseUrl by viewModel.customDatabaseUrl.collectAsStateWithLifecycle()
    val customStoreCode by viewModel.customStoreCode.collectAsStateWithLifecycle()
    var showGoogleSyncDialog by remember { mutableStateOf(false) }

    val excludedPromoProducts by viewModel.excludedPromoProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    var showPromoExclusionManagerDialog by remember { mutableStateOf(false) }
    var showAddPromoExclusionDialog by remember { mutableStateOf(false) }

    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()
    val activeCashierCount by viewModel.activeCashierCount.collectAsStateWithLifecycle()
    val activeCashiers by viewModel.activeCashiers.collectAsStateWithLifecycle()
    val activeCashierLabel by viewModel.activeCashierLabel.collectAsStateWithLifecycle()
    val showLoginRoleDialog by viewModel.showLoginRoleDialog.collectAsStateWithLifecycle()
    val showActiveCashiersDialog by viewModel.showActiveCashiersDialog.collectAsStateWithLifecycle()

    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var passwordChangeSuccess by remember { mutableStateOf<String?>(null) }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    var showSoundPickerDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.setNotificationEnabled(true)
        }
    }

    if (showSoundPickerDialog) {
        NotificationSoundPickerDialog(
            availableSounds = availableSounds,
            currentSelectedUri = soundUri,
            onSoundSelected = { title, uri ->
                viewModel.setSelectedNotificationSound(title, uri)
            },
            onPlayPreview = { uri ->
                viewModel.playPreviewSound(uri)
            },
            onStopPreview = {
                viewModel.stopPreviewSound()
            },
            onRefreshSounds = {
                viewModel.refreshDeviceNotificationSounds()
            },
            onDismiss = {
                showSoundPickerDialog = false
            }
        )
    }

    // Launcher for creating a JSON backup file in user's internal phone storage folder
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportBackupToJsonUri(context, it)
        }
    }

    // Launcher for opening and restoring a JSON backup file from phone storage
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importBackupFromJsonUri(context, it)
        }
    }

    // Launcher for exporting APK file to user's phone storage
    val exportApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportAppApkToUri(context, it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pengaturan & Cadangan Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kustomisasi struk belanja toko dan cadangkan data (.JSON) ke penyimpanan internal ponsel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Akun & Hak Akses (Pemilik vs Kasir) & Monitoring Kasir Aktif
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_auth_role_settings"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (currentUserRole == UserRole.PEMILIK) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentUserRole == UserRole.PEMILIK) Icons.Default.Shield else Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = if (currentUserRole == UserRole.PEMILIK) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Akun & Hak Akses Peran",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Peran Aktif: ${currentUserRole.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (currentUserRole == UserRole.PEMILIK) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.openLoginRoleDialog() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_switch_role_settings")
                        ) {
                            Text("Ganti Akun", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time Active Cashier Devices Info Card
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Perangkat Kasir Aktif (Real-Time)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D)
                                    )
                                }

                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🟢 $activeCashierCount Kasir Online",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Menampilkan status perangkat: $activeCashierLabel. Informasi diperbarui secara langsung dan otomatis antar HP.",
                                fontSize = 11.sp,
                                color = Color(0xFF166534),
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.openActiveCashiersDialog() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Lihat Detail Semua Perangkat Kasir Aktif", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fitur Khusus Akun Pemilik: Ubah Kata Sandi Pemilik
                    if (currentUserRole == UserRole.PEMILIK) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Ubah Kata Sandi Pemilik",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Kata sandi default: 1234567890. Anda dapat mengubah kata sandi baru untuk mengamankan hak akses pembatalan transaksi toko.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = oldPasswordInput,
                                    onValueChange = {
                                        oldPasswordInput = it
                                        passwordChangeError = null
                                        passwordChangeSuccess = null
                                    },
                                    label = { Text("Kata Sandi Lama", fontSize = 12.sp) },
                                    placeholder = { Text("1234567890", fontSize = 12.sp) },
                                    singleLine = true,
                                    visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showOldPassword = !showOldPassword }) {
                                            Icon(
                                                imageVector = if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_old_owner_password"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = newPasswordInput,
                                    onValueChange = {
                                        newPasswordInput = it
                                        passwordChangeError = null
                                        passwordChangeSuccess = null
                                    },
                                    label = { Text("Kata Sandi Baru", fontSize = 12.sp) },
                                    placeholder = { Text("Minimal 4 karakter", fontSize = 12.sp) },
                                    singleLine = true,
                                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                            Icon(
                                                imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_new_owner_password"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = {
                                        confirmPasswordInput = it
                                        passwordChangeError = null
                                        passwordChangeSuccess = null
                                    },
                                    label = { Text("Konfirmasi Kata Sandi Baru", fontSize = 12.sp) },
                                    placeholder = { Text("Ulangi kata sandi baru", fontSize = 12.sp) },
                                    singleLine = true,
                                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_confirm_owner_password"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                if (passwordChangeError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = passwordChangeError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (passwordChangeSuccess != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = passwordChangeSuccess ?: "",
                                        color = Color(0xFF16A34A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.resetOwnerPasswordToDefault()
                                            oldPasswordInput = ""
                                            newPasswordInput = ""
                                            confirmPasswordInput = ""
                                            passwordChangeError = null
                                            passwordChangeSuccess = "Kata sandi pemilik dikembalikan ke default: 1234567890"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset Bawaan", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (newPasswordInput != confirmPasswordInput) {
                                                passwordChangeError = "Konfirmasi kata sandi tidak cocok!"
                                                return@Button
                                            }
                                            val res = viewModel.changeOwnerPassword(oldPasswordInput, newPasswordInput)
                                            if (res.isSuccess) {
                                                passwordChangeSuccess = "Kata sandi pemilik berhasil diperbarui!"
                                                passwordChangeError = null
                                                oldPasswordInput = ""
                                                newPasswordInput = ""
                                                confirmPasswordInput = ""
                                            } else {
                                                passwordChangeError = res.exceptionOrNull()?.message ?: "Gagal mengubah kata sandi"
                                                passwordChangeSuccess = null
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("btn_save_owner_password")
                                    ) {
                                        Text("Simpan Sandi Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        // Info untuk Akun Kasir
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Akun Kasir aktif (tanpa sandi). Untuk mengubah kata sandi pemilik atau menghapus transaksi batal, silakan ganti ke Akun Pemilik.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 0: Google Account & Online Real-Time Multi-Device Sync
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_google_sync_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentGoogleUser != null) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (currentGoogleUser != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                ),
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (currentGoogleUser != null) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Firebase Realtime Online (10 HP)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🟢 Terhubung otomatis & sinkron data real-time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TextButton(
                            onClick = { showGoogleSyncDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Pengaturan", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Info box showing permanent store connection
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Database Toko: toko-subur-50bde",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Kode Multi-HP: $customStoreCode (Sinkron 10 HP)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (currentGoogleUser != null) {
                                IconButton(
                                    onClick = { viewModel.signOutGoogle() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Keluar Akun",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Realtime toggle & status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sinkronisasi Real-Time Otomatis",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (syncMessage.isNotBlank()) syncMessage else "Data stok, transaksi kasir, & laporan sinkron instan antar HP",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRealtimeSyncEnabled,
                            onCheckedChange = { viewModel.setRealtimeSyncEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.syncAllLocalToCloud() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_sync_now_settings"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Singkron Sekarang", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showGoogleSyncDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detail Multi-HP", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 1: Edit Struk Pembayaran
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Identitas & Tampilan Nama Toko",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ubah nama toko yang tampil pada layar kasir dan struk pembayaran (misal: TOKO MAKMUR atau TOKO SUBUR)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Logo Aplikasi Toko Subur
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_toko_subur_logo_1790711721794),
                            contentDescription = "Logo Aplikasi Toko Subur",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Logo Resmi Aplikasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Logo TOKO SUBUR aktif sebagai ikon peluncur aplikasi dan lambang kasir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nama Toko (Header Struk & Layar)
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko (Layar Kasir & Struk)") },
                        placeholder = { Text("Contoh: TOKO SUBUR") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_store_name"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Alamat Toko (Header Struk)
                    OutlinedTextField(
                        value = storeAddress,
                        onValueChange = { storeAddress = it },
                        label = { Text("Alamat Toko (Bagian Atas Struk)") },
                        placeholder = { Text("Jl. Kembang Kuning No.17, Surabaya") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_store_address"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // No Telepon Toko
                    OutlinedTextField(
                        value = storePhone,
                        onValueChange = { storePhone = it },
                        label = { Text("Nomor Telepon / Kontak Toko") },
                        placeholder = { Text("0812-3456-7890") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_store_phone"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Kata Penutup Struk (Footer Struk)
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Kata Penutup Struk (Bagian Paling Bawah Struk)") },
                        placeholder = { Text("Terima kasih telah berbelanja!\nBarang yang sudah dibeli tidak dapat ditukar.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_receipt_footer"),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val cleanName = storeName.trim().ifBlank { "TOKO SUBUR" }
                            storeName = cleanName
                            viewModel.updateReceiptSettings(
                                storeName = cleanName,
                                storeAddress = storeAddress,
                                storePhone = storePhone,
                                receiptFooter = receiptFooter
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_receipt_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Nama Toko & Pengaturan")
                    }
                }
            }

            // Struk Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Preview Header Aplikasi
                    Text(
                        text = "--- Pratinjau Header Tampilan Layar Kasir ---",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = storeName.ifBlank { "TOKO SUBUR" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = storeAddress.ifBlank { "Jl. Kembang Kuning No.17, Surabaya" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "--- Pratinjau Tampilan Struk Kasir ---",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = storeName.ifBlank { "TOKO SUBUR" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = storeAddress.ifBlank { "Jl. Kembang Kuning No.17, Surabaya" },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF475569)
                    )
                    if (storePhone.isNotBlank()) {
                        Text(
                            text = "Telp: $storePhone",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF475569)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFCBD5E1))
                    Text(
                        text = "1x Beras Rojolele 5kg    Rp 72.000\n1x Minyak Bimoli 2L       Rp 36.000\n--------------------------------\nTOTAL:                   Rp 108.000",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF334155)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFCBD5E1))

                    Text(
                        text = receiptFooter.ifBlank { "Terima kasih atas kunjungan Anda!" },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Section 2: Pengaturan Nominal Cepat Pembayaran (Kasir)
            // "Serta tambahkan fitur edit pada menu pengaturan untuk menghapus & menambahkan nominal lain"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Money,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tombol Nominal Cepat Pembayaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kelola angka tombol cepat saat pembayaran kasir. Setiap angka otomatis ditambahkan 000.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Daftar Nominal Saat Ini (Klik 'x' untuk menghapus):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val nominalsList = storeSettings.quickNominals.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (nom in nominalsList) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$nom (${nom}.000)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.removeQuickNominal(nom) },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .testTag("delete_nominal_$nom")
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Hapus $nom",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Add new nominal input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newNominalInput,
                            onValueChange = { newNominalInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tambah Angka Nominal") },
                            placeholder = { Text("Contoh: 15 atau 200") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_nominal_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newNominalInput.isNotBlank()) {
                                    viewModel.addQuickNominal(newNominalInput)
                                    newNominalInput = ""
                                }
                            },
                            enabled = newNominalInput.isNotBlank(),
                            modifier = Modifier.testTag("add_nominal_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Contoh: Ketik 15 maka tombol '15' akan muncul di kasir dan bernilai Rp 15.000 saat diklik.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section: Daftar Pengecualian 10 Produk Terlaris (Promo Toko)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_promo_exclusions_settings"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pengecualian 10 Produk Terlaris",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Daftar produk promo toko yang tidak dimasukkan ke 10 terlaris",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (excludedPromoProducts.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${excludedPromoProducts.size} Promo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Jika terdapat produk yang sedang dalam masa promosi/diskon toko, masukkan produk tersebut ke daftar ini. Hal ini bertujuan agar produk promo tidak mendominasi atau masuk ke dalam 10 produk terlaris mingguan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (excludedPromoProducts.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Belum ada produk promo yang dikecualikan. Semua produk penjualan dihitung ke dalam peringkat 10 terlaris.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            excludedPromoProducts.take(4).forEach { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                                text = item.productName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "🏷️ ${item.reason}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFD97706)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeExcludedPromoProduct(item.productId) },
                                            modifier = Modifier.size(28.dp).testTag("delete_excluded_promo_${item.productId}")
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Hapus Pengecualian",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (excludedPromoProducts.size > 4) {
                                TextButton(
                                    onClick = { showPromoExclusionManagerDialog = true },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Lihat semua ${excludedPromoProducts.size} produk promo ->", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddPromoExclusionDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_add_promo_exclusion_settings"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah Produk Promo", fontSize = 12.sp)
                        }

                        if (excludedPromoProducts.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showPromoExclusionManagerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_manage_promo_settings")
                            ) {
                                Text("Kelola", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Section: Notifikasi Suara Stok Menipis & Peringatan Audio
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("low_stock_notification_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifikasi Suara Stok Menipis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Peringatan audio otomatis saat stok produk mendekati batas minimum",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch Aktifkan Notifikasi
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aktifkan Peringatan Notifikasi",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isNotificationEnabled) "Notifikasi aktif dan bersuara saat ada stok menipis" else "Notifikasi dimatikan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isNotificationEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotificationEnabled(isChecked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("toggle_notification_switch")
                            )
                        }
                    }

                    // Izin Notifikasi (Android 13+)
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Izin Notifikasi Belum Diberikan",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Android membutuhkan izin pengguna agar aplikasi dapat memunculkan notifikasi dan memutar suara peringatan stok menipis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("grant_notification_permission_btn")
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Berikan Izin Notifikasi Sekarang")
                                }
                            }
                        }
                    }

                    if (isNotificationEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Pengaturan Suara dari Penyimpanan Internal
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Suara Notifikasi Pilihan",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = soundTitle,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Diambil dari nada sistem / penyimpanan internal ponsel",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.playPreviewSound(soundUri)
                                            },
                                            modifier = Modifier.testTag("play_sound_preview_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Putar Suara",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        viewModel.refreshDeviceNotificationSounds()
                                        showSoundPickerDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_sound_picker_btn"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buka Daftar Suara Penyimpanan Ponsel (${availableSounds.size})")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Stok & Tombol Uji Notifikasi
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Status Stok Terbaru:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lowStockCount > 0) "$lowStockCount produk menipis" else "Semua stok aman",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notifikasi ini otomatis mendeteksi transaksi atau perubahan stok terbaru dan dapat diabaikan atau dihapus (swipe) kapan saja seperti aplikasi pada umumnya.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.testSoundNotification()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("test_sound_notif_btn"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Uji Notifikasi Suara", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.triggerManualLowStockCheck()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("check_low_stock_btn"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Kirim Notifikasi Stok", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Backup & Restore Data (.JSON ke Penyimpanan Lokal)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cadangkan & Pulihkan Data (.JSON)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Akses penyimpanan internal ponsel dengan pilihan folder lokal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Fitur ini menyimpan seluruh data katalog produk, stok, riwayat penjualan, pengeluaran, dan pengaturan toko ke file berkas format .JSON di penyimpanan ponsel Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cadangkan Data Button
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val suggestedName = "toko_subur_backup_$timestamp.json"
                            exportJsonLauncher.launch(suggestedName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_json_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadangkan Data ke Folder Lokal (.JSON)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pulihkan Data Button
                    OutlinedButton(
                        onClick = {
                            importJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_restore_json_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pulihkan Data dari Berkas .JSON")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Info box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pilihan folder internal ponsel (Downloads/Documents/Internal Storage) dikelola aman melalui Penyimpanan Sistem Android.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 4: Berkas Instalasi APK (TokoSubur.apk)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Berkas Instalasi APK (TokoSubur.apk)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Simpan langsung berkas .apk siap pasang ke penyimpanan HP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Berkas .APK ini sudah ditandatangani resmi (signed) dan siap dipasang langsung di HP Android Anda maupun dibagikan ke ponsel kasir lainnya tanpa perlu komputer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            exportApkLauncher.launch("TokoSubur.apk")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_apk_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan Berkas TokoSubur.apk ke HP", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 5: Privasi Media Galeri Ponsel (.nomedia)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Privasi Gambar dari Galeri HP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sembunyikan seluruh gambar, logo, dan berkas aplikasi dari Galeri foto ponsel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Status: Aktif & Terlindungi (.nomedia)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Semua direktori data dan cache aplikasi diproteksi dengan berkas .nomedia sehingga sistem galeri ponsel (Google Photos / Galeri HP) otomatis mengabaikan dan tidak menampilkan gambar aplikasi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.enforceHideImagesFromGallery() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enforce_hide_gallery_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Perbarui & Pastikan Perlindungan Galeri")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

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
            onDismiss = { showGoogleSyncDialog = false },
            customDatabaseUrl = customDatabaseUrl,
            customStoreCode = customStoreCode,
            onUpdateCustomDatabaseUrl = { viewModel.updateCustomDatabaseUrl(it) },
            onUpdateCustomStoreCode = { viewModel.updateCustomStoreCode(it) }
        )
    }

    if (showLoginRoleDialog) {
        LoginRoleDialog(
            currentRole = currentUserRole,
            onLoginOwner = { password -> viewModel.loginAsOwner(password) },
            onLoginCashier = { viewModel.loginAsCashier() },
            onDismiss = { viewModel.dismissLoginRoleDialog() }
        )
    }

    if (showActiveCashiersDialog) {
        ActiveCashiersDialog(
            activeCount = activeCashierCount,
            activeCashiers = activeCashiers,
            currentUserRole = currentUserRole,
            currentDeviceId = viewModel.authRoleManager.deviceId,
            onDismiss = { viewModel.dismissActiveCashiersDialog() }
        )
    }

    if (showPromoExclusionManagerDialog) {
        PromoExclusionManagerDialog(
            excludedList = excludedPromoProducts,
            allProducts = allProducts,
            onAddExclusion = { prod, reason ->
                viewModel.addExcludedPromoProduct(prod, reason)
            },
            onRemoveExclusion = { pId ->
                viewModel.removeExcludedPromoProduct(pId)
            },
            onDismiss = { showPromoExclusionManagerDialog = false }
        )
    }

    if (showAddPromoExclusionDialog) {
        val excludedIds = remember(excludedPromoProducts) { excludedPromoProducts.map { it.productId }.toSet() }
        val eligibleProducts = remember(allProducts, excludedIds) {
            allProducts.filter { it.id !in excludedIds }
        }
        AddPromoExclusionPickerDialog(
            availableProducts = eligibleProducts,
            onProductSelected = { prod, reason ->
                viewModel.addExcludedPromoProduct(prod, reason)
                showAddPromoExclusionDialog = false
            },
            onDismiss = { showAddPromoExclusionDialog = false }
        )
    }
}
