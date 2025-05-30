//
// NOTE: THIS FILE IS AUTO-GENERATED by the "ExtensionsGenerator".kt
// See: https://github.com/tdlibx/td-ktx-generator/
//
package com.xxcactussell.mategram.kotlinx.telegram.extensions

import kotlin.Boolean
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramFlow
import com.xxcactussell.mategram.kotlinx.telegram.extensions.BaseKtx
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getGiftUpgradePreview
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.sendGift
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.FormattedText
import org.drinkless.tdlib.TdApi.Gift
import org.drinkless.tdlib.TdApi.MessageSender

/**
 * Interface for access [TdApi.Gift] extension functions. Can be used alongside with other extension
 * interfaces of the package. Must contain [TelegramFlow] instance field to access its functionality
 */
interface GiftKtx : BaseKtx {
  /**
   * Instance of the [TelegramFlow] connecting extensions to the Telegram Client
   */
  override val api: TelegramFlow

  /**
   * Suspend function, which returns examples of possible upgraded gifts for a regular gift.
   *
   *
   * @return [TdApi.GiftUpgradePreview] Contains examples of possible upgraded gifts for the given
   * regular gift.
   */
  suspend fun Gift.getUpgradePreview() = api.getGiftUpgradePreview(this.id)

  /**
   * Suspend function, which sends a gift to another user or channel chat. May return an error with
   * a message &quot;STARGIFT_USAGE_LIMITED&quot; if the gift was sold out.
   *
   * @param ownerId Identifier of the user or the channel chat that will receive the gift.  
   * @param text Text to show along with the gift; 0-getOption(&quot;gift_text_length_max&quot;)
   * characters. Only Bold, Italic, Underline, Strikethrough, Spoiler, and CustomEmoji entities are
   * allowed. Must be empty if the receiver enabled paid messages.  
   * @param isPrivate Pass true to show gift text and sender only to the gift receiver; otherwise,
   * everyone will be able to see them.  
   * @param payForUpgrade Pass true to additionally pay for the gift upgrade and allow the receiver
   * to upgrade it for free.
   */
  suspend fun Gift.send(
    ownerId: MessageSender?,
    text: FormattedText?,
    isPrivate: Boolean,
    payForUpgrade: Boolean
  ) = api.sendGift(this.id, ownerId, text, isPrivate, payForUpgrade)
}
