package com.xxcactussell.mategram.ui.chat

import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.converUnixTimeStampForChatList
import com.xxcactussell.mategram.kotlinx.telegram.core.isUserContact
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.closeChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.createPrivateChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatFolder
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.openChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessageVoiceNote

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ChatListView(
    viewModel: MainViewModel = viewModel(),
    window: Window
) {
    val me by viewModel.me.collectAsState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    var chatMe by remember { mutableStateOf<TdApi.Chat?>(null) }
    val photo = chatMe?.photo?.small
    var avatarMePath by remember { mutableStateOf(downloadedFiles[chatMe?.photo?.small?.id]?.local?.path) }
    val chats by viewModel.visibleChats.collectAsState()
    val folders by viewModel.chatFolders.collectAsState()
    var selectedChat by remember { mutableStateOf<TdApi.Chat?>(null) }
    var selectedChatForInfoPane by remember { mutableStateOf<TdApi.Chat?>(null) }
    var filterFolderChipValue by remember { mutableStateOf<TdApi.ChatFolder?>(null) }
    var pinnedChats by remember { mutableStateOf(emptyList<Long>()) }
    var includedChats by remember { mutableStateOf(emptyList<Long>()) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var chatPaneWidth by remember { mutableStateOf(0.dp) }
    var isRefreshing by remember { mutableStateOf(false) }
    var paddingValuesOpened by remember { mutableStateOf(PaddingValues(0.dp, 108.dp, 0.dp, 0.dp)) }
    val paddingValues by remember { mutableStateOf(PaddingValues(0.dp, 92.dp, 0.dp, 0.dp)) }
    var isChatOpened by remember { mutableStateOf(true) }
    var voiceNoteChatId by remember { mutableStateOf<Long?>(null) }
    var voiceNotePlaying by remember { mutableStateOf<MessageVoiceNote?>(null) }
    var idMessageOfVoiceNote by remember { mutableStateOf<Long?>(null) }
    var isVoicePlaying by remember { mutableStateOf(false) }
    var isVoicePanelOpened by remember { mutableStateOf(false) }

    LaunchedEffect(me) {
        if (me!=null) {
            chatMe = api.createPrivateChat(me!!.id, true)
        }
    }

    LaunchedEffect(photo) {
        if (photo?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(chatMe?.photo?.small)
        } else {
            avatarMePath = photo?.local?.path
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[photo?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            avatarMePath = downloadedFile.local?.path
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
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
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
                    paddingValuesOpened = padding
                    Column(modifier = Modifier.padding(padding)) {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                scope.launch {
                                    isRefreshing = true
                                    viewModel.updateChatsFromNetworkForView()
                                    delay(2000)
                                    isRefreshing = false
                                }
                            }
                        ) {
                            LazyColumn {
                                item {
                                    LazyRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        items(folders) { folderMain ->
                                            println("РИСУЕМ $folderMain")
                                            var folder by remember {
                                                mutableStateOf<TdApi.ChatFolder?>(
                                                    null
                                                )
                                            }
                                            LaunchedEffect(folders) {
                                                folder = api.getChatFolder(folderMain.chatFolderId)
                                            }
                                            if (folder != null) {
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
                                                            pinnedChats =
                                                                folder!!.pinnedChatIds.toList()
                                                            includedChats =
                                                                folder!!.includedChatIds.toMutableList()

                                                            scope.launch(Dispatchers.IO) {
                                                                val filteredChats =
                                                                    chats.filter { chat ->
                                                                        when {
                                                                            folder!!.includeBots && chat.type is TdApi.ChatTypePrivate -> {
                                                                                val userId =
                                                                                    (chat.type as TdApi.ChatTypePrivate).userId
                                                                                val user =
                                                                                    api.getUser(
                                                                                        userId
                                                                                    )
                                                                                user.type is TdApi.UserTypeBot
                                                                            }

                                                                            folder!!.includeGroups && (chat.type is TdApi.ChatTypeBasicGroup ||
                                                                                    (chat.type is TdApi.ChatTypeSupergroup && !(chat.type as TdApi.ChatTypeSupergroup).isChannel)) -> true

                                                                            folder!!.includeChannels && chat.type is TdApi.ChatTypeSupergroup &&
                                                                                    (chat.type as TdApi.ChatTypeSupergroup).isChannel -> true

                                                                            folder!!.includeContacts && chat.type is TdApi.ChatTypePrivate &&
                                                                                    isUserContact((chat.type as TdApi.ChatTypePrivate).userId) -> true

                                                                            folder!!.includeNonContacts && chat.type is TdApi.ChatTypePrivate &&
                                                                                    !isUserContact((chat.type as TdApi.ChatTypePrivate).userId) -> true

                                                                            else -> false
                                                                        }
                                                                    }

                                                                includedChats += filteredChats.map { it.id }

                                                                if (folder!!.excludeRead) {
                                                                    includedChats =
                                                                        includedChats.filter { chatId ->
                                                                            api.getChat(chatId).unreadCount > 0
                                                                        }.toMutableList()
                                                                }

                                                                if (folder!!.excludeMuted) {
                                                                    includedChats =
                                                                        includedChats.filter { chatId ->
                                                                            api.getChat(chatId).notificationSettings.muteFor == 0
                                                                        }.toMutableList()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    selected = filterFolderChipValue == folder,
                                                    label = { Text(text = folder!!.name.text.text) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
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
                                                api.openChat(chat.id)
                                                navigator.navigateTo(
                                                    ListDetailPaneScaffoldRole.Detail,
                                                    chat.id
                                                )
                                                isChatOpened = true
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Обычные чаты
                                items(chats.filter {
                                    it.id in includedChats && it.id != (chatMe?.id ?: 0L)
                                }) { chat ->
                                    ChatItem(
                                        chat = chat,
                                        viewModel = viewModel,
                                        isSelected = chat.id == selectedChatId,
                                        onChatClick = {
                                            scope.launch {
                                                api.openChat(chat.id)
                                                navigator.navigateTo(
                                                    ListDetailPaneScaffoldRole.Detail,
                                                    chat.id
                                                )
                                                isChatOpened = true
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        detailPane = {
            selectedChat?.let { chat ->
                AnimatedPane (
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        chatPaneWidth = with(density) { coordinates.size.width.toDp() }
                    },
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
                        onBackClick = { finalPosition ->
                            scope.launch {
                                navigator.navigateBack()
                                if (finalPosition > 0) {
                                    viewModel.saveScrollPosition(chat.id, finalPosition)
                                }
                                api.closeChat(chat.id)
                            }
                            isChatOpened = false
                        },
                        onShowInfo = {
                            selectedChatForInfoPane = selectedChat
                            showBottomSheet = true
                        },
                        window = window,
                        isVoicePlaying = isVoicePlaying,
                        idMessageOfVoiceNote = idMessageOfVoiceNote,
                        onTogglePlay = { messageId, content, chatId ->
                            if (idMessageOfVoiceNote != messageId) {
                                isVoicePlaying = false
                                isVoicePanelOpened = false
                                Handler(Looper.getMainLooper()).post {
                                    voiceNoteChatId = chatId
                                    idMessageOfVoiceNote = messageId
                                    voiceNotePlaying = content
                                    isVoicePanelOpened = true
                                    isVoicePlaying = true
                                }
                            } else {
                                isVoicePlaying = !isVoicePlaying
                            }
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
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (isChatOpened) paddingValues else paddingValuesOpened)
                .widthIn(max = chatPaneWidth),
            visible = isVoicePanelOpened,
            enter = slideIn { IntOffset(0, it.height * -2) } + fadeIn(),
            exit = slideOut { IntOffset(0, it.height * -2) } + fadeOut()
        ) {
            ElevatedCard(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                idMessageOfVoiceNote?.let {
                    VoiceNotePlayer(
                        content = voiceNotePlaying,
                        viewModel = viewModel,
                        chatId = voiceNoteChatId,
                        messageId = it,
                        isPlaying = isVoicePlaying,
                        onCloseClicked = {
                            isVoicePanelOpened = false
                            isVoicePlaying = false
                            idMessageOfVoiceNote = null
                            voiceNotePlaying = null
                        },
                        onStopedPlay = {
                            scope.launch {
                                viewModel.markVoiceNoteAsListened(voiceNoteChatId, idMessageOfVoiceNote)
                            }
                            isVoicePanelOpened = false
                            isVoicePlaying = false
                            voiceNotePlaying = null
                        },
                        onPlayClicked = { isVoicePlaying = !isVoicePlaying },
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { chatId ->
            navigator.navigateTo(ThreePaneScaffoldRole.Primary, chatId)
        }
    }
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
    val scope = rememberCoroutineScope()

    val photo = chat.photo?.small
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    var avatarPath by remember { mutableStateOf(downloadedFiles[chat.photo?.small?.id]?.local?.path)}

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                scope.launch {
                    onChatClick(chat.id)
                }
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
                Box(modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)) {
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
                Row (verticalAlignment = Alignment.CenterVertically) {
                    var messageContent by remember { mutableStateOf<MessageContent?>(null) }
                    LaunchedEffect(chat) {
                        messageContent = getMessageContent(chat.id, chat.lastMessage?.id ?: 0L, viewModel)
                    }
                    if (chat.lastMessage?.isOutgoing == true) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Вы:")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    messageContent?.thumbnail?.let {
                        ByteArrayImage(
                            imageData = it,
                            contentDescription = "Медиа в ответе",
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    messageContent?.textForReply?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            chat.lastMessage?.date?.toLong()
            Column (
                horizontalAlignment = Alignment.End,
            )
            {
                Text(converUnixTimeStampForChatList(chat.lastMessage?.date?.toLong() ?: 0L), style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
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
}

@Composable
fun VoiceNotePlayer(
    content: MessageVoiceNote?,
    viewModel: MainViewModel,
    chatId: Long?,
    messageId: Long?,
    isPlaying: Boolean,
    onCloseClicked: () -> Unit,
    onPlayClicked: () -> Unit,
    onStopedPlay: () -> Unit
) {
    val voiceNote = content?.voiceNote
    val voiceFile = voiceNote?.voice
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val context = LocalContext.current
    val isDownloaded = voiceFile?.local?.isDownloadingCompleted == true || downloadedFiles[voiceFile?.id]?.local?.isDownloadingCompleted == true

    val exoPlayer = remember(messageId) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF

            // Добавляем слушатель для отслеживания состояния плеера
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Когда воспроизведение завершено, вызываем onCloseClicked
                        onStopedPlay()
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    val progress = remember(messageId) { mutableFloatStateOf(0f) }
    var currentPosition by remember(messageId) { mutableLongStateOf(0L) }
    var totalDuration by remember(messageId) { mutableLongStateOf(0L) }

    LaunchedEffect(messageId) {
        while (true) {
            delay(100)
            if (exoPlayer.duration > 0) {
                progress.floatValue = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                currentPosition = exoPlayer.currentPosition
                totalDuration = exoPlayer.duration
            }
        }
    }


    LaunchedEffect(voiceFile) {
        when {
            voiceFile == null -> return@LaunchedEffect
            !voiceFile.local.isDownloadingCompleted && chatId != null && messageId != null -> {
                viewModel.addFileToDownloads(voiceFile, chatId, messageId)
            }
            voiceFile.local.isDownloadingCompleted -> {
                voiceFile.local.path?.let { path ->
                    exoPlayer.setMediaItem(MediaItem.fromUri(path))
                    exoPlayer.prepare()
                }
            }
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[voiceFile?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            downloadedFile.local.path?.let { path ->
                exoPlayer.setMediaItem(MediaItem.fromUri(path))
                exoPlayer.prepare()
            }
        }
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    val waveform = voiceNote?.waveform

    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
        Row {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (isDownloaded) {
                            onPlayClicked()
                        }
                    }
            ) {
                if (!isDownloaded) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        modifier = Modifier.align(Alignment.Center),
                        painter = painterResource(
                            if (isPlaying) R.drawable.baseline_pause_24
                            else R.drawable.baseline_play_arrow_24
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = "Управление воспроизведением"
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))

            val primaryColor = MaterialTheme.colorScheme.primaryContainer
            val progressColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
            ) {
                val step = size.width / (waveform?.size ?: 1)
                val progressIndex = ((waveform?.size ?: 1) * progress.floatValue).toInt()

                waveform?.forEachIndexed { index, amplitude ->
                    val height = (amplitude / 255f) * size.height
                    val color = if (index <= progressIndex) progressColor else primaryColor

                    drawLine(
                        color = color,
                        start = Offset(index * step, size.height / 2 - height / 2),
                        end = Offset(index * step, size.height / 2 + height / 2),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))

            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = buildString {
                    val currentSeconds = (currentPosition / 1000).toInt()
                    val totalSeconds = (totalDuration / 1000).toInt()
                    append(formatDuration(currentSeconds))
                    append(" / ")
                    append(formatDuration(totalSeconds))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        exoPlayer.release()
                        onCloseClicked()
                    }
            ) {
                Icon(
                    modifier = Modifier.align(Alignment.Center),
                    painter = painterResource(
                        R.drawable.baseline_close_24
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = "Управление воспроизведением"
                )
            }

        }
    }
}