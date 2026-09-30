package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ProductEntity

object NotificationHelper {

    private const val BASE_CHANNEL_ID = "toko_makmur_low_stock"
    const val LOW_STOCK_NOTIFICATION_ID = 2001

    private var currentPreviewRingtone: Ringtone? = null

    /**
     * Mengakses daftar suara notifikasi yang tersimpan di penyimpanan internal sistem ponsel
     */
    fun getDeviceNotificationSounds(context: Context): List<NotificationSoundItem> {
        val soundList = mutableListOf<NotificationSoundItem>()

        // 1. Tambahkan opsi Standar / Default Sistem
        soundList.add(
            NotificationSoundItem(
                title = "Suara Notifikasi Default Sistem",
                uriString = "",
                isDefault = true
            )
        )

        try {
            val ringtoneManager = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_NOTIFICATION)
            }
            val cursor = ringtoneManager.cursor

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)?.toString() ?: ""
                    if (title.isNotBlank() && uri.isNotBlank()) {
                        // Hindari duplikasi jika ada title yang sama persis
                        if (soundList.none { it.uriString == uri }) {
                            soundList.add(
                                NotificationSoundItem(
                                    title = title,
                                    uriString = uri
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Jika daftar notifikasi kosong (beberapa perangkat/emulator hanya memiliki ringtone),
        // ambil juga suara dari tipe Ringtone
        if (soundList.size <= 1) {
            try {
                val ringtoneManager = RingtoneManager(context).apply {
                    setType(RingtoneManager.TYPE_RINGTONE)
                }
                val cursor = ringtoneManager.cursor
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = ringtoneManager.getRingtoneUri(cursor.position)?.toString() ?: ""
                        if (title.isNotBlank() && uri.isNotBlank() && soundList.none { it.uriString == uri }) {
                            soundList.add(
                                NotificationSoundItem(
                                    title = title,
                                    uriString = uri
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return soundList
    }

    /**
     * Memutar preview suara notifikasi untuk didengarkan pengguna
     */
    fun playPreviewSound(context: Context, uriString: String) {
        stopPreviewSound()
        try {
            val soundUri = if (uriString.isBlank()) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(uriString)
            }

            val ringtone = RingtoneManager.getRingtone(context, soundUri)
            if (ringtone != null) {
                currentPreviewRingtone = ringtone
                ringtone.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menghentikan pemutaran preview suara
     */
    fun stopPreviewSound() {
        try {
            currentPreviewRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            currentPreviewRingtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Membangun channel notifikasi di Android 8.0+ dengan suara yang dikustomisasi
     */
    private fun createNotificationChannel(context: Context, soundUri: Uri): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${BASE_CHANNEL_ID}_${soundUri.toString().hashCode()}"
            val channelName = "Peringatan Stok Menipis"
            val channelDescription = "Notifikasi suara ketika ada produk toko yang stoknya menipis"

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDescription
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            return channelId
        }
        return BASE_CHANNEL_ID
    }

    /**
     * Menampilkan notifikasi suara untuk produk yang mulai menipis.
     * Notifikasi ini dapat diabaikan (dismissible/swipe away) dan autoCancel saat ditekan.
     */
    fun showLowStockNotification(
        context: Context,
        lowStockProducts: List<ProductEntity>,
        soundUriString: String
    ) {
        if (lowStockProducts.isEmpty()) return

        val soundUri: Uri = if (soundUriString.isBlank()) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            Uri.parse(soundUriString)
        }

        val channelId = createNotificationChannel(context, soundUri)

        // Intent untuk membuka aplikasi ke layar Produk dengan filter stok menipis aktif
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "PRODUK")
            putExtra("filter_low_stock", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            LOW_STOCK_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val count = lowStockProducts.size
        val title = "⚠️ Peringatan: $count Produk Stok Menipis!"
        val shortContent = if (count == 1) {
            val p = lowStockProducts.first()
            "${p.name} sisa ${p.stok} (Batas min: ${p.minimumStokAlert})"
        } else {
            "Ada $count produk di toko yang mendekati batas minimum stok."
        }

        val bigTextBuilder = StringBuilder()
        bigTextBuilder.append("Segera lakukan pengadaan atau cek gudang:\n")
        lowStockProducts.take(8).forEach { prod ->
            val status = if (prod.stok <= 0) "HABIS (0)" else "Sisa ${prod.stok}"
            bigTextBuilder.append("• ${prod.name} : $status (Min: ${prod.minimumStokAlert})\n")
        }
        if (lowStockProducts.size > 8) {
            bigTextBuilder.append("... dan ${lowStockProducts.size - 8} produk lainnya.")
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(bigTextBuilder.toString().trimEnd())
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setAutoCancel(true) // Hilang saat disentuh
            .setOngoing(false)   // Dapat diabaikan/di-swipe seperti notifikasi biasa
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(LOW_STOCK_NOTIFICATION_ID, notification)

                // Mainkan suara secara eksplisit agar terdengar jelas saat pemicu aktif
                try {
                    val ringtone = RingtoneManager.getRingtone(context, soundUri)
                    ringtone?.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: SecurityException) {
            // Izin belum diberikan di Android 13+
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Membatalkan / membersihkan notifikasi
     */
    fun cancelLowStockNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(LOW_STOCK_NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
