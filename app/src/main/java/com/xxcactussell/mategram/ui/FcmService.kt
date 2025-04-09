package com.xxcactussell.mategram.ui

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.xxcactussell.mategram.NotificationViewModel
import com.xxcactussell.mategram.TelegramRepository
import com.xxcactussell.mategram.TelegramRepository.api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicBoolean

class FcmService : FirebaseMessagingService() {
    companion object {
        private var notificationViewModel: NotificationViewModel? = null
        private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val observerLock = AtomicBoolean(false)
    }

    override fun onCreate() {
        super.onCreate()
        TelegramRepository.initialize(this)
        synchronized(FcmService::class.java) {
            if (notificationViewModel == null) {
                notificationViewModel = NotificationViewModel(application)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token received: $token")
        FcmManager.getInstance(applicationContext).saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: ${message.data}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = Gson().toJson(message.data.toMap())
                api.attachClient()

                // Process push notification
                TdApi.ProcessPushNotification(jsonString)

                // Start observer only if not already started
                if (observerLock.compareAndSet(false, true)) {
                    notificationViewModel?.let { viewModel ->
                        if (!viewModel.isNotificationObserverActive()) {
                            Log.d("FCM_NOTIFY", "Starting notification observer")
                            viewModel.startObservingNotifications()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM_NOTIFY", "Error processing notification", e)
                observerLock.set(false)
            }
        }
    }

}