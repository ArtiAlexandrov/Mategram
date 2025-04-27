package com.xxcactussell.mategram

import org.json.JSONObject
import android.util.Log
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import org.drinkless.tdlib.TdApi

data class ChatState(
    val scrollPosition: Int,
    val lastReadMessageId: Long
)

/**
 * Обновляет поле clientData объекта чата и отправляет обновление в TDLib.
 *
 * @param chat Объект чата, для которого обновляем данные.
 * @param newScrollPosition Новое положение прокрутки (индекс первого видимого элемента).
 * @param lastReadMessageId Идентификатор последнего просмотренного сообщения.
 * @param tdClient TDLib-клиент для отправки запроса SetChatClientData.
 */
fun updateChatClientData(
    chat: TdApi.Chat,
    newScrollPosition: Int,
    lastReadMessageId: Long,
) {
    try {
        val json = if (chat.clientData.isNullOrEmpty()) JSONObject() else JSONObject(chat.clientData)
        json.put("scrollPosition", newScrollPosition)
        json.put("lastReadMessageId", lastReadMessageId)
        val updatedData = json.toString()

        // Обновляем локальное значение clientData (на стороне клиента, если требуется)
        chat.clientData = updatedData

        // Создаем запрос для обновления clientData в TDLib
        val request = TdApi.SetChatClientData().apply {
            chatId = chat.id
            clientData = updatedData
        }
        // Отправляем запрос TDLib для сохранения данных
        api.client?.send(request) { result ->
            when(result) {
                is TdApi.Error -> {
                    Log.e("TDLib", "SetChatClientData failed: ${result.message}")
                }
                else -> {
                    Log.d("TDLib", "SetChatClientData successful")
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Восстанавливает локальное состояние чата из поля clientData.
 *
 * @param chat Объект чата, откуда читаем clientData.
 * @return ChatState с восстановленным положением прокрутки и id последнего прочитанного сообщения,
 *         либо null, если clientData отсутствует.
 */
fun restoreChatState(chat: TdApi.Chat): ChatState? {
    return try {
        if (chat.clientData.isNullOrEmpty()) {
            null
        } else {
            val json = JSONObject(chat.clientData)
            ChatState(
                scrollPosition = json.optInt("scrollPosition", 0),
                lastReadMessageId = json.optLong("lastReadMessageId", 0)
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}