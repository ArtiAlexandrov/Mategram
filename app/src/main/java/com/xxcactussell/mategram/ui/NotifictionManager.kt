package com.xxcactussell.mategram.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.xxcactussell.mategram.MainActivity
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.downloadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

object NotificationHelper {
    private const val CHANNEL_ID = "telegram_messages"
    private const val GROUP_KEY = "telegram_messages_group"

    fun showMessageNotification(
        context: Context,
        chatId: Long,
        chatTitle: String,
        messages: List<String>,
        chatPhotoFile: TdApi.File?,
        notificationId: Int,
        unreadCount: Int
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("chat_id", chatId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(chatTitle)
                .setSummaryText("$unreadCount новых сообщений")

            messages.asReversed().forEach { message ->
                inboxStyle.addLine(message)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_local_fire_department_24)
                .setLargeIcon(iconBitmap)
                .setContentTitle(chatTitle)
                .setContentText(messages.first())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY)
                .setStyle(inboxStyle)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        }
    }
}