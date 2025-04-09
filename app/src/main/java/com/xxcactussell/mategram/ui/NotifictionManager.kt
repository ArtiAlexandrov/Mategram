package com.xxcactussell.mategram.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import com.xxcactussell.mategram.MainActivity
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatHistory
import org.drinkless.tdlib.TdApi

object NotificationManager {
    private const val CHANNEL_ID = "telegram_messages"
    private const val CHANNEL_NAME = "Messages"

    fun createNotificationChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о новых сообщениях"
            enableLights(true)
            enableVibration(true)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    fun showMessageNotification(
        context: Context,
        chatId: Long,
        chatTitle: String,
        message: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_local_fire_department_24)
            .setContentTitle(chatTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}