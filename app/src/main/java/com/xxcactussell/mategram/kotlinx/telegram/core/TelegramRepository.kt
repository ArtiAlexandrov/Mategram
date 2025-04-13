package com.xxcactussell.mategram.kotlinx.telegram.core

import android.annotation.SuppressLint
import android.content.Context
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.checkAuthenticationCode
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.checkAuthenticationPassword
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatFolder
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChatHistory
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChats
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.logOut
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setAuthenticationPhoneNumber
import com.xxcactussell.mategram.kotlinx.telegram.flows.newMessageFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.telegram.extensions.ChatKtx
import com.xxcactussell.mategram.kotlinx.telegram.extensions.UserKtx
import com.xxcactussell.mategram.kotlinx.telegram.flows.chatAddedToListFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.chatDraftMessageFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.chatLastMessageFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.chatPositionFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.chatReadInboxFlow
import com.xxcactussell.mategram.kotlinx.telegram.flows.newChatFlow
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.telegram.flows.fileFlow
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramRepository : UserKtx, ChatKtx {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    val chatUpdatesScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val api: TelegramFlow = TelegramFlow()

    var appContext: Context? = null
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
        requireNotNull(appContext) {
            "appContext не должен быть null! Убедитесь, что вызывается initialize() перед использованием TelegramRepository."
        }
    }

    private val _authStateFlow = MutableSharedFlow<TdApi.AuthorizationState>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val authStateFlow = _authStateFlow.asSharedFlow()

    // Запускаем подключение клиента TDLib.
    fun checkAuthState() {
        api.attachClient()

    }

    fun sendPhone(phone: String) {
        coroutineScope.launch {
            println("Отправляем номер телефона: $phone")
            api.setAuthenticationPhoneNumber(phone, null)
        }
    }

    fun sendCode(code: String) {
        coroutineScope.launch {
            println("Отправляем код подтверждения: $code")
            api.checkAuthenticationCode(code)
        }
    }

    fun sendPassword(password: String) {
        coroutineScope.launch {
            println("Отправляем пароль: $password")
            api.checkAuthenticationPassword(password)
        }
    }

    fun logOut() {
        coroutineScope.launch {
            println("Выходим из аккаунта...")
            api.logOut()
        }
    }


    val messageFlow: Flow<Message> = api.newMessageFlow()

    val chatFoldersFlow: Flow<TdApi.UpdateChatPosition> = api.chatPositionFlow()

    // Загрузка идентификаторов чатов с сервера
    suspend fun loadChatIds(limit: Int = Int.MAX_VALUE): List<Long> {
        return api.getChats(null, limit).chatIds.toList()
    }

    // Загрузка деталей чата
    suspend fun loadChatDetails(chatId: Long): TdApi.Chat {
        return api.getChat(chatId)
    }

    suspend fun getFile(fileId: Int): TdApi.File {
        return api.getFile(fileId)
    }

    suspend fun loadChatFolder(chatFolderId: Int): TdApi.ChatFolder {
        return api.getChatFolder(chatFolderId)
    }

    suspend fun getMessagesForChat(chatId: Long, fromMessage: Long): TdApi.Messages {
        val result: TdApi.Messages = api.getChatHistory(chatId, fromMessage, 0, 50, false)
        return result
    }


    // Собираем события для loadchats

    val chatPositionUpdate: Flow<TdApi.UpdateChatPosition> = api.chatPositionFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )

    val newChatFlowUpdate: Flow<TdApi.Chat> = api.newChatFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )
    val chatLastMessageUpdate: Flow<TdApi.UpdateChatLastMessage> = api.chatLastMessageFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )
    val chatAddedToList: Flow<TdApi.UpdateChatAddedToList> = api.chatAddedToListFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )

    val chatReadInbox: Flow<TdApi.UpdateChatReadInbox> = api.chatReadInboxFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )

    val chatDraftUpdate: Flow<TdApi.UpdateChatDraftMessage> = api.chatDraftMessageFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )

    val fileUpdateFLow: Flow<TdApi.UpdateFile> = api.fileFlow().shareIn(
        scope = chatUpdatesScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 5
    )
}

suspend fun isUserContact(userId: Long): Boolean {
    val result = api.getUser(userId)
    return (result as? TdApi.User)?.isContact ?: false
}

fun convertUnixTimestampToDate(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

fun convertUnixTimestampToDateByDay(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return format.format(date)
}

fun converUnixTimeStampForChatList(timestamp: Long): String {
    val dateTime = Date(timestamp * 1000)
    val date = timestamp / 86400
    var format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    if (date == getDayFromDate(System.currentTimeMillis() / 1000)) {
        format = SimpleDateFormat("HH:mm", Locale.getDefault())
    } else if (date < getDayFromDate(System.currentTimeMillis() / 1000)) {
        format = SimpleDateFormat("dd.MM", Locale.getDefault())
    }
    return format.format(dateTime)
}

fun getDayFromDate(date: Long): Long {
    return date / 86400
}

@SuppressLint("DefaultLocale")
fun formatFileSize(sizeInBytes: Int): String {
    val size = sizeInBytes.toDouble()
    val units = listOf("Б", "КБ", "МБ", "ГБ", "ТБ")

    var convertedSize = size
    var unitIndex = 0

    while (convertedSize >= 1024 && unitIndex < units.lastIndex) {
        convertedSize /= 1024
        unitIndex++
    }

    return String.format("%.2f %s", convertedSize, units[unitIndex])
}

