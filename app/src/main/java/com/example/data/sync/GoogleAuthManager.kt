package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class GoogleAuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("google_auth_prefs", Context.MODE_PRIVATE)

    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    init {
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val email = prefs.getString("user_email", null)
        val id = prefs.getString("user_id", null)
        val name = prefs.getString("user_name", null)
        val photo = prefs.getString("user_photo", null)

        if (!email.isNullOrBlank() && !id.isNullOrBlank()) {
            _currentUser.value = GoogleUser(
                id = id,
                email = email,
                displayName = name ?: email.substringBefore("@"),
                photoUrl = photo,
                loginTimestamp = prefs.getLong("login_timestamp", System.currentTimeMillis())
            )
        }
    }

    private fun saveUserToPrefs(user: GoogleUser) {
        prefs.edit()
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.displayName)
            .putString("user_photo", user.photoUrl)
            .putLong("login_timestamp", user.loginTimestamp)
            .apply()
        _currentUser.value = user
    }

    /**
     * Attempts Google Sign-In via Android Credential Manager.
     */
    suspend fun signInWithGoogle(webClientId: String = ""): Result<GoogleUser> {
        return try {
            val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)

            if (webClientId.isNotBlank()) {
                googleIdOptionBuilder.setServerClientId(webClientId)
            } else {
                // Default fallback client ID for Google ID option builder
                googleIdOptionBuilder.setServerClientId("757649110530-apps.googleusercontent.com")
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOptionBuilder.build())
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = GoogleUser(
                    id = googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName
                        ?: googleIdTokenCredential.givenName
                        ?: googleIdTokenCredential.id.substringBefore("@"),
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    idToken = googleIdTokenCredential.idToken,
                    loginTimestamp = System.currentTimeMillis()
                )
                saveUserToPrefs(user)
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Kredensial Google tidak dikenali"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("GoogleAuth", "Login Google dibatalkan oleh pengguna")
            Result.failure(Exception("Login dibatalkan oleh pengguna."))
        } catch (e: NoCredentialException) {
            Log.w("GoogleAuth", "Tidak ada kredensial Google di perangkat ini: ${e.message}")
            Result.failure(Exception("Tidak ditemukan akun Google yang tersimpan di perangkat ini. Silakan masukkan email Google secara manual."))
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "GetCredentialException: ${e.message}", e)
            Result.failure(Exception("Gagal menghubungi layanan akun Google: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Direct email login fallback (supports multi-device testing and manual sync account linking).
     */
    fun signInWithEmailDirect(email: String, displayName: String? = null): Result<GoogleUser> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Format email Google tidak valid."))
        }

        val name = displayName?.trim()?.takeIf { it.isNotBlank() } ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val user = GoogleUser(
            id = cleanEmail.replace(".", "_"),
            email = cleanEmail,
            displayName = name,
            loginTimestamp = System.currentTimeMillis()
        )
        saveUserToPrefs(user)
        return Result.success(user)
    }

    suspend fun signOut() {
        try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: Exception) {
            Log.w("GoogleAuth", "Error clearing credential state: ${e.message}")
        }
        prefs.edit().clear().apply()
        _currentUser.value = null
    }
}
