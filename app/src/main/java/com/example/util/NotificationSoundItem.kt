package com.example.util

data class NotificationSoundItem(
    val title: String,
    val uriString: String,
    val isDefault: Boolean = false,
    val isCustomFile: Boolean = false
)
