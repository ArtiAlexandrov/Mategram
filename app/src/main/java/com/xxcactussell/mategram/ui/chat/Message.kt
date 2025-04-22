package com.xxcactussell.mategram.ui.chat

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageReplyToMessage
import org.drinkless.tdlib.TdApi.MessageVideo
import org.drinkless.tdlib.TdApi.Video
import kotlin.math.roundToInt

@Composable
fun Message() {

}



@Composable
fun RepliedMessage(replyTo: TdApi.MessageReplyTo, viewModel: MainViewModel, onClick: () -> Unit) {
    var messageToReply by remember { mutableStateOf<TdApi.Message?>(null) }
    var messageContent by remember { mutableStateOf<MessageContent?>(null) }
    var messageTextToReply by remember { mutableStateOf<String?>(null) }
    var replyTitle by remember { mutableStateOf("") }

    LaunchedEffect(replyTo) {
        if (replyTo is MessageReplyToMessage) {
            try {
                val chatId = replyTo.chatId
                val messageId = replyTo.messageId
                messageToReply = viewModel.getMessageById(replyTo)

                if (messageToReply != null) {
                    // Получаем контент сообщения
                    messageContent = getMessageContent(chatId, messageId, viewModel)
                    messageTextToReply = messageContent?.textForReply

                    // Получаем имя отправителя
                    when (val sender = messageToReply!!.senderId) {
                        is TdApi.MessageSenderChat -> {
                            val chatReply = api.getChat(sender.chatId)
                            replyTitle = chatReply.title
                        }
                        is TdApi.MessageSenderUser -> {
                            val user = api.getUser(sender.userId)
                            replyTitle = "${user.firstName} ${user.lastName}".trim()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RepliedMessage", "Error loading reply message", e)
            }
        }
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(R.drawable.baseline_reply_24),
            contentDescription = "Reply"
        )

        Spacer(modifier = Modifier.width(4.dp))

        if (replyTitle.isNotEmpty()) {
            Text(
                text = replyTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier
                .clip(
                    CircleShape
                )
                .background(MaterialTheme.colorScheme.onSurface)
                .size(4.dp))
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Text(
                text = "Сообщение удалено",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier
                .clip(
                    CircleShape
                )
                .background(MaterialTheme.colorScheme.onSurface)
                .size(4.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }

        messageContent?.thumbnail?.let {
            ByteArrayImage(
                imageData = it,
                contentDescription = "Медиа в ответе",
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = messageTextToReply ?: "Контент недоступен",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class MediaAlbum(val messages: List<TdApi.Message>, val isOutgoing: Boolean, val replyTo: TdApi.MessageReplyTo?, val date: Int, val id: Long)

@Composable
fun DisplayAlbum(
    messages: List<TdApi.Message>,
    onMediaClick: (Long) -> Unit,
    viewModel: MainViewModel,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    if (messages.isEmpty()) return

    val album = MediaAlbum(
        messages = messages,
        isOutgoing = messages[0].isOutgoing,
        replyTo = messages.firstOrNull { it.replyTo != null }?.replyTo,
        date = messages[0].date,
        id = messages[0].id
    )

    MediaCarousel(
        album = album,
        viewModel = viewModel,
        onMediaClick = { message -> onMediaClick(message.id) },
        downloadedFiles = downloadedFiles
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCarousel(
    album: MediaAlbum,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onMediaClick: (TdApi.Message) -> Unit,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    val carouselState = rememberCarouselState { album.messages.size }

    Column(modifier = modifier) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            preferredItemWidth = 200.dp,
            minSmallItemWidth = 20.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(16.dp)
        ) { index ->
            val message = album.messages[index]
            when (message.content) {
                is MessagePhoto -> {
                    Box(
                        modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                    ) {
                        PhotoContent(message = message, viewModel = viewModel, onMediaClick = onMediaClick, downloadedFiles = downloadedFiles)
                    }
                }
                is MessageVideo -> {
                    Box(
                        modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                    ) {
                        VideoContent(message = message, viewModel = viewModel, onMediaClick = onMediaClick, downloadedFiles = downloadedFiles)
                    }
                }
            }
        }

        // Caption and page indicators
        val firstMessageCaption = album.messages.firstNotNullOfOrNull { message ->
            when (val content = message.content) {
                is MessagePhoto -> content.caption.text.takeIf { !it.isNullOrEmpty() }
                is MessageVideo -> content.caption.text.takeIf { !it.isNullOrEmpty() }
                else -> null
            }
        }



        if (!firstMessageCaption.isNullOrEmpty()) {
            Text(
                text = firstMessageCaption,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PhotoContent(
    message: TdApi.Message,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    downloadedFiles: MutableMap<Int, TdApi. File?>,
    onMediaClick: (TdApi.Message) -> Unit
) {
    val photo = (message.content as MessagePhoto).photo.sizes.lastOrNull()?.photo
    var photoPath by remember { mutableStateOf(photo?.local?.path) }

    LaunchedEffect(photo) {
        if (photo?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(photo)
        } else {
            photoPath = photo?.local?.path
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[photo?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            photoPath = downloadedFile.local?.path
        }
    }


    AsyncImage(
        model = photoPath,
        contentDescription = "Фото сообщения",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .height(320.dp)
            .clickable(onClick = { onMediaClick(message) })
    )
}

@Composable
fun VideoContent(
    message: TdApi.Message,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onMediaClick: (TdApi.Message) -> Unit,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    val videoContent = (message.content as MessageVideo).video
    val thumbnailFile = (videoContent as Video).thumbnail?.file
    var thumbnailPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(thumbnailFile) {
        if (thumbnailFile?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(thumbnailFile)
        } else {
            thumbnailPath = thumbnailFile?.local?.path
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[thumbnailFile?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            thumbnailPath = downloadedFile.local?.path
        }
    }

    Box(
        modifier = modifier
            .height(320.dp)
    ) {
        AsyncImage(
            model = thumbnailPath,
            contentDescription = "Видео превью",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = { onMediaClick(message) },
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_play_arrow_24),
                contentDescription = "Запустить видео",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun ByteArrayImage(
    imageData: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageData) {
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
    }

    Image(
        bitmap = bitmap?.asImageBitmap() ?: ImageBitmap(1, 1),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}


@Composable
fun DraggableBox(
    modifier: Modifier = Modifier,
    onDragComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 20.dp.toPx() }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "dragAnimation"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX <= -swipeThreshold) {
                            onDragComplete()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        // Restrict drag to left only (negative values)
                        offsetX = newOffset.coerceIn(-swipeThreshold, 0f)
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun getAnnotatedString(formattedText: TdApi.FormattedText?): AnnotatedString {
    return buildAnnotatedString {
        if (formattedText != null) {
            formattedText.entities.forEach { entity ->
                val start = entity.offset
                val end = entity.offset + entity.length
                val range = start until end
                when (entity.type) {
                    is TdApi.TextEntityTypeTextUrl -> {
                        addStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            start, end
                        )
                        addLink(
                            url = LinkAnnotation.Url((entity.type as TdApi.TextEntityTypeTextUrl).url),
                            start = start,
                            end = end
                        )
                    }

                    is TdApi.TextEntityTypeUrl -> {
                        val url =
                            formattedText.text.substring(
                                start,
                                end
                            )
                        addStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            ),
                            start, end
                        )
                        addLink(
                            url = LinkAnnotation.Url(url),
                            start = start,
                            end = end
                        )
                    }

                    is TdApi.TextEntityTypeSpoiler -> {
                        addStyle(
                            SpanStyle(
                                background = MaterialTheme.colorScheme.onSurface,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeBold -> {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeItalic -> {
                        addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeCode -> {
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                            ),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypePreCode -> {
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace
                            ),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeMention -> {
                        addStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.primary),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeHashtag -> {
                        addStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.primary),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeUnderline -> {
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeStrikethrough -> {
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypeBlockQuote -> {
                        addStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic
                            ),
                            start, end
                        )
                    }

                    is TdApi.TextEntityTypePre -> {
                        addStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace
                            ),
                            start, end
                        )
                    }
                }
            }
        }
        if (formattedText != null) {
            append(formattedText.text)
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatDuration(seconds: Int): String {
    return when {
        seconds < 3600 -> String.format("%02d:%02d", seconds / 60, seconds % 60)
        else -> String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    }
}

@Composable
fun AnimatedVoiceIndicator(isPlaying: Boolean) {
    if (isPlaying) {
        // При воспроизведении – три полоски, высота которых изменяется случайным образом
        val bar1 = remember { Animatable(10f) }
        val bar2 = remember { Animatable(15f) }
        val bar3 = remember { Animatable(20f) }
        LaunchedEffect(isPlaying) {
            // Параллельные анимации для трёх полосок
            launch {
                while (true) {
                    bar1.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                    )
                }
            }
            launch {
                while (true) {
                    bar2.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 350, easing = LinearEasing)
                    )
                }
            }
            launch {
                while (true) {
                    bar3.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(bar1.value.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(bar2.value.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(bar3.value.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
            )
        }
    } else {
        // При паузе – три маленьких круга 4.dp
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }
        }
    }
}
