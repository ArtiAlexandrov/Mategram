//
// NOTE: THIS FILE IS AUTO-GENERATED by the "ExtensionsGenerator".kt
// See: https://github.com/tdlibx/td-ktx-generator/
//
package com.xxcactussell.mategram.kotlinx.telegram.extensions

import kotlin.Long
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramFlow
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setChatProfileAccentColor
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setProfileAccentColor
import com.xxcactussell.mategram.kotlinx.telegram.extensions.BaseKtx
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.ProfileAccentColor

/**
 * Interface for access [TdApi.ProfileAccentColor] extension functions. Can be used alongside with
 * other extension interfaces of the package. Must contain [TelegramFlow] instance field to access its
 * functionality
 */
interface ProfileAccentColorKtx : BaseKtx {
  /**
   * Instance of the [TelegramFlow] connecting extensions to the Telegram Client
   */
  override val api: TelegramFlow

  /**
   * Suspend function, which changes accent color and background custom emoji for profile of a
   * supergroup or channel chat. Requires canChangeInfo administrator right.
   *
   * @param chatId Chat identifier.  
   * @param profileBackgroundCustomEmojiId Identifier of a custom emoji to be shown on the chat's
   * profile photo background; 0 if none. Use chatBoostLevelFeatures.canSetProfileBackgroundCustomEmoji
   * to check whether a custom emoji can be set.
   */
  suspend fun ProfileAccentColor.setChat(chatId: Long, profileBackgroundCustomEmojiId: Long) =
      api.setChatProfileAccentColor(chatId, this.id, profileBackgroundCustomEmojiId)

  /**
   * Suspend function, which changes accent color and background custom emoji for profile of the
   * current user; for Telegram Premium users only.
   *
   * @param profileBackgroundCustomEmojiId Identifier of a custom emoji to be shown on the user's
   * profile photo background; 0 if none.
   */
  suspend fun ProfileAccentColor.set(profileBackgroundCustomEmojiId: Long) =
      api.setProfileAccentColor(this.id, profileBackgroundCustomEmojiId)
}
