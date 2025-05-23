//
// NOTE: THIS FILE IS AUTO-GENERATED by the "TdApiKtxGenerator".kt
// See: https://github.com/tdlibx/td-ktx-generator/
//
package com.xxcactussell.mategram.kotlinx.telegram.coroutines

import kotlin.Int
import kotlin.Long
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramFlow
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.Sessions

/**
 * Suspend function, which confirms an unconfirmed session of the current user from another device.
 *
 * @param sessionId Session identifier.
 */
suspend fun TelegramFlow.confirmSession(sessionId: Long) =
    this.sendFunctionLaunch(TdApi.ConfirmSession(sessionId))

/**
 * Suspend function, which returns all active sessions of the current user.
 *
 * @return [Sessions] Contains a list of sessions.
 */
suspend fun TelegramFlow.getActiveSessions(): Sessions =
    this.sendFunctionAsync(TdApi.GetActiveSessions())

/**
 * Suspend function, which changes the period of inactivity after which sessions will automatically
 * be terminated.
 *
 * @param inactiveSessionTtlDays New number of days of inactivity before sessions will be
 * automatically terminated; 1-366 days.
 */
suspend fun TelegramFlow.setInactiveSessionTtl(inactiveSessionTtlDays: Int) =
    this.sendFunctionLaunch(TdApi.SetInactiveSessionTtl(inactiveSessionTtlDays))

/**
 * Suspend function, which terminates all other sessions of the current user.
 */
suspend fun TelegramFlow.terminateAllOtherSessions() =
    this.sendFunctionLaunch(TdApi.TerminateAllOtherSessions())

/**
 * Suspend function, which terminates a session of the current user.
 *
 * @param sessionId Session identifier.
 */
suspend fun TelegramFlow.terminateSession(sessionId: Long) =
    this.sendFunctionLaunch(TdApi.TerminateSession(sessionId))
