package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object NoMediaHelper {
    private const val TAG = "NoMediaHelper"
    private const val NOMEDIA_FILE = ".nomedia"

    /**
     * Ensures all app-specific directories (internal storage, external files, cache, and subfolders)
     * contain a .nomedia file so that Android MediaStore and gallery applications (Google Photos,
     * Galeri HP, etc.) ignore and do not display images or assets belonging to this app.
     */
    fun hideAppImagesFromGallery(context: Context): Boolean {
        var allSuccess = true
        try {
            val directories = mutableListOf<File>()

            // 1. Internal application storage
            context.filesDir?.let { directories.add(it) }
            context.cacheDir?.let { directories.add(it) }

            // Subdirectories in filesDir
            File(context.filesDir, "images").apply { mkdirs(); directories.add(this) }
            File(context.filesDir, "receipts").apply { mkdirs(); directories.add(this) }
            File(context.filesDir, "backups").apply { mkdirs(); directories.add(this) }
            File(context.filesDir, "exports").apply { mkdirs(); directories.add(this) }

            // 2. External app-specific storage (scanned by Android MediaStore if .nomedia is absent)
            try {
                context.getExternalFilesDir(null)?.let { directories.add(it) }
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { directories.add(it) }
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { directories.add(it) }
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { directories.add(it) }
                context.externalCacheDir?.let { directories.add(it) }
            } catch (e: Exception) {
                Log.w(TAG, "External directory access warning: ${e.message}")
            }

            for (dir in directories) {
                if (dir.exists() || dir.mkdirs()) {
                    val noMediaFile = File(dir, NOMEDIA_FILE)
                    if (!noMediaFile.exists()) {
                        val created = noMediaFile.createNewFile()
                        if (!created && !noMediaFile.exists()) {
                            allSuccess = false
                        }
                    }
                }
            }
            Log.d(TAG, "Secured ${directories.size} directories with .nomedia to hide app images from gallery")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply .nomedia files: ${e.message}", e)
            allSuccess = false
        }
        return allSuccess
    }

    /**
     * Checks if .nomedia protection is active in key app directories.
     */
    fun isNoMediaProtectionActive(context: Context): Boolean {
        val internalFile = File(context.filesDir, NOMEDIA_FILE)
        return internalFile.exists()
    }
}
