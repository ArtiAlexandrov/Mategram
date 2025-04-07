package com.xxcactussell.mategram.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessageText
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.isUserContact
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import kotlinx.coroutines.Dispatchers
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageVideo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListView(navController: NavController,
                 viewModel: MainViewModel = viewModel()
    ) {
    val scope = rememberCoroutineScope()

    val chats by viewModel.visibleChats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.updateChatsFromNetworkForView()
    }

    val folders by viewModel.chatFolders.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorBottomAppBar = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    var filterFolderChipValue: TdApi. ChatFolder? by remember { mutableStateOf(null) }
    var pinnedChats by remember { mutableStateOf(emptyList<Long>()) }
    var includedChats by remember { mutableStateOf(emptyList<Long>()) }

    LaunchedEffect(chats) {
        pinnedChats = chats.filter { chat ->
            chat.positions?.firstOrNull()?.isPinned == true
        }.map { it.id }
        includedChats = chats.filter { chat ->
            chat.positions?.firstOrNull()?.isPinned == false
        }.map { it.id }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Чаты") },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Localized description",
                        )
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = "Localized description",
                        )
                    }
                },
                scrollBehavior = scrollBehaviorBottomAppBar,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.offset(y = 4.dp),
                onClick = { /* do something */ },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Filled.Add, "Localized description")
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection).nestedScroll(scrollBehaviorBottomAppBar.nestedScrollConnection) // Связываем прокрутку LazyColumn с TopAppBar
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
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
                                                    val userId = (chat.type as TdApi.ChatTypePrivate).userId
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
            // Список чатов
            items(chats) { chat ->
                if (chat.id in pinnedChats) {
                    ChatItem(
                        chat = chat,
                        viewModel = viewModel,
                        onChatClick = { chatId ->
                            // Переход на экран деталей с передачей chatId в маршрут
                            navController.navigate("chat_detail/$chatId")
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            // Список чатов
            items(chats) { chat ->
                if (chat.id in includedChats) {
                    ChatItem(
                        chat = chat,
                        viewModel = viewModel,
                        onChatClick = { chatId ->
                            // Переход на экран деталей с передачей chatId в маршрут
                            navController.navigate("chat_detail/$chatId")
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// Карточка чата с индикатором непрочитанных сообщений
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ChatItem(chat: TdApi.Chat, viewModel: MainViewModel, onChatClick: (chatId: Long) -> Unit) {
    var avatarPath by remember { mutableStateOf<String?>(null) }

    println("PHOTOPATH: $avatarPath")
    LaunchedEffect(chat) {
        avatarPath = viewModel.getChatAvatarPath(chat)
    }
    val containerColorCard = if (chat.unreadCount > 0) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
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
                Box(modifier = Modifier.size(48.dp)) {
                    CircularProgressIndicator(color = Color.Gray)
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
