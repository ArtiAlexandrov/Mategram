## Mategram

A modern Telegram client built with Jetpack Compose, Kotlin, and Material 3 Design, offering a sleek and intuitive user experience. Designed for speed, efficiency, and seamless interaction, it leverages the power of Kotlin to deliver a smooth and responsive interface. Whether messaging, sharing media, or managing chats, this client is crafted to enhance your communication experience with the latest Android technologies.

Made with Kotlin and love ❤️

> **Note:** I am just a novice in coding, so the code may contain errors or non-optimal solutions. Contributions, suggestions, and bug reports are very welcome!


## Core Features

- **Telegram Authorization**
  - Supports phone number, code, password, and 2FA login.
  - Session storage and restoration.

- **TDLib Integration**
  - Custom wrapper over TDLib (via kotlinx.telegram).
  - All main TDLib methods are available via coroutines and extension functions.
  - Secure database encryption key generation and storage.

- **Jetpack Compose UI**
  - All UI is built with Jetpack Compose.
  - Uses Material 3 Design.
  - Main screens implemented: chat list, chat, login, 2FA, code, profile, etc.

- **Chat List**
  - Supports main list, archive, and folders (if supported by TDLib).
  - Filtering by folder (selected folder is not reset on chat updates).
  - Chat pinning, sorting, unread indicators.

- **Chat Screen**
  - Loads and displays messages.
  - Sending messages, quick reply.
  - Reply, forward, sender avatar (groups/channels).
  - Scroll to replied message.
  - Date display and day separators.

- **Media**
  - Loads and displays photos, videos, audio, documents.
  - Caching and reusing downloaded files.

- **Chat Folders**
  - Displays all folders, filters chats by folder.
  - Folder filter is not reset on chat updates.

- **Push Notifications**
  - Integrated with Firebase Cloud Messaging (FCM).
  - Receives and handles push notifications, updates token.

- **Security**
  - Generates and stores database encryption key securely.
  - Uses secure storage methods.

---

## Technical Details

- **Kotlin, Coroutines, StateFlow** — for async and reactive UI.
- **Firebase** — for push notifications.
- **Material 3** — for modern design.
- **TDLib** — via custom wrapper and extension functions.
- **Folders, filters, pinning** — implemented at UI and logic level.
- **Media** — supports all popular formats, caching, smooth display.

---

### Getting Started

1. **Firebase Cloud Messaging (FCM):**
   - Register your app in the [Firebase Console](https://console.firebase.google.com/).
   - Download your `google-services.json` and place it in app.

2. **Telegram API Credentials:**
   - Obtain your `api_id` and `api_hash` from [my.telegram.org](https://my.telegram.org/auth).

3. **Create TelegramCredentials.kt:**
   - Create a file at TelegramCredentials.kt with the following content:
   
```kotlin
package com.xxcactussell.mategram.kotlinx.telegram.core

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Base64
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.appContext
import java.security.SecureRandom

class TelegramCredentials {
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
            SecureRandom().nextBytes(this)
        }

        init {
            appContext?.let {
                if (loadEncryptionKey(it) == null) {
                    saveEncryptionKey(it, encryptionKey)
                }
            }
        }

        const val USE_FILE_DATABASE = true
        const val USE_CHAT_INFO_DATABASE = true
        const val USE_TEST_DC = false
        val databaseDirectory = appContext?.filesDir?.absolutePath
        val filesDirectory = appContext?.getExternalFilesDir(null)?.absolutePath
        const val USE_MESSAGE_DATABASE = true
        const val USE_SECRET_CHATS = true
        const val API_ID = YOUR_API_ID // <-- Replace with your api_id
        const val API_HASH = "YOUR_API_HASH" // <-- Replace with your api_hash
        val systemLanguageCode = Resources.getSystem().configuration.locales[0].toString()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val systemVersion: String = Build.VERSION.RELEASE
        const val APPLICATION_VERSION = "0.0.9 beta"
    }
}
```

- Replace `YOUR_API_ID` and `"YOUR_API_HASH"` with your actual values.

4. **Now you can build and run the project!**

---

**Telegram channel:** https://t.me/mategram_client


