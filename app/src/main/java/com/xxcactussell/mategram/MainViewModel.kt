package com.xxcactussell.mategram

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxcactussell.mategram.domain.entity.AuthState
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramCredentials
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramException
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.loadChatDetails
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.addFileToDownloads
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.cancelDownloadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.downloadFile
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
import org.drinkless.tdlib.TdApi.File
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream


class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelegramRepository
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun getMyString(id: Int): String {
        return getApplication<Application>().getString(id)
    }

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
                                    TelegramCredentials.USE_TEST_DC,
                                    TelegramCredentials.databaseDirectory,
                                    TelegramCredentials.filesDirectory,
                                    TelegramCredentials.encryptionKey,
                                    TelegramCredentials.USE_FILE_DATABASE,
                                    TelegramCredentials.USE_CHAT_INFO_DATABASE,
                                    TelegramCredentials.USE_MESSAGE_DATABASE,
                                    TelegramCredentials.USE_SECRET_CHATS,
                                    TelegramCredentials.API_ID,
                                    TelegramCredentials.API_HASH,
                                    TelegramCredentials.systemLanguageCode,
                                    TelegramCredentials.deviceModel,
                                    TelegramCredentials.systemVersion,
                                    TelegramCredentials.APPLICATION_VERSION
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
            launch {
                repository.fileUpdateFLow.collect { update ->
                    handleFileUpdate(update)
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

    fun sendVoiceNote(chatId: Long, filePath: String, replyToMessageId: TdApi.InputMessageReplyTo?) {
        viewModelScope.launch {
            try {
                val inputFile = TdApi.InputFileLocal(filePath)

                val duration = getAudioDuration(filePath)

                val waveform = generateWaveform(filePath)

                val caption = TdApi.FormattedText("", emptyArray<TdApi.TextEntity>())

                // Create the voice note message with null self-destruct type
                val voiceNote = TdApi.InputMessageVoiceNote().apply {
                    this.voiceNote = inputFile
                    this.duration = duration
                    this.waveform = waveform
                    this.caption = caption
                    this.selfDestructType = null
                }

                // Send the message
                api.sendMessage(
                    chatId = chatId,
                    messageThreadId = 0,
                    replyTo = replyToMessageId,
                    options = TdApi.MessageSendOptions(),
                    replyMarkup = null,
                    inputMessageContent = voiceNote
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sending voice note: ${e.message}")
            }
        }
    }
    private fun getAudioDuration(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            (durationStr?.toLong() ?: 0L).toInt() / 1000 // Convert milliseconds to seconds
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error getting audio duration: ${e.message}")
            0
        } finally {
            retriever.release()
        }
    }

    private fun generateWaveform(filePath: String): ByteArray {
        val retriever = MediaMetadataRetriever()
        val audioFile = java.io.File(filePath)
        val waveform = ByteArray(100) // TDLib expects 100 samples for voice notes

        try {
            retriever.setDataSource(filePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0

            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(filePath)

            // Find the audio track
            val audioTrackIndex = (0 until mediaExtractor.trackCount)
                .find { mediaExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: return ByteArray(100) { 50.toByte() } // Default waveform if no audio track found

            mediaExtractor.selectTrack(audioTrackIndex)
            val format = mediaExtractor.getTrackFormat(audioTrackIndex)

            // Get audio properties
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val samples = (durationMs * sampleRate / 1000).toInt()

            // Calculate samples per waveform point
            val samplesPerPoint = samples / waveform.size

            // Read audio data
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm")
            decoder.configure(format, null, null, 0)
            decoder.start()

            var currentWaveformIndex = 0
            var maxAmplitude = 0.0f
            var samplesRead = 0

            val info = MediaCodec.BufferInfo()

            while (currentWaveformIndex < waveform.size) {
                val inputBufferId = decoder.dequeueInputBuffer(10000)
                if (inputBufferId >= 0) {
                    val buffer = decoder.getInputBuffer(inputBufferId)
                    val sampleSize = mediaExtractor.readSampleData(buffer!!, 0)

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, mediaExtractor.sampleTime, 0)
                        mediaExtractor.advance()
                    }
                }

                val outputBufferId = decoder.dequeueOutputBuffer(info, 10000)
                if (outputBufferId >= 0) {
                    val buffer = decoder.getOutputBuffer(outputBufferId)
                    val shortBuffer = buffer?.asShortBuffer()

                    if (shortBuffer != null) {
                        while (shortBuffer.hasRemaining() && currentWaveformIndex < waveform.size) {
                            val amplitude = Math.abs(shortBuffer.get() / 32768.0f)
                            maxAmplitude = maxOf(maxAmplitude, amplitude)
                            samplesRead++

                            if (samplesRead >= samplesPerPoint) {
                                // Convert to 5-bit format (0-31 range) as required by TDLib
                                waveform[currentWaveformIndex] = (maxAmplitude * 31).toInt().toByte()
                                currentWaveformIndex++
                                maxAmplitude = 0.0f
                                samplesRead = 0
                            }
                        }
                    }

                    decoder.releaseOutputBuffer(outputBufferId, false)

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }

            decoder.stop()
            decoder.release()
            mediaExtractor.release()

        } catch (e: Exception) {
            Log.e("MainViewModel", "Error generating waveform: ${e.message}")
            return ByteArray(100) { 50.toByte() } // Return default waveform on error
        } finally {
            retriever.release()
        }

        return waveform
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


    private var _downloadedFiles = MutableStateFlow<MutableMap<Int, TdApi.File?>>(mutableMapOf())
    var downloadedFiles: StateFlow<MutableMap<Int, TdApi.File?>> = _downloadedFiles

    private fun handleFileUpdate(update: TdApi.UpdateFile) {
        viewModelScope.launch {
            val file = update.file

            _downloadedFiles.update { currentMap ->
                currentMap.toMutableMap().apply { this[file.id] = file }
            }
        }
    }

    suspend fun downloadFile(file: TdApi.File?, priority: Int = 32) {
        if (file != null) {
            api.downloadFile(file.id, priority, 0, 0, false)
        }
    }

    suspend fun cancelFileDownload(file: File?) {
        if (file != null) {
            api.cancelDownloadFile(file.id, false)
        }
    }

    suspend fun addFileToDownloads(file: TdApi.File, chatId: Long, messageId: Long, priority: Int = 32) {
        api.addFileToDownloads(file.id, chatId, messageId, priority)
    }

    fun installApk(context: Context, apkPath: String) {
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider",
            java.io.File(apkPath)
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent) // Запускаем установку APK
    }

    fun isApkFile(filePath: String): Boolean {
        return filePath.endsWith(".apk") || getMimeType(filePath) == "application/vnd.android.package-archive"
    }

    fun decompressTgs(filePath: String): String {
        val file = java.io.File(filePath)
        val inputStream = GZIPInputStream(FileInputStream(file))
        val outputStream = ByteArrayOutputStream()

        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } != -1) {
            outputStream.write(buffer, 0, length)
        }

        inputStream.close()
        return outputStream.toString(Charsets.UTF_8) // Возвращаем JSON строку
    }

    suspend fun markAsRead(message: TdApi.Message) {
        println("Отмечаем сообщение ${message.id} как прочитанное в чате ${message.chatId}")
        api.viewMessages(message.chatId, longArrayOf(message.id), null, true)
    }

    fun markVoiceNoteAsListened(chatId: Long?, messageId: Long?) {
        viewModelScope.launch {
            try {
                if (chatId != null && messageId != null) {
                    api.client?.send(
                        TdApi.OpenMessageContent(
                            chatId,
                            messageId
                        )
                    ) { result ->
                        Log.d("LOG", "$result")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error marking voice note as listened", e)
            }
        }
    }

    fun getMessageById(replyMessage: TdApi.MessageReplyToMessage): TdApi.Message? {
        var message: TdApi.Message? = null
        try {
            api.client?.send(TdApi.GetMessage(replyMessage.chatId, replyMessage.messageId)) { response ->
                message = (response as TdApi.Message)
            }
        } catch (e: TelegramException) {
            message = null
        }
        return message
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

    private val notificationHelper = com.xxcactussell.mategram.notifications.NotificationHelper

    private var notificationJob: Job? = null
    private val jobLock = AtomicBoolean(false)
    private val _isObservingNotifications = MutableStateFlow(false)

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
                    val isChannelPost = message.isChannelPost
                    when (val sender = message.senderId) {
                        is TdApi.MessageSenderUser -> {
                            val user = api.getUser(sender.userId)
                            chatPhoto = user.profilePhoto?.small
                            title = if (chat.type is TdApi.ChatTypeBasicGroup || chat.type is TdApi.ChatTypeSupergroup) {
                                chat.title
                            } else {
                                "${chat.unreadCount} новых сообщений"
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
                        notificationHelper.showMessageNotification(
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