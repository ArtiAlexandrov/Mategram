package com.xxcactussell.mategram.ui.chat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.view.WindowInsets
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.convertUnixTimestampToDateByDay
import com.xxcactussell.mategram.kotlinx.telegram.core.formatFileSize
import com.xxcactussell.mategram.kotlinx.telegram.core.getDayFromDate
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageReplyToMessage
import org.drinkless.tdlib.TdApi.MessageText
import org.drinkless.tdlib.TdApi.MessageVideo
import org.drinkless.tdlib.TdApi.Video
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.FileProvider
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.xxcactussell.mategram.getMimeType
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import org.drinkless.tdlib.TdApi.MessageDocument
import org.drinkless.tdlib.TdApi.MessageSticker
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailPane(
    chatId: Long,
    window: Window,
    onBackClick: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    onShowInfo: () -> Unit
) {
    // Загружаем объект чата асинхронно при изменении chatId.
    var chat: TdApi.Chat? by remember { mutableStateOf(null) }
    LaunchedEffect(chatId) {
        chat = api.getChat(chatId)
        viewModel.getMessagesForChat(chatId)
    }
    var textNewMessage by remember { mutableStateOf("") }
    // Если chat != null, вызываем Flow для получения пути аватарки, иначе используем flowOf(null):
    // Функция remember здесь устанавливает зависимость от chat,
    // так что при изменении chat будет пересчитано значение avatarPath.

    val scope = rememberCoroutineScope()
    val messagesForChat by viewModel.mapOfMessages.getOrPut(chatId) {
        MutableStateFlow(mutableListOf())
    }.collectAsState()
    var groupedMessages = groupMessagesByAlbum(messagesForChat)
    val listState = rememberLazyListState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val photo = chat?.photo?.small
    var avatarPath by remember { mutableStateOf(downloadedFiles[chat?.photo?.small?.id]?.local?.path) }
    var inputMessageToReply by remember { mutableStateOf<TdApi.InputMessageReplyTo?>(null) }
    var messageIdToReply by remember { mutableStateOf<Long?>(null) }
    var messageTextToReply by remember { mutableStateOf<String?>(null) }
    var senderNameForReply by remember { mutableStateOf<String?>(null) }
    var selectedMessageId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(photo) {
        if (photo?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(chat?.photo?.small)
        } else {
            avatarPath = photo?.local?.path
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[photo?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            avatarPath = downloadedFile.local?.path
        }
    }

    var isLoadingMore by remember { mutableStateOf(false) }

    LaunchedEffect(listState, messagesForChat) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
        .collect { visibleItems ->
            if (visibleItems.isEmpty() || isLoadingMore) return@collect

            val lastVisibleItem = visibleItems.last()
            groupedMessages = groupMessagesByAlbum(messagesForChat)

            // Проверяем, близки ли мы к концу списка
            if (lastVisibleItem.index >= groupedMessages.size - 5) {
                val lastMessage = when (val item = groupedMessages.lastOrNull()) {
                    is MediaAlbum -> item.messages.last()
                    is TdApi.Message -> item
                    else -> null
                }

                if (lastMessage != null) {
                    isLoadingMore = true
                    try {
                        viewModel.getMessagesForChat(
                            chatId = chatId,
                            fromMessage = lastMessage.id
                        )
                    } finally {
                        isLoadingMore = false
                    }
                }
            }
        }
    }

// Прокрутка к первому непрочитанному сообщению при первой загрузке чата
    LaunchedEffect(chat?.unreadCount) {
        val unreadIndex = chat?.unreadCount ?: 0
        if (unreadIndex > 0 && unreadIndex < messagesForChat.size) {
            listState.animateScrollToItem(unreadIndex - 1)
        }
    }

// Отслеживание видимых сообщений и отметка их как прочитанных
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isEmpty()) return@collect

                val unreadCount = chat?.unreadCount ?: 0
                if (unreadCount == 0) return@collect

                val visibleIndexes = visibleItems.map { it.index }
                val visibleUnreadMessages = messagesForChat.filterIndexed { index, message ->
                    index in visibleIndexes &&
                            index < unreadCount &&
                            !message.isOutgoing
                }

                visibleUnreadMessages.forEach { message ->
                    viewModel.markAsRead(message)
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Row(
                        modifier = Modifier.clickable {
                            onShowInfo()
                        }
                    )  {
                        if (avatarPath != null) {
                            AsyncImage(
                                model = avatarPath,
                                contentDescription = "Аватарка чата",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        }  else {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                                Text(
                                    text = chat?.title?.firstOrNull()?.toString() ?: "Ч",
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = chat?.title ?: "Безымянный чат",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(R.drawable.baseline_arrow_back_24), contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Row(modifier = Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .background(Color.Transparent),
                verticalAlignment = Alignment.Bottom) {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .wrapContentSize(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row (modifier = Modifier
                        .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom) {
                        IconButton(
                            modifier = Modifier.size(56.dp),
                            onClick = { }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_add_circle_outline_24),
                                contentDescription = "Назад"
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            TextField(
                                value = textNewMessage,
                                onValueChange = { textNewMessage = it },
                                placeholder = { Text("Cообщение") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )
                        }
                        IconButton(
                            modifier = Modifier.size(56.dp),
                            onClick = { }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_insert_emoticon_24),
                                contentDescription = "Назад"
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                FilledIconButton(
                    modifier = Modifier.size(56.dp),
                    onClick = {
                        if (textNewMessage != "") {
                            viewModel.sendMessage(
                                chatId = chatId,
                                text = textNewMessage,
                                replyToMessageId = inputMessageToReply,
                            )
                            textNewMessage = ""
                            inputMessageToReply = null
                        }
                    },
                ) {
                    Icon(painterResource(R.drawable.baseline_send_24), contentDescription = "Extended floating action button.")
                }
            }
        }
    ) { innerPadding ->
        // Основной контент экрана чата.
        Box (modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding))
        {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
            ) {
                items(groupedMessages.size) { index ->
                    var isDateShown by remember { mutableStateOf(false) }
                    var date by remember { mutableStateOf("") }
                    var dateToCompare by remember { mutableLongStateOf(0L) }
                    var nextDateToCompare by remember { mutableLongStateOf(0L) }
                    Spacer(modifier = Modifier.height(8.dp))

                    val messageId = if(groupedMessages[index] is MediaAlbum) {
                        (groupedMessages[index] as MediaAlbum).id
                    } else {
                        (groupedMessages[index] as TdApi.Message).id
                    }

                    val isOutgoing = if(groupedMessages[index] is MediaAlbum) {
                        (groupedMessages[index] as MediaAlbum).isOutgoing
                    } else {
                        (groupedMessages[index] as TdApi.Message).isOutgoing
                    }

                    val replyTo = if(groupedMessages[index] is MediaAlbum) {
                        (groupedMessages[index] as MediaAlbum).replyTo
                    } else {
                        (groupedMessages[index] as TdApi.Message).replyTo
                    }

                    val dateMessage = if (groupedMessages[index] is MediaAlbum) {
                        convertUnixTimestampToDateByDay(
                            (groupedMessages[index] as MediaAlbum).date.toLong()
                        )
                    } else {
                        convertUnixTimestampToDateByDay(
                            (groupedMessages[index] as TdApi.Message).date.toLong()
                        )
                    }

                    DraggableBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onDragComplete = {
                            inputMessageToReply =
                                TdApi.InputMessageReplyToMessage(messageId, null)
                            messageIdToReply = messageId
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (!isOutgoing) Alignment.Start else Alignment.End
                        ) {
                            RepliedMessage(
                                replyTo = replyTo,
                                viewModel = viewModel,
                                onClick = {

                                }
                            )
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 320.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        isDateShown = !isDateShown
                                        date = dateMessage
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!isOutgoing)
                                        MaterialTheme.colorScheme.inversePrimary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            ) {
                                when (val item = groupedMessages[index]) {
                                    is MediaAlbum -> {
                                        MediaCarousel(
                                            album = item,
                                            viewModel = viewModel,
                                            onMediaClick = { message ->
                                                selectedMessageId = message.id
                                            },
                                            downloadedFiles = downloadedFiles
                                        )
                                    }
                                    is TdApi.Message -> {
                                        when (val content = item.content) {
                                            is MessageText -> {
                                                val formattedText = content.text
                                                Text(
                                                    modifier = Modifier.padding(16.dp),
                                                    text = getAnnotatedString(formattedText),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            is MessageVideo -> {
                                                val videoContent = content.video
                                                val thumbnailFile = (videoContent as Video).thumbnail?.file
                                                var thumbnailPath by remember { mutableStateOf<String?>(null) }
                                                val caption = content.caption


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


                                                Column {

                                                    if (caption.text != "" && content.showCaptionAboveMedia) {
                                                        Text(
                                                            modifier = Modifier.padding(16.dp),
                                                            text = getAnnotatedString(caption),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .width(320.dp)
                                                            .height(320.dp)
                                                            .clip(RoundedCornerShape(24.dp))
                                                    ) {
                                                        AsyncImage(
                                                            model = thumbnailPath,
                                                            contentDescription = "Видео превью",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                selectedMessageId = item.id
                                                            },
                                                            modifier = Modifier.align(Alignment.Center)
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.baseline_play_arrow_24),
                                                                contentDescription = "Запустить видео",
                                                                modifier = Modifier.size(64.dp),
                                                                tint = Color.White
                                                            )
                                                        }
                                                    }

                                                    if (caption.text != "" && !content.showCaptionAboveMedia) {
                                                        Text(
                                                            modifier = Modifier.padding(16.dp),
                                                            text = getAnnotatedString(caption),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }

                                                }
                                            }
                                            is MessagePhoto -> {
                                                val photoInChat = content.photo?.sizes?.lastOrNull()?.photo
                                                var photoPath by remember { mutableStateOf(photo?.local?.path) }

                                                LaunchedEffect(photoInChat) {
                                                    if (photoInChat?.local?.isDownloadingCompleted == false) {
                                                        viewModel.downloadFile(photoInChat)
                                                    } else {
                                                        photoPath = photoInChat?.local?.path
                                                    }
                                                }

                                                LaunchedEffect(downloadedFiles.values) {
                                                    val downloadedFile = downloadedFiles[photoInChat?.id]
                                                    if (downloadedFile?.local?.isDownloadingCompleted == true) {
                                                        photoPath = downloadedFile.local?.path
                                                    }
                                                }

                                                val caption = content.caption

                                                Column {
                                                    if (caption.text != "" && content.showCaptionAboveMedia) {
                                                        Text(
                                                            modifier = Modifier.padding(16.dp),
                                                            text = getAnnotatedString(caption),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                    AsyncImage(
                                                        model = photoPath,
                                                        contentDescription = "Фото сообщения",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .width(320.dp)
                                                            .heightIn(max = 320.dp)
                                                            .clip(RoundedCornerShape(24.dp))
                                                            .clickable {
                                                                selectedMessageId = item.id
                                                            }
                                                    )
                                                    if (caption.text != "" && !content.showCaptionAboveMedia) {
                                                        Text(
                                                            modifier = Modifier.padding(16.dp),
                                                            text = getAnnotatedString(caption),
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }
                                            is MessageDocument -> {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp)
                                                ) {
                                                    val document = content.document.document
                                                    val downloadedSize = downloadedFiles[document.id]?.local?.downloadedSize ?: 0L
                                                    val downloadProgress = downloadedSize.toFloat() / document.expectedSize.toFloat()
                                                    val documentThumbnail = content.document.thumbnail?.file
                                                    var documentThumbnailPath by remember { mutableStateOf(documentThumbnail?.local?.path) }
                                                    val documentName = content.document?.fileName.toString()
                                                    val uploadedSize by remember { mutableStateOf<String?>(formatFileSize(document?.expectedSize?.toInt() ?: 0)) }
                                                    var isDownloading by remember { mutableStateOf(false) }
                                                    var isFileDownloaded by remember { mutableStateOf(false) }

                                                    LaunchedEffect(documentThumbnail) {
                                                        if(documentThumbnail?.local?.isDownloadingCompleted == false) {
                                                            viewModel.addFileToDownloads(documentThumbnail, chatId, messageId)
                                                        } else {
                                                            documentThumbnailPath = documentThumbnail?.local?.path
                                                        }
                                                    }

                                                    LaunchedEffect(document) {
                                                        if(document.local.isDownloadingCompleted) {
                                                            isFileDownloaded = true
                                                        }
                                                    }

                                                    LaunchedEffect(downloadedFiles.values) {
                                                        val downloadedFile = downloadedFiles[documentThumbnail?.id]
                                                        if (downloadedFile?.local?.isDownloadingCompleted == true) {
                                                            documentThumbnailPath = downloadedFile.local.path
                                                        }
                                                    }


                                                    LaunchedEffect(downloadedFiles.values) {
                                                        val downloadedFile = downloadedFiles[document?.id]
                                                        if (downloadedFile?.local?.isDownloadingCompleted == true) {
                                                            isFileDownloaded = true
                                                            isDownloading = false
                                                        }
                                                    }

                                                    val context = LocalContext.current

                                                    val onDownloadClick: () -> Unit = {
                                                        scope.launch {
                                                            if (!isFileDownloaded) {
                                                                isDownloading = true
                                                                viewModel.addFileToDownloads(document, chatId, messageId)
                                                            } else {
                                                                val mimeType = getMimeType(documentName)
                                                                val filePath = document.local.path

                                                                if (viewModel.isApkFile(filePath)) {
                                                                    val canInstall = context.packageManager.canRequestPackageInstalls()
                                                                    if (!canInstall) {
                                                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                                            data = Uri.parse("package:${context.packageName}")
                                                                        }
                                                                        context.startActivity(intent) // Открываем настройки для разрешения
                                                                        return@launch
                                                                    }
                                                                    viewModel.installApk(context, filePath)
                                                                } else {
                                                                    val fileUri = FileProvider.getUriForFile(
                                                                        context,
                                                                        "${context.packageName}.provider",
                                                                        File(filePath)
                                                                    )

                                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                        setDataAndType(fileUri, mimeType)
                                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    }

                                                                    try {
                                                                        context.startActivity(intent)
                                                                    } catch (e: ActivityNotFoundException) {
                                                                        Toast.makeText(context, "Нет приложения для открытия файла", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }


                                                    // Отображение в зависимости от состояния файла
                                                    Box(
                                                        modifier = Modifier
                                                            .size(80.dp)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                            .clickable { onDownloadClick() }, // Клик для загрузки
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        when {
                                                            isDownloading -> {
                                                                CircularProgressIndicator(
                                                                    progress = { downloadProgress },
                                                                    modifier = Modifier.size(40.dp),
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                                IconButton (
                                                                    onClick = {
                                                                        scope.launch {
                                                                            viewModel.cancelFileDownload(content.document.document)
                                                                            isDownloading = false
                                                                        }
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        painterResource(R.drawable.baseline_close_24),
                                                                        "Отменить"
                                                                    )
                                                                }
                                                            }

                                                            isFileDownloaded -> {
                                                                if (documentThumbnailPath != null) {
                                                                    AsyncImage(
                                                                        model = documentThumbnailPath,
                                                                        contentDescription = "Документ",
                                                                        modifier = Modifier
                                                                            .size(80.dp)
                                                                            .clip(RoundedCornerShape(16.dp))
                                                                            .clickable { onDownloadClick() }, // Клик для повторной загрузки
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.baseline_insert_drive_file_24),
                                                                        contentDescription = "Документ",
                                                                        modifier = Modifier.size(40.dp),
                                                                        tint = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }

                                                            else -> {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.ic_download),
                                                                    contentDescription = "Скачать документ",
                                                                    modifier = Modifier.size(40.dp),
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    Column {
                                                        Text(
                                                            text = documentName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Row {
                                                            if (isDownloading) {
                                                                Text(
                                                                    text = formatFileSize(downloadedSize.toInt() ?: 0) + " / ",
                                                                    style = MaterialTheme.typography.labelMedium
                                                                )
                                                            }
                                                            if (uploadedSize != null) {
                                                                Text(
                                                                    text = uploadedSize!!,
                                                                    style = MaterialTheme.typography.labelMedium
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            is MessageSticker -> {
                                                val sticker = content.sticker
                                                val stickerFile = sticker?.sticker
                                                var stickerPath by remember { mutableStateOf<String?>(null) }

                                                LaunchedEffect (stickerFile) {
                                                    if (stickerFile?.local?.isDownloadingCompleted == false) {
                                                        viewModel.addFileToDownloads(stickerFile, chatId, messageId)
                                                    } else {
                                                        stickerPath = stickerFile?.local?.path
                                                    }
                                                }

                                                LaunchedEffect(downloadedFiles.values) {
                                                    val downloadedFile = downloadedFiles[stickerFile?.id]
                                                    if (downloadedFile?.local?.isDownloadingCompleted == true) {
                                                        stickerPath = downloadedFile.local?.path
                                                    }
                                                }

                                                if (stickerPath != null) {
                                                    when(sticker.format) {
                                                        is TdApi.StickerFormatWebp -> {
                                                            AsyncImage(
                                                                model = stickerPath,
                                                                contentDescription = "Стикер",
                                                                contentScale = ContentScale.Fit,
                                                                modifier = Modifier
                                                                    .size(200.dp)
                                                                    .clip(RoundedCornerShape(24.dp))
                                                            )
                                                        }
                                                        is TdApi.StickerFormatWebm -> {
                                                            AndroidView(
                                                                factory = { context ->
                                                                    PlayerView(context).apply {
                                                                        val player = ExoPlayer.Builder(context).build()
                                                                        this.player = player
                                                                        val mediaItem = MediaItem.fromUri(stickerPath!!)
                                                                        player.setMediaItem(mediaItem)
                                                                        player.repeatMode = Player.REPEAT_MODE_ALL
                                                                        player.prepare()
                                                                        player.play()
                                                                        this.useController = false
                                                                    }
                                                                },
                                                                modifier = Modifier
                                                                    .size(200.dp)
                                                                    .clip(RoundedCornerShape(24.dp))
                                                            )
                                                        }
                                                        is TdApi.StickerFormatTgs -> {
                                                            val tgsJson = viewModel.decompressTgs(stickerPath!!)
                                                            val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))
                                                            val progress by animateLottieCompositionAsState(composition)

                                                            LottieAnimation(
                                                                composition = composition,
                                                                iterations = Int.MAX_VALUE,
                                                                modifier = Modifier
                                                                    .size(200.dp)
                                                                    .clip(RoundedCornerShape(24.dp))
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(200.dp)
                                                            .clip(RoundedCornerShape(24.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .align(Alignment.Center),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            else -> {
                                                Text(
                                                    modifier = Modifier.padding(16.dp),
                                                    text = stringResource(R.string.unsupportedMessage),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (isDateShown) {
                                Text(
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    text = date,
                                )
                            }
                        }
                    }
                    try {
                        when (groupedMessages[index]) {
                            is MediaAlbum -> {
                                dateToCompare = getDayFromDate(
                                    (groupedMessages[index] as MediaAlbum).date.toLong()
                                )
                            }

                            is TdApi.Message -> {
                                dateToCompare = getDayFromDate(
                                    (groupedMessages[index] as TdApi.Message).date.toLong()
                                )
                            }
                        }
                        when (groupedMessages[index + 1]) {
                            is MediaAlbum -> {
                                nextDateToCompare = getDayFromDate(
                                    (groupedMessages[index + 1] as MediaAlbum).date.toLong()
                                )
                            }

                            is TdApi.Message -> {
                                nextDateToCompare = getDayFromDate(
                                    (groupedMessages[index + 1] as TdApi.Message).date.toLong()
                                )
                            }
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        // Игнорируем ошибку выхода за пределы массива
                    }
                    if (nextDateToCompare < dateToCompare) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = convertUnixTimestampToDateByDay(dateToCompare * 86400),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                // Индикатор загрузки
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            var messageContent = MessageContent(
                null,
                null,
                null
            )
            if (inputMessageToReply != null) {
                LaunchedEffect(messageIdToReply) {
                    var messageToReply: TdApi.Message? = null
                    if (messageIdToReply != null) {
                        messageToReply = api.getMessage(chatId, messageIdToReply!!)

                        if (messageToReply.senderId is TdApi.MessageSenderChat) {
                            val chatReply =
                                api.getChat((messageToReply.senderId as TdApi.MessageSenderChat).chatId)
                            senderNameForReply = chatReply.title
                        } else if (messageToReply.senderId is TdApi.MessageSenderUser) {
                            // Получаем информацию о пользователе
                            val userId =
                                (messageToReply.senderId as TdApi.MessageSenderUser).userId
                            val user = api.getUser(userId)
                            senderNameForReply =
                                user.firstName + " " + user.lastName
                            // Извлекаем имя и фамилию пользователя
                        }
                    }
                    messageContent =
                        messageToReply?.let { getMessageContent(chatId, it.id, viewModel) }!!
                    messageTextToReply = messageContent.textForReply
                }
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                visible = inputMessageToReply != null,
                enter = slideIn { IntOffset(0, it.height * 2) } + fadeIn(),
                exit = slideOut { IntOffset(0, it.height * 2) } + fadeOut()
            ) {
                ElevatedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(40.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.baseline_reply_24), "Ответ"
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        if (messageContent.thumbnail != null) {
                            ByteArrayImage(
                                imageData = messageContent.thumbnail!!,
                                contentDescription = "Медиа в ответе",
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$senderNameForReply",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$messageTextToReply",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                inputMessageToReply = null
                                messageIdToReply = null
                            }
                        ) {
                            Icon(painterResource(R.drawable.baseline_close_24), "Отменить")
                        }
                    }
                }
            }

        }
    }
    selectedMessageId?.let { messageId ->
        ChatImageViewer(
            chatId = chatId,
            messageId = messageId,
            viewModel = viewModel,
            window = window,
            onDismiss = {
                window.insetsController?.show(WindowInsets.Type.systemBars())
                selectedMessageId = null
            }
        )
    }
}

@Composable
fun RepliedMessage(replyTo: TdApi.MessageReplyTo?, viewModel: MainViewModel, onClick: () -> Unit) {
    if (replyTo != null) {
        var messageToReply by remember { mutableStateOf<TdApi.Message?>(null) }
        var messageContent by remember { mutableStateOf<MessageContent?>(null) }
        var messageTextToReply by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(replyTo) {
            if (replyTo is MessageReplyToMessage) {
                messageToReply = viewModel.getMessageById(replyTo)
            }
            messageContent = messageToReply?.let { getMessageContent(it.chatId, it.id, viewModel) }!!
            messageTextToReply = messageContent?.textForReply
            Log.d("MESSAGE TH", "Message thumbnail: ${messageContent!!.thumbnail}")
            Log.d("MESSAGE TH", "Message to reply: $messageToReply")
        }

        var replyTitle by remember { mutableStateOf("") }
        LaunchedEffect(messageToReply) {
            if (messageToReply != null) {
                if (messageToReply!!.senderId is TdApi.MessageSenderChat) {
                    val chatReply =
                        api.getChat((messageToReply!!.senderId as TdApi.MessageSenderChat).chatId)
                    replyTitle = chatReply.title
                } else if (messageToReply!!.senderId is TdApi.MessageSenderUser) {
                    // Получаем информацию о пользователе
                    val userId =
                        (messageToReply!!.senderId as TdApi.MessageSenderUser).userId
                    val user = api.getUser(userId)
                    replyTitle =
                        user.firstName + " " + user.lastName
                    // Извлекаем имя и фамилию пользователя
                }
            }
        }
        Row(
            modifier = Modifier
                .widthIn(max = 200.dp)
                .clickable {
                    onClick()
                }
        ) {
            Column {
                Text(
                    text = replyTitle,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    messageContent?.thumbnail?.let {
                        ByteArrayImage(
                            imageData = it,
                            contentDescription = "Медиа в ответе",
                            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (messageTextToReply != null) {
                        Text(
                            text = messageTextToReply!!,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}


private fun groupMessagesByAlbum(messages: List<TdApi.Message>): List<Any> {
    val result = mutableListOf<Any>()
    var currentAlbumMessages = mutableListOf<TdApi.Message>()

    messages.forEach { message ->
        if (message.mediaAlbumId != 0L) {
            if (currentAlbumMessages.isEmpty() ||
                currentAlbumMessages[0].mediaAlbumId == message.mediaAlbumId
            ) {
                currentAlbumMessages.add(message)
            } else {
                if (currentAlbumMessages.size > 1) {
                    val isOutgoing = currentAlbumMessages[0].isOutgoing
                    val date = currentAlbumMessages[0].date
                    val id = currentAlbumMessages[0].id
                    // Находим первое сообщение с replyTo в альбоме
                    val replyTo = currentAlbumMessages.firstOrNull { it.replyTo != null }?.replyTo
                    result.add(MediaAlbum(
                        messages = currentAlbumMessages.toList(),
                        isOutgoing = isOutgoing,
                        replyTo = replyTo,
                        date = date,
                        id = id
                    ))
                } else {
                    result.add(currentAlbumMessages[0])
                }
                currentAlbumMessages = mutableListOf(message)
            }
        } else {
            if (currentAlbumMessages.isNotEmpty()) {
                if (currentAlbumMessages.size > 1) {
                    val isOutgoing = currentAlbumMessages[0].isOutgoing
                    val date = currentAlbumMessages[0].date
                    val id = currentAlbumMessages[0].id
                    // Находим первое сообщение с replyTo в альбоме
                    val replyTo = currentAlbumMessages.firstOrNull { it.replyTo != null }?.replyTo
                    result.add(MediaAlbum(
                        messages = currentAlbumMessages.toList(),
                        isOutgoing = isOutgoing,
                        replyTo = replyTo,
                        date = date,
                        id = id
                    ))
                } else {
                    result.add(currentAlbumMessages[0])
                }
                currentAlbumMessages = mutableListOf()
            }
            result.add(message)
        }
    }

    if (currentAlbumMessages.isNotEmpty()) {
        if (currentAlbumMessages.size > 1) {
            val isOutgoing = currentAlbumMessages[0].isOutgoing
            val date = currentAlbumMessages[0].date
            val id = currentAlbumMessages[0].id
            // Находим первое сообщение с replyTo в альбоме
            val replyTo = currentAlbumMessages.firstOrNull { it.replyTo != null }?.replyTo
            result.add(MediaAlbum(
                messages = currentAlbumMessages.toList(),
                isOutgoing = isOutgoing,
                replyTo = replyTo,
                date = date,
                id = id
            ))
        } else {
            result.add(currentAlbumMessages[0])
        }
    }

    return result
}

// Создадим класс для хранения альбома
data class MediaAlbum(val messages: List<TdApi.Message>, val isOutgoing: Boolean, val replyTo: TdApi.MessageReplyTo?, val date: Int, val id: Long)

// Создадим Composable для отображения карусели
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
            .clickable(onClick = { onMediaClick(message) } )
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
private fun getAnnotatedString(formattedText: TdApi.FormattedText): AnnotatedString {
    return buildAnnotatedString {
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
        append(formattedText.text)
    }
}