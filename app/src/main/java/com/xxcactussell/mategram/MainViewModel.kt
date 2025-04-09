package com.xxcactussell.mategram

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxcactussell.mategram.domain.entity.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.TelegramRepository.loadChatDetails
import com.xxcactussell.mategram.TelegramRepository.loadChatFolder
import com.xxcactussell.mategram.TelegramRepository.loadChatIds
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.addFileToDownloads
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMe
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.sendMessage
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setTdlibParameters
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.viewMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.FileInputStream


class MainViewModel(application: Application) : AndroidViewModel(application) {



    private val repository = TelegramRepository

    init {
        viewModelScope.launch {
            TelegramRepository.authStateFlow.collect { newState ->
                println("AuthViewModel: получено состояние: $newState")
            }
        }
    }

    val authState: StateFlow<AuthState> = TelegramRepository.authStateFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AuthState.Initial
        )

    // Проверка текущего состояния авторизации
    fun performAuthResult() {
        viewModelScope.launch {
            repository.checkAuthState()
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

    private val _visibleChats = MutableStateFlow<List<TdApi.Chat>>(emptyList())
    val visibleChats: StateFlow<List<TdApi.Chat>> = _visibleChats

    private var _me = MutableStateFlow<TdApi.User?>(null)
    val me : StateFlow<TdApi.User?> = _me

    init {
        observeAuthorizationState()
    }

    private fun observeAuthorizationState() {
        viewModelScope.launch(Dispatchers.IO) {
            authState.collect { state ->
                if (state is AuthState.Ready) {
                    loadFolders()
                    observeChatUpdates()
                    observeNewMessagesFromChat()
                    _me.value = api.getMe()
                }
            }
        }
    }

    private var _chatFolders = MutableStateFlow<List<TdApi.ChatFolder>>(emptyList())
    var chatFolders: StateFlow<List<TdApi.ChatFolder>> = _chatFolders
    private val chatFoldersIds = mutableListOf<Int>()

    fun updateChatsFromNetworkForView(limit: Int = Int.MAX_VALUE) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val chatsIds = loadChatIds(limit)
                val chatsFromNetwork = chatsIds.map { loadChatDetails(it) }
                _visibleChats.value = chatsFromNetwork
            }
        } catch (e: Exception) {
            println("Ошибка загрузки чатов: ${e.message}")
        }
    }

    private fun loadFolders() {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val chatsIds = loadChatIds()
                val chatsFromNetwork = chatsIds.map { loadChatDetails(it) }
                chatsFromNetwork.forEach { chat ->
                    if(chat.chatLists != null) {
                        chat.chatLists.forEach { position ->
                            if (position is TdApi.ChatListFolder) {
                                if(!chatFoldersIds.contains(position.chatFolderId)) {
                                    val folderId = position.chatFolderId
                                    chatFoldersIds.add(folderId)
                                }
                            }
                        }
                    }
                }
                chatFoldersIds.sorted()
                chatFoldersIds.forEach {folderId ->
                    val chatFolder = loadChatFolder(folderId)
                    _chatFolders.value = _chatFolders.value.toMutableList().apply {
                        add(chatFolder)
                    }
                }
            }
        } catch (e: Exception) {
            println("Ошибка загрузки чатов: ${e.message}")
        }
    }

    private fun observeChatUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            TelegramRepository.messageFlow.collect { message ->
                updateChatList(message.chatId)
            }
        }
    }



    private var _messagesFromChat = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val messagesFromChat: StateFlow<List<TdApi.Message>> = _messagesFromChat

    private var chatIdForHandler: Long? = null

    fun setHandlerForChat(chatId: Long?) {
        chatIdForHandler = chatId
    }

    fun getMessagesForChat(chatId: Long, fromMessage: Long = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = TelegramRepository.getMessagesForChat(chatId, fromMessage)

                _messagesFromChat.update { currentMessages ->
                    if (fromMessage == 0L) {
                        response.messages.toList()
                    } else {
                        // Добавляем только новые сообщения
                        val existingIds = currentMessages.map { it.id }.toSet()
                        val uniqueNewMessages = response.messages.filter { it.id !in existingIds }
                        (currentMessages + uniqueNewMessages).distinctBy { it.id }
                    }
                }
            } catch (e: Exception) {
                println("Ошибка загрузки сообщений: ${e.message}")
            }
        }
    }


    private fun observeNewMessagesFromChat() {
        viewModelScope.launch(Dispatchers.IO) {
            TelegramRepository.getNewMessageFlow.collect { message ->
                if (message.chatId == chatIdForHandler) {
                    val currentMessages = _messagesFromChat.value
                    if (currentMessages.none { it.id == message.id }) { // Проверяем, есть ли сообщение
                        val newList = currentMessages.toMutableList().apply {
                            add(0, message) // Добавляем новое сообщение в начало
                        }
                        _messagesFromChat.value = newList
                        println("Добавлено новое сообщение, общее число: ${newList.size}")
                    }
                }
            }
        }
    }



    suspend fun updateChatList(chatId: Long) {
        val updatedChat = loadChatDetails(chatId)
        _visibleChats.value = _visibleChats.value.map { if (it.id == chatId) updatedChat else it }.sortedWith(compareByDescending<TdApi.Chat> { chat ->
            chat.positions?.firstOrNull()?.order ?: 0L // Проверяем positions на null
        }.thenByDescending { chatFilter ->
            chatFilter.id // Если order одинаковый, сортируем по id
        })
    }

    private val _avatarPaths = MutableStateFlow<Map<Long, String?>>(emptyMap())

    private val fileIdToChatId = mutableMapOf<Int, Long>()

    suspend fun getChatAvatarPath(chat: TdApi.Chat, size: String = "s"): String? {
        var avatarFileId: Int? = null
        if (size == "s") {
            avatarFileId = chat.photo?.small?.id
        } else {
            avatarFileId = chat.photo?.big?.id
        }
        val chatId = chat.id
        if (avatarFileId != null) {
            val file = TelegramRepository.getFile(avatarFileId)
            if (file.local.isDownloadingCompleted) {
                updateAvatarPath(chatId, file.local.path)
            } else {
                ensureFileDownload(avatarFileId)
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

    suspend fun getMessageById(replyMessage: TdApi.MessageReplyToMessage): TdApi.Message? {
        return api.getMessage(replyMessage.chatId, replyMessage.messageId)
    }


}

fun getMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}
