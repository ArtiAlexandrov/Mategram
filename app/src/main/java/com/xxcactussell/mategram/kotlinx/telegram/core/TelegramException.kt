package com.xxcactussell.mategram.kotlinx.telegram.core

import org.drinkless.tdlib.TdApi


sealed class TelegramException(message: String) : Throwable(message) {
    data object ClientNotAttached :
        TelegramException(
            "Client is not attached. Please call TelegramScope.attachClient() " +
                    "before calling a Telegram function"
        ) {
        private fun readResolve(): Any = ClientNotAttached
    }

    class Error(message: String) : TelegramException(message)
    class UnexpectedResult(result: TdApi.Object) : TelegramException("unexpected result: $result")
}