package com.xxcactussell.mategram.kotlinx.telegram.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import org.drinkless.tdlib.TdApi


class ResultHandlerSharedFlow(
    private val _sharedFlow: MutableSharedFlow<TdApi.Object?> = MutableSharedFlow(
        replay = 10,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
) : TelegramFlow.ResultHandlerFlow, Flow<TdApi.Object> by _sharedFlow.filterNotNull() {
    override fun onResult(result: TdApi.Object?) {
        result?.let {
            if (!_sharedFlow.tryEmit(it)) {
                println("Ошибка: Буфер переполнен")
            }
        }
    }
}