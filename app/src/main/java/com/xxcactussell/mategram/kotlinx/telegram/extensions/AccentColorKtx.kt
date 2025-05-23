//
// NOTE: THIS FILE IS AUTO-GENERATED by the "ExtensionsGenerator".kt
// See: https://github.com/tdlibx/td-ktx-generator/
//
package com.xxcactussell.mategram.kotlinx.telegram.extensions

import kotlin.Long
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramFlow
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setAccentColor
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setChatAccentColor
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.AccentColor

/**
 * Interface for access [TdApi.AccentColor] extension functions. Can be used alongside with other
 * extension interfaces of the package. Must contain [TelegramFlow] instance field to access its
 * functionality
 */
interface AccentColorKtx : BaseKtx {
  /**
   * Instance of the [TelegramFlow] connecting extensions to the Telegram Client
   */
  override val api: TelegramFlow

  /**
   * Suspend function, which changes accent color and background custom emoji for the current user;
   * for Telegram Premium users only.
   *
   * @param backgroundCustomEmojiId Identifier of a custom emoji to be shown on the reply header and
   * link preview background; 0 if none.
   */
  suspend fun AccentColor.set(backgroundCustomEmojiId: Long) = api.setAccentColor(this.id,
      backgroundCustomEmojiId)

  /**
   * Suspend function, which changes accent color and background custom emoji of a channel chat.
   * Requires canChangeInfo administrator right.
   *
   * @param chatId Chat identifier.  
   * @param backgroundCustomEmojiId Identifier of a custom emoji to be shown on the reply header and
   * link preview background; 0 if none. Use chatBoostLevelFeatures.canSetBackgroundCustomEmoji to
   * check whether a custom emoji can be set.
   */
  suspend fun AccentColor.setChat(chatId: Long, backgroundCustomEmojiId: Long) =
      api.setChatAccentColor(chatId, this.id, backgroundCustomEmojiId)
}
