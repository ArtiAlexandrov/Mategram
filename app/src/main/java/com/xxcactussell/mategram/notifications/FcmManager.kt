package com.xxcactussell.mategram.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FcmManager(context: Context) {
    private val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        initializeFcm()
    }

    private fun initializeFcm() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM", "Failed to get token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                saveFcmToken(token)
                registerDeviceToken(token)
            }
    }

    fun saveFcmToken(token: String) {
        Log.d("FCM", "Saving token: $token")
        prefs.edit().putString("fcm_token", token).apply()
    }

    fun registerDeviceToken(token: String) {
        api.registerDeviceTokenByClient(token)
    }

    fun getFcmToken(): String? = prefs.getString("fcm_token", null)

    companion object {
        private var instance: FcmManager? = null

        fun getInstance(context: Context): FcmManager {
            return instance ?: synchronized(this) {
                instance ?: FcmManager(context.applicationContext).also { instance = it }
            }
        }
    }
}