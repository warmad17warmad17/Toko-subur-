package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.sync.GoogleUser
import com.example.data.sync.SyncStatus
import com.example.util.DateFormatter

@Composable
fun GoogleSyncDialog(
    currentUser: GoogleUser?,
    syncStatus: SyncStatus,
    syncMessage: String,
    lastSyncTime: Long,
    isRealtimeSyncEnabled: Boolean,
    onToggleRealtimeSync: (Boolean) -> Unit,
    onSignInGoogle: () -> Unit,
    onSignInManual: (email: String, name: String) -> Unit,
    onSignOut: () -> Unit,
    onForceSyncAll: () -> Unit,
    onDismiss: () -> Unit,
    customDatabaseUrl: String = "",
    customStoreCode: String = "",
    onUpdateCustomDatabaseUrl: ((String) -> Unit)? = null,
    onUpdateCustomStoreCode: ((String) -> Unit)? = null
) {
    var showManualEmailInput by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    var databaseUrlInput by remember(customDatabaseUrl) { mutableStateOf(customDatabaseUrl) }
    var storeCodeInput by remember(customStoreCode) { mutableStateOf(customStoreCode) }
    var showFirebaseGuide by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var showGoogleLoginSection by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("google_sync_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Firebase Realtime Online",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sinkronisasi 10 HP Tanpa Refresh",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (syncStatus) {
                            SyncStatus.SYNCED -> Color(0xFF10B981).copy(alpha = 0.12f)
                            SyncStatus.SYNCING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            SyncStatus.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            SyncStatus.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (syncStatus) {
                                    SyncStatus.SYNCED -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Terhubung Online (Multi-HP Aktif)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                    SyncStatus.SYNCING -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Menyambungkan Database...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    SyncStatus.OFFLINE -> {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Mode Offline (Data Lokal di HP)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    SyncStatus.ERROR -> {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Koneksi Bermasalah",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            if (lastSyncTime > 0) {
                                Text(
                                    text = DateFormatter.formatFullDateTime(lastSyncTime),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = syncMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account / Store Identity Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (currentUser != null) MaterialTheme.colorScheme.primary else Color(0xFF16A34A),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (currentUser != null) {
                                        Text(
                                            text = currentUser.displayName.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Store,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentUser != null) currentUser.displayName else "TOKO SUBUR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (currentUser != null) currentUser.email else "Jalur Sinkron: ${storeCodeInput.ifBlank { "toko_subur" }} (10 HP)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (currentUser != null) {
                                OutlinedButton(
                                    onClick = onSignOut,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Keluar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Optional Google Sign-In (Collapsed by default)
                if (currentUser == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showGoogleLoginSection = !showGoogleLoginSection },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Akun Google Pemilik (Opsional)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (showGoogleLoginSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (showGoogleLoginSection) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Aplikasi sudah otomatis terhubung ke Realtime Database tanpa login. Login Google hanya opsional jika Anda ingin mengaitkan email pemilik.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onSignInGoogle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("btn_google_signin"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Masuk dengan Akun Google", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Firebase Realtime Database Configuration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedSettings = !showAdvancedSettings },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "URL Firebase Realtime Database",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (showAdvancedSettings) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Salin URL Realtime Database dari Firebase Console (disarankan region asia-southeast1):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = databaseUrlInput,
                                onValueChange = { databaseUrlInput = it },
                                placeholder = { Text("https://myproject-default-rtdb.asia-southeast1.firebasedatabase.app", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_firebase_db_url"),
                                singleLine = false,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onUpdateCustomDatabaseUrl?.invoke(databaseUrlInput.trim())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan & Hubungkan Database", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-Time Sync Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sinkronisasi Otomatis Antar HP",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Setiap kasir transaksi atau edit produk langsung terupdate di 10 HP secara real-time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = isRealtimeSyncEnabled,
                            onCheckedChange = onToggleRealtimeSync
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Force Full Sync Button
                Button(
                    onClick = onForceSyncAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_force_sync"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Singkronkan Seluruh Data Sekarang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Firebase Console Instructions Section (Expandable)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFirebaseGuide = !showFirebaseGuide },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📖 Petunjuk Pengaturan Firebase Console",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Icon(
                                if (showFirebaseGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (showFirebaseGuide) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Ikuti 5 langkah mudah berikut di Firebase Console agar 10 HP kasir terhubung secara online:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            InstructionStep(
                                number = "1",
                                title = "Buat Realtime Database",
                                desc = "Buka console.firebase.google.com -> pilih project Anda -> di menu kiri pilih Build > Realtime Database -> klik 'Create Database'."
                            )
                            InstructionStep(
                                number = "2",
                                title = "Pilih Lokasi Server (Region)",
                                desc = "Pilih region 'Singapore (asia-southeast1)' agar koneksi paling cepat dan lancar dari Indonesia."
                            )
                            InstructionStep(
                                number = "3",
                                title = "Atur Aturan Keamanan (Rules)",
                                desc = "Klik tab 'Rules', ubah kodenya menjadi:\n{\n  \"rules\": {\n    \".read\": true,\n    \".write\": true\n  }\n}\nlalu klik 'Publish'."
                            )
                            InstructionStep(
                                number = "4",
                                title = "Salin URL Database",
                                desc = "Salin URL di bagian atas (contoh: https://namaproject-default-rtdb.asia-southeast1.firebasedatabase.app), lalu tempel ke kolom 'URL Firebase Realtime Database' di atas."
                            )
                            InstructionStep(
                                number = "5",
                                title = "Hubungkan ke-10 Ponsel Kasir",
                                desc = "Di semua 10 ponsel HP kasir, masukkan Kode Toko yang sama (misal: TOKO-SUBUR) atau login dengan akun Google yang sama. Semua stok dan penjualan akan tersinkron otomatis secara real-time tanpa perlu refresh!"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(
    number: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
