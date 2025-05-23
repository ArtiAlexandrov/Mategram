//
// NOTE: THIS FILE IS AUTO-GENERATED by the "ExtensionsGenerator".kt
// See: https://github.com/tdlibx/td-ktx-generator/
//
package com.xxcactussell.mategram.kotlinx.telegram.extensions

import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramFlow
import com.xxcactussell.mategram.kotlinx.telegram.extensions.BaseKtx
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.addFileToDownloads
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.cancelDownloadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.cancelPreliminaryUploadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.deleteFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.downloadFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.editBotMediaPreview
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getAttachedStickerSets
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getFile
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getFileDownloadedPrefixSize
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getSuggestedFileName
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.readFilePart
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.removeFileFromDownloads
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.reportChatPhoto
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.toggleDownloadIsPaused
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.File
import org.drinkless.tdlib.TdApi.InputStoryContent
import org.drinkless.tdlib.TdApi.ReportReason

/**
 * Interface for access [TdApi.File] extension functions. Can be used alongside with other extension
 * interfaces of the package. Must contain [TelegramFlow] instance field to access its functionality
 */
interface FileKtx : BaseKtx {
  /**
   * Instance of the [TelegramFlow] connecting extensions to the Telegram Client
   */
  override val api: TelegramFlow

  /**
   * Suspend function, which adds a file from a message to the list of file downloads. Download
   * progress and completion of the download will be notified through updateFile updates. If message
   * database is used, the list of file downloads is persistent across application restarts. The
   * downloading is independent of download using downloadFile, i.e. it continues if downloadFile is
   * canceled or is used to download a part of the file.
   *
   * @param chatId Chat identifier of the message with the file.  
   * @param messageId Message identifier.  
   * @param priority Priority of the download (1-32). The higher the priority, the earlier the file
   * will be downloaded. If the priorities of two files are equal, then the last one for which
   * downloadFile/addFileToDownloads was called will be downloaded first.
   *
   * @return [TdApi.File] Represents a file.
   */
  suspend fun File.addToDownloads(
    chatId: Long,
    messageId: Long,
    priority: Int
  ) = api.addFileToDownloads(this.id, chatId, messageId, priority)

  /**
   * Suspend function, which stops the downloading of a file. If a file has already been downloaded,
   * does nothing.
   *
   * @param onlyIfPending Pass true to stop downloading only if it hasn't been started, i.e. request
   * hasn't been sent to server.
   */
  suspend fun File.cancelDownload(onlyIfPending: Boolean) = api.cancelDownloadFile(this.id,
      onlyIfPending)

  /**
   * Suspend function, which stops the preliminary uploading of a file. Supported only for files
   * uploaded by using preliminaryUploadFile.
   */
  suspend fun File.cancelPreliminaryUpload() = api.cancelPreliminaryUploadFile(this.id)

  /**
   * Suspend function, which deletes a file from the TDLib file cache.
   */
  suspend fun File.delete() = api.deleteFile(this.id)

  /**
   * Suspend function, which downloads a file from the cloud. Download progress and completion of
   * the download will be notified through updateFile updates.
   *
   * @param priority Priority of the download (1-32). The higher the priority, the earlier the file
   * will be downloaded. If the priorities of two files are equal, then the last one for which
   * downloadFile/addFileToDownloads was called will be downloaded first.  
   * @param offset The starting position from which the file needs to be downloaded.  
   * @param limit Number of bytes which need to be downloaded starting from the &quot;offset&quot;
   * position before the download will automatically be canceled; use 0 to download without a limit.  
   * @param synchronous Pass true to return response only after the file download has succeeded, has
   * failed, has been canceled, or a new downloadFile request with different offset/limit parameters
   * was sent; pass false to return file state immediately, just after the download has been started.
   *
   * @return [TdApi.File] Represents a file.
   */
  suspend fun File.download(
    priority: Int,
    offset: Long,
    limit: Long,
    synchronous: Boolean
  ) = api.downloadFile(this.id, priority, offset, limit, synchronous)

  /**
   * Suspend function, which replaces media preview in the list of media previews of a bot. Returns
   * the new preview after edit is completed server-side.
   *
   * @param botUserId Identifier of the target bot. The bot must be owned and must have the main Web
   * App.  
   * @param languageCode Language code of the media preview to edit.  
   * @param content Content of the new preview.
   *
   * @return [TdApi.BotMediaPreview] Describes media previews of a bot.
   */
  suspend fun File.editBotMediaPreview(
    botUserId: Long,
    languageCode: String?,
    content: InputStoryContent?
  ) = api.editBotMediaPreview(botUserId, languageCode, this.id, content)

  /**
   * Suspend function, which returns a list of sticker sets attached to a file, including regular,
   * mask, and emoji sticker sets. Currently, only animations, photos, and videos can have attached
   * sticker sets.
   *
   *
   * @return [TdApi.StickerSets] Represents a list of sticker sets.
   */
  suspend fun File.getAttachedStickerSets() = api.getAttachedStickerSets(this.id)

  /**
   * Suspend function, which returns information about a file. This is an offline method.
   *
   *
   * @return [TdApi.File] Represents a file.
   */
  suspend fun File.get() = api.getFile(this.id)

  /**
   * Suspend function, which returns file downloaded prefix size from a given offset, in bytes.
   *
   * @param offset Offset from which downloaded prefix size needs to be calculated.
   *
   * @return [TdApi.FileDownloadedPrefixSize] Contains size of downloaded prefix of a file.
   */
  suspend fun File.getDownloadedPrefixSize(offset: Long) = api.getFileDownloadedPrefixSize(this.id,
      offset)

  /**
   * Suspend function, which returns suggested name for saving a file in a given directory.
   *
   * @param directory Directory in which the file is expected to be saved.
   *
   * @return [TdApi.Text] Contains some text.
   */
  suspend fun File.getSuggestedName(directory: String?) = api.getSuggestedFileName(this.id,
      directory)

  /**
   * Suspend function, which reads a part of a file from the TDLib file cache and returns read
   * bytes. This method is intended to be used only if the application has no direct access to TDLib's
   * file system, because it is usually slower than a direct read from the file.
   *
   * @param offset The offset from which to read the file.  
   * @param count Number of bytes to read. An error will be returned if there are not enough bytes
   * available in the file from the specified position. Pass 0 to read all available data from the
   * specified position.
   *
   * @return [TdApi.FilePart] Contains a part of a file.
   */
  suspend fun File.readPart(offset: Long, count: Long) = api.readFilePart(this.id, offset, count)

  /**
   * Suspend function, which removes a file from the file download list.
   *
   * @param deleteFromCache Pass true to delete the file from the TDLib file cache.
   */
  suspend fun File.removeFromDownloads(deleteFromCache: Boolean) =
      api.removeFileFromDownloads(this.id, deleteFromCache)

  /**
   * Suspend function, which reports a chat photo to the Telegram moderators. A chat photo can be
   * reported only if chat.canBeReported.
   *
   * @param chatId Chat identifier.  
   * @param reason The reason for reporting the chat photo.  
   * @param text Additional report details; 0-1024 characters.
   */
  suspend fun File.reportChatPhoto(
    chatId: Long,
    reason: ReportReason?,
    text: String?
  ) = api.reportChatPhoto(chatId, this.id, reason, text)

  /**
   * Suspend function, which changes pause state of a file in the file download list.
   *
   * @param isPaused Pass true if the download is paused.
   */
  suspend fun File.toggleDownloadIsPaused(isPaused: Boolean) = api.toggleDownloadIsPaused(this.id,
      isPaused)
}
