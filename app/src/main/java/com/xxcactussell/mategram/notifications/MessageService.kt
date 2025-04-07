package com.xxcactussell.mategram.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.TelegramRepository
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class MessageService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification()) // Запускаем сервис с уведомлением
        listenForMessages() // Запускаем поток сбора сообщений
        return START_STICKY
    }

    private fun listenForMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            TelegramRepository.messageFlow.collect { message ->
                sendNotification(applicationContext, message)
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "message_service"
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "Фоновый сервис", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Сервис проверки новых сообщений запущен")
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


suspend fun sendNotification(context: Context, message: TdApi.Message) {
    val chat = api.getChat(message.chatId)
    println("Notification for Chat: $chat")
    val notificationSettings = chat.notificationSettings as TdApi.ChatNotificationSettings
    val isMuted = if (notificationSettings.useDefaultMuteFor) {
        chat.defaultDisableNotification
    } else {
        !chat.defaultDisableNotification
    }
    if (!message.isOutgoing && isMuted) {
        val channelId = "new_messages"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val chatTitle = chat.title
        val messageText = if (message.content is TdApi.MessageText) {
            (message.content as TdApi.MessageText).text.text
        } else {
            "Вложение"
        }

        // Загружаем аватарку чата
        val avatarBitmap: Bitmap? = chat.photo?.small?.let { photoSize ->
            val imageUrl = api.getFile(photoSize.id).local?.path
            imageUrl?.let { BitmapFactory.decodeFile(it) }
        }

        // Создаём канал уведомлений (Android 8+)
        val channel = NotificationChannel(
            channelId,
            "Новые сообщения",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Убедитесь, что у вас есть подходящая иконка
            .setContentTitle(chatTitle)
            .setContentText(messageText)
            .setLargeIcon(avatarBitmap) // Добавляем аватарку чата
            .setAutoCancel(true)

        notificationManager.notify(message.id.toInt(), notificationBuilder.build())
    }
}