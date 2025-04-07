package com.xxcactussell.mategram.kotlinx.telegram.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import org.drinkless.tdlib.TdApi


class ResultHandlerSharedFlow(
    private val _sharedFlow: MutableSharedFlow<TdApi.Object?> = MutableSharedFlow(replay = 5, extraBufferCapacity = 64)
) : TelegramFlow.ResultHandlerFlow, Flow<TdApi.Object> by _sharedFlow.filterNotNull() {
    val sharedFlow = _sharedFlow.asSharedFlow()
    override fun onResult(result: TdApi.Object?) {
        println("ResultHandler: получено обновление TDLib: $result") // 👀 Логируем входящее обновление

        result?.let {
            _sharedFlow.tryEmit(it) // Безопасная отправка в поток
        } ?: println("Ошибка: result = null") // 👀 Проверяем, если result = null
    }
}