package com.xxcactussell.mategram

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxcactussell.mategram.domain.entity.AuthState
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramCredentials
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.loadChatDetails
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.addFileToDownloads
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.cancelDownloadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.downloadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatFolder
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMe
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.Chat
import org.drinkless.tdlib.TdApi.File
import org.drinkless.tdlib.TdApi.MessageSenderUser
import org.drinkless.tdlib.TdApi.StickerFullType
import org.drinkless.tdlib.TdApi.StickerFullTypeCustomEmoji
import org.drinkless.tdlib.TdApi.User
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TelegramRepository
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private val chatUpdatesScope = TelegramRepository.chatUpdatesScope

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

                        else -> { /* ignore other states here */
                        }
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
    val me: StateFlow<TdApi.User?> = _me

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
                repository.chatFoldersUpdateFlow.collect { update ->
                    handleChatFoldersUpdate(update)
                }
            }
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
            launch {
                repository.userStatusUpdateFlow.collect { update ->
                    handleUserOnlineUpdate(update)
                }
            }
            launch {
                repository.userChatActionFlow.collect { update ->
                    handleUserChatActionUpdate(update)
                }
            }

        }
    }

    private suspend fun handleChatPosition(update: TdApi.UpdateChatPosition) {
        val chatId = update.chatId
        Log.d("ChatUpdater", "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private suspend fun handleChatDraft(update: TdApi.UpdateChatDraftMessage) {
        val chatId = update.chatId
        Log.d("ChatUpdater", "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private suspend fun handleChatAddedToList(update: TdApi.UpdateChatAddedToList) {
        val chatId = update.chatId
        Log.d("ChatUpdater", "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private suspend fun handleChatLastMessage(update: TdApi.UpdateChatLastMessage) {
        val chatId = update.chatId
        Log.d("ChatUpdater", "$chatId")
        handleChatUpdate(api.getChat(chatId))
    }

    private fun handleNewChatUpdate(chat: Chat) {
        Log.d("ChatUpdater", "${chat.id}")
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


    private var _chatFolders = MutableStateFlow<List<TdApi.ChatFolder>>(emptyList())
    var chatFolders: StateFlow<List<TdApi.ChatFolder>> = _chatFolders

    private fun handleChatFoldersUpdate(update: TdApi.UpdateChatFolders) {
        chatUpdatesScope.launch {
            val newList: MutableList<TdApi.ChatFolder> = mutableListOf()
            update.chatFolders.forEach { chatFolderInfo ->
                val chatFolder = api.getChatFolder(chatFolderInfo.id)
                newList += chatFolder
                Log.d("CHATFOLDER", "$chatFolder")
            }
            _chatFolders.value = newList
            Log.d("CHATFOLDER", "$newList ${_chatFolders.value}")
        }
    }

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

    fun sendVoiceNote(
        chatId: Long,
        filePath: String,
        replyToMessageId: TdApi.InputMessageReplyTo?
    ) {
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
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
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
            val durationMs =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    ?: 0

            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(filePath)

            // Find the audio track
            val audioTrackIndex = (0 until mediaExtractor.trackCount)
                .find {
                    mediaExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
                        ?.startsWith("audio/") == true
                }
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
            val decoder = MediaCodec.createDecoderByType(
                format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            )
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
                        decoder.queueInputBuffer(
                            inputBufferId,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                    } else {
                        decoder.queueInputBuffer(
                            inputBufferId,
                            0,
                            sampleSize,
                            mediaExtractor.sampleTime,
                            0
                        )
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
                                waveform[currentWaveformIndex] =
                                    (maxAmplitude * 31).toInt().toByte()
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

    fun sendMessage(
        chatId: Long,
        text: String,
        replyToMessageId: TdApi.InputMessageReplyTo? = null
    ) {
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

    suspend fun addFileToDownloads(
        file: TdApi.File,
        chatId: Long,
        messageId: Long,
        priority: Int = 32
    ) {
        api.addFileToDownloads(file.id, chatId, messageId, priority)
    }

    fun installApk(context: Context, apkPath: String) {
        val apkUri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider",
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getMessageById(replyMessage: TdApi.MessageReplyToMessage): TdApi.Message? {
        return try {
            suspendCancellableCoroutine { continuation ->
                api.client?.send(
                    TdApi.GetMessage(
                        replyMessage.chatId,
                        replyMessage.messageId
                    )
                ) { response ->
                    if (response is TdApi.Message) {
                        continuation.resume(response) { }
                    } else {
                        continuation.resume(null) { }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("getMessageById", "Error getting message", e)
            null
        }
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

    private val context = application
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { context.filesDir.resolve("chat_scroll_positions.preferences_pb") }
        )
    }

    private val _userStatuses = MutableStateFlow<Map<Long, ChatInfo>>(emptyMap())
    val userStatuses: StateFlow<Map<Long, ChatInfo>> = _userStatuses.asStateFlow()

    data class ChatInfo(
        val status: TdApi.UserStatus = TdApi.UserStatusEmpty(),
        val action: TdApi.ChatAction? = null
    )


    private fun handleUserOnlineUpdate(update: TdApi.UpdateUserStatus) {
        Log.d("USERSTATUS", "$userStatuses")
        _userStatuses.update { currentMap ->
            val currentInfo = currentMap[update.userId] ?: ChatInfo()
            currentMap + (update.userId to currentInfo.copy(status = update.status))
        }
    }

    suspend fun updateCurrentStatus(userId: Long) {
        val userUpdated = api.getUser(userId)
        Log.d("USERSTATUS", "$userStatuses")
        _userStatuses.update { currentMap ->
            val currentInfo = currentMap[userId] ?: ChatInfo()
            currentMap + (userUpdated.id to currentInfo.copy(status = userUpdated.status))
        }
    }

    private fun handleUserChatActionUpdate(update: TdApi.UpdateChatAction) {
        if (update.senderId is TdApi.MessageSenderUser) {
            val userId = (update.senderId as TdApi.MessageSenderUser).userId
            Log.d("USERSTATUS", "$userStatuses")
            _userStatuses.update { currentMap ->
                val currentInfo = currentMap[userId] ?: ChatInfo()
                currentMap + (userId to currentInfo.copy(action = update.action))
            }
        }
    }

    fun formatLastSeenTime(timestamp: Int): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - timestamp
        return when {
            diff < 60 -> "был(а) только что"
            diff < 3600 -> "был(а) ${diff / 60} мин. назад"
            diff < 86400 -> "был(а) ${diff / 3600} ч. назад"
            else -> "был(а) ${diff / 86400} дн. назад"
        }
    }

    //USER PERMISSIONS

    // In MainViewModel
    data class ChatPermissions(
        val canSendBasicMessages: Boolean = false,
        val canSendAudios: Boolean = false,
        val canSendDocuments: Boolean = false,
        val canSendPhotos: Boolean = false,
        val canSendVideos: Boolean = false,
        val canSendVideoNotes: Boolean = false,
        val canSendVoiceNotes: Boolean = false,
        val canSendPolls: Boolean = false,
        val canSendOtherMessages: Boolean = false,
        val canAddLinkPreviews: Boolean = false,
        val canChangeInfo: Boolean = false,
        val canInviteUsers: Boolean = false,
        val canPinMessages: Boolean = false,
        val canCreateTopics: Boolean = false
    )

    private val _chatPermissions = MutableStateFlow<Map<Long, ChatPermissions>>(emptyMap())
    val chatPermissions: StateFlow<Map<Long, ChatPermissions>> = _chatPermissions.asStateFlow()

    fun updateChatPermissions(chat: TdApi.Chat) {
        viewModelScope.launch {
            try {
                when (val type = chat.type) {
                    is TdApi.ChatTypePrivate -> {
                        api.client?.send(TdApi.GetUser(type.userId)) { result ->
                            val user = result as TdApi.User
                            // For private chats, we need to check if the user is blocked
                            api.client?.send(
                                TdApi.GetBlockedMessageSenders(
                                    TdApi.BlockListMain(),
                                    0,
                                    100
                                )
                            ) { blocked ->
                                val blockedList = (blocked as TdApi.MessageSenders).senders
                                val isBlocked = blockedList.any { sender ->
                                    (sender as? TdApi.MessageSenderUser)?.userId == user.id
                                }

                                val permissions = ChatPermissions(
                                    canSendBasicMessages = !isBlocked && !chat.hasProtectedContent,
                                    canSendAudios = !isBlocked && !chat.hasProtectedContent,
                                    canSendDocuments = !isBlocked && !chat.hasProtectedContent,
                                    canSendPhotos = !isBlocked && !chat.hasProtectedContent,
                                    canSendVideos = !isBlocked && !chat.hasProtectedContent,
                                    canSendVideoNotes = !isBlocked && !chat.hasProtectedContent,
                                    canSendVoiceNotes = !isBlocked && !chat.hasProtectedContent,
                                    canSendPolls = !isBlocked && !chat.hasProtectedContent && user.type !is TdApi.UserTypeBot,
                                    canSendOtherMessages = !isBlocked && !chat.hasProtectedContent,
                                    canAddLinkPreviews = !isBlocked && !chat.hasProtectedContent,
                                    canChangeInfo = false,
                                    canInviteUsers = false,
                                    canPinMessages = false,
                                    canCreateTopics = false
                                )
                                _chatPermissions.update { it + (chat.id to permissions) }
                            }
                        }
                    }

                    is TdApi.ChatTypeBasicGroup -> {
                        api.client?.send(TdApi.GetBasicGroupFullInfo(type.basicGroupId)) { result ->
                            val info = result as TdApi.BasicGroupFullInfo
                            api.client?.send(
                                TdApi.GetChatMember(
                                    chat.id,
                                    TdApi.MessageSenderUser(me.value?.id ?: 0L)
                                )
                            ) { memberResult ->
                                val member = memberResult as TdApi.ChatMember
                                updatePermissionsForMember(chat, member)
                            }
                        }
                    }

                    is TdApi.ChatTypeSupergroup -> {
                        api.client?.send(TdApi.GetSupergroupFullInfo(type.supergroupId)) { result ->
                            val info = result as TdApi.SupergroupFullInfo
                            api.client?.send(
                                TdApi.GetChatMember(
                                    chat.id,
                                    TdApi.MessageSenderUser(me.value?.id ?: 0L)
                                )
                            ) { memberResult ->
                                val member = memberResult as TdApi.ChatMember
                                if (type.isChannel) {
                                    updateChannelPermissions(chat, member, info)
                                } else {
                                    updateSupergroupPermissions(chat, member, info)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Permissions", "Failed to update permissions for chat ${chat.id}", e)
                _chatPermissions.update { it + (chat.id to ChatPermissions()) }
            }
        }
    }

    private fun updatePermissionsForMember(chat: TdApi.Chat, member: TdApi.ChatMember) {
        val permissions = when (member.status) {
            is TdApi.ChatMemberStatusCreator -> {
                ChatPermissions(
                    canSendBasicMessages = true,
                    canSendAudios = true,
                    canSendDocuments = true,
                    canSendPhotos = true,
                    canSendVideos = true,
                    canSendVideoNotes = true,
                    canSendVoiceNotes = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddLinkPreviews = true,
                    canChangeInfo = true,
                    canInviteUsers = true,
                    canPinMessages = true,
                    canCreateTopics = false // Basic groups don't support topics
                )
            }

            is TdApi.ChatMemberStatusAdministrator -> {
                val adminRights = member.status as TdApi.ChatMemberStatusAdministrator
                ChatPermissions(
                    canSendBasicMessages = true,
                    canSendAudios = true,
                    canSendDocuments = true,
                    canSendPhotos = true,
                    canSendVideos = true,
                    canSendVideoNotes = true,
                    canSendVoiceNotes = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddLinkPreviews = true,
                    canChangeInfo = adminRights.rights.canChangeInfo,
                    canInviteUsers = adminRights.rights.canInviteUsers,
                    canPinMessages = adminRights.rights.canPinMessages,
                    canCreateTopics = false // Basic groups don't support topics
                )
            }

            is TdApi.ChatMemberStatusMember -> ChatPermissions(
                canSendBasicMessages = !chat.hasProtectedContent,
                canSendAudios = !chat.hasProtectedContent,
                canSendDocuments = !chat.hasProtectedContent,
                canSendPhotos = !chat.hasProtectedContent,
                canSendVideos = !chat.hasProtectedContent,
                canSendVideoNotes = !chat.hasProtectedContent,
                canSendVoiceNotes = !chat.hasProtectedContent,
                canSendPolls = !chat.hasProtectedContent,
                canSendOtherMessages = !chat.hasProtectedContent,
                canAddLinkPreviews = !chat.hasProtectedContent
            )

            is TdApi.ChatMemberStatusRestricted -> {
                val restrictions = member.status as TdApi.ChatMemberStatusRestricted
                ChatPermissions(
                    canSendBasicMessages = restrictions.permissions.canSendBasicMessages,
                    canSendAudios = restrictions.permissions.canSendAudios,
                    canSendDocuments = restrictions.permissions.canSendDocuments,
                    canSendPhotos = restrictions.permissions.canSendPhotos,
                    canSendVideos = restrictions.permissions.canSendVideos,
                    canSendVideoNotes = restrictions.permissions.canSendVideoNotes,
                    canSendVoiceNotes = restrictions.permissions.canSendVoiceNotes,
                    canSendPolls = restrictions.permissions.canSendPolls,
                    canSendOtherMessages = restrictions.permissions.canSendOtherMessages,
                    canAddLinkPreviews = restrictions.permissions.canAddLinkPreviews
                )
            }

            else -> ChatPermissions()
        }
        _chatPermissions.update { it + (chat.id to permissions) }
    }

    private fun updateChannelPermissions(
        chat: TdApi.Chat,
        member: TdApi.ChatMember,
        info: TdApi.SupergroupFullInfo
    ) {
        val permissions = when (member.status) {
            is TdApi.ChatMemberStatusAdministrator -> {
                val adminRights = member.status as TdApi.ChatMemberStatusAdministrator
                ChatPermissions(
                    canSendBasicMessages = adminRights.rights.canPostMessages,
                    canSendAudios = adminRights.rights.canPostMessages,
                    canSendDocuments = adminRights.rights.canPostMessages,
                    canSendPhotos = adminRights.rights.canPostMessages,
                    canSendVideos = adminRights.rights.canPostMessages,
                    canSendVideoNotes = adminRights.rights.canPostMessages,
                    canSendVoiceNotes = adminRights.rights.canPostMessages,
                    canSendPolls = adminRights.rights.canPostMessages,
                    canSendOtherMessages = adminRights.rights.canPostMessages,
                    canAddLinkPreviews = adminRights.rights.canPostMessages,
                    canChangeInfo = adminRights.rights.canChangeInfo,
                    canInviteUsers = adminRights.rights.canInviteUsers,
                    canPinMessages = adminRights.rights.canPinMessages,
                    canCreateTopics = false // Channels don't support topics
                )
            }

            is TdApi.ChatMemberStatusCreator -> {
                ChatPermissions(
                    canSendBasicMessages = true,
                    canSendAudios = true,
                    canSendDocuments = true,
                    canSendPhotos = true,
                    canSendVideos = true,
                    canSendVideoNotes = true,
                    canSendVoiceNotes = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddLinkPreviews = true,
                    canChangeInfo = true,
                    canInviteUsers = true,
                    canPinMessages = true,
                    canCreateTopics = false // Channels don't support topics
                )
            }

            else -> ChatPermissions()
        }
        _chatPermissions.update { it + (chat.id to permissions) }
    }


    private fun updateSupergroupPermissions(
        chat: TdApi.Chat,
        member: TdApi.ChatMember,
        info: TdApi.SupergroupFullInfo
    ) {
        // Similar to updateForumPermissions but without topic creation
        val permissions = when (member.status) {
            is TdApi.ChatMemberStatusAdministrator -> {
                val adminRights = member.status as TdApi.ChatMemberStatusAdministrator
                ChatPermissions(
                    canSendBasicMessages = true,
                    canSendAudios = true,
                    canSendDocuments = true,
                    canSendPhotos = true,
                    canSendVideos = true,
                    canSendVideoNotes = true,
                    canSendVoiceNotes = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddLinkPreviews = true,
                    canChangeInfo = adminRights.rights.canChangeInfo,
                    canInviteUsers = adminRights.rights.canInviteUsers,
                    canPinMessages = adminRights.rights.canPinMessages,
                    canCreateTopics = false
                )
            }

            is TdApi.ChatMemberStatusCreator -> {
                ChatPermissions(
                    canSendBasicMessages = true,
                    canSendAudios = true,
                    canSendDocuments = true,
                    canSendPhotos = true,
                    canSendVideos = true,
                    canSendVideoNotes = true,
                    canSendVoiceNotes = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddLinkPreviews = true,
                    canChangeInfo = true,
                    canInviteUsers = true,
                    canPinMessages = true,
                    canCreateTopics = false
                )
            }

            is TdApi.ChatMemberStatusMember -> {
                ChatPermissions(
                    canSendBasicMessages = true,
                    canSendAudios = true,
                    canSendDocuments = true,
                    canSendPhotos = true,
                    canSendVideos = true,
                    canSendVideoNotes = true,
                    canSendVoiceNotes = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddLinkPreviews = true,
                    canCreateTopics = false
                )
            }

            is TdApi.ChatMemberStatusRestricted -> {
                val restrictions = member.status as TdApi.ChatMemberStatusRestricted
                ChatPermissions(
                    canSendBasicMessages = restrictions.permissions.canSendBasicMessages,
                    canSendAudios = restrictions.permissions.canSendAudios,
                    canSendDocuments = restrictions.permissions.canSendDocuments,
                    canSendPhotos = restrictions.permissions.canSendPhotos,
                    canSendVideos = restrictions.permissions.canSendVideos,
                    canSendVideoNotes = restrictions.permissions.canSendVideoNotes,
                    canSendVoiceNotes = restrictions.permissions.canSendVoiceNotes,
                    canSendPolls = restrictions.permissions.canSendPolls,
                    canSendOtherMessages = restrictions.permissions.canSendOtherMessages,
                    canAddLinkPreviews = restrictions.permissions.canAddLinkPreviews,
                    canCreateTopics = restrictions.permissions.canCreateTopics
                )
            }

            else -> ChatPermissions()
        }
        _chatPermissions.update { it + (chat.id to permissions) }
    }

    private val _userFullInfo = MutableStateFlow<TdApi.UserFullInfo?>(null)
    val userFullInfo: StateFlow<TdApi.UserFullInfo?> = _userFullInfo

    private val _supergroupFullInfo = MutableStateFlow<TdApi.SupergroupFullInfo?>(null)
    val supergroupFullInfo: StateFlow<TdApi.SupergroupFullInfo?> = _supergroupFullInfo

    private val _basicGroupFullInfo = MutableStateFlow<TdApi.BasicGroupFullInfo?>(null)
    val basicGroupFullInfo: StateFlow<TdApi.BasicGroupFullInfo?> = _basicGroupFullInfo


    private val _userForView = MutableStateFlow<TdApi.User?>(null)
    val userForView: StateFlow<TdApi.User?> = _userForView

    private val _supergroupForView = MutableStateFlow<TdApi.Supergroup?>(null)
    val supergroupForView: StateFlow<TdApi.Supergroup?> = _supergroupForView

    private val _basicGroupForView = MutableStateFlow<TdApi.BasicGroup?>(null)
    val basicGroupForView: StateFlow<TdApi.BasicGroup?> = _basicGroupForView


    private val _membersOfGroup = MutableStateFlow<List<TdApi.User>?>(null)
    val membersOfGroup: StateFlow<List<TdApi.User>?> = _membersOfGroup

    private val _membersOfSuperGroup = MutableStateFlow<List<TdApi.User>?>(null)
    val membersOfSuperGroup: StateFlow<List<TdApi.User>?> = _membersOfSuperGroup

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadMembers(info: TdApi.BasicGroupFullInfo?) {
        viewModelScope.launch {
            val members = mutableListOf<TdApi.User>()
            val memberIds =
                info?.members?.map { it.memberId }?.filterIsInstance<TdApi.MessageSenderUser>()
                    ?: emptyList()

            // Собираем пользователей параллельно
            memberIds.map { memberId ->
                async {
                    suspendCancellableCoroutine<TdApi.User?> { cont ->
                        api.client?.send(TdApi.GetUser(memberId.userId)) { response ->
                            cont.resume(response as? TdApi.User, null)
                        }
                    }
                }
            }.awaitAll().filterNotNull().let { users ->
                members += users
            }

            _membersOfGroup.value = members
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadMembersFromSupergroup(info: TdApi.SupergroupFullInfo?, supergroupId: Long?) {
        viewModelScope.launch {
            val members = mutableListOf<TdApi.User>()
            if (info != null && supergroupId != null && info.canGetMembers) {
                val memberIds = suspendCancellableCoroutine<Array<TdApi.ChatMember>> { cont ->
                    api.client?.send(
                        TdApi.GetSupergroupMembers(
                            supergroupId,
                            null,
                            0,
                            200
                        )
                    ) { result ->
                        if (result is TdApi.ChatMembers) {
                            cont.resume(result.members, null)
                        } else {
                            cont.resume(emptyArray(), null)
                        }
                    }
                }

                for (memberId in memberIds) {
                    if (memberId.memberId is TdApi.MessageSenderUser) {
                        val user = suspendCancellableCoroutine<TdApi.User?> { cont ->
                            api.client?.send(TdApi.GetUser((memberId.memberId as TdApi.MessageSenderUser).userId)) { response ->
                                cont.resume(response as? TdApi.User, null)
                            }
                        }
                        if (user != null) members += user
                    }
                }
            }
            _membersOfSuperGroup.value = members
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            api.client?.send(TdApi.GetUser(userId)) { result ->
                if (result is TdApi.User) {
                    _userForView.value = result
                }
            }
        }
    }

    fun loadSupergroup(supergroupId: Long) {
        viewModelScope.launch {
            api.client?.send(TdApi.GetSupergroup(supergroupId)) { result ->
                if (result is TdApi.Supergroup) {
                    _supergroupForView.value = result
                }
            }
        }
    }

    fun loadBasicGroup(basicGroupId: Long) {
        viewModelScope.launch {
            api.client?.send(TdApi.GetBasicGroup(basicGroupId)) { result ->
                if (result is TdApi.BasicGroup) {
                    _basicGroupForView.value = result
                }
            }
        }
    }

    fun loadUserFullInfo(userId: Long) {
        viewModelScope.launch {
            api.client?.send(TdApi.GetUserFullInfo(userId)) { result ->
                if (result is TdApi.UserFullInfo) {
                    _userFullInfo.value = result
                }
            }
        }
    }

    fun loadSupergroupFullInfo(supergroupId: Long) {
        viewModelScope.launch {
            api.client?.send(TdApi.GetSupergroupFullInfo(supergroupId)) { result ->
                if (result is TdApi.SupergroupFullInfo) {
                    _supergroupFullInfo.value = result
                }
            }
        }
    }

    fun loadBasicGroupFullInfo(basicGroupId: Long) {
        viewModelScope.launch {
            api.client?.send(TdApi.GetBasicGroupFullInfo(basicGroupId)) { result ->
                if (result is TdApi.BasicGroupFullInfo) {
                    _basicGroupFullInfo.value = result
                }
            }
        }
    }

    private val _userProfilePhotos = MutableStateFlow<List<TdApi.ChatPhoto>>(emptyList())
    val userProfilePhotos: StateFlow<List<TdApi.ChatPhoto>> = _userProfilePhotos

    fun loadUserProfilePhotos(userId: Long, offset: Int = 0, limit: Int = 10) {
        api.client?.send(TdApi.GetUserProfilePhotos(userId, offset, limit)) { result ->
            if (result is TdApi.ChatPhotos) {
                _userProfilePhotos.value = result.photos?.toList() ?: emptyList()
            }
        }
    }


    private val _customEmojiMapFlow = MutableStateFlow<Map<Long, String>>(emptyMap())
    val customEmojiMapFlow: StateFlow<Map<Long, String>> = _customEmojiMapFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadCustomEmojis(formattedText: TdApi.FormattedText) {
        viewModelScope.launch {
            val emojiIds = formattedText.entities
                ?.filter { it.type is TdApi.TextEntityTypeCustomEmoji }
                ?.map { (it.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId }
                ?.distinct()
                ?: emptyList()

            if (emojiIds.isEmpty()) return@launch

            val stickers = suspendCancellableCoroutine<TdApi.Stickers?> { cont ->
                api.client?.send(TdApi.GetCustomEmojiStickers(emojiIds.toLongArray())) { result ->
                    cont.resume(result as? TdApi.Stickers, null)
                }
            }

            // Копируем текущую map, чтобы не терять уже загруженные эмодзи
            val currentMap = _customEmojiMapFlow.value.toMutableMap()

            stickers?.stickers?.forEach { sticker ->
                val id = (sticker.fullType as TdApi.StickerFullTypeCustomEmoji).customEmojiId
                if (currentMap.containsKey(id)) return@forEach // Уже есть, пропускаем

                val file = sticker.sticker
                val path = file?.local?.path
                val isDownloaded = file?.local?.isDownloadingCompleted == true

                if (isDownloaded && !path.isNullOrEmpty()) {
                    currentMap[id] = path
                } else if (file != null && file.local?.isDownloadingCompleted == false) {
                    // Запускаем загрузку файла, если он не скачан
                    downloadFile(file)
                }
            }
            _customEmojiMapFlow.value = currentMap
        }
    }
}

fun formatCompactNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> {
            val millions = number / 1_000_000f
            if (millions % 1 == 0f) "${millions.toInt()}M"
            else "%.1fM".format(millions)
        }
        number >= 1000 -> {
            val thousands = number / 1000f
            if (thousands % 1 == 0f) "${thousands.toInt()}K"
            else "%.1fK".format(thousands)
        }
        else -> number.toString()
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