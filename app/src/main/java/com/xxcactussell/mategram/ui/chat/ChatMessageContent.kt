package com.xxcactussell.mategram.ui.chat

import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import org.drinkless.tdlib.TdApi.MessageAnimatedEmoji
import org.drinkless.tdlib.TdApi.MessageAnimation
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageText
import org.drinkless.tdlib.TdApi.MessageVideo
import org.drinkless.tdlib.TdApi.MessageAudio
import org.drinkless.tdlib.TdApi.MessageContact
import org.drinkless.tdlib.TdApi.MessageDice
import org.drinkless.tdlib.TdApi.MessageDocument
import org.drinkless.tdlib.TdApi.MessagePoll
import org.drinkless.tdlib.TdApi.MessageSticker
import org.drinkless.tdlib.TdApi.MessageVenue
import org.drinkless.tdlib.TdApi.MessageVideoNote
import org.drinkless.tdlib.TdApi.MessageVoiceNote

data class MessageContent(
    var plainText: String?,
    var textForReply: String?,
    var thumbnail: ByteArray?
    ) {
    operator fun invoke(
        plainText: String?, textForReply: String?, thumbnail: ByteArray?
    ) {
        this.plainText = plainText
        this.textForReply = textForReply
        this.thumbnail = thumbnail
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageContent

        if (plainText != other.plainText) return false
        if (textForReply != other.textForReply) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = plainText?.hashCode() ?: 0
        result = 31 * result + (textForReply?.hashCode() ?: 0)
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}

suspend fun getMessageContent(chatId: Long, messageId: Long, viewModel: MainViewModel) : MessageContent {
    val messageToReply = api.getMessage(chatId, messageId)

    val messageContent = MessageContent(
        null,
        null,
        null
    )

    when (val content = messageToReply.content) {
        is MessageText -> {
            messageContent(
                content.text.text,
                content.text.text,
                null
            )
        }
        is MessageAnimation -> {
            messageContent(
                content.caption.text,
                content.caption.text,
                content.animation.minithumbnail?.data
            )
        }
        is MessagePhoto -> {
            messageContent(
                content.caption.text,
                content.caption.text,
                content.photo.minithumbnail?.data
            )
        }
        is MessageVideo -> {
            messageContent(
                content.caption.text,
                content.caption.text,
                content.video.minithumbnail?.data
            )
        }
        is MessageAudio -> {
            messageContent(
                content.caption.text,
                "🎵 " + content.caption.text,
                null
            )
        }
        is MessageContact -> {
            messageContent(
                null,
                "👤 " + content.contact.firstName + " " + content.contact.lastName,
                null
            )
        }
        is MessageDocument -> {
            messageContent(
                content.caption.text,
                content.document.fileName + "' " + content.caption.text,
                content.document.minithumbnail?.data
            )
        }
        is MessageVideoNote -> {
            messageContent(
                null,
                viewModel.getMyString(R.string.videoNote),
                content.videoNote.minithumbnail?.data
            )
        }
        is MessageVoiceNote -> {
            messageContent(
                null,
                viewModel.getMyString(R.string.voiceNote),
                null
            )
        }
        is MessageSticker -> {
            messageContent(
                null,
                content.sticker.emoji + " " + viewModel.getMyString(R.string.sticker),
                null
            )
        }
        is MessageVenue -> {
            messageContent(
                null,
                content.venue.title,
                null
            )
        }
        is MessagePoll -> {
            messageContent(
                null,
                content.poll.question.text,
                null
            )
        }
        is MessageAnimatedEmoji -> {
            messageContent(
                null,
                (content.animatedEmoji.sticker?.emoji),
                null
            )
        }
        is MessageDice -> {
            messageContent(
                null,
                (content.emoji),
                null
            )
        }
        else -> {
            messageContent(
                viewModel.getMyString(R.string.unsupportedMessage),
                viewModel.getMyString(R.string.unsupportedMessage),
                null
            )
        }
    }
    return messageContent
}



/**
 *             MessagePaidMedia.CONSTRUCTOR,
 *             MessageExpiredPhoto.CONSTRUCTOR,
 *             MessageExpiredVideo.CONSTRUCTOR,
 *             MessageExpiredVideoNote.CONSTRUCTOR,
 *             MessageExpiredVoiceNote.CONSTRUCTOR,
 *             MessageLocation.CONSTRUCTOR,
 *             MessageDice.CONSTRUCTOR,
 *             MessageGame.CONSTRUCTOR,
 *             MessageStory.CONSTRUCTOR,
 *             MessageInvoice.CONSTRUCTOR,
 *             MessageCall.CONSTRUCTOR,
 *             MessageVideoChatScheduled.CONSTRUCTOR,
 *             MessageVideoChatStarted.CONSTRUCTOR,
 *             MessageVideoChatEnded.CONSTRUCTOR,
 *             MessageInviteVideoChatParticipants.CONSTRUCTOR,
 *             MessageBasicGroupChatCreate.CONSTRUCTOR,
 *             MessageSupergroupChatCreate.CONSTRUCTOR,
 *             MessageChatChangeTitle.CONSTRUCTOR,
 *             MessageChatChangePhoto.CONSTRUCTOR,
 *             MessageChatDeletePhoto.CONSTRUCTOR,
 *             MessageChatAddMembers.CONSTRUCTOR,
 *             MessageChatJoinByLink.CONSTRUCTOR,
 *             MessageChatJoinByRequest.CONSTRUCTOR,
 *             MessageChatDeleteMember.CONSTRUCTOR,
 *             MessageChatUpgradeTo.CONSTRUCTOR,
 *             MessageChatUpgradeFrom.CONSTRUCTOR,
 *             MessagePinMessage.CONSTRUCTOR,
 *             MessageScreenshotTaken.CONSTRUCTOR,
 *             MessageChatSetBackground.CONSTRUCTOR,
 *             MessageChatSetTheme.CONSTRUCTOR,
 *             MessageChatSetMessageAutoDeleteTime.CONSTRUCTOR,
 *             MessageChatBoost.CONSTRUCTOR,
 *             MessageForumTopicCreated.CONSTRUCTOR,
 *             MessageForumTopicEdited.CONSTRUCTOR,
 *             MessageForumTopicIsClosedToggled.CONSTRUCTOR,
 *             MessageForumTopicIsHiddenToggled.CONSTRUCTOR,
 *             MessageSuggestProfilePhoto.CONSTRUCTOR,
 *             MessageCustomServiceAction.CONSTRUCTOR,
 *             MessageGameScore.CONSTRUCTOR,
 *             MessagePaymentSuccessful.CONSTRUCTOR,
 *             MessagePaymentSuccessfulBot.CONSTRUCTOR,
 *             MessagePaymentRefunded.CONSTRUCTOR,
 *             MessageGiftedPremium.CONSTRUCTOR,
 *             MessagePremiumGiftCode.CONSTRUCTOR,
 *             MessageGiveawayCreated.CONSTRUCTOR,
 *             MessageGiveaway.CONSTRUCTOR,
 *             MessageGiveawayCompleted.CONSTRUCTOR,
 *             MessageGiveawayWinners.CONSTRUCTOR,
 *             MessageGiftedStars.CONSTRUCTOR,
 *             MessageGiveawayPrizeStars.CONSTRUCTOR,
 *             MessageGift.CONSTRUCTOR,
 *             MessageUpgradedGift.CONSTRUCTOR,
 *             MessageRefundedUpgradedGift.CONSTRUCTOR,
 *             MessageContactRegistered.CONSTRUCTOR,
 *             MessageUsersShared.CONSTRUCTOR,
 *             MessageChatShared.CONSTRUCTOR,
 *             MessageBotWriteAccessAllowed.CONSTRUCTOR,
 *             MessageWebAppDataSent.CONSTRUCTOR,
 *             MessageWebAppDataReceived.CONSTRUCTOR,
 *             MessagePassportDataSent.CONSTRUCTOR,
 *             MessagePassportDataReceived.CONSTRUCTOR,
 *             MessageProximityAlertTriggered.CONSTRUCTOR,
 *             MessageUnsupported.CONSTRUCTOR
 */