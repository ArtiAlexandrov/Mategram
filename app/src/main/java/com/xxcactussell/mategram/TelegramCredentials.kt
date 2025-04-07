package com.xxcactussell.mategram

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Base64
import com.xxcactussell.mategram.TelegramRepository.appContext
import java.io.File
import java.security.SecureRandom

class TelegramCredentials(context: Context) {
    companion object {
        private fun saveEncryptionKey(context: Context, key: ByteArray) {
            val prefs = context.getSharedPreferences("tdlib_prefs", Context.MODE_PRIVATE)
            val encodedKey = Base64.encodeToString(key, Base64.DEFAULT)
            prefs.edit().putString("database_encryption_key", encodedKey).apply()
            println("Encryption key saved: $encodedKey")
        }

        private fun loadEncryptionKey(context: Context): ByteArray? {
            val prefs = context.getSharedPreferences("tdlib_prefs", Context.MODE_PRIVATE)
            val encodedKey = prefs.getString("database_encryption_key", null) ?: return null
            println("Encryption key loaded: $encodedKey")
            return Base64.decode(encodedKey, Base64.DEFAULT)
        }

        val encryptionKey: ByteArray = appContext?.let { loadEncryptionKey(it) } ?: ByteArray(32).apply {
            SecureRandom().nextBytes(this) // Используем SecureRandom для генерации ключа
        }

        init {
            appContext?.let {
                if (loadEncryptionKey(it) == null) {
                    saveEncryptionKey(it, encryptionKey)
                }
            }
        }

        const val useFileDatabase = true
        const val useChatInfoDatabase = true
        const val useTestDc = false
        val databaseDirectory = appContext?.filesDir?.absolutePath
        val filesDirectory = appContext?.getExternalFilesDir(null)?.absolutePath
        const val useMessageDatabase = true
        const val useSecretChats = true
        const val apiId = 10729986
        const val apiHash = "8c782711669292c1ea20e2582c2424f9"
        val systemLanguageCode = Resources.getSystem().configuration.locales[0].toString()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val systemVersion: String = Build.VERSION.RELEASE
        const val applicationVersion = "1.0"
    }
}
