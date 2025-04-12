package com.xxcactussell.mategram.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import org.drinkless.tdlib.TdApi

@Composable
fun ChatInfoContent(
    chat: TdApi.Chat,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {

    var avatarPath by remember { mutableStateOf<String?>("") }
    LaunchedEffect(chat.photo) {
        avatarPath = viewModel.getChatAvatarPath(chat, "b")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Фото чата
        if (chat.photo != null) {
            AsyncImage(
                model = avatarPath,
                contentDescription = "Фото чата",
                modifier = Modifier
                    .height(200.dp)
                    .width(200.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .align(Alignment.CenterHorizontally),

                contentScale = ContentScale.Crop

            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Название чата
        Text(
            text = chat.title,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Тип чата
        val chatType = when (chat.type) {
            is TdApi.ChatTypePrivate -> "Личный чат"
            is TdApi.ChatTypeBasicGroup -> "Группа"
            is TdApi.ChatTypeSupergroup ->
                if ((chat.type as TdApi.ChatTypeSupergroup).isChannel)
                    "Канал" else "Супергруппа"
            else -> "Неизвестный тип"
        }
        Text(
            text = chatType,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Статистика
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Сообщений:")
                }
                if (chat.type is TdApi.ChatTypeSupergroup) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Подписчиков:")
                    }
                }
            }
        }
    }
}