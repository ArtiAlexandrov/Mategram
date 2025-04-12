package com.xxcactussell.mategram.kotlinx.telegram.core

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Основной класс для взаимодействия с Telegram API через TDLib.
 * Использует **SharedFlow** для обработки событий, что предотвращает потерю данных.
 */
class TelegramFlow @OptIn(ExperimentalCoroutinesApi::class) constructor(
    val resultHandler: ResultHandlerFlow = ResultHandlerSharedFlow()
) : Flow<TdApi.Object> by resultHandler, Closeable {

    interface ResultHandlerFlow : Client.ResultHandler, Flow<TdApi.Object>

    /** Экземпляр Telegram [Client]. `null`, если не прикреплён. */
    var client: Client? = null

    /**
     * Подключение к TDLib.
     * @param existingClient если передан, используется этот клиент. Если `null`, создаётся новый.
     */
    fun attachClient(existingClient: Client? = null) {
        if (client != null) return // клиент уже подключён
        println("Создание нового TDLib клиента...")
        client = existingClient ?: Client.create(resultHandler, null, null)
    }

    /**
     * Получение потока обновлений из TDLib для заданного типа [T].
     */
    inline fun <reified T : TdApi.Object> getUpdatesFlowOfType(): Flow<T> =
        resultHandler.buffer(64).filterIsInstance()

    /**
     * Отправка запроса в TDLib с ожиданием результата.
     * @param function TDLib API вызов.
     */
    suspend inline fun <reified ExpectedResult : TdApi.Object> sendFunctionAsync(
        function: TdApi.Function<ExpectedResult>
    ): ExpectedResult = suspendCoroutine { continuation ->
        val resultHandler: (TdApi.Object) -> Unit = { result ->
            when (result) {
                is ExpectedResult -> continuation.resume(result)
                is TdApi.Error -> continuation.resumeWithException(
                    TelegramException.Error(result.message)
                )
                else -> continuation.resumeWithException(
                    TelegramException.UnexpectedResult(result)
                )
            }
        }
        client?.send(function, resultHandler) { throwable ->
            continuation.resumeWithException(
                TelegramException.Error(throwable?.message ?: "unknown")
            )
        } ?: throw TelegramException.ClientNotAttached
    }

    /**
     * Отправка запроса в TDLib, не ожидая результат.
     * Использует `SharedFlow`, чтобы данные не терялись.
     */
    suspend inline fun <reified ExpectedResult : TdApi.Object> sendFunctionLaunch(
        function: TdApi.Function<ExpectedResult>
    ) {
        sendFunctionAsync(function)
    }

    fun registerDeviceTokenByClient(token: String) {

        client?.send(TdApi.SetOption(
            "notification_group_size_max",
            TdApi.OptionValueInteger(5)
        )) { response ->
            when (response) {
                is TdApi.Ok -> Log.d("FCM", "Successfully set notification_group_size_max")
                is TdApi.Error -> Log.e("FCM", "Failed to set notification_group_size_max: ${response.message}")
            }
        }

        // Максимальное количество групп уведомлений
        client?.send(TdApi.SetOption(
            "notification_group_count_max",
            TdApi.OptionValueInteger(5)
        )) { response ->
            when (response) {
                is TdApi.Ok -> Log.d("FCM", "Successfully set notification_group_count_max")
                is TdApi.Error -> Log.e("FCM", "Failed to set notification_group_count_max: ${response.message}")
            }
        }

        try {
            val deviceToken = TdApi.DeviceTokenFirebaseCloudMessaging(
                token,
                false
            )

            client?.send(TdApi.RegisterDevice(deviceToken, longArrayOf())) { response ->
                when (response) {
                    is TdApi.Ok -> Log.d("FCM", "Device token registered successfully")
                    is TdApi.Error -> Log.e("FCM", "Failed to register device: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error registering device token", e)
        }
    }

    /** Закрытие TDLib-клиента. */
    override fun close() {
        println("TelegramFlow закрыт")
    }
}