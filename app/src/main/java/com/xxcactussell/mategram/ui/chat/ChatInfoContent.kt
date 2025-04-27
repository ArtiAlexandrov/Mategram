package com.xxcactussell.mategram.ui.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.formatCompactNumber
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getBasicGroup
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getSupergroup
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
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
import org.drinkless.tdlib.TdApi.Supergroup
import org.drinkless.tdlib.TdApi.User
import org.drinkless.tdlib.TdApi.UserFullInfo
import org.drinkless.tdlib.TdApi.UserStatusLastMonth
import org.drinkless.tdlib.TdApi.UserStatusLastWeek
import org.drinkless.tdlib.TdApi.UserStatusOffline
import org.drinkless.tdlib.TdApi.UserStatusOnline
import org.drinkless.tdlib.TdApi.UserTypeBot

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChatInfoContent(
    chat: TdApi.Chat,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
    isMe: Boolean = false,
    isAdmin: Boolean = false
) {
    val photoList = chat.photo?.let { listOfNotNull(it.small, it.big) } ?: emptyList()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    var avatarPaths by remember { mutableStateOf(photoList.map { downloadedFiles[it.id]?.local?.path }) }

    // --- Full Info State ---
    val userFullInfo by viewModel.userFullInfo.collectAsState()
    val supergroupFullInfo by viewModel.supergroupFullInfo.collectAsState()
    val basicGroupFullInfo by viewModel.basicGroupFullInfo.collectAsState()

    val userForView by viewModel.userForView.collectAsState()
    val supergroupForView by viewModel.supergroupForView.collectAsState()
    val basicGroupForView by viewModel.basicGroupForView.collectAsState()

    val membersOfGroup by viewModel.membersOfGroup.collectAsState()
    val membersOfSuperGroup by viewModel.membersOfSuperGroup.collectAsState()

    val userStatuses by viewModel.userStatuses.collectAsState()
    var statusText by remember { mutableStateOf("") }

    var user by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(chat) {
        user =
            when (val type = chat.type) {
                is ChatTypePrivate -> api.getUser(type.userId)
                is ChatTypeSupergroup -> api.getSupergroup(type.supergroupId)
                is ChatTypeBasicGroup -> api.getBasicGroup(type.basicGroupId)
                else -> null
            }
        when (chat.type) {
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
        if (chat.type is ChatTypePrivate && user != null) {
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



    // --- Request Full Info ---
    LaunchedEffect(chat.id, chat.type) {
        when (chat.type) {
            is TdApi.ChatTypePrivate -> {
                viewModel.loadUserFullInfo(chat.id)
                viewModel.loadUser(chat.id)
            }
            is TdApi.ChatTypeSupergroup -> {
                viewModel.loadSupergroupFullInfo((chat.type as TdApi.ChatTypeSupergroup).supergroupId)
                viewModel.loadSupergroup((chat.type as TdApi.ChatTypeSupergroup).supergroupId)
            }
            is TdApi.ChatTypeBasicGroup -> {
                viewModel.loadBasicGroupFullInfo((chat.type as TdApi.ChatTypeBasicGroup).basicGroupId)
                viewModel.loadBasicGroup((chat.type as TdApi.ChatTypeBasicGroup).basicGroupId)
            }
        }
    }

    LaunchedEffect(supergroupForView) {
        when (chat.type) {
            is TdApi.ChatTypeSupergroup -> {
                viewModel.loadMembersFromSupergroup(supergroupFullInfo, supergroupForView?.id)
            }
        }
    }

    LaunchedEffect(basicGroupForView) {
        when (chat.type) {
            is TdApi.ChatTypeBasicGroup -> {
                viewModel.loadMembers(basicGroupFullInfo)
            }
        }
    }
    // Download all available avatars (media carousel)
    LaunchedEffect(photoList) {
        photoList.forEach { photo ->
            if (photo.local?.isDownloadingCompleted == false) {
                viewModel.downloadFile(photo)
            }
        }
        avatarPaths = photoList.map { it.local?.path }
    }
    LaunchedEffect(downloadedFiles.values) {
        avatarPaths = photoList.map { downloadedFiles[it.id]?.local?.path ?: it.local?.path }
    }

    val userProfilePhotos by viewModel.userProfilePhotos.collectAsState()

    // Загружаем все аватарки пользователя
    LaunchedEffect(chat.id, chat.type) {
        if (chat.type is TdApi.ChatTypePrivate) {
            viewModel.loadUserProfilePhotos(chat.id)
        }
    }
    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (chat.type is TdApi.ChatTypePrivate && userProfilePhotos.isNotEmpty()) {
                        ProfilePhotos(
                            photos = userProfilePhotos,
                            downloadedFiles = downloadedFiles,
                            viewModel = viewModel
                        )
                    } else if ((chat.type is TdApi.ChatTypeSupergroup || chat.type is TdApi.ChatTypeBasicGroup) && chat.photo != null) {
                        // Оборачиваем в список для совместимости с ProfilePhotos
                        ProfilePhotos(
                            photos = chat.photo,
                            downloadedFiles = downloadedFiles,
                            viewModel = viewModel
                        )
                    }
                }
            }
            item {
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    headlineContent = {
                        Text(
                            text = chat.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    },
                    supportingContent = {
                        ChatStatusText(
                            status = statusText,
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {}
                        ) {
                            Icon(painterResource(R.drawable.baseline_dots), "menu")
                        }
                    }
                )
            }

            val links: Array<String>? = when (chat.type) {
                is TdApi.ChatTypePrivate -> userForView?.usernames?.activeUsernames
                is TdApi.ChatTypeSupergroup -> supergroupForView?.usernames?.activeUsernames
                else -> emptyArray<String>()
            }

            val bioText: String? = when (chat.type) {
                is TdApi.ChatTypePrivate -> userFullInfo?.bio?.text?.takeIf { it.isNotEmpty() }
                is TdApi.ChatTypeSupergroup -> supergroupFullInfo?.description?.takeIf { it.isNotEmpty() }
                is TdApi.ChatTypeBasicGroup -> basicGroupFullInfo?.description?.takeIf { it.isNotEmpty() }
                else -> null
            }

            itemsIndexed(links ?: emptyArray<String>()) { index, link ->

                val isFirst = index == 0
                val isLast = index == (links?.lastIndex ?: -1)
                val onlyOne = (links?.size ?: 0) == 1
                val hasBio = !bioText.isNullOrEmpty()

                val shape = when {
                    onlyOne && !hasBio -> RoundedCornerShape(24.dp)
                    onlyOne && hasBio -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
                    isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
                    isLast && !hasBio -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
                    else -> RoundedCornerShape(4.dp)
                }

                Card(
                    Modifier.fillMaxWidth().padding(vertical = 1.dp)
                        .padding(horizontal = 16.dp),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        contentColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                {
                    ListItem(
                        trailingContent = {
                            Icon(painterResource(R.drawable.baseline_qr), "link")
                        },
                        headlineContent = {
                            Text(
                                text = "@$link",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }

            item {
                val shape = when {
                    links?.isEmpty() == true -> RoundedCornerShape(24.dp)
                    else -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
                }
                when (chat.type) {
                    is TdApi.ChatTypePrivate -> {
                        if (userFullInfo is UserFullInfo && userFullInfo?.bio != null && userFullInfo?.bio?.text != "") {
                            Card(
                                Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                    .padding(horizontal = 16.dp),
                                shape = shape,
                                colors = CardDefaults.cardColors(
                                    contentColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                            {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = getAnnotatedString(userFullInfo!!.bio),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    is TdApi.ChatTypeSupergroup -> {
                        supergroupFullInfo?.description?.let {
                            if (it != "") {
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                        .padding(horizontal = 16.dp),
                                    shape = shape,
                                    colors = CardDefaults.cardColors(
                                        contentColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                )
                                {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is TdApi.ChatTypeBasicGroup -> {
                        basicGroupFullInfo?.description?.let {
                            if (it != "") {
                                Card(
                                    Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                        .padding(horizontal = 16.dp),
                                    shape = shape,
                                    colors = CardDefaults.cardColors(
                                        contentColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                )
                                {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

            }

            item {
                var notificationsEnabled by remember { mutableStateOf(chat.notificationSettings?.muteFor == 0) }
                Card(
                    modifier = Modifier.padding(vertical = 8.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notificationsEnabled) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                "Уведомления",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = {
                                    notificationsEnabled = it
                                    // TODO: Update notification settings via viewModel
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
            // --- Content by chat type ---
            when (chat.type) {
                is TdApi.ChatTypeBasicGroup -> {
                    // Group
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                    item {
                        Text("Участники", style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    itemsIndexed(membersOfGroup ?: emptyList()) { index, member ->

                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
                            membersOfGroup?.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
                            else -> RoundedCornerShape(4.dp)
                        }

                        val photo = member.profilePhoto?.small
                        var avatarPath by remember { mutableStateOf(downloadedFiles[member.profilePhoto?.small?.id]?.local?.path) }

                        LaunchedEffect(photo) {
                            if (photo?.local?.isDownloadingCompleted == false) {
                                viewModel.downloadFile(member.profilePhoto?.small)
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
                            Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                .padding(horizontal = 16.dp),
                            shape = shape,
                            colors = CardDefaults.cardColors(
                                contentColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                        {
                            ListItem(
                                modifier = Modifier.clip(shape),
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                    ) {
                                        if (avatarPath != null) {
                                            AsyncImage(
                                                model = avatarPath,
                                                contentDescription = "Аватарка чата",
                                                modifier = Modifier
                                                    .size(48.dp)
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
                                                    text = chat.title?.firstOrNull()
                                                        ?.toString()
                                                        ?: "Ч",
                                                    modifier = Modifier.align(Alignment.Center),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                },
                                headlineContent = {
                                    Text(
                                        text = "${member.firstName} ${member.lastName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                is TdApi.ChatTypeSupergroup -> {
                    if (isAdmin) {
                        item {
                            Text("Участники", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(onClick = { /* TODO: Show subscribers */ }) { Text("Подписчики") }
                                Button(onClick = { /* TODO: Show admins */ }) { Text("Администраторы") }
                                Button(onClick = { /* TODO: Show removed users */ }) { Text("Removed Users") }
                                Button(onClick = { /* TODO: Show channel settings */ }) { Text("Настройки канала") }
                            }
                        }
                    }
                    if (supergroupForView?.isChannel == false) {
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                        item {
                            Text("Участники", style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        itemsIndexed(membersOfSuperGroup ?: emptyList()) { index, member ->

                            val shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
                                membersOfSuperGroup?.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
                                else -> RoundedCornerShape(4.dp)
                            }

                            val photo = member.profilePhoto?.small
                            var avatarPath by remember { mutableStateOf(downloadedFiles[member.profilePhoto?.small?.id]?.local?.path) }

                            LaunchedEffect(photo) {
                                if (photo?.local?.isDownloadingCompleted == false) {
                                    viewModel.downloadFile(member.profilePhoto?.small)
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
                                Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                    .padding(horizontal = 16.dp),
                                shape = shape,
                                colors = CardDefaults.cardColors(
                                    contentColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                            {
                                ListItem(
                                    modifier = Modifier.clip(shape),
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                        ) {
                                            if (avatarPath != null) {
                                                AsyncImage(
                                                    model = avatarPath,
                                                    contentDescription = "Аватарка чата",
                                                    modifier = Modifier
                                                        .size(48.dp)
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
                                                        text = chat.title?.firstOrNull()
                                                            ?.toString()
                                                            ?: "Ч",
                                                        modifier = Modifier.align(Alignment.Center),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    headlineContent = {
                                        Text(
                                            text = "${member.firstName} ${member.lastName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // --- If it's me ---
            if (isMe) {
                {

                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotos(
    photos: Any?,
    downloadedFiles: Map<Int, TdApi.File?>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    when (photos) {
        is List<*> -> {
            val paths = remember(photos, downloadedFiles) {
                photos.mapNotNull { photo ->
                    when (photo) {
                        is TdApi.ChatPhoto -> {
                            val largest = photo.sizes?.maxByOrNull { it.photo?.expectedSize ?: 0 }
                            val fileId = largest?.photo?.id
                            downloadedFiles[fileId]?.local?.path ?: largest?.photo?.local?.path
                        }
                        is TdApi.ChatPhotoInfo -> {
                            val fileId = photo.big?.id ?: photo.small?.id
                            downloadedFiles[fileId]?.local?.path ?: photo.big?.local?.path ?: photo.small?.local?.path
                        }
                        else -> null
                    }
                }
            }

            LaunchedEffect(photos) {
                photos.forEach { photo ->
                    when (photo) {
                        is TdApi.ChatPhoto -> {
                            val largest = photo.sizes?.maxByOrNull { it.photo?.expectedSize ?: 0 }
                            largest?.photo?.let { file ->
                                if (file.local?.isDownloadingCompleted == false) viewModel.downloadFile(file)
                            }
                        }
                        is TdApi.ChatPhotoInfo -> {
                            photo.big?.let { file ->
                                if (file.local?.isDownloadingCompleted == false) viewModel.downloadFile(file)
                            }
                        }
                    }
                }
            }

            if (paths.size > 1) {
                val carouselState = rememberCarouselState { paths.size }

                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = 300.dp,
                    itemSpacing = 16.dp,
                    modifier = modifier.height(300.dp)
                ) { itemIndex ->
                    val path = paths.getOrNull(itemIndex)
                    if (path != null) {
                        Box(
                            modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                        ) {
                            AsyncImage(
                                model = path,
                                contentDescription = "Chat avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            } else if (paths.size == 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(150.dp))
                        .size(300.dp)
                ) {
                    AsyncImage(
                        model = paths[0],
                        contentDescription = "Chat avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        is TdApi.ChatPhoto -> {
            val largest = photos.sizes?.maxByOrNull { it.photo?.expectedSize ?: 0 }
            val fileId = largest?.photo?.id
            val path = downloadedFiles[fileId]?.local?.path ?: largest?.photo?.local?.path
            LaunchedEffect(photos) {
                largest?.photo?.let { file ->
                    if (file.local?.isDownloadingCompleted == false) viewModel.downloadFile(file)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = path,
                    contentDescription = "Chat avatar",
                    modifier = Modifier
                        .clip(RoundedCornerShape(150.dp))
                        .size(300.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Crop
                )
            }
        }
        is TdApi.ChatPhotoInfo -> {
            val fileId = photos.big?.id ?: photos.small?.id
            val path = downloadedFiles[fileId]?.local?.path ?: photos.big?.local?.path ?: photos.small?.local?.path
            LaunchedEffect(photos) {
                photos.big?.let { file ->
                    if (file.local?.isDownloadingCompleted == false) viewModel.downloadFile(file)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = path,
                    contentDescription = "Chat avatar",
                    modifier = Modifier
                        .clip(RoundedCornerShape(150.dp))
                        .size(300.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}