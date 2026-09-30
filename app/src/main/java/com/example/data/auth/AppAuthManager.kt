package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class UserRole(val label: String) {
    PEMILIK("Pemilik"),
    KASIR("Kasir")
}

data class CashierDeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val cashierNumber: Int = 1,
    val role: String = "KASIR",
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
)

class AppAuthManager(private val context: Context) {

    companion object {
        const val PREFS_NAME = "app_auth_role_prefs"
        const val KEY_CURRENT_ROLE = "current_role"
        const val KEY_OWNER_PASSWORD = "owner_password"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_CASHIER_NUMBER = "cashier_number"
        const val DEFAULT_OWNER_PASSWORD = "1234567890"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUserRole = MutableStateFlow(loadInitialRole())
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _assignedCashierNumber = MutableStateFlow(loadCashierNumber())
    val assignedCashierNumber: StateFlow<Int> = _assignedCashierNumber.asStateFlow()

    val deviceId: String = getOrCreateDeviceId()
    val deviceName: String = getFormattedDeviceName()

    private fun loadInitialRole(): UserRole {
        val saved = prefs.getString(KEY_CURRENT_ROLE, UserRole.KASIR.name)
        return try {
            UserRole.valueOf(saved ?: UserRole.KASIR.name)
        } catch (_: Exception) {
            UserRole.KASIR
        }
    }

    private fun loadCashierNumber(): Int {
        return prefs.getInt(KEY_CASHIER_NUMBER, 1).coerceIn(1, 9)
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = "hp_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    private fun getFormattedDeviceName(): String {
        val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model"
    }

    fun getOwnerPassword(): String {
        return prefs.getString(KEY_OWNER_PASSWORD, DEFAULT_OWNER_PASSWORD) ?: DEFAULT_OWNER_PASSWORD
    }

    /**
     * Login sebagai Pemilik. Memerlukan kata sandi (default 1234567890).
     */
    fun loginAsOwner(password: String): Result<Unit> {
        val currentPass = getOwnerPassword()
        return if (password == currentPass) {
            prefs.edit().putString(KEY_CURRENT_ROLE, UserRole.PEMILIK.name).apply()
            _currentUserRole.value = UserRole.PEMILIK
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Kata sandi pemilik salah! Silakan coba lagi."))
        }
    }

    /**
     * Login sebagai Kasir. Tidak memerlukan kata sandi.
     */
    fun loginAsCashier(): Result<Unit> {
        prefs.edit().putString(KEY_CURRENT_ROLE, UserRole.KASIR.name).apply()
        _currentUserRole.value = UserRole.KASIR
        return Result.success(Unit)
    }

    /**
     * Mengubah kata sandi pemilik. Dapat dilakukan dari menu Pengaturan oleh akun pemilik.
     */
    fun changeOwnerPassword(oldPassword: String, newPassword: String): Result<Unit> {
        val currentPass = getOwnerPassword()
        if (oldPassword != currentPass) {
            return Result.failure(IllegalArgumentException("Kata sandi lama salah!"))
        }
        val trimmed = newPassword.trim()
        if (trimmed.length < 4) {
            return Result.failure(IllegalArgumentException("Kata sandi baru minimal 4 karakter!"))
        }
        prefs.edit().putString(KEY_OWNER_PASSWORD, trimmed).apply()
        return Result.success(Unit)
    }

    /**
     * Mengembalikan kata sandi pemilik ke default (1234567890)
     */
    fun resetOwnerPasswordToDefault(): Result<Unit> {
        prefs.edit().putString(KEY_OWNER_PASSWORD, DEFAULT_OWNER_PASSWORD).apply()
        return Result.success(Unit)
    }

    fun setAssignedCashierNumber(num: Int) {
        val clamped = num.coerceIn(1, 9)
        _assignedCashierNumber.value = clamped
        prefs.edit().putInt(KEY_CASHIER_NUMBER, clamped).apply()
    }
}
