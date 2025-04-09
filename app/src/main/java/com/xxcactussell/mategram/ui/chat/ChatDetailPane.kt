package com.xxcactussell.mategram.ui.chat

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.convertUnixTimestampToDate
import com.xxcactussell.mategram.formatFileSize
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageReplyToMessage
import org.drinkless.tdlib.TdApi.MessageText
import org.drinkless.tdlib.TdApi.MessageVideo
import org.drinkless.tdlib.TdApi.Video


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailPane(
    chatId: Long,
    onBackClick: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    onShowInfo: () -> Unit
) {
    // Загружаем объект чата асинхронно при изменении chatId.
    var chat: TdApi.Chat? by remember { mutableStateOf(null) }
    LaunchedEffect(chatId) {
        chat = api.getChat(chatId)
    }
    var textNewMessage by remember { mutableStateOf("") }
    // Если chat != null, вызываем Flow для получения пути аватарки, иначе используем flowOf(null):
    // Функция remember здесь устанавливает зависимость от chat,
    // так что при изменении chat будет пересчитано значение avatarPath.

    val scope = rememberCoroutineScope()
    val messagesForChat by viewModel.messagesFromChat.collectAsState()
    var groupedMessages = groupMessagesByAlbum(messagesForChat)
    val listState = rememberLazyListState()
    var avatarPath by remember { mutableStateOf<String?>(null) }
    println("PHOTOPATH: $avatarPath")

    LaunchedEffect(chat) {
        avatarPath = chat?.let { viewModel.getChatAvatarPath(it) }
    }

    LaunchedEffect(chatId) {
        viewModel.setHandlerForChat(chatId)
        viewModel.getMessagesForChat(chatId)
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
    LaunchedEffect(messagesForChat.size) {
        val unreadIndex = chat?.unreadCount ?: 0
        if (unreadIndex > 0 && unreadIndex < messagesForChat.size) {
            listState.animateScrollToItem(unreadIndex - 1)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
            .collect { visibleIndexes ->
                val visibleUnreadMessages = messagesForChat.filterIndexed { index, message ->
                    index in visibleIndexes && index < (chat?.unreadCount
                        ?: 0) && !message.isOutgoing
                }

                visibleUnreadMessages.forEach { message ->
                    viewModel.markAsRead(message)
                }
                viewModel.updateChatsFromNetworkForView()
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
                        } else {
                            // Если аватарка еще не загружена, показываем индикатор загрузки.
                            Box(modifier = Modifier.size(36.dp)) {
                                CircularProgressIndicator(color = Color.Gray)
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
            Row(modifier = Modifier.padding(8.dp).navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f).wrapContentSize(),
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
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {
                            TextField(
                                value = textNewMessage,
                                onValueChange = { textNewMessage = it },
                                placeholder = { Text("Cообщение") },
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 5
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
                        viewModel.sendMessage(
                            chatId = chatId,
                            text = textNewMessage,
                            replyToMessageId = null,
                        )
                        textNewMessage = ""
                    },
                ) {
                    Icon(painterResource(R.drawable.baseline_send_24), contentDescription = "Extended floating action button.")
                }
            }
        }
    ) { innerPadding ->
        // Основной контент экрана чата.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            reverseLayout = true
        ) {
            items(groupedMessages.size) { index ->
                var isDateShown by remember { mutableStateOf(false) }
                var date by remember { mutableStateOf("") }
                Spacer(modifier = Modifier.height(8.dp))
                when (val item = groupedMessages[index]) {
                    is MediaAlbum -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (!item.isOutgoing) Alignment.Start else Alignment.End
                            ) {


                                if (item.replyTo != null) {
                                    var messageToReply by remember { mutableStateOf<TdApi.Message?>(null) }
                                    LaunchedEffect(messagesForChat) {
                                        if (item.replyTo is MessageReplyToMessage) {
                                            messageToReply =
                                                viewModel.getMessageById((item.replyTo as MessageReplyToMessage))
                                        }
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
                                                replyTitle = user.firstName + " " + user.lastName
                                                // Извлекаем имя и фамилию пользователя
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.widthIn(max = 200.dp)
                                            .clickable {
                                                scope.launch {
                                                    var replyIndex =
                                                        messagesForChat.indexOfFirst { it.id == messageToReply?.id }

                                                    while (replyIndex == -1) { // Прерываем, если больше сообщений нет
                                                        replyIndex =
                                                            messagesForChat.indexOfFirst { it.id == messageToReply?.id }
                                                        listState.animateScrollToItem(messagesForChat.indexOfFirst { it.id == messagesForChat.lastOrNull()?.id })
                                                    }
                                                    delay(300)
                                                    listState.animateScrollToItem(replyIndex) // Прокручиваем к нужному сообщению
                                                }
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
                                                if (messageToReply != null) {
                                                    if (messageToReply!!.content is MessagePhoto) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_image_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as MessagePhoto).caption.text
                                                                ?: "Нет сообщения",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is MessageVideo) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_video_file_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as MessageVideo).caption.text
                                                                ?: "Нет сообщения",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is MessageText) {
                                                        Text(
                                                            text = (messageToReply!!.content as MessageText).text.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageAudio) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_audio_file_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageAudio).caption.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageContact) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_perm_contact_calendar_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageContact).contact.toString()
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageDocument) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_edit_document_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageDocument).caption.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageGame) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_videogame_asset_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageGame).game.toString()
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageGiveaway) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_emoji_events_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageGiveaway).prize.toString()
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessagePoll) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_poll_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessagePoll).poll.question.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageVideoNote) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_slow_motion_video_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Видеосообщение",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageVoiceNote) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_record_voice_over_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Аудиосообщение",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageSticker) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_emoji_emotions_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageSticker).sticker.emoji
                                                                ?: "Стикер",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageAnimation) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_image_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageAnimation).caption.text
                                                                ?: "Стикер",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Сообщение недоступно",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))




                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 320.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable {
                                            isDateShown = !isDateShown
                                            date = convertUnixTimestampToDate(
                                                item.date.toLong()
                                            )
                                        },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!item.isOutgoing)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                ) {
                                    MediaCarousel(
                                        album = item,
                                        viewModel = viewModel
                                    )
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
                    }
                    is TdApi.Message -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (!item.isOutgoing) Alignment.Start else Alignment.End
                            ) {


                                if (item.replyTo != null) {
                                    var messageToReply by remember { mutableStateOf<TdApi.Message?>(null) }
                                    LaunchedEffect(messagesForChat) {
                                        if (item.replyTo is MessageReplyToMessage) {
                                            messageToReply =
                                                viewModel.getMessageById((item.replyTo as MessageReplyToMessage))
                                        }
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
                                                replyTitle = user.firstName + " " + user.lastName
                                                // Извлекаем имя и фамилию пользователя
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.widthIn(max = 200.dp)
                                            .clickable {
                                                scope.launch {
                                                    var replyIndex =
                                                        messagesForChat.indexOfFirst { it.id == messageToReply?.id }

                                                    while (replyIndex == -1) { // Прерываем, если больше сообщений нет
                                                        replyIndex =
                                                            messagesForChat.indexOfFirst { it.id == messageToReply?.id }
                                                        listState.animateScrollToItem(messagesForChat.indexOfFirst { it.id == messagesForChat.lastOrNull()?.id })
                                                    }
                                                    delay(300)
                                                    listState.animateScrollToItem(replyIndex) // Прокручиваем к нужному сообщению
                                                }
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
                                                if (messageToReply != null) {
                                                    if (messageToReply!!.content is MessagePhoto) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_image_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as MessagePhoto).caption.text
                                                                ?: "Нет сообщения",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is MessageVideo) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_video_file_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as MessageVideo).caption.text
                                                                ?: "Нет сообщения",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is MessageText) {
                                                        Text(
                                                            text = (messageToReply!!.content as MessageText).text.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageAudio) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_audio_file_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageAudio).caption.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageContact) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_perm_contact_calendar_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageContact).contact.toString()
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageDocument) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_edit_document_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageDocument).caption.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageGame) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_videogame_asset_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageGame).game.toString()
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageGiveaway) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_emoji_events_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageGiveaway).prize.toString()
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessagePoll) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_poll_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessagePoll).poll.question.text
                                                                ?: "Нет сообщений",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageVideoNote) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_slow_motion_video_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Видеосообщение",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageVoiceNote) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_record_voice_over_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Аудиосообщение",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageSticker) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_emoji_emotions_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageSticker).sticker.emoji
                                                                ?: "Стикер",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    } else if (messageToReply!!.content is TdApi.MessageAnimation) {
                                                        Icon(
                                                            painterResource(R.drawable.baseline_image_24),
                                                            "photo",
                                                            Modifier
                                                                .size(16.dp)
                                                                .align(Alignment.CenterVertically)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = (messageToReply!!.content as TdApi.MessageAnimation).caption.text
                                                                ?: "Стикер",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "Сообщение недоступно",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))


                                Card(
                                    modifier = Modifier.widthIn(max = 320.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable {
                                            isDateShown = !isDateShown
                                            date = convertUnixTimestampToDate(
                                                item.date.toLong()
                                            )
                                        },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!item.isOutgoing)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                ) {
                                    if (item.content is MessageText) {
                                        Text(
                                            modifier = Modifier.padding(16.dp),
                                            text = (item.content as MessageText).text.text,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else if (item.content is TdApi.MessageSticker) {
                                        val sticker =
                                            (item.content as TdApi.MessageSticker).sticker
                                        val stickerFile = sticker?.sticker
                                        var stickerPath by remember { mutableStateOf<String?>(null) }

                                        LaunchedEffect(stickerFile) {
                                            stickerPath = viewModel.getStickerFromChat(stickerFile)
                                        }

                                        if (stickerPath != null) {
                                            if (stickerPath!!.endsWith(".webp")) {
                                                // Если формат WebP — используем AsyncImage
                                                AsyncImage(
                                                    model = stickerPath,
                                                    contentDescription = "Стикер",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(200.dp)
                                                        .clip(RoundedCornerShape(24.dp))
                                                )
                                            } else {
                                                // Если формат WebM — используем VideoView
                                                AndroidView(
                                                    factory = { context ->
                                                        PlayerView(context).apply {
                                                            val player =
                                                                ExoPlayer.Builder(context).build()
                                                            this.player = player
                                                            val mediaItem =
                                                                MediaItem.fromUri(stickerPath!!)

                                                            player.setMediaItem(mediaItem)
                                                            player.repeatMode =
                                                                Player.REPEAT_MODE_ALL // Зацикливаем воспроизведение
                                                            player.prepare()
                                                            player.play()

                                                            // Убираем элементы управления
                                                            this.useController = false
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(200.dp)
                                                        .clip(RoundedCornerShape(24.dp))
                                                )

                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.size(200.dp)
                                                    .clip(RoundedCornerShape(24.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(40.dp)
                                                        .align(Alignment.Center),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else if (item.content is TdApi.MessagePhoto) {
                                        // Получаем нужный PhotoSize с type = "x"
                                        val photoSize =
                                            (item.content as TdApi.MessagePhoto)
                                                .photo?.sizes?.find { it.type == "x" }

                                        // Запускаем загрузку фото и храним результат в `imagePath`
                                        var imagePath by remember { mutableStateOf<String?>(null) }

                                        LaunchedEffect(photoSize) {
                                            imagePath = viewModel.getPhotoPreviewFromChat(photoSize)
                                        }

                                        // Передаём `imagePath` в `AsyncImage`


                                        val caption =
                                            (item.content as MessagePhoto).caption.text
                                        if (caption != "") {
                                            AsyncImage(
                                                model = imagePath,
                                                contentDescription = "Фото сообщения",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .width(320.dp)
                                                    .heightIn(max = 320.dp)
                                                    .clip(RoundedCornerShape(24.dp))
                                            )
                                            Text(
                                                modifier = Modifier.padding(16.dp),
                                                text = caption,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            AsyncImage(
                                                model = imagePath,
                                                contentDescription = "Фото сообщения",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .widthIn(max = 320.dp)
                                                    .clip(RoundedCornerShape(24.dp))
                                            )
                                        }
                                    } else if (item.content is TdApi.MessageVideo) {
                                        val videoContent =
                                            (item.content as TdApi.MessageVideo).video
                                        val thumbnailFile = (videoContent as Video).thumbnail?.file
                                        var thumbnailPath by remember { mutableStateOf<String?>(null) }

                                        LaunchedEffect(thumbnailFile) {
                                            thumbnailPath =
                                                viewModel.getThumbnailVideoFromChat(thumbnailFile)
                                        }

                                        val caption =
                                            (item.content as TdApi.MessageVideo).caption.text

                                        if (caption.isNotEmpty()) {
                                            Column {
                                                Box(
                                                    modifier = Modifier
                                                        .width(320.dp)
                                                        .height(320.dp)
                                                        .clip(RoundedCornerShape(24.dp))
                                                ) {
                                                    // Отображаем превью вместо видео
                                                    AsyncImage(
                                                        model = thumbnailPath,
                                                        contentDescription = "Видео превью",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    // Кнопка "Play" по центру
                                                    IconButton(
                                                        onClick = {

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
                                                Text(
                                                    modifier = Modifier.padding(16.dp),
                                                    text = caption,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .widthIn(max = 320.dp)
                                                    .clip(RoundedCornerShape(24.dp))
                                            ) {
                                                // Отображаем превью вместо видео
                                                AsyncImage(
                                                    model = thumbnailPath,
                                                    contentDescription = "Видео превью",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                        .widthIn(max = 320.dp)
                                                        .height(320.dp)
                                                )

                                                // Кнопка "Play" по центру
                                                IconButton(
                                                    onClick = {

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
                                        }
                                    } else if (item.content is TdApi.MessageDocument) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            val document =
                                                (item.content as TdApi.MessageDocument).document
                                            val documentFile = document?.document
                                            val documentThumbnail = document?.thumbnail
                                            val documentName = document?.fileName.toString()

                                            var documentThumbnailPath by remember {
                                                mutableStateOf<String?>(
                                                    null
                                                )
                                            }
                                            var uploadedSize by remember { mutableStateOf<String?>("") }
                                            var isDownloading by remember { mutableStateOf(false) }
                                            var isFileDownloaded by remember { mutableStateOf(false) }

                                            // Загрузка миниатюры
                                            if (documentThumbnail != null) {
                                                LaunchedEffect(documentThumbnail) {
                                                    documentThumbnailPath =
                                                        viewModel.getDocumentThumbnail(
                                                            documentThumbnail
                                                        )
                                                }
                                            }

                                            // Получаем размер файла
                                            LaunchedEffect(uploadedSize) {
                                                val fileSize =
                                                    documentFile?.expectedSize?.toInt() ?: 0
                                                uploadedSize = formatFileSize(fileSize)
                                            }

                                            val context = LocalContext.current

                                            val onDownloadClick: () -> Unit = {
                                                scope.launch {
                                                    isDownloading = true
                                                    documentFile?.let { file ->
                                                        val downloadedFilePath =
                                                            viewModel.downloadFileToDownloads(
                                                                context,
                                                                file.id,
                                                                documentName
                                                            )
                                                        if (downloadedFilePath != null) {
                                                            isFileDownloaded = true
                                                            isDownloading = false
                                                            Toast.makeText(
                                                                context,
                                                                "Файл загружен: $downloadedFilePath",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            isDownloading = false
                                                            Toast.makeText(
                                                                context,
                                                                "Ошибка загрузки файла",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
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
                                                            modifier = Modifier.size(40.dp),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
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
                                                if (uploadedSize != null) {
                                                    Text(
                                                        text = uploadedSize!!,
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            modifier = Modifier.padding(16.dp),
                                            text = item.toString(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
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
    }
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
                    // Находим первое сообщение с replyTo в альбоме
                    val replyTo = currentAlbumMessages.firstOrNull { it.replyTo != null }?.replyTo
                    result.add(MediaAlbum(
                        messages = currentAlbumMessages.toList(),
                        isOutgoing = isOutgoing,
                        replyTo = replyTo,
                        date = date
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
                    // Находим первое сообщение с replyTo в альбоме
                    val replyTo = currentAlbumMessages.firstOrNull { it.replyTo != null }?.replyTo
                    result.add(MediaAlbum(
                        messages = currentAlbumMessages.toList(),
                        isOutgoing = isOutgoing,
                        replyTo = replyTo,
                        date = date
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
            // Находим первое сообщение с replyTo в альбоме
            val replyTo = currentAlbumMessages.firstOrNull { it.replyTo != null }?.replyTo
            result.add(MediaAlbum(
                messages = currentAlbumMessages.toList(),
                isOutgoing = isOutgoing,
                replyTo = replyTo,
                date = date
            ))
        } else {
            result.add(currentAlbumMessages[0])
        }
    }

    return result
}

// Создадим класс для хранения альбома
data class MediaAlbum(val messages: List<TdApi.Message>, val isOutgoing: Boolean, val replyTo: TdApi.MessageReplyTo?, val date: Int) {
}

// Создадим Composable для отображения карусели
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCarousel(
    album: MediaAlbum,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
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
                is TdApi.MessagePhoto -> {
                    Box(
                        modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                    ) {
                        PhotoContent(message = message, viewModel = viewModel)
                    }
                }
                is TdApi.MessageVideo -> {
                    Box(
                        modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                    ) {
                        VideoContent(message = message, viewModel = viewModel)
                    }
                }
            }
        }

        // Caption and page indicators
        val firstMessageCaption = album.messages.firstNotNullOfOrNull { message ->
            when (val content = message.content) {
                is TdApi.MessagePhoto -> content.caption.text.takeIf { !it.isNullOrEmpty() }
                is TdApi.MessageVideo -> content.caption.text.takeIf { !it.isNullOrEmpty() }
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
    modifier: Modifier = Modifier
) {
    val photoSize = (message.content as TdApi.MessagePhoto).photo?.sizes?.find { it.type == "x" }
    var imagePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photoSize) {
        imagePath = viewModel.getPhotoPreviewFromChat(photoSize)
    }

    AsyncImage(
        model = imagePath,
        contentDescription = "Фото сообщения",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .height(320.dp)
    )
}

@Composable
fun VideoContent(
    message: TdApi.Message,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val videoContent = (message.content as TdApi.MessageVideo).video
    val thumbnailFile = (videoContent as TdApi.Video).thumbnail?.file
    var thumbnailPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(thumbnailFile) {
        thumbnailPath = viewModel.getThumbnailVideoFromChat(thumbnailFile)
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
            onClick = { /* TODO: Implement video playback */ },
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