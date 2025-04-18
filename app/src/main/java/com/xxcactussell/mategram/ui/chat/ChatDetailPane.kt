package com.xxcactussell.mategram.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.view.WindowInsets
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.xxcactussell.mategram.formatCompactNumber
import com.xxcactussell.mategram.getMimeType
import com.xxcactussell.mategram.kotlinx.telegram.core.convertUnixTimestampToDate
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getBasicGroup
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getBasicGroupFullInfo
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatAdministrators
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getSupergroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.TdApi.BasicGroup
import org.drinkless.tdlib.TdApi.ChatActionCancel
import org.drinkless.tdlib.TdApi.ChatActionChoosingContact
import org.drinkless.tdlib.TdApi.ChatActionChoosingLocation
import org.drinkless.tdlib.TdApi.ChatActionChoosingSticker
import org.drinkless.tdlib.TdApi.ChatActionRecordingVideo
import org.drinkless.tdlib.TdApi.ChatActionRecordingVideoNote
import org.drinkless.tdlib.TdApi.ChatActionRecordingVoiceNote
import org.drinkless.tdlib.TdApi.ChatActionStartPlayingGame
import org.drinkless.tdlib.TdApi.ChatActionTyping
import org.drinkless.tdlib.TdApi.ChatActionUploadingDocument
import org.drinkless.tdlib.TdApi.ChatActionUploadingPhoto
import org.drinkless.tdlib.TdApi.ChatActionUploadingVideo
import org.drinkless.tdlib.TdApi.ChatActionUploadingVideoNote
import org.drinkless.tdlib.TdApi.ChatActionUploadingVoiceNote
import org.drinkless.tdlib.TdApi.ChatActionWatchingAnimations
import org.drinkless.tdlib.TdApi.ChatTypeBasicGroup
import org.drinkless.tdlib.TdApi.ChatTypePrivate
import org.drinkless.tdlib.TdApi.ChatTypeSupergroup
import org.drinkless.tdlib.TdApi.EndGroupCallRecording
import org.drinkless.tdlib.TdApi.MessageAnimation
import org.drinkless.tdlib.TdApi.MessageDocument
import org.drinkless.tdlib.TdApi.MessageOriginChannel
import org.drinkless.tdlib.TdApi.MessageOriginChat
import org.drinkless.tdlib.TdApi.MessageOriginUser
import org.drinkless.tdlib.TdApi.MessageSticker
import org.drinkless.tdlib.TdApi.MessageVoiceNote
import org.drinkless.tdlib.TdApi.Supergroup
import org.drinkless.tdlib.TdApi.User
import org.drinkless.tdlib.TdApi.UserStatusLastMonth
import org.drinkless.tdlib.TdApi.UserStatusLastWeek
import org.drinkless.tdlib.TdApi.UserStatusOffline
import org.drinkless.tdlib.TdApi.UserStatusOnline
import org.drinkless.tdlib.TdApi.UserStatusRecently
import org.drinkless.tdlib.TdApi.UserType
import org.drinkless.tdlib.TdApi.UserTypeBot
import org.drinkless.tdlib.TdApi.VoiceNote
import java.io.File
import kotlin.math.roundToInt

private data class AlbumState(
    val messages: MutableList<TdApi.Message> = mutableListOf(),
    var currentAlbumId: Long = 0L
)

