package com.xxcactussell.mategram.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.convertUnixTimestampToDate
import com.xxcactussell.mategram.formatFileSize
import com.xxcactussell.mategram.isUserContact
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.createPrivateChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.ui.chat.ChatDetailPane
import com.xxcactussell.mategram.ui.chat.ChatInfoPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageReplyToMessage
import org.drinkless.tdlib.TdApi.MessageText
import org.drinkless.tdlib.TdApi.MessageVideo
import org.drinkless.tdlib.TdApi.Video
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ChatListView(
    viewModel: MainViewModel = viewModel()
) {
    val me by viewModel.me.collectAsState()
    var avatarMePath by remember { mutableStateOf<String?>("") }
    var chatMe by remember { mutableStateOf<TdApi.Chat?>(null) }
    val chats by viewModel.visibleChats.collectAsState()
    val folders by viewModel.chatFolders.collectAsState()
    var selectedChat by remember { mutableStateOf<TdApi.Chat?>(null) }
    var selectedChatForInfoPane by remember { mutableStateOf<TdApi.Chat?>(null) }
    var filterFolderChipValue by remember { mutableStateOf<TdApi.ChatFolder?>(null) }
    var pinnedChats by remember { mutableStateOf(emptyList<Long>()) }
    var includedChats by remember { mutableStateOf(emptyList<Long>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.updateChatsFromNetworkForView()
    }

    LaunchedEffect(me) {
        if (me!=null) {
            chatMe = api.createPrivateChat(me!!.id, true)
            avatarMePath = viewModel.getChatAvatarPath(chatMe!!)
        }
    }

    LaunchedEffect(chats) {
        pinnedChats = chats.filter { chat ->
            chat.positions?.firstOrNull()?.isPinned == true
        }.map { it.id }
        includedChats = chats.filter { chat ->
            chat.positions?.firstOrNull()?.isPinned == false
        }.map { it.id }
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    val navigator = rememberListDetailPaneScaffoldNavigator<Long>(
        scaffoldDirective = calculatePaneScaffoldDirective(
            currentWindowAdaptiveInfo()
        )
    )
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    // Отслеживаем выбранный чат через navigator
    val selectedChatId = navigator.currentDestination?.contentKey

    // Обновляем selectedChat когда меняется selectedChatId
    LaunchedEffect(selectedChatId) {
        selectedChat = chats.find { it.id == selectedChatId }
        selectedChatForInfoPane = chats.find { it.id == selectedChatId }
    }

    if (showBottomSheet && selectedChatForInfoPane != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets(0) },
            modifier = Modifier.navigationBarsPadding()
        ) {
            ChatInfoContent(
                chat = selectedChatForInfoPane!!,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }

    NavigableListDetailPaneScaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.ime),
        listPane = {
            AnimatedPane (
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    topBar = {
                        LargeTopAppBar(
                            title = { Text("Чаты") },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                if (avatarMePath != null) {
                                    AsyncImage(
                                        model = avatarMePath,
                                        contentDescription = "Аватарка чата",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedChatForInfoPane = chatMe
                                                showBottomSheet = true
                                            }
                                    )
                                } else {
                                    // Если аватарка еще не загружена, показываем индикатор загрузки.
                                    Box(modifier = Modifier.size(36.dp)) {
                                        CircularProgressIndicator(color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar(
                            actions = {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(
                                        painter = painterResource(R.drawable.outline_contacts_24),
                                        contentDescription = "Localized description"
                                    )
                                }
                                IconButton(onClick = {
                                    if (chatMe != null) {
                                        scope.launch {
                                            navigator.navigateTo(ThreePaneScaffoldRole.Primary, chatMe!!.id)
                                        }
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_bookmark_border_24),
                                        contentDescription = "Localized description",
                                    )
                                }
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(
                                        painter = painterResource(R.drawable.outline_phone_24),
                                        contentDescription = "Localized description",
                                    )
                                }
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { /* do something */ },
                                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                                ) {
                                    Icon(painterResource(R.drawable.baseline_edit_24), "Localized description")
                                }
                            }
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) // Связываем прокрутку LazyColumn с TopAppBar
                ) { padding ->
                    Column(modifier = Modifier.padding(padding)) {
                        LazyColumn {
                            item {
                                LazyRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    items(folders) { folder ->
                                        println("РИСУЕМ $folder")
                                        FilterChip(
                                            onClick = {
                                                if (filterFolderChipValue == folder) {
                                                    filterFolderChipValue = null
                                                    pinnedChats = chats.filter { chat ->
                                                        chat.positions?.firstOrNull()?.isPinned == true
                                                    }.map { it.id }
                                                    includedChats = chats.filter { chat ->
                                                        chat.positions?.firstOrNull()?.isPinned == false
                                                    }.map { it.id }
                                                } else {
                                                    filterFolderChipValue = folder
                                                    pinnedChats = folder.pinnedChatIds.toList()
                                                    includedChats = folder.includedChatIds.toMutableList()

                                                    scope.launch(Dispatchers.IO) {
                                                        val filteredChats = chats.filter { chat ->
                                                            when {
                                                                folder.includeBots && chat.type is TdApi.ChatTypePrivate -> {
                                                                    val userId =
                                                                        (chat.type as TdApi.ChatTypePrivate).userId
                                                                    val user = api.getUser(userId)
                                                                    user.type is TdApi.UserTypeBot
                                                                }

                                                                folder.includeGroups && (chat.type is TdApi.ChatTypeBasicGroup ||
                                                                        (chat.type is TdApi.ChatTypeSupergroup && !(chat.type as TdApi.ChatTypeSupergroup).isChannel)) -> true

                                                                folder.includeChannels && chat.type is TdApi.ChatTypeSupergroup &&
                                                                        (chat.type as TdApi.ChatTypeSupergroup).isChannel -> true

                                                                folder.includeContacts && chat.type is TdApi.ChatTypePrivate &&
                                                                        isUserContact((chat.type as TdApi.ChatTypePrivate).userId) -> true

                                                                folder.includeNonContacts && chat.type is TdApi.ChatTypePrivate &&
                                                                        !isUserContact((chat.type as TdApi.ChatTypePrivate).userId) -> true

                                                                else -> false
                                                            }
                                                        }

                                                        includedChats += filteredChats.map { it.id }

                                                        if (folder.excludeRead) {
                                                            includedChats = includedChats.filter { chatId ->
                                                                api.getChat(chatId).unreadCount > 0
                                                            }.toMutableList()
                                                        }

                                                        if (folder.excludeMuted) {
                                                            includedChats = includedChats.filter { chatId ->
                                                                api.getChat(chatId).notificationSettings.muteFor == 0
                                                            }.toMutableList()
                                                        }
                                                    }
                                                }
                                            },
                                            selected = filterFolderChipValue == folder,
                                            label = { Text(text = folder.name.text.text) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }
                            // Закрепленные чаты
                            items(chats.filter {
                                it.id in pinnedChats && it.id != (chatMe?.id ?: 0L)
                            }) { chat ->
                                ChatItem(
                                    chat = chat,
                                    viewModel = viewModel,
                                    isSelected = chat.id == selectedChatId,
                                    onChatClick = {
                                        scope.launch {
                                            navigator.navigateTo(ThreePaneScaffoldRole.Primary, chat.id)
                                        }
                                    },
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Обычные чаты
                            items(chats.filter { it.id in includedChats && it.id != (chatMe?.id ?: 0L) }) { chat ->
                                ChatItem(
                                    chat = chat,
                                    viewModel = viewModel,
                                    isSelected = chat.id == selectedChatId,
                                    onChatClick = {
                                        scope.launch {
                                            navigator.navigateTo(ThreePaneScaffoldRole.Primary, chat.id)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        },
        detailPane = {
            selectedChat?.let { chat ->
                AnimatedPane (
                    enterTransition = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = LinearOutSlowInEasing
                        ),
                        initialOffsetX = { it }
                    ),
                    exitTransition = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = FastOutLinearInEasing
                        ),
                        targetOffsetX = { it }
                    )
                ) {
                    ChatDetailPane(
                        chatId = chat.id,
                        onBackClick = {
                            scope.launch {
                                navigator.navigateBack()
                            }
                        },
                        onShowInfo = {
                            selectedChatForInfoPane = selectedChat
                            showBottomSheet = true
                        }
                    )
                }
            }
        },
        navigator = navigator,
        paneExpansionState =
        rememberPaneExpansionState(
            keyProvider = navigator.scaffoldValue
        ),
        paneExpansionDragHandle = { state ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier =
                Modifier.paneExpansionDraggable(
                    state,
                    LocalMinimumInteractiveComponentSize.current,
                    interactionSource,
                    state.defaultDragHandleSemantics()
                ),
                interactionSource = interactionSource
            )
        }
    )
}

// Обновляем ChatItem, добавляя параметр isSelected
@Composable
private fun ChatItem(
    chat: TdApi.Chat,
    viewModel: MainViewModel,
    isSelected: Boolean,
    onChatClick: (chatId: Long) -> Unit
) {
    // Существующий код ChatItem с добавлением выделения для выбранного чата
    val containerColorCard = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        chat.unreadCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }

    var avatarPath by remember { mutableStateOf<String?>(null) }

    // Следим за изменением фото чата
    LaunchedEffect(chat.photo) {
        avatarPath = viewModel.getChatAvatarPath(chat)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                // При клике вызываем переданный callback для открытия чата
                onChatClick(chat.id)
            },
        colors = CardDefaults.cardColors(
            containerColor = containerColorCard
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (avatarPath != null) {
                AsyncImage(
                    model = avatarPath,
                    contentDescription = "Аватарка чата",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                // Показываем placeholder или индикатор загрузки
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                    Text(
                        text = chat.title?.firstOrNull()?.toString() ?: "Ч",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(text = chat.title ?: "Без названия", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (chat.positions?.firstOrNull()?.isPinned == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Card (
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        {
                            Icon(
                                painterResource(R.drawable.baseline_push_pin_24),
                                "pushpin",
                                Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )
                        }
                    }
                }
                Row {
                    if (chat.lastMessage?.isOutgoing == true) {
                        Text(text = "Вы:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (chat.lastMessage?.content is MessagePhoto) {
                        Icon(
                            painterResource(R.drawable.baseline_image_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as MessagePhoto).caption.text
                                ?: "Нет сообщения",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is MessageVideo) {
                        Icon(
                            painterResource(R.drawable.baseline_video_file_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as MessageVideo).caption.text
                                ?: "Нет сообщения",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is MessageText) {
                        Text(
                            text = (chat.lastMessage?.content as MessageText).text.text
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageAudio) {
                        Icon(
                            painterResource(R.drawable.baseline_audio_file_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageAudio).caption.text
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageContact) {
                        Icon(
                            painterResource(R.drawable.baseline_perm_contact_calendar_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageContact).contact.toString()
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageDocument) {
                        Icon(
                            painterResource(R.drawable.baseline_edit_document_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageDocument).caption.text
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageGame) {
                        Icon(
                            painterResource(R.drawable.baseline_videogame_asset_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageGame).game.toString()
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageGiveaway) {
                        Icon(
                            painterResource(R.drawable.baseline_emoji_events_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageGiveaway).prize.toString()
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessagePoll) {
                        Icon(
                            painterResource(R.drawable.baseline_poll_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessagePoll).poll.question.text
                                ?: "Нет сообщений",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageVideoNote) {
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
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageVoiceNote) {
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
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageSticker) {
                        Icon(
                            painterResource(R.drawable.baseline_emoji_emotions_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageSticker).sticker.emoji
                                ?: "Стикер",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (chat.lastMessage?.content is TdApi.MessageAnimation) {
                        Icon(
                            painterResource(R.drawable.baseline_image_24),
                            "photo",
                            Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (chat.lastMessage?.content as TdApi.MessageAnimation).caption.text
                                ?: "Стикер",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }//messageAnimation, , , messageDice, , , messageGiveawayWinners, messageInvoice, messageLocation, messagePaidMedia, messageSticker, messageStory,, messageVenue
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (chat.unreadCount > 0) {
                Badge (
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("${chat.unreadCount}")
                }
            }
        }
    }
}
