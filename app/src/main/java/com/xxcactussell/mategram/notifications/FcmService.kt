package com.xxcactussell.mategram.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.xxcactussell.mategram.NotificationViewModel
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramCredentials
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getAuthorizationState
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setTdlibParameters
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
                Log.d("FCM", "Processing message: ${message.data}")
                val jsonString = Gson().toJson(message.data.toMap())
                api.attachClient()


                when (val state = api.getAuthorizationState()) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        Log.d("FCM", "Initializing TDLib parameters")
                        api.setTdlibParameters(
                            TelegramCredentials.USE_TEST_DC,
                            TelegramCredentials.databaseDirectory,
                            TelegramCredentials.filesDirectory,
                            TelegramCredentials.encryptionKey,
                            TelegramCredentials.USE_FILE_DATABASE,
                            TelegramCredentials.USE_CHAT_INFO_DATABASE,
                            TelegramCredentials.USE_MESSAGE_DATABASE,
                            TelegramCredentials.USE_SECRET_CHATS,
                            TelegramCredentials.API_ID,
                            TelegramCredentials.API_HASH,
                            TelegramCredentials.systemLanguageCode,
                            TelegramCredentials.deviceModel,
                            TelegramCredentials.systemVersion,
                            TelegramCredentials.APPLICATION_VERSION
                        )
                    }
                }


                // Start observer only if not already started
                if (observerLock.compareAndSet(false, true)) {
                    notificationViewModel?.let { viewModel ->
                        if (!viewModel.isNotificationObserverActive()) {
                            Log.d("FCM_NOTIFY", "Starting notification observer")
                            viewModel.startObservingNotifications()
                        }
                    }
                }

                // Process push notification
                TdApi.ProcessPushNotification(jsonString)

            } catch (e: Exception) {
                Log.e("FCM_NOTIFY", "Error processing notification", e)
                observerLock.set(false)
            }
        }
    }

}