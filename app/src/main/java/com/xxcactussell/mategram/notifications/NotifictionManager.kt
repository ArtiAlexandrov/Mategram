package com.xxcactussell.mategram.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.xxcactussell.mategram.MainActivity
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.downloadFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

object NotificationHelper {
    private const val CHANNEL_ID = "telegram_messages"
    private const val GROUP_KEY = "telegram_messages_group"

    data class MessageInfo(
        val text: String,
        val timestamp: Long,
        val senderName: String
    )

    fun showMessageNotification(
        context: Context,
        chatId: Long,
        chatTitle: String,
        messages: List<MessageInfo>,
        chatPhotoFile: TdApi.File?,
        notificationId: Int,
        unreadCount: Int,
        isChannelPost: Boolean
    ) {
        val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        viewModelScope.launch {
            // Convert TDLib file to bitmap
            val iconBitmap = chatPhotoFile?.let { file ->
                try {
                    val localFile = api.downloadFile(
                        file.id,
                        1,
                        0,
                        0,
                        true
                    )

                    BitmapFactory.decodeFile(localFile.local?.path)?.let { bitmap ->
                        // Create circular bitmap for avatar
                        val output = Bitmap.createBitmap(
                            bitmap.width,
                            bitmap.height,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(output)
                        val paint = Paint().apply {
                            isAntiAlias = true
                            shader =
                                BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                        }
                        canvas.drawCircle(
                            bitmap.width / 2f,
                            bitmap.height / 2f,
                            bitmap.width / 2f,
                            paint
                        )
                        output
                    }
                } catch (e: Exception) {
                    Log.e("NotificationHelper", "Error loading avatar", e)
                    null
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("chat_id", chatId)
                putExtra("from_notification", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                chatId.toInt(), // Use chatId as request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create messaging style
            val messagingStyle = NotificationCompat.MessagingStyle(
                Person.Builder().apply {
                    setName(chatTitle)
                }.build()
            )
                .setConversationTitle(chatTitle)
                .setGroupConversation(false)

            // Add messages in reverse order (newest first)
            messages.forEach { messageInfo ->
                val message = NotificationCompat.MessagingStyle.Message(
                    messageInfo.text,
                    messageInfo.timestamp,
                    Person.Builder().apply {
                        setName(messageInfo.senderName)
                    }.build()
                )
                messagingStyle.addMessage(message)
            }
            var notification: android.app.Notification? = null
            if (isChannelPost) {
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle(messages.first().senderName)
                    .setSummaryText("$unreadCount новых сообщений")

                messages.forEach { message ->
                    inboxStyle.addLine(message.text)
                }

                notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(messages.first().senderName)
                    .setContentText(messages.first().text)
                    .setLargeIcon(iconBitmap)
                    .setStyle(inboxStyle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY)
                    .setNumber(unreadCount)
                    .build()
            } else {
                notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(iconBitmap)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY)
                    .setNumber(unreadCount)
                    .setStyle(messagingStyle)
                    .build()
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }
    }
}