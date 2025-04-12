package com.xxcactussell.mategram.notifications

import android.content.Context

class NotificationSettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

    fun saveNotificationSettings(
        globalEnabled: Boolean,
        scopeSettings: Map<String, NotificationScopeSettings>,
        chatSettings: Map<Long, NotificationChatSettings>
    ) {
        prefs.edit().apply {
            // Save global settings
            putBoolean("notifications_enabled", globalEnabled)

            // Save scope settings
            scopeSettings.forEach { (scope, settings) ->
                putInt("${scope}_mute_for", settings.muteFor)
                putBoolean("${scope}_show_preview", settings.showPreview)
                putBoolean("${scope}_sound_enabled", settings.soundEnabled)
            }

            // Save chat-specific settings
            chatSettings.forEach { (chatId, settings) ->
                putInt("chat_${chatId}_mute_for", settings.muteFor)
                putBoolean("chat_${chatId}_show_preview", settings.showPreview)
                putBoolean("chat_${chatId}_sound_enabled", settings.soundEnabled)
                putBoolean("chat_${chatId}_use_default", settings.useDefault)
            }

            // Save last update timestamp
            putLong("last_update", System.currentTimeMillis())
        }.apply()
    }

    fun loadNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            globalEnabled = prefs.getBoolean("notifications_enabled", true),
            scopeSettings = loadScopeSettings(),
            chatSettings = loadChatSettings()
        )
    }

    private fun loadScopeSettings(): Map<String, NotificationScopeSettings> {
        val scopes = listOf(
            "private_chats",
            "group_chats",
            "channel_chats"
        )

        return scopes.associateWith { scope ->
            NotificationScopeSettings(
                muteFor = prefs.getInt("${scope}_mute_for", 0),
                showPreview = prefs.getBoolean("${scope}_show_preview", true),
                soundEnabled = prefs.getBoolean("${scope}_sound_enabled", true)
            )
        }
    }

    private fun loadChatSettings(): Map<Long, NotificationChatSettings> {
        val chatSettings = mutableMapOf<Long, NotificationChatSettings>()

        // Get all preferences and filter chat-specific ones
        prefs.all.forEach { (key, _) ->
            if (key.startsWith("chat_")) {
                val chatId = key.substringAfter("chat_")
                    .substringBefore("_")
                    .toLongOrNull()

                chatId?.let {
                    chatSettings[it] = NotificationChatSettings(
                        muteFor = prefs.getInt("chat_${chatId}_mute_for", 0),
                        showPreview = prefs.getBoolean("chat_${chatId}_show_preview", true),
                        soundEnabled = prefs.getBoolean("chat_${chatId}_sound_enabled", true),
                        useDefault = prefs.getBoolean("chat_${chatId}_use_default", true)
                    )
                }
            }
        }

        return chatSettings
    }

    companion object {
        private var instance: NotificationSettingsManager? = null

        @Synchronized
        fun getInstance(context: Context): NotificationSettingsManager {
            if (instance == null) {
                instance = NotificationSettingsManager(context.applicationContext)
            }
            return instance!!
        }
    }
}

// Data classes for settings
data class NotificationSettings(
    val globalEnabled: Boolean,
    val scopeSettings: Map<String, NotificationScopeSettings>,
    val chatSettings: Map<Long, NotificationChatSettings>
)

data class NotificationScopeSettings(
    val muteFor: Int,
    val showPreview: Boolean,
    val soundEnabled: Boolean
)

data class NotificationChatSettings(
    val muteFor: Int,
    val showPreview: Boolean,
    val soundEnabled: Boolean,
    val useDefault: Boolean
)