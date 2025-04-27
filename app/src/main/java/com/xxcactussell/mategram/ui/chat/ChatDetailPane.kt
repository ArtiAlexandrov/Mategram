package com.xxcactussell.mategram.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.formatCompactNumber
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.convertUnixTimestampToDate
import com.xxcactussell.mategram.kotlinx.telegram.core.convertUnixTimestampToDateByDay
import com.xxcactussell.mategram.kotlinx.telegram.core.getDayFromDate
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getBasicGroup
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getSupergroup
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.restoreChatState
import com.xxcactussell.mategram.updateChatClientData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
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
import org.drinkless.tdlib.TdApi.MessageOriginChannel
import org.drinkless.tdlib.TdApi.MessageOriginChat
import org.drinkless.tdlib.TdApi.MessageOriginUser
import org.drinkless.tdlib.TdApi.MessageReplyToMessage
import org.drinkless.tdlib.TdApi.MessageVoiceNote
import org.drinkless.tdlib.TdApi.Supergroup
import org.drinkless.tdlib.TdApi.User
import org.drinkless.tdlib.TdApi.UserStatusLastMonth
import org.drinkless.tdlib.TdApi.UserStatusLastWeek
import org.drinkless.tdlib.TdApi.UserStatusOffline
import org.drinkless.tdlib.TdApi.UserStatusOnline
import org.drinkless.tdlib.TdApi.UserTypeBot
import java.io.File
import kotlin.math.roundToInt

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
    val photo = chat.photo?.small
    var avatarPath by remember { mutableStateOf(downloadedFiles[chat.photo?.small?.id]?.local?.path) }
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

    LaunchedEffect(chat) {
        restoreChatState(chat)?.let { state ->
            // Прокручиваем к сохранённой позиции
            listState.scrollToItem(state.scrollPosition)
            // Возможно, вы захотите сделать дополнительную логику по выделению сообщения с id lastReadMessageId
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { newIndex ->
                // В данном примере используем chat.lastReadInboxMessageId как последний просмотренный.
                // При необходимости можно вычислять его иначе.
                updateChatClientData(chat, newIndex, chat.lastReadInboxMessageId)
            }
    }

    // Отслеживание видимых сообщений и отметка их как прочитанных
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isEmpty()) return@collect

                val unreadCount = chat.unreadCount
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
                    var spaceBetweenMessage = 4.dp


                    fun shouldShowAuthorInfo(): Boolean {
                        return when {
                            message.isOutgoing -> false
                            chat.type is ChatTypePrivate -> false
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

                    if (shouldShowAuthorInfo() || (nextMessage != null && message.date - nextMessage.date > 120)) {
                        spaceBetweenMessage = 16.dp
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
                                    .padding(16.dp, spaceBetweenMessage, 16.dp, 0.dp),
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
                                                    MaterialTheme.colorScheme.surfaceContainer
                                                else
                                                    MaterialTheme.colorScheme.secondaryContainer
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
                                                        containerColor = MaterialTheme.colorScheme.inversePrimary
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
                                                                            fromMessage = lastMessageId
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
                                                        containerColor = MaterialTheme.colorScheme.inversePrimary
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
                                .padding(16.dp, spaceBetweenMessage, 16.dp, 0.dp),
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
                                                MaterialTheme.colorScheme.surfaceContainer
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        if (showAuthor) {
                                            Card(
                                                modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.inversePrimary
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
                                                    containerColor = MaterialTheme.colorScheme.inversePrimary
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
                                                    containerColor = MaterialTheme.colorScheme.inversePrimary
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

private fun stopAndSendRecording(
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
                    modifier = Modifier.clickable { onShowInfo() },
                    verticalAlignment = Alignment.CenterVertically
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
                                .size(36.dp)
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
                    Column (Modifier.weight(1f)) {
                        Text(
                            text = chat?.title ?: "Безымянный чат",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (statusText.isNotEmpty()) {
                            ChatStatusText(
                                status = statusText,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    IconButton(
                        onClick = {},
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            painterResource(R.drawable.baseline_dots), "menu"
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = { onBackClick(listState.firstVisibleItemIndex) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Icon(
                    painterResource(R.drawable.baseline_arrow_back_24),
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun ChatStatusText(
    status: String,
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

fun TdApi.ChatType.isPrivate(): Boolean = this is ChatTypePrivate


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