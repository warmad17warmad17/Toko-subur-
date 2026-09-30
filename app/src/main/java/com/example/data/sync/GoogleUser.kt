package com.example.data.sync

/**
 * Represents the signed-in Google Account user session.
 */
data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val loginTimestamp: Long = System.currentTimeMillis()
)