@Composable
fun ChatDetailPane(
    chatId: Long,
    chat: TdApi.Chat,
    window: Window,
    onBackClick: (Int) -> Unit,
    viewModel: MainViewModel = viewModel(),
    onShowInfo: () -> Unit,
    isVoicePlaying: Boolean,
    idMessageOfVoiceNote: Long?,
    onTogglePlay: (Long, MessageVoiceNote, Long) -> Unit
) {

    // Загружаем объект чата асинхронно при изменении chatId.
    LaunchedEffect(chatId) {
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
    val listState = rememberLazyListState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val photo = chat?.photo?.small
    var avatarPath by remember { mutableStateOf(downloadedFiles[chat?.photo?.small?.id]?.local?.path) }
    var inputMessageToReply by remember { mutableStateOf<TdApi.InputMessageReplyTo?>(null) }
    var messageIdToReply by remember { mutableStateOf<Long?>(null) }
    var messageTextToReply by remember { mutableStateOf<String?>(null) }
    var senderNameForReply by remember { mutableStateOf<String?>(null) }
    var selectedMessageId by remember { mutableStateOf<Long?>(null) }
    var currentMessageMode by remember { mutableStateOf("voice") }
    val lastMessageMode by remember { mutableStateOf("voice") }
    var isRecording by remember { mutableStateOf(false) }

    val chatPermissions by viewModel.chatPermissions.collectAsState()
    LaunchedEffect(chatId) {
        viewModel.updateChatPermissions(chat)
    }

    val isCanWrite = chatPermissions[chatId]?.canSendBasicMessages == true

    BackHandler(enabled = true) {
        onBackClick(listState.firstVisibleItemIndex)
    }

    LaunchedEffect(photo) {
        if (photo?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(chat.photo?.small)
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

                // Проверяем, близки ли мы к концу списка
                if (lastVisibleItem.index >= messagesForChat.size - 5) {
                    val lastMessage = messagesForChat.lastOrNull()

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

    LaunchedEffect(idMessageOfVoiceNote, isVoicePlaying) {
        Log.d("VoiceNote", "LaunchedEffect triggered: messageId=$idMessageOfVoiceNote, isPlaying=$isVoicePlaying")

        if (idMessageOfVoiceNote != null && !isVoicePlaying) {
            delay(100)

            // Ищем текущее сообщение
            val currentIndex = messagesForChat.indexOfFirst { message ->
                val found = message.id == idMessageOfVoiceNote
                if (found) Log.d("VoiceNote", "Found current message at index")
                found
            }

            if (currentIndex != -1) {
                // Ищем следующее голосовое сообщение в обратном направлении
                val nextVoiceNote = messagesForChat.take(currentIndex).reversed().firstOrNull { message ->
                    val isVoiceNote = message.content is MessageVoiceNote &&
                            !(message.content as MessageVoiceNote).isListened
                    if (isVoiceNote) Log.d(
                        "VoiceNote",
                        "Found next unlistened voice note: ${message.id}"
                    )
                    isVoiceNote
                }

                if (nextVoiceNote is TdApi.Message && nextVoiceNote.content is MessageVoiceNote) {
                    Log.d("VoiceNote", "Playing next voice note: ${nextVoiceNote.id}")
                    viewModel.markVoiceNoteAsListened(chatId, idMessageOfVoiceNote) // Отмечаем текущее как прослушанное
                    delay(100) // Даем время на обновление состояния
                    onTogglePlay(
                        nextVoiceNote.id,
                        nextVoiceNote.content as MessageVoiceNote,
                        chatId
                    )
                } else {
                    Log.d("VoiceNote", "No more unlistened voice notes found")
                    viewModel.markVoiceNoteAsListened(chatId, idMessageOfVoiceNote) // Отмечаем последнее как прослушанное
                }
            } else {
                Log.d("VoiceNote", "Current message not found in the list")
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
            ChatTopBar(
                chat = chat,
                avatarPath = avatarPath,
                onShowInfo = { onShowInfo() },
                onBackClick = { index -> onBackClick(index) },
                listState = listState,
                viewModel = viewModel
            )
        },
        bottomBar = {
            if (isCanWrite) {
                ChatBottomBar(
                    chatId = chatId,
                    textNewMessage = textNewMessage,
                    onTextChange = { textNewMessage = it },
                    currentMessageMode = currentMessageMode,
                    lastMessageMode = lastMessageMode,
                    onCurrentModeChange = { currentMessageMode = it },
                    isRecording = isRecording,
                    onRecordingChange = { isRecording = it },
                    inputMessageToReply = inputMessageToReply,
                    onReplyChange = { inputMessageToReply = it },
                    viewModel = viewModel,
                    onModeChange = {
                        if (currentMessageMode == "text") {
                            if (textNewMessage.isNotEmpty()) {
                                viewModel.sendMessage(
                                    chatId = chatId,
                                    text = textNewMessage,
                                    replyToMessageId = inputMessageToReply,
                                )
                                textNewMessage = ""
                                inputMessageToReply = null
                                currentMessageMode = lastMessageMode
                            }
                        } else if (currentMessageMode == "voice") {
                            currentMessageMode = "video"
                        } else if (currentMessageMode == "video") {
                            currentMessageMode = "voice"
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Основной контент экрана чата.
        Box (modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding))
        {
            val albumState = remember { AlbumState() }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
            ) {

                items(messagesForChat.size) { index ->
                    var isDateShown by remember { mutableStateOf(false) }
                    var date by remember { mutableStateOf("") }
                    var dateToCompare by remember { mutableLongStateOf(0L) }
                    var nextDateToCompare by remember { mutableLongStateOf(0L) }
                    var senderTitle by remember { mutableStateOf("") }
                    var isForwarded by remember { mutableStateOf(false) }


                    LaunchedEffect(messagesForChat[index]) {
                        val message = messagesForChat[index]

                        // Проверяем forwardInfo вместо сравнения senderId
                        if (message.forwardInfo != null) {
                            isForwarded = true
                            when (val origin = message.forwardInfo!!.origin) {
                                is MessageOriginUser -> {
                                    val user = api.getUser(origin.senderUserId)
                                    senderTitle = "${user.firstName} ${user.lastName}".trim()
                                }
                                is MessageOriginChat -> {
                                    val chatForward = api.getChat(origin.senderChatId)
                                    senderTitle = chatForward.title
                                }
                                is MessageOriginChannel -> {
                                    val chatForward = api.getChat(origin.chatId)
                                    senderTitle = chatForward.title
                                    if (origin.authorSignature.isNotEmpty()) {
                                        senderTitle += " (${origin.authorSignature})"
                                    }
                                }
                                else -> {
                                    senderTitle = "Переслано"
                                }
                            }
                        } else {
                            isForwarded = false
                            // Если сообщение не переслано, получаем имя отправителя как обычно
                            when (val sender = message.senderId) {
                                is TdApi.MessageSenderChat -> {
                                    val chatForward = api.getChat(sender.chatId)
                                    senderTitle = chatForward.title
                                }
                                is TdApi.MessageSenderUser -> {
                                    val user = api.getUser(sender.userId)
                                    senderTitle = "${user.firstName} ${user.lastName}".trim()
                                }
                            }
                        }
                    }

                    val message = messagesForChat[index]
                    val nextMessage = if (index + 1 < messagesForChat.size) messagesForChat[index + 1] else null
                    val previousMessage = if (index > 0) messagesForChat[index - 1] else null

                    var senderName by remember { mutableStateOf("") }
                    var senderAvatarPath by remember { mutableStateOf<String?>(null) }
                    var showAuthor by remember { mutableStateOf(false) }
                    var showPlaceholder by remember { mutableStateOf(false) }



                    fun shouldShowAuthorInfo(): Boolean {
                        return when {
                            message.isOutgoing -> false
                            chat.type is TdApi.ChatTypePrivate -> false
                            message.isChannelPost -> false
                            else -> {
                                if (nextMessage == null) {
                                    showPlaceholder = true
                                    return true
                                }

                                when (message.senderId) {
                                    is TdApi.MessageSenderUser -> {
                                        val currentUserId = (message.senderId as TdApi.MessageSenderUser).userId
                                        when (val prevSender = nextMessage.senderId) {
                                            is TdApi.MessageSenderUser ->
                                            {
                                                showPlaceholder = true
                                                currentUserId != prevSender.userId
                                            }
                                            else -> true
                                        }
                                    }
                                    is TdApi.MessageSenderChat -> {
                                        val currentChatId = (message.senderId as TdApi.MessageSenderChat).chatId
                                        when (val prevSender = nextMessage.senderId) {
                                            is TdApi.MessageSenderChat -> {
                                                showPlaceholder = true
                                                currentChatId != prevSender.chatId
                                            }
                                            else -> true
                                        }
                                    }
                                    else -> true
                                }
                            }
                        }
                    }

                    LaunchedEffect(message) {
                        when (message.senderId) {
                            is TdApi.MessageSenderUser -> {
                                val userId = (message.senderId as TdApi.MessageSenderUser).userId
                                val user = api.getUser(userId)
                                senderName = when {
                                    message.isChannelPost && !message.authorSignature.isNullOrEmpty() ->
                                        message.authorSignature
                                    else -> "${user.firstName} ${user.lastName}".trim()
                                }
                                // Get user's avatar only for groups
                                if (shouldShowAuthorInfo() && user.profilePhoto != null) {
                                    val photoProfile = user.profilePhoto?.small
                                    if (photoProfile?.local?.isDownloadingCompleted == false) {
                                        viewModel.downloadFile(photoProfile)
                                    } else {
                                        senderAvatarPath = photoProfile?.local?.path
                                    }
                                }
                            }
                            is TdApi.MessageSenderChat -> {
                                senderName = chat.title
                                // Get chat's avatar only for groups
                                if (shouldShowAuthorInfo() && chat.photo != null) {
                                    val photoProfile = chat.photo?.small
                                    if (photoProfile?.local?.isDownloadingCompleted == false) {
                                        viewModel.downloadFile(photoProfile)
                                    } else {
                                        senderAvatarPath = photoProfile?.local?.path
                                    }
                                }
                            }
                        }
                        showAuthor = shouldShowAuthorInfo()
                    }


                    if (message.mediaAlbumId != 0L) {
                        // Если это первое сообщение альбома или предыдущее сообщение из другого альбома
                        if (previousMessage?.mediaAlbumId != message.mediaAlbumId) {
                            DraggableBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp, 0.dp, 16.dp, 8.dp),
                                onDragComplete = {
                                    inputMessageToReply = TdApi.InputMessageReplyToMessage(message.id, null)
                                    messageIdToReply = message.id
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Show avatar only for groups and if sender changes
                                    if (showPlaceholder && !showAuthor) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                        )
                                    }
                                    if (showAuthor) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                        ) {
                                            if (senderAvatarPath != null) {
                                                AsyncImage(
                                                    model = senderAvatarPath,
                                                    contentDescription = "Аватар отправителя",
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = senderName.firstOrNull()?.toString()
                                                            ?: "?",
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (!message.isOutgoing || message.isChannelPost) Alignment.Start else Alignment.End
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .widthIn(max = 320.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .clickable {
                                                    isDateShown = !isDateShown
                                                    date =
                                                        convertUnixTimestampToDate(message.date.toLong())
                                                },
                                            shape = RoundedCornerShape(24.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (!message.isOutgoing)
                                                    MaterialTheme.colorScheme.inversePrimary
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            if (showAuthor) {
                                                Card(
                                                    modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                ) {
                                                    Text(
                                                        modifier = Modifier.padding(8.dp, 4.dp),
                                                        text = senderName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                            // Собираем все сообщения альбома
                                            val albumMessages = mutableListOf<TdApi.Message>()
                                            var i = index
                                            while (i < messagesForChat.size && messagesForChat[i].mediaAlbumId == message.mediaAlbumId) {
                                                albumMessages.add(messagesForChat[i])
                                                i++
                                            }


                                            if (isForwarded) {
                                                Card(
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .height(32.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 8.dp,
                                                            vertical = 8.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.mdi__share_all),
                                                            contentDescription = "Reply"
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = senderTitle,
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                color = MaterialTheme.colorScheme.primary
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            if (message.replyTo is MessageReplyToMessage) {
                                                Card(
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .height(32.dp)
                                                        .clickable {
                                                            scope.launch {
                                                                if (message.replyTo !is MessageReplyToMessage) return@launch

                                                                // Сначала ищем сообщение в текущем списке
                                                                val indexReply =
                                                                    messagesForChat.indexOfFirst { it.id == (message.replyTo as MessageReplyToMessage).messageId }
                                                                if (indexReply != -1) {
                                                                    // Сообщение найдено, прокручиваем к нему
                                                                    listState.animateScrollToItem(
                                                                        indexReply
                                                                    )
                                                                    return@launch
                                                                }

                                                                // Если сообщение не найдено, начинаем загрузку
                                                                isLoadingMore = true
                                                                try {
                                                                    var lastMessageId =
                                                                        messagesForChat.lastOrNull()?.id
                                                                    while (lastMessageId != null && !isLoadingMore) {
                                                                        // Загружаем следующую порцию сообщений
                                                                        viewModel.getMessagesForChat(
                                                                            chatId = chatId,
                                                                            fromMessage = lastMessageId!!
                                                                        )

                                                                        // Проверяем, появилось ли нужное сообщение
                                                                        val newIndex =
                                                                            messagesForChat.indexOfFirst { it.id == (message.replyTo as MessageReplyToMessage).messageId }
                                                                        if (newIndex != -1) {
                                                                            // Нашли сообщение, прокручиваем к нему
                                                                            listState.animateScrollToItem(
                                                                                newIndex
                                                                            )
                                                                            break
                                                                        }

                                                                        // Если достигли конца и сообщение не найдено
                                                                        if (listState.isLastItemVisible()) {
                                                                            lastMessageId =
                                                                                messagesForChat.lastOrNull()?.id
                                                                        } else {
                                                                            break
                                                                        }

                                                                        delay(100) // Небольшая задержка между загрузками
                                                                    }
                                                                } finally {
                                                                    isLoadingMore = false
                                                                }
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                                    )
                                                ) {
                                                    RepliedMessage(
                                                        replyTo = message.replyTo as MessageReplyToMessage,
                                                        viewModel = viewModel,
                                                        onClick = { }
                                                    )
                                                }
                                            }

                                            // Отображаем альбом только один раз - при первом сообщении альбома
                                            DisplayAlbum(
                                                messages = albumMessages,
                                                onMediaClick = { selectedMessageId = it },
                                                viewModel = viewModel,
                                                downloadedFiles = downloadedFiles
                                            )
                                        }
                                        if (isDateShown) {
                                            Text(
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                text = date
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        DraggableBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 0.dp, 16.dp, 8.dp),
                            onDragComplete = {
                                inputMessageToReply = TdApi.InputMessageReplyToMessage(message.id, null)
                                messageIdToReply = message.id
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Show avatar only for groups and if sender changes
                                if (showPlaceholder && !showAuthor) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                    )
                                }
                                if (showAuthor) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                    ) {
                                        if (senderAvatarPath != null) {
                                            AsyncImage(
                                                model = senderAvatarPath,
                                                contentDescription = "Аватар отправителя",
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = senderName.firstOrNull()?.toString()
                                                        ?: "?",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (!message.isOutgoing || message.isChannelPost) Alignment.Start else Alignment.End) {
                                    Card(
                                        modifier = Modifier
                                            .widthIn(max = 320.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .clickable {
                                                isDateShown = !isDateShown
                                                date =
                                                    convertUnixTimestampToDate(message.date.toLong())
                                            },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (!message.isOutgoing)
                                                MaterialTheme.colorScheme.inversePrimary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        if (showAuthor) {
                                            Card(
                                                modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            ) {
                                                Text(
                                                    modifier = Modifier.padding(8.dp, 4.dp),
                                                    text = senderName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                        if (isForwarded) {
                                            Card(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .height(32.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.mdi__share_all),
                                                        contentDescription = "Reply"
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = senderTitle,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = MaterialTheme.colorScheme.primary
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        if (message.replyTo is MessageReplyToMessage) {
                                            Card(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .height(32.dp)
                                                    .clickable {
                                                        scope.launch {
                                                            if (message.replyTo !is MessageReplyToMessage) return@launch

                                                            // Сначала ищем сообщение в текущем списке
                                                            val indexReply =
                                                                messagesForChat.indexOfFirst { it.id == (message.replyTo as MessageReplyToMessage).messageId }
                                                            if (indexReply != -1) {
                                                                // Сообщение найдено, прокручиваем к нему
                                                                listState.animateScrollToItem(
                                                                    indexReply
                                                                )
                                                                return@launch
                                                            }

                                                            // Если сообщение не найдено, начинаем загрузку
                                                            isLoadingMore = true
                                                            try {
                                                                var lastMessageId =
                                                                    messagesForChat.lastOrNull()?.id
                                                                while (lastMessageId != null && !isLoadingMore) {
                                                                    // Загружаем следующую порцию сообщений
                                                                    viewModel.getMessagesForChat(
                                                                        chatId = chatId,
                                                                        fromMessage = lastMessageId!!
                                                                    )

                                                                    // Проверяем, появилось ли нужное сообщение
                                                                    val newIndex =
                                                                        messagesForChat.indexOfFirst { it.id == (message.replyTo as MessageReplyToMessage).messageId }
                                                                    if (newIndex != -1) {
                                                                        // Нашли сообщение, прокручиваем к нему
                                                                        listState.animateScrollToItem(
                                                                            newIndex
                                                                        )
                                                                        break
                                                                    }

                                                                    // Если достигли конца и сообщение не найдено
                                                                    if (listState.isLastItemVisible()) {
                                                                        lastMessageId =
                                                                            messagesForChat.lastOrNull()?.id
                                                                    } else {
                                                                        break
                                                                    }

                                                                    delay(100) // Небольшая задержка между загрузками
                                                                }
                                                            } finally {
                                                                isLoadingMore = false
                                                            }
                                                        }
                                                    },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                                )
                                            ) {
                                                RepliedMessage(
                                                    replyTo = message.replyTo as MessageReplyToMessage,
                                                    viewModel = viewModel,
                                                    onClick = { }
                                                )
                                            }
                                        }

                                        MessageItem(
                                            onMediaClick = { id -> selectedMessageId = id },
                                            viewModel = viewModel,
                                            idMessageOfVoiceNote = idMessageOfVoiceNote,
                                            messageId = message.id,
                                            isVoicePlaying = isVoicePlaying,
                                            chatId = chatId,
                                            onTogglePlay = { msgId, content, chatId ->
                                                onTogglePlay(msgId, content, chatId)
                                            },
                                            item = message
                                        )
                                    }
                                    if (isDateShown) {
                                        Text(
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            text = date
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Date separator logic
                    try {
                        dateToCompare = getDayFromDate(message.date.toLong())
                        nextDateToCompare = nextMessage?.let { getDayFromDate(it.date.toLong()) } ?: 0L
                    } catch (e: Exception) {
                        // Ignore date parsing errors
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
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
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp)),
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

fun LazyListState.isLastItemVisible(): Boolean {
    val layoutInfo = layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    return if (visibleItemsInfo.isEmpty()) {
        false
    } else {
        val lastVisibleItem = visibleItemsInfo.last()
        val lastItem = layoutInfo.totalItemsCount - 1
        lastVisibleItem.index == lastItem
    }
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
private fun DisplayAlbum(
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

@Composable
fun ChatBottomBar(
    modifier: Modifier = Modifier,
    chatId: Long,
    textNewMessage: String,
    onTextChange: (String) -> Unit,
    currentMessageMode: String,
    lastMessageMode: String,
    onCurrentModeChange: (String) -> Unit,
    isRecording: Boolean,
    onRecordingChange: (Boolean) -> Unit,
    inputMessageToReply: TdApi.InputMessageReplyTo?,
    onReplyChange: (TdApi.InputMessageReplyTo?) -> Unit,
    viewModel: MainViewModel,
    onModeChange: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp, 8.dp, 72.dp, 8.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    modifier = Modifier.size(56.dp),
                    onClick = { }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_add_circle_outline_24),
                        contentDescription = "Добавить"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = textNewMessage,
                        onValueChange = { text ->
                            onTextChange(text)
                            if (currentMessageMode == "voice" || currentMessageMode == "video") {
                                onCurrentModeChange("text")
                            }
                            if (text.isEmpty()) {
                                onCurrentModeChange(lastMessageMode)
                            }
                        },
                        placeholder = { Text("Сообщение") },
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
                        contentDescription = "Эмодзи"
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            VoiceRecordButton(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterEnd),
                currentMessageMode = currentMessageMode,
                onModeChange = {
                    onModeChange()
                },
                onSendVoiceNote = { filePath ->
                    viewModel.sendVoiceNote(
                        chatId = chatId,
                        filePath = filePath,
                        replyToMessageId = inputMessageToReply
                    )
                    onReplyChange(null)
                },
                isRecording = isRecording,
                onRecordingChange = onRecordingChange
            )
        }

    }
}

@SuppressLint("DefaultLocale")
@Composable
fun VoiceRecordButton(
    modifier: Modifier = Modifier,
    currentMessageMode: String,
    onModeChange: () -> Unit,
    onSendVoiceNote: (String) -> Unit,
    isRecording: Boolean,
    onRecordingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorderState = remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<String?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var willCancel by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var amplitude by remember { mutableFloatStateOf(0f) }

    val hasRecordPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Требуется разрешение на запись звука", Toast.LENGTH_SHORT).show()
        }
    }

    // Timer and amplitude update
    LaunchedEffect(isRecording) {
        recordingDuration = 0
        while (isRecording) {
            delay(100)
            recordingDuration++
            try {
                recorderState.value?.let { recorder ->
                    try {
                        amplitude = recorder.maxAmplitude.toFloat().div(32768f)
                    } catch (e: Exception) {
                        // Игнорируем любые ошибки получения амплитуды
                        Log.e("VoiceRecorder", "Failed to get amplitude", e)
                        amplitude = 0f
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceRecorder", "Recorder state access error", e)
                amplitude = 0f
            }
        }
    }

    Box {
        // Recording overlay with hint and timer
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .align(Alignment.CenterStart),
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier.padding(16.dp, 8.dp, 72.dp, 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recording indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "recording")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
                    )

                    Text(
                        text = String.format("%d:%02d", recordingDuration / 10 / 60, recordingDuration / 10 % 60),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_trash),
                        contentDescription = "Trash",
                        modifier = Modifier.clickable {
                            scope.launch(Dispatchers.IO) {
                                willCancel = true
                                recorderState.value?.let { recorder ->
                                    outputFile?.let { filePath ->
                                        stopRecording(recorder, filePath) { path ->
                                            File(path).delete()
                                        }
                                    }
                                }
                                onRecordingChange(false)
                                recorderState.value = null
                                outputFile = null
                            }
                        }
                    )

                    Icon(
                        painter = painterResource(R.drawable.baseline_arrow_left_24),
                        contentDescription = "Left"
                    )
                    Text(
                        text = when {
                            isLocked -> "Запись закреплена"
                            willCancel -> "Отпустите для отмены"
                            else -> "Для отмены проведите пальцем"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (willCancel) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

        }

        // Animated waveform circles
        val errorColor = MaterialTheme.colorScheme.error
        // Record button section
        val animatedAmplitudes = remember {
            List(3) { Animatable(28f) }
        }

        LaunchedEffect(amplitude) {
            animatedAmplitudes.forEachIndexed { index, animatable ->
                launch {
                    animatable.animateTo(
                        targetValue = 28f + amplitude * (index + 1) * 20,
                        animationSpec = tween(
                            durationMillis = 100,
                            easing = LinearEasing
                        )
                    )
                }
            }
        }

        if (isRecording) {
            Canvas(modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(56.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                animatedAmplitudes.forEachIndexed { index, animatable ->
                    drawCircle(
                        color = errorColor.copy(alpha = 0.3f / (index + 1)),
                        radius = animatable.value.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        // Record button
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        offsetX.roundToInt(),
                        offsetY.roundToInt()
                    )
                }
                .align(Alignment.CenterEnd)
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isRecording -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var upBeforeTimeout = true

                        try {
                            withTimeout(300) {
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    if (isRecording) {
                                        // Короткое нажатие во время записи - отправляем
                                        scope.launch(Dispatchers.IO) {
                                            stopAndSendRecording(
                                                recorderState.value,
                                                outputFile,
                                                onSendVoiceNote
                                            )
                                            onRecordingChange(false)
                                            recorderState.value = null
                                            outputFile = null
                                            isLocked = false
                                        }
                                    } else {
                                        // Короткое нажатие без записи - меняем режим
                                        onModeChange()
                                    }
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            upBeforeTimeout = false
                            // Долгое нажатие - начинаем запись если режим voice
                            if (currentMessageMode == "voice" && !isRecording) {
                                if (!hasRecordPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@awaitEachGesture
                                }
                                scope.launch(Dispatchers.IO) {
                                    startRecording(context) { recorder, file ->
                                        recorderState.value = recorder
                                        outputFile = file.absolutePath
                                        onRecordingChange(true)
                                    }
                                }

                                // Отслеживаем движение пальца
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val position = event.changes.first()

                                        if (!position.pressed) {
                                            // Палец убрали
                                            if (!isLocked) {
                                                if (willCancel) {
                                                    scope.launch(Dispatchers.IO) {
                                                        recorderState.value?.let { recorder ->
                                                            outputFile?.let { filePath ->
                                                                stopRecording(
                                                                    recorder,
                                                                    filePath
                                                                ) { path ->
                                                                    File(path).delete()
                                                                }
                                                            }
                                                        }
                                                        onRecordingChange(false)
                                                        recorderState.value = null
                                                        outputFile = null
                                                    }
                                                } else {
                                                    scope.launch(Dispatchers.IO) {
                                                        stopAndSendRecording(
                                                            recorderState.value,
                                                            outputFile,
                                                            onSendVoiceNote
                                                        )
                                                        onRecordingChange(false)
                                                        recorderState.value = null
                                                        outputFile = null
                                                    }
                                                }
                                            }
                                            break
                                        }

                                        if (position.positionChanged() && !isLocked) {
                                            val change =
                                                position.position - position.previousPosition
                                            offsetX += change.x
                                            offsetY += change.y
                                            offsetX = offsetX.coerceIn(-200f, 0f)
                                            offsetY = offsetY.coerceIn(-200f, 0f)

                                            if (offsetY < -100f) {
                                                // Закрепляем запись
                                                isLocked = true
                                                scope.launch {
                                                    // Возвращаем кнопку на место
                                                    animate(offsetX, 0f) { value, _ ->
                                                        offsetX = value
                                                    }
                                                    animate(offsetY, 0f) { value, _ ->
                                                        offsetY = value
                                                    }
                                                }
                                                // Важно: НЕ делаем break здесь
                                            } else if (offsetX < -100f) {
                                                // Отменяем запись
                                                willCancel = true
                                                scope.launch(Dispatchers.IO) {
                                                    recorderState.value?.let { recorder ->
                                                        outputFile?.let { filePath ->
                                                            stopRecording(
                                                                recorder,
                                                                filePath
                                                            ) { path ->
                                                                File(path).delete()
                                                            }
                                                        }
                                                    }
                                                    onRecordingChange(false)
                                                    recorderState.value = null
                                                    outputFile = null
                                                }
                                                break
                                            }
                                        }
                                        position.consume()
                                    }
                                } finally {
                                    if (!isLocked) {
                                        offsetX = 0f
                                        offsetY = 0f
                                        willCancel = false
                                        onRecordingChange(false)
                                    }
                                }

                                // Если запись закреплена, ждем нового нажатия для отправки
                                if (isLocked) {
                                    try {
                                        while (true) {
                                            val lockedEvent = awaitPointerEvent()
                                            val lockedPosition = lockedEvent.changes.first()

                                            if (!lockedPosition.pressed) {
                                                scope.launch(Dispatchers.IO) {
                                                    stopAndSendRecording(
                                                        recorderState.value,
                                                        outputFile,
                                                        onSendVoiceNote
                                                    )
                                                    onRecordingChange(false)
                                                    recorderState.value = null
                                                    outputFile = null
                                                    isLocked = false
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                break
                                            }
                                            lockedPosition.consume()
                                        }
                                    } finally {
                                        if (!isLocked) {
                                            offsetX = 0f
                                            offsetY = 0f
                                            willCancel = false
                                            onRecordingChange(false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) {
            Icon(
                painterResource(
                    when {
                        isRecording -> R.drawable.baseline_send_24
                        currentMessageMode == "voice" -> R.drawable.baseline_voice_message
                        currentMessageMode == "video" -> R.drawable.baseline_camera
                        else -> R.drawable.baseline_send_24
                    }
                ),
                contentDescription = "Запись голосового сообщения",
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private suspend fun stopAndSendRecording(
    recorder: MediaRecorder?,
    filePath: String?,
    onSendVoiceNote: (String) -> Unit
) {
    recorder?.let { rec ->
        filePath?.let { path ->
            stopRecording(rec, path) { savedPath ->
                onSendVoiceNote(savedPath)
            }
        }
    }
}

private fun startRecording(context: Context, onStart: (MediaRecorder, File) -> Unit) {
    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.ogg")
    val recorder =
        MediaRecorder(context)

    recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.OGG)
        setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
        setAudioChannels(1)
        setAudioEncodingBitRate(64000)
        setAudioSamplingRate(16000)
        setOutputFile(file.absolutePath)
        prepare()
        start()
    }

    onStart(recorder, file)
}

private fun stopRecording(recorder: MediaRecorder, filePath: String, onStop: (String) -> Unit) {
    try {
        recorder.stop()
        recorder.release()
        onStop(filePath)
    } catch (e: Exception) {
        Log.e("VoiceRecorder", "Stop recording failed", e)
        throw e
    }
}


@Composable
private fun MessageItem(
    viewModel: MainViewModel,
    idMessageOfVoiceNote: Long?,
    messageId: Long,
    isVoicePlaying: Boolean,
    onMediaClick: (Long) -> Unit,
    chatId: Long,
    onTogglePlay: (Long, MessageVoiceNote, Long) -> Unit,
    item: TdApi.Message
) {
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val scope = rememberCoroutineScope()
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
                ) {
                    AsyncImage(
                        model = thumbnailPath,
                        contentDescription = "Видео превью",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = {
                            onMediaClick(item.id)
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
        is MessageAnimation -> {
            val animationInChat = content.animation.animation
            var animationPath by remember { mutableStateOf<String?>("") }

            LaunchedEffect(animationInChat) {
                if (animationInChat?.local?.isDownloadingCompleted == false) {
                    viewModel.downloadFile(animationInChat)
                } else {
                    animationPath = animationInChat?.local?.path
                }
                Log.d("GIF_MSG", "${content.animation.mimeType}")
            }

            LaunchedEffect(downloadedFiles.values) {
                val downloadedFile = downloadedFiles[animationInChat?.id]
                if (downloadedFile?.local?.isDownloadingCompleted == true) {
                    animationPath = downloadedFile.local?.path
                }
                Log.d("GIF_MSG", "$animationPath")
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
                if(content.animation.mimeType == "image/gif") {
                    AsyncImage(
                        model = animationPath,
                        contentDescription = "Анимация в сообщении",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(320.dp)
                            .heightIn(max = 320.dp)
                            .clickable {
                                onMediaClick(item.id)
                            }
                    )
                } else {
                    AnimationPlayer(
                        animationPath = animationPath,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .heightIn(max = 320.dp)
                    )
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
            var photoPath by remember { mutableStateOf<String?>("") }

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
                        .clickable {
                            onMediaClick(item.id)
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
                                        .clip(
                                            RoundedCornerShape(
                                                16.dp
                                            )
                                        )
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
        is MessageVoiceNote -> {
            var isListened by remember { mutableStateOf(false) }

            LaunchedEffect(isVoicePlaying) {
                isListened = content.isListened
            }
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onTogglePlay(messageId, content, chatId) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (idMessageOfVoiceNote == messageId && isVoicePlaying)
                                R.drawable.baseline_pause_24
                            else
                                R.drawable.baseline_play_arrow_24
                        ),
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVoiceIndicator(isPlaying = idMessageOfVoiceNote == messageId && isVoicePlaying)
                }

                val seconds = content.voiceNote.duration
                Text(
                    text = formatDuration(seconds),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Используем состояние из актуального сообщения
                if (!isListened) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    chat: TdApi.Chat?,
    avatarPath: String?,
    onShowInfo: () -> Unit,
    onBackClick: (Int) -> Unit,
    listState: LazyListState,
    viewModel: MainViewModel
) {
    val userStatuses by viewModel.userStatuses.collectAsState()
    var statusText by remember { mutableStateOf("") }

    var user by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(chat) {
        user =
            when (val type = chat?.type) {
                is ChatTypePrivate -> api.getUser(type.userId)
                is ChatTypeSupergroup -> api.getSupergroup(type.supergroupId)
                is ChatTypeBasicGroup -> api.getBasicGroup(type.basicGroupId)
                else -> null
            }
        when (chat?.type) {
            is ChatTypePrivate -> {
                viewModel.updateCurrentStatus((chat.type as ChatTypePrivate).userId)
            }
            is ChatTypeBasicGroup -> {
                statusText = "${formatCompactNumber((user as BasicGroup).memberCount)} участников"
            }
            is ChatTypeSupergroup -> {
                statusText = if ((chat.type as ChatTypeSupergroup).isChannel) {
                    "${formatCompactNumber((user as Supergroup).memberCount)} подписчиков"
                } else {
                    "${formatCompactNumber((user as Supergroup).memberCount)} участников"
                }
            }
        }
    }
    LaunchedEffect(userStatuses.values) {
        if (chat != null && chat.type is ChatTypePrivate && user != null) {
            when (userStatuses[chat.id]?.action) {
                is ChatActionCancel, null -> {
                    statusText = if ((user as User).type is UserTypeBot) {
                        "${formatCompactNumber(((user as User).type as UserTypeBot).activeUserCount)} пользователей в месяц"
                    } else {
                        when (val status = userStatuses[chat.id]?.status) {
                            is UserStatusOnline -> "Онлайн"
                            is UserStatusOffline -> {
                                viewModel.formatLastSeenTime(status.wasOnline)
                            }

                            is UserStatusLastWeek -> "Был(-а) на этой неделе..."
                            is UserStatusLastMonth -> "Был(-а) в этом месяце..."
                            else -> "Был(-а) недавно"
                        }
                    }
                }
                is ChatActionTyping -> { statusText = "Печатает..." }
                is ChatActionRecordingVideo -> { statusText = "Записывает видео..." }
                is ChatActionUploadingVideo -> { statusText = "Загружает видео..." }
                is ChatActionRecordingVoiceNote -> { statusText = "Записывает аудиосообщение..." }
                is ChatActionUploadingVoiceNote -> { statusText = "Загружает аудиосообщение..." }
                is ChatActionUploadingPhoto -> { statusText = "Загружает фото..." }
                is ChatActionUploadingDocument -> { statusText = "Загружает документ..." }
                is ChatActionChoosingSticker -> { statusText = "Выбирает стикер..." }
                is ChatActionChoosingLocation -> { statusText = "Выбирает локацию..." }
                is ChatActionChoosingContact -> { statusText = "Выбирает контакт..." }
                is ChatActionStartPlayingGame -> { statusText = "Играет в игру..." }
                is ChatActionRecordingVideoNote -> { statusText = "Записывает видеосообщение..." }
                is ChatActionUploadingVideoNote -> { statusText = "Загружает видеосообщение..." }
                is ChatActionWatchingAnimations -> { statusText = "Смотрит анимацию..." }

            }
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            Column {
                Row(
                    modifier = Modifier.clickable { onShowInfo() }
                ) {
                    if (avatarPath != null) {
                        AsyncImage(
                            model = avatarPath,
                            contentDescription = "Аватарка чата",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = chat?.title?.firstOrNull()?.toString() ?: "Ч",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text(
                            text = chat?.title ?: "Безымянный чат",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (statusText.isNotEmpty()) {
                            ChatStatusText(
                                status = statusText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { onBackClick(listState.firstVisibleItemIndex) }) {
                Icon(
                    painterResource(R.drawable.baseline_arrow_back_24),
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun ChatStatusText(
    status: String,
    modifier: Modifier = Modifier
) {
    val isTyping = status.endsWith("...")

    Text(
        text = status,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        color = if (isTyping) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

fun TdApi.ChatType.isPrivate(): Boolean = this is TdApi.ChatTypePrivate


@Composable
fun AnimationPlayer(
    animationPath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(animationPath) {
        if (!animationPath.isNullOrEmpty()) {
            try {
                val uri = Uri.fromFile(File(animationPath))
                player.apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    repeatMode = Player.REPEAT_MODE_ALL
                    playWhenReady = true
                    prepare()
                }
            } catch (e: Exception) {
                Log.e("AnimationPlayer", "Error loading animation: $e")
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
            }
        },
        modifier = modifier
    )
}