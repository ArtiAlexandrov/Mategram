package com.xxcactussell.mategram

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.loadChatDetails
import com.xxcactussell.mategram.domain.entity.AuthState
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramCredentials
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMe
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.openChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.sendMessage
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setOption
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setTdlibParameters
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.viewMessages
import com.xxcactussell.mategram.kotlinx.telegram.flows.authorizationStateFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.notificationFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.notificationGroupFlow
import com.xxcactussell.mategram.notifications.FcmManager
import com.xxcactussell.mategram.notifications.NotificationHelper
import com.xxcactussell.mategram.notifications.NotificationSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.Chat
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelegramRepository
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun setAuthState(state: AuthState) {
        viewModelScope.launch {
            _authState.value = state
        }
    }

    init {
        viewModelScope.launch {
            repository.authStateFlow.collect { state ->
                Log.d("MainViewModel", "Raw auth state: $state")
            }
        }
    }

    fun performAuthResult() {
        viewModelScope.launch {
            repository.checkAuthState()

            api.authorizationStateFlow()
                .onEach { state ->
                    when (state) {
                        is TdApi.AuthorizationStateWaitTdlibParameters -> {
                            Log.d("MainViewModel", "Setting TDLib parameters...")
                            try {
                                api.setTdlibParameters(
                                    TelegramCredentials.useTestDc,
                                    TelegramCredentials.databaseDirectory,
                                    TelegramCredentials.filesDirectory,
                                    TelegramCredentials.encryptionKey,
                                    TelegramCredentials.useFileDatabase,
                                    TelegramCredentials.useChatInfoDatabase,
                                    TelegramCredentials.useMessageDatabase,
                                    TelegramCredentials.useSecretChats,
                                    TelegramCredentials.apiId,
                                    TelegramCredentials.apiHash,
                                    TelegramCredentials.systemLanguageCode,
                                    TelegramCredentials.deviceModel,
                                    TelegramCredentials.systemVersion,
                                    TelegramCredentials.applicationVersion
                                )
                                Log.d("MainViewModel", "TDLib parameters set successfully")
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Error setting TDLib parameters", e)
                            }
                        }
                        else -> { /* ignore other states here */ }
                    }
                }
                .map { state ->
                    when (state) {
                        is TdApi.AuthorizationStateWaitTdlibParameters -> {
                            Log.d("MainViewModel", "Mapped: Waiting for TDLib parameters")
                            AuthState.WaitTdlibParameters
                        }
                        is TdApi.AuthorizationStateWaitPhoneNumber -> {
                            Log.d("MainViewModel", "Mapped: Waiting for phone")
                            AuthState.WaitPhone
                        }
                        is TdApi.AuthorizationStateWaitCode -> {
                            Log.d("MainViewModel", "Mapped: Waiting for code")
                            AuthState.WaitCode
                        }
                        is TdApi.AuthorizationStateWaitPassword -> {
                            Log.d("MainViewModel", "Mapped: Waiting for password")
                            AuthState.WaitPassword
                        }
                        is TdApi.AuthorizationStateReady -> {
                            Log.d("MainViewModel", "Mapped: Ready")
                            AuthState.Ready
                        }
                        else -> {
                            Log.d("MainViewModel", "Mapped: Unknown state: $state")
                            AuthState.NoAuth
                        }
                    }
                }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { mappedState ->
                    Log.d("MainViewModel", "Updating state to: $mappedState")
                    _authState.value = mappedState
                }
        }
    }
    private val _navigationEvent = MutableSharedFlow<Long>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun openChat(chatId: Long) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "Opening chat: $chatId")
                api.openChat(chatId)

                // Emit navigation event
                _navigationEvent.emit(chatId)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to open chat: ${e.message}")
            }
        }
    }

    // Отправка номера телефона
    fun sendPhone(phone: String) {
        viewModelScope.launch {
            println("MainViewModel: отправляем номер телефона: $phone")
            repository.sendPhone(phone)
        }
    }

    // Отправка кода подтверждения
    fun sendCode(code: String) {
        viewModelScope.launch {
            println("MainViewModel: отправляем код подтверждения: $code")
            repository.sendCode(code)
        }
    }

    // Отправка пароля
    fun sendPassword(password: String) {
        viewModelScope.launch {
            println("MainViewModel: отправляем пароль: $password")
            repository.sendPassword(password)
        }
    }

    // Выход из аккаунта
    fun logOut() {
        viewModelScope.launch {
            println("MainViewModel: выходим из аккаунта...")
            repository.logOut()
        }
    }

    private var _me = MutableStateFlow<TdApi.User?>(null)
    val me : StateFlow<TdApi.User?> = _me

    init {
        observeAuthorizationState()
    }

    private fun observeAuthorizationState() {
        viewModelScope.launch(Dispatchers.IO) {
            authState.collect { state ->
                if (state is AuthState.Ready) {
                    val context = getApplication<Application>().applicationContext
                    FcmManager(context).getFcmToken()
                        ?.let { FcmManager(context).registerDeviceToken(it) }
                    _me.value = api.getMe()
                    setNotificationOptions()
                    observeAllChatUpdates()
                    updateChatsFromNetworkForView()
                    observeChatUpdates()
                    observeUnreadCount()
                }
            }
        }
    }

    private fun setNotificationOptions() {
        viewModelScope.launch {
            // Установка максимального количества групп уведомлений
            api.setOption(
                name = "notification_group_count_max",
                value = TdApi.OptionValueInteger(10)
            )

            // Установка максимального размера группы уведомлений
            api.setOption(
                name = "notification_group_size_max",
                value = TdApi.OptionValueInteger(20)
            )
        }
    }




    private val _visibleChats = MutableStateFlow<List<Chat>>(emptyList())
    val visibleChats = _visibleChats.asStateFlow()
    private val chatMap = mutableMapOf<Long, Chat>()

    private val chatUpdatesScope = TelegramRepository.chatUpdatesScope

    fun updateChatsFromNetworkForView(limit: Int = 15) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Запускаем загрузку чатов
                api.client?.send(TdApi.LoadChats(TdApi.ChatListMain(), limit)) { response ->
                    when (response) {
                        is TdApi.Ok -> Log.d("ChatUpdater", "Chats were loaded successfully")
                        is TdApi.Error -> Log.e("ChatUpdater", "Failed to load chats: $response")
                    }
                }
            } catch (e: Exception) {
                println("Error loading chats: ${e.message}")
            }
        }
    }

    private fun observeAllChatUpdates() {
        chatUpdatesScope.launch {
            launch {
                repository.newChatFlowUpdate.collect { chat ->
                    handleNewChatUpdate(chat)
                }
            }
            launch {
                repository.chatLastMessageUpdate.collect { update ->
                    handleChatLastMessage(update)
                }
            }
            launch {
                repository.chatAddedToList.collect { update ->
                    handleChatAddedToList(update)
                }
            }
            launch {
                repository.chatDraftUpdate.collect { update ->
                    handleChatDraft(update)
                }
            }
            launch {
                repository.chatPositionUpdate.collect { update ->
                    handleChatPosition(update)
                }
            }
        }
    }

    private suspend fun handleChatPosition(update: TdApi.UpdateChatPosition) {
        val chatId = update.chatId
        Log.d("ChatUpdater" , "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private suspend fun handleChatDraft(update: TdApi.UpdateChatDraftMessage) {
        val chatId = update.chatId
        Log.d("ChatUpdater" , "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private suspend fun handleChatAddedToList(update: TdApi.UpdateChatAddedToList) {
        val chatId = update.chatId
        Log.d("ChatUpdater" , "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private suspend fun handleChatLastMessage(update: TdApi.UpdateChatLastMessage) {
        val chatId = update.chatId
        Log.d("ChatUpdater" , "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private fun handleNewChatUpdate(chat: Chat) {
        Log.d("ChatUpdater" , "${chat.id}")
        handleChatUpdate(chat)
    }

    private fun observeChatUpdates() {
        chatUpdatesScope.launch(Dispatchers.IO) {
            TelegramRepository.messageFlow.collect { message ->
                val chat = api.getChat(message.chatId)
                handleChatUpdate(chat)

                val messageFlow = mapOfMessages.getOrPut(message.chatId) {
                    MutableStateFlow(mutableListOf())
                }
                messageFlow.update { currentList ->
                    (mutableListOf(message) + currentList).toMutableList()
                }
            }
        }
    }


    private fun observeUnreadCount() {
        viewModelScope.launch(Dispatchers.IO) {
            TelegramRepository.chatReadInbox.collect { update ->
                val chatId = update.chatId
                val chat = api.getChat(chatId)
                handleChatUpdate(chat)
            }
        }
    }



    private var _chatFolders = MutableStateFlow<List<TdApi.ChatListFolder>>(emptyList())
    var chatFolders: StateFlow<List<TdApi.ChatListFolder>> = _chatFolders

    private fun handleChatUpdate(chat: Chat) {
        synchronized(chatMap) {
            chatMap[chat.id] = chat
        }
        updateVisibleChats()
    }

    private fun updateVisibleChats() {
        viewModelScope.launch {
            // Создаем новый список для безопасного обновления
            val sortedChats = synchronized(chatMap) {
                chatMap.values.sortedWith(
                    compareByDescending<Chat> { chat ->
                        chat.positions?.firstOrNull()?.order ?: 0L
                    }.thenByDescending { it.id }
                )
            }
            _visibleChats.value = sortedChats
        }
    }

    val mapOfMessages = ConcurrentHashMap<Long, MutableStateFlow<MutableList<TdApi.Message>>>()

    fun getMessagesForChat(chatId: Long, fromMessage: Long = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val newMessages = TelegramRepository.getMessagesForChat(chatId, fromMessage)

            val messageFlow = mapOfMessages.getOrPut(chatId) {
                MutableStateFlow(mutableListOf())
            }

            messageFlow.update { currentMessages ->
                if (fromMessage == 0L) {
                    // Если это первая загрузка, просто заменяем список
                    newMessages.messages.toMutableList()
                } else {
                    // Добавляем только новые сообщения
                    val existingIds = currentMessages.map { it.id }.toSet()
                    val uniqueNewMessages = newMessages.messages.filter { it.id !in existingIds }
                    (currentMessages + uniqueNewMessages).distinctBy { it.id }.toMutableList()
                }
            }
        }
    }

    private val _avatarPaths = MutableStateFlow<Map<Long, String?>>(emptyMap())
    private val fileIdToChatId = mutableMapOf<Int, Long>()

    suspend fun getChatAvatarPath(chat: Chat, size: String = "s"): String? {
        var avatarFileId: Int? = null
        if (size == "s") {
            avatarFileId = chat.photo?.small?.id
        } else {
            avatarFileId = chat.photo?.big?.id
        }

        if (avatarFileId != null) {
            val file = TelegramRepository.getFile(avatarFileId)
            if (!file.local.isDownloadingCompleted) {
                ensureFileDownload(avatarFileId)
                // Ждём завершения загрузки
                while (!TelegramRepository.getFile(avatarFileId).local.isDownloadingCompleted) {
                    delay(100)
                }
                // Получаем обновлённый файл
                return TelegramRepository.getFile(avatarFileId).local.path
            }
            return file.local.path
        }
        return null
    }

    suspend fun getDocumentThumbnail(documentThumbnail: TdApi.Thumbnail): String? {
        var file = TelegramRepository.getFile(documentThumbnail.file.id)
        if (!file.local.isDownloadingCompleted) {
            println("Файл не загружен, запускаем загрузку...")
            ensureFileDownload(documentThumbnail.file.id)

            // Добавляем ожидание загрузки
            file = awaitFileDownload(documentThumbnail.file.id)
        }

        return file.local?.path
    }

    suspend fun getPhotoPreviewFromChat(photoSize: TdApi.PhotoSize?): String? {
        if (photoSize != null) {
            var file = photoSize.photo?.let { TelegramRepository.getFile(it.id) }

            if (file != null && !file.local.isDownloadingCompleted) {
                println("Файл не загружен, запускаем загрузку...")
                ensureFileDownload(photoSize.photo.id)

                // Добавляем ожидание загрузки
                file = awaitFileDownload(photoSize.photo.id)
            }

            return file?.local?.path
        }
        return null
    }

    suspend fun getThumbnailVideoFromChat(videoFile: TdApi.File?): String? {
        if (videoFile != null) {
            var file = videoFile
            if (!file.local.isDownloadingCompleted) {
                println("Файл не загружен, запускаем загрузку...")
                ensureFileDownload(videoFile.id)

                // Добавляем ожидание загрузки
                file = awaitFileDownload(videoFile.id)
            }
            return file.local?.path
        }
        return null
    }

    suspend fun getStickerFromChat(stickerFile: TdApi.File?): String? {
        var file = stickerFile
        if (file != null && !file.local.isDownloadingCompleted) {
            println("Стикер не загружен, запускаем загрузку...")
            ensureFileDownload(file.id)

            // Добавляем ожидание загрузки
            file = awaitFileDownload(file.id)
        }
        if (file != null) {
            return file.local?.path
        }
        return null
    }

    private suspend fun awaitFileDownload(fileId: Int): TdApi.File {
        while (true) {
            val file = TelegramRepository.getFile(fileId)
            if (file.local.isDownloadingCompleted) {
                return file
            }
            delay(500) // Ожидание перед повторной проверкой
        }
    }


    private fun ensureFileDownload(fileId: Int) {
        viewModelScope.launch {
            val downloadRequest = TdApi.DownloadFile(fileId, 32, 0, 0, false)
            api.client?.send(downloadRequest) { result ->
                if (result is TdApi.File) {
                    println("Загрузка файла начата: ${result.id}")
                } else if (result is TdApi.Error) {
                    println("Ошибка загрузки файла: ${result.message}")
                }
            }
        }
    }

    private fun updateAvatarPath(chatId: Long, path: String?) {
        _avatarPaths.value = _avatarPaths.value.toMutableMap().apply {
            this[chatId] = path
        }
    }

    private fun findChatIdByFileId(fileId: Int): Long? {
        return fileIdToChatId[fileId]
    }

    fun sendMessage(chatId: Long, text: String, replyToMessageId: TdApi.InputMessageReplyTo? = null) {
        viewModelScope.launch {
            val inputMessage = TdApi.InputMessageText(
                TdApi.FormattedText(text, null),
                null,
                true
            )
            api.sendMessage(chatId, 0, replyToMessageId, null, null, inputMessage)
        }
    }

    suspend fun downloadFileToDownloads(context: Context, fileId: Int, fileName: String = "document.pdf"): Uri? {
        return withContext(Dispatchers.IO) {
            val channelId = "download_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Создаём канал уведомлений (Android 8+)
            val channel = NotificationChannel(
                channelId,
                "Загрузка файла",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(fileName)
                .setProgress(100, 0, true)
                .setOngoing(true)

            notificationManager.notify(fileId, notificationBuilder.build())

            // Запускаем загрузку файла
            ensureFileDownload(fileId)
            println("Запуск загрузки файла...")

            // Ожидаем завершения загрузки
            val downloadedFile = awaitFileDownload(fileId)
            if (!downloadedFile.local.isDownloadingCompleted) {
                println("Ошибка: файл не был загружен.")
                notificationManager.cancel(fileId)
                return@withContext null
            }

            // Определяем MIME-тип файла
            val mimeType = getMimeType(fileName)

            // Используем MediaStore API для сохранения файла в `Downloads`
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it).use { outputStream ->
                    FileInputStream(downloadedFile.local.path).use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }
                println("Файл сохранён в Downloads: $uri")

                // ✅ **Создаём Intent для открытия файла**
                val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openFileIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // ✅ **Уведомление о завершении загрузки**
                notificationBuilder
                    .setSmallIcon(R.drawable.baseline_download_done_24)
                    .setContentText("Файл $fileName загружен! Нажмите, чтобы открыть")
                    .setProgress(0, 0, false)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setOngoing(false)

                notificationManager.notify(fileId, notificationBuilder.build())

                return@withContext uri
            }

            println("Ошибка сохранения файла.")
            notificationManager.cancel(fileId)
            return@withContext null
        }
    }

    suspend fun markAsRead(message: TdApi.Message) {
        println("Отмечаем сообщение ${message.id} как прочитанное в чате ${message.chatId}")
        api.viewMessages(message.chatId, longArrayOf(message.id), null, true)
    }

    suspend fun getMessageById(replyMessage: TdApi.MessageReplyToMessage): TdApi.Message {
        return api.getMessage(replyMessage.chatId, replyMessage.messageId)
    }

    fun handleNotificationOpen(chatId: Long) {
        viewModelScope.launch {
            try {
                // Ensure TDLib is ready
                api.authorizationStateFlow().firstOrNull()?.let { state ->
                    when (state) {
                        is TdApi.AuthorizationStateReady -> {
                            // Open chat
                            TdApi.OpenChat(chatId)
                        }
                        else -> {
                            Log.e("MainViewModel", "TDLib not ready when opening chat")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error opening chat from notification", e)
            }
        }
    }

    fun setOnline(b: Boolean) {
        viewModelScope.launch {
            api.setOption("online", TdApi.OptionValueBoolean(b))
        }
    }
}

fun getMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val NotificationHelper = com.xxcactussell.mategram.notifications.NotificationHelper

    private var notificationJob: Job? = null
    private val jobLock = AtomicBoolean(false)
    private val _isObservingNotifications = MutableStateFlow(false)
    val isObservingNotifications = _isObservingNotifications.asStateFlow()

    fun startObservingNotifications() {
        if (!jobLock.compareAndSet(false, true)) {
            Log.d("NotificationJob", "Observer already starting or running")
            return
        }

        notificationJob = viewModelScope.launch {
            try {
                // Parallel collection of notification flows
                coroutineScope {
                    launch {
                        api.notificationGroupFlow()
                            .distinctUntilChanged { old, new ->
                                old.notificationGroupId == new.notificationGroupId &&
                                        old.addedNotifications?.lastOrNull()?.id == new.addedNotifications?.lastOrNull()?.id
                            }
                            .collect { update ->
                                Log.d("Notifications", "Group update received: ${update.notificationGroupId}")
                                update.addedNotifications?.lastOrNull()?.let { lastNotification ->
                                    handleNotification(TdApi.UpdateNotification(update.notificationGroupId, lastNotification))
                                }
                            }
                    }

                    launch {
                        api.notificationFlow()
                            .distinctUntilChanged { old, new ->
                                old.notification.id == new.notification.id
                            }
                            .collect { update ->
                                Log.d("Notifications", "Single notification received: ${update.notification.id}")
                                handleNotification(update)
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error observing notifications", e)
            } finally {
                jobLock.set(false)
            }
        }
    }

    private fun stopObservingNotifications() {
        notificationJob?.cancel()
        notificationJob = null
        _isObservingNotifications.value = false
    }

    // Call this when checking status
    fun isNotificationObserverActive(): Boolean {
        return notificationJob?.isActive == true && jobLock.get()
    }

    override fun onCleared() {
        super.onCleared()
        notificationJob?.cancel()
        jobLock.set(false)
    }

    private val messageCache = mutableMapOf<Long, MutableList<NotificationHelper.MessageInfo>>() // chatId to messages

    private fun handleNotification(update: TdApi.UpdateNotification) {
        viewModelScope.launch {
            Log.d("NotificationDebug", "Received notification: ${update.notification.type}")

            val settingsManager = NotificationSettingsManager.getInstance(getApplication())
            val settings = settingsManager.loadNotificationSettings()

            // Check global notification settings first
            if (!settings.globalEnabled) {
                Log.d("NotificationDebug", "Notifications globally disabled")
                return@launch
            }

            when (val content = update.notification.type) {
                is TdApi.NotificationTypeNewMessage -> {
                    val message = content.message
                    val chat = loadChatDetails(message.chatId)
                    if (chat.unreadCount == 0) {
                        Log.d("NotificationDebug", "Chat ${chat.id} has 0 unread messages, skipping notification")
                        return@launch
                    }
                    // Get scope settings based on chat type
                    val scopeKey = when (chat.type) {
                        is TdApi.ChatTypePrivate, is TdApi.ChatTypeSecret -> "private_chats"
                        is TdApi.ChatTypeBasicGroup -> "group_chats"
                        is TdApi.ChatTypeSupergroup -> {
                            if ((chat.type as TdApi.ChatTypeSupergroup).isChannel) "channel_chats"
                            else "group_chats"
                        }
                        else -> "private_chats"
                    }

                    val scopeSettings = settings.scopeSettings[scopeKey]
                    val chatSettings = settings.chatSettings[chat.id]

                    // Check if notifications are muted
                    val isMuted = when {
                        chatSettings?.useDefault == false && chatSettings.muteFor > 0 -> true
                        chatSettings?.useDefault == true && (scopeSettings?.muteFor ?: 0) > 0 -> true
                        else -> false
                    }

                    if (isMuted) {
                        Log.d("NotificationDebug", "Notifications muted for chat ${chat.id}")
                        return@launch
                    }

                    // Process sender information
                    var senderName = ""
                    var title = ""
                    var chatPhoto: TdApi.File? = null
                    var isChannelPost = message.isChannelPost
                    when (val sender = message.senderId) {
                        is TdApi.MessageSenderUser -> {
                            val user = api.getUser(sender.userId)
                            chatPhoto = user.profilePhoto?.small
                            if (chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup) {
                                title = chat.title
                            } else {
                                title = "${chat.unreadCount} новых сообщений"
                            }
                            senderName = user.firstName + " " + user.lastName
                            Log.d("NotificationDebug", "Received message from user: ${user.firstName} ${user.lastName}")
                        }
                        is TdApi.MessageSenderChat -> {
                            val chatSender = api.getChat(sender.chatId)
                            senderName = chatSender.title ?: "Неизвестный чат"
                            Log.d("NotificationDebug", "Received message from chat: ${chat.title}")
                            chatPhoto = chat.photo?.small
                        }
                    }

                    if (!message.isOutgoing) {
                        val messageInfo =
                            com.xxcactussell.mategram.notifications.NotificationHelper.MessageInfo(
                                text = when (message.content) {
                                    is TdApi.MessageText -> (message.content as TdApi.MessageText).text.text
                                    is TdApi.MessagePhoto -> "📷 ${(message.content as TdApi.MessagePhoto).caption.text}"
                                    is TdApi.MessageVideo -> "🎥 ${(message.content as TdApi.MessageVideo).caption.text}"
                                    is TdApi.MessageSticker -> (message.content as TdApi.MessageSticker).sticker.emoji
                                    else -> "Новое сообщение"
                                },
                                timestamp = message.date * 1000L,
                                senderName = senderName
                            )

                        val messages = messageCache.getOrPut(chat.id) { mutableListOf() }
                        messages.add(messageInfo)

                        while (messages.size > chat.unreadCount || messages.size > 5) {
                            messages.removeAt(0)
                        }

                        // Check preview settings
                        val showPreview = when {
                            chatSettings?.useDefault == false -> chatSettings.showPreview
                            else -> scopeSettings?.showPreview ?: true
                        }
                        NotificationHelper.showMessageNotification(
                            context = getApplication(),
                            chatId = chat.id,
                            chatTitle = title,
                            messages = messages,
                            chatPhotoFile = chatPhoto,
                            notificationId = update.notificationGroupId,
                            unreadCount = chat.unreadCount,
                            isChannelPost = isChannelPost
                        )
                    }
                }
            }
        }
    }
}