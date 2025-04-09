package com.xxcactussell.mategram.ui

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.TelegramRepository
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.processPushNotification
import org.drinkless.tdlib.TdApi

class FcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token received: $token")
        FcmManager.getInstance(applicationContext).saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: $message")
        // Process message...

        TelegramRepository.initialize(this)
        api.attachClient()

        MainViewModel(application).observeNotifications()
    }
}