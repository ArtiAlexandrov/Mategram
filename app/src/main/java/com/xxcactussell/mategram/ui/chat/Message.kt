package com.xxcactussell.mategram.ui.chat

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.getMimeType
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.core.formatFileSize
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getChat
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getUser
import com.xxcactussell.mategram.ui.StickerPlayer
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.MessageAnimatedEmoji
import org.drinkless.tdlib.TdApi.MessageAnimation
import org.drinkless.tdlib.TdApi.MessageDice
import org.drinkless.tdlib.TdApi.MessageDocument
import org.drinkless.tdlib.TdApi.MessagePhoto
import org.drinkless.tdlib.TdApi.MessageReplyToMessage
import org.drinkless.tdlib.TdApi.MessageSticker
import org.drinkless.tdlib.TdApi.MessageText
import org.drinkless.tdlib.TdApi.MessageVideo
import org.drinkless.tdlib.TdApi.MessageVoiceNote
import org.drinkless.tdlib.TdApi.Video
import java.io.File
import kotlin.math.roundToInt

@Composable
fun Message() {

}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MessageItem(
    viewModel: MainViewModel,
    idMessageOfVoiceNote: Long?,
    messageId: Long,
    isVoicePlaying: Boolean,
    onMediaClick: (Long) -> Unit,
    chatId: Long,
    onTogglePlay: (Long, MessageVoiceNote, Long) -> Unit,
    item: TdApi.Message,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    val scope = rememberCoroutineScope()
    val customEmojiMap by viewModel.customEmojiMapFlow.collectAsState() // customEmojiId -> path

    when (val content = item.content) {
        is MessageText -> {
            val formattedText = content.text

            LaunchedEffect(formattedText) {
                viewModel.loadCustomEmojis(formattedText)
            }
            // Собираем inlineContent для всех кастомных эмодзи
            val inlineContent = remember(customEmojiMap, formattedText, downloadedFiles) {
                val map = mutableMapOf<String, InlineTextContent>()
                formattedText?.entities?.forEach { entity ->
                    if (entity.type is TdApi.TextEntityTypeCustomEmoji) {
                        val emojiId = (entity.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId
                        val path = customEmojiMap[emojiId]
                        if (path != null) {
                            map[emojiId.toString()] = InlineTextContent(
                                Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.Center)
                            ) {
                                when {
                                    path.endsWith(".tgs") -> {
                                        val tgsJson = viewModel.decompressTgs(path)
                                        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))
                                        LottieAnimation(
                                            composition = composition,
                                            iterations = Int.MAX_VALUE,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webp") -> {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Custom emoji",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webm") -> {
                                        StickerPlayer(
                                            path,
                                            Modifier.size(24.dp)
                                        )
                                    }
                                    else -> {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                map
            }

            Text(
                modifier = Modifier.padding(16.dp),
                text = getAnnotatedString(formattedText),
                style = MaterialTheme.typography.bodyMedium,
                inlineContent = inlineContent
            )
        }
        is MessageVideo -> {
            val videoContent = content.video
            val thumbnailFile = (videoContent as Video).thumbnail?.file
            var thumbnailPath by remember { mutableStateOf<String?>(null) }
            val caption = content.caption
            val formattedText = content.caption

            LaunchedEffect(formattedText) {
                viewModel.loadCustomEmojis(formattedText)
            }
            // Собираем inlineContent для всех кастомных эмодзи
            val inlineContent = remember(customEmojiMap, formattedText) {
                val map = mutableMapOf<String, InlineTextContent>()
                formattedText?.entities?.forEach { entity ->
                    if (entity.type is TdApi.TextEntityTypeCustomEmoji) {
                        val emojiId = (entity.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId
                        val path = customEmojiMap[emojiId]
                        if (path != null) {
                            map[emojiId.toString()] = InlineTextContent(
                                Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.Center)
                            ) {
                                when {
                                    path.endsWith(".tgs") -> {
                                        val tgsJson = viewModel.decompressTgs(path)
                                        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))
                                        LottieAnimation(
                                            composition = composition,
                                            iterations = Int.MAX_VALUE,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webp") -> {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Custom emoji",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webm") -> {
                                        StickerPlayer(
                                            path,
                                            Modifier.size(24.dp)
                                        )
                                    }
                                    else -> {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                map
            }

            LaunchedEffect(thumbnailFile) {
                if (thumbnailFile?.local?.isDownloadingCompleted == false) {
                    viewModel.downloadFile(thumbnailFile)
                } else {
                    thumbnailPath = thumbnailFile?.local?.path
                }
            }

            LaunchedEffect(downloadedFiles.values) {
                val downloadedFile = downloadedFiles[thumbnailFile?.id]
                if (downloadedFile?.local?.isDownloadingCompleted == true) {
                    thumbnailPath = downloadedFile.local?.path
                }
            }


            Column {

                if (caption.text != "" && content.showCaptionAboveMedia) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = getAnnotatedString(formattedText),
                        style = MaterialTheme.typography.bodyMedium,
                        inlineContent = inlineContent
                    )
                }

                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(320.dp)
                ) {
                    AsyncImage(
                        model = thumbnailPath,
                        contentDescription = "Видео превью",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = {
                            onMediaClick(item.id)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_play_arrow_24),
                            contentDescription = "Запустить видео",
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                    }
                }

                if (caption.text != "" && !content.showCaptionAboveMedia) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = getAnnotatedString(formattedText),
                        style = MaterialTheme.typography.bodyMedium,
                        inlineContent = inlineContent
                    )
                }

            }
        }
        is MessageAnimation -> {
            val animationInChat = content.animation.animation
            var animationPath by remember { mutableStateOf<String?>("") }
            val formattedText = content.caption

            LaunchedEffect(formattedText) {
                viewModel.loadCustomEmojis(formattedText)
            }
            // Собираем inlineContent для всех кастомных эмодзи
            val inlineContent = remember(customEmojiMap, formattedText) {
                val map = mutableMapOf<String, InlineTextContent>()
                formattedText?.entities?.forEach { entity ->
                    if (entity.type is TdApi.TextEntityTypeCustomEmoji) {
                        val emojiId = (entity.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId
                        val path = customEmojiMap[emojiId]
                        if (path != null) {
                            map[emojiId.toString()] = InlineTextContent(
                                Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.Center)
                            ) {
                                when {
                                    path.endsWith(".tgs") -> {
                                        val tgsJson = viewModel.decompressTgs(path)
                                        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))
                                        LottieAnimation(
                                            composition = composition,
                                            iterations = Int.MAX_VALUE,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webp") -> {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Custom emoji",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webm") -> {
                                        StickerPlayer(
                                            path,
                                            Modifier.size(24.dp)
                                        )
                                    }
                                    else -> {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                map
            }

            LaunchedEffect(animationInChat) {
                if (animationInChat?.local?.isDownloadingCompleted == false) {
                    viewModel.downloadFile(animationInChat)
                } else {
                    animationPath = animationInChat?.local?.path
                }
            }

            LaunchedEffect(downloadedFiles.values) {
                val downloadedFile = downloadedFiles[animationInChat?.id]
                if (downloadedFile?.local?.isDownloadingCompleted == true) {
                    animationPath = downloadedFile.local?.path
                }
            }

            val caption = content.caption

            Column {
                if (caption.text != "" && content.showCaptionAboveMedia) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = getAnnotatedString(formattedText),
                        style = MaterialTheme.typography.bodyMedium,
                        inlineContent = inlineContent
                    )
                }
                if(content.animation.mimeType == "image/gif") {
                    AsyncImage(
                        model = animationPath,
                        contentDescription = "Анимация в сообщении",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(320.dp)
                            .heightIn(max = 320.dp)
                            .clickable {
                                onMediaClick(item.id)
                            }
                    )
                } else {
                    AnimationPlayer(
                        animationPath = animationPath,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .heightIn(max = 320.dp)
                    )
                }
                if (caption.text != "" && !content.showCaptionAboveMedia) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = getAnnotatedString(formattedText),
                        style = MaterialTheme.typography.bodyMedium,
                        inlineContent = inlineContent
                    )
                }
            }

        }
        is MessagePhoto -> {
            val photoInChat = content.photo?.sizes?.lastOrNull()?.photo
            var photoPath by remember { mutableStateOf<String?>("") }
            val formattedText = content.caption

            LaunchedEffect(formattedText) {
                viewModel.loadCustomEmojis(formattedText)
            }
            // Собираем inlineContent для всех кастомных эмодзи
            val inlineContent = remember(customEmojiMap, formattedText) {
                val map = mutableMapOf<String, InlineTextContent>()
                formattedText?.entities?.forEach { entity ->
                    if (entity.type is TdApi.TextEntityTypeCustomEmoji) {
                        val emojiId = (entity.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId
                        val path = customEmojiMap[emojiId]
                        if (path != null) {
                            map[emojiId.toString()] = InlineTextContent(
                                Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.Center)
                            ) {
                                when {
                                    path.endsWith(".tgs") -> {
                                        val tgsJson = viewModel.decompressTgs(path)
                                        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))
                                        LottieAnimation(
                                            composition = composition,
                                            iterations = Int.MAX_VALUE,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webp") -> {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Custom emoji",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    path.endsWith(".webm") -> {
                                        StickerPlayer(
                                            path,
                                            Modifier.size(24.dp)
                                        )
                                    }
                                    else -> {
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                map
            }

            LaunchedEffect(photoInChat) {
                if (photoInChat?.local?.isDownloadingCompleted == false) {
                    viewModel.downloadFile(photoInChat)
                } else {
                    photoPath = photoInChat?.local?.path
                }
            }

            LaunchedEffect(downloadedFiles.values) {
                val downloadedFile = downloadedFiles[photoInChat?.id]
                if (downloadedFile?.local?.isDownloadingCompleted == true) {
                    photoPath = downloadedFile.local?.path
                }
            }

            val caption = content.caption

            Column {
                if (caption.text != "" && content.showCaptionAboveMedia) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = getAnnotatedString(formattedText),
                        style = MaterialTheme.typography.bodyMedium,
                        inlineContent = inlineContent
                    )
                }
                AsyncImage(
                    model = photoPath,
                    contentDescription = "Фото сообщения",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(320.dp)
                        .heightIn(max = 320.dp)
                        .clickable {
                            onMediaClick(item.id)
                        }
                )
                if (caption.text != "" && !content.showCaptionAboveMedia) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = getAnnotatedString(formattedText),
                        style = MaterialTheme.typography.bodyMedium,
                        inlineContent = inlineContent
                    )
                }
            }
        }
        is MessageDice -> {
            val value = content.value

            fun getStickersFromDiceStickers(diceStickers: TdApi.DiceStickers?): List<TdApi.Sticker> =
                when (diceStickers) {
                    is TdApi.DiceStickersRegular -> listOfNotNull(diceStickers.sticker)
                    is TdApi.DiceStickersSlotMachine -> listOfNotNull(
                        diceStickers.background,
                        diceStickers.lever,
                        diceStickers.leftReel,
                        diceStickers.centerReel,
                        diceStickers.rightReel
                    )
                    else -> emptyList()
                }

            val stickers = remember(value, content.initialState, content.finalState) {
                when {
                    value == 0 && content.initialState != null -> getStickersFromDiceStickers(content.initialState)
                    value != 0 && content.finalState != null -> getStickersFromDiceStickers(content.finalState)
                    else -> emptyList()
                }
            }

            val stickerPaths = remember(stickers) { mutableStateListOf<String?>().apply { repeat(stickers.size) { add(null) } } }

            LaunchedEffect(stickers) {
                stickers.forEachIndexed { idx, sticker ->
                    val localPath = sticker.sticker.local?.path
                    if (sticker.sticker.local?.isDownloadingCompleted == false) {
                        viewModel.downloadFile(sticker.sticker)
                    }
                    stickerPaths[idx] = localPath
                }
            }

            val usedStickerIds = remember(stickers) { stickers.map { it.sticker.id } }
            val usedDownloadedFiles = usedStickerIds.map { downloadedFiles[it] }
            LaunchedEffect(usedDownloadedFiles) {
                stickers.forEachIndexed { idx, sticker ->
                    val downloadedFile = downloadedFiles[sticker.sticker.id]
                    if (downloadedFile?.local?.isDownloadingCompleted == true) {
                        stickerPaths[idx] = downloadedFile.local?.path
                    }
                }
            }

            Box {
                if (stickers.isNotEmpty() && stickerPaths.all { it != null }) {
                    if (stickers.size == 1) {
                        val path = stickerPaths.first()
                        if (path != null && path.endsWith(".tgs")) {
                            val tgsJson = remember(path) { viewModel.decompressTgs(path) }
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.JsonString(tgsJson)
                            )
                            LottieAnimation(
                                composition = composition,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(24.dp))
                            )
                        }
                    } else {
                        Box {
                            stickerPaths.forEach { path ->
                                if (path != null && path.endsWith(".tgs")) {
                                    val tgsJson = remember(path) { viewModel.decompressTgs(path) }
                                    val composition by rememberLottieComposition(
                                        LottieCompositionSpec.JsonString(tgsJson)
                                    )
                                    LottieAnimation(
                                        composition = composition,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp)
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        is MessageDocument -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val document = content.document.document
                val downloadedSize = downloadedFiles[document.id]?.local?.downloadedSize ?: 0L
                val downloadProgress = downloadedSize.toFloat() / document.expectedSize.toFloat()
                val documentThumbnail = content.document.thumbnail?.file
                var documentThumbnailPath by remember { mutableStateOf(documentThumbnail?.local?.path) }
                val documentName = content.document?.fileName.toString()
                val uploadedSize by remember { mutableStateOf<String?>(formatFileSize(document?.expectedSize?.toInt() ?: 0)) }
                var isDownloading by remember { mutableStateOf(false) }
                var isFileDownloaded by remember { mutableStateOf(false) }

                LaunchedEffect(documentThumbnail) {
                    if(documentThumbnail?.local?.isDownloadingCompleted == false) {
                        viewModel.addFileToDownloads(documentThumbnail, chatId, messageId)
                    } else {
                        documentThumbnailPath = documentThumbnail?.local?.path
                    }
                }

                LaunchedEffect(document) {
                    if(document.local.isDownloadingCompleted) {
                        isFileDownloaded = true
                    }
                }

                LaunchedEffect(downloadedFiles.values) {
                    val downloadedFile = downloadedFiles[documentThumbnail?.id]
                    if (downloadedFile?.local?.isDownloadingCompleted == true) {
                        documentThumbnailPath = downloadedFile.local.path
                    }
                }


                LaunchedEffect(downloadedFiles.values) {
                    val downloadedFile = downloadedFiles[document?.id]
                    if (downloadedFile?.local?.isDownloadingCompleted == true) {
                        isFileDownloaded = true
                        isDownloading = false
                    }
                }

                val context = LocalContext.current

                val onDownloadClick: () -> Unit = {
                    scope.launch {
                        if (!isFileDownloaded) {
                            isDownloading = true
                            viewModel.addFileToDownloads(document, chatId, messageId)
                        } else {
                            val mimeType = getMimeType(documentName)
                            val filePath = document.local.path

                            if (viewModel.isApkFile(filePath)) {
                                val canInstall = context.packageManager.canRequestPackageInstalls()
                                if (!canInstall) {
                                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent) // Открываем настройки для разрешения
                                    return@launch
                                }
                                viewModel.installApk(context, filePath)
                            } else {
                                val fileUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    File(filePath)
                                )

                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(fileUri, mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                try {
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "Нет приложения для открытия файла", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }


                // Отображение в зависимости от состояния файла
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onDownloadClick() }, // Клик для загрузки
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isDownloading -> {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton (
                                onClick = {
                                    scope.launch {
                                        viewModel.cancelFileDownload(content.document.document)
                                        isDownloading = false
                                    }
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_close_24),
                                    "Отменить"
                                )
                            }
                        }

                        isFileDownloaded -> {
                            if (documentThumbnailPath != null) {
                                AsyncImage(
                                    model = documentThumbnailPath,
                                    contentDescription = "Документ",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                16.dp
                                            )
                                        )
                                        .clickable { onDownloadClick() }, // Клик для повторной загрузки
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_insert_drive_file_24),
                                    contentDescription = "Документ",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        else -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = "Скачать документ",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = documentName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        if (isDownloading) {
                            Text(
                                text = formatFileSize(downloadedSize.toInt()) + " / ",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (uploadedSize != null) {
                            Text(
                                text = uploadedSize!!,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
        is MessageAnimatedEmoji -> {
            val animatedEmoji = content.animatedEmoji
            val stickerFile = animatedEmoji.sticker?.sticker
            var stickerPath by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(stickerFile) {
                if (stickerFile?.local?.isDownloadingCompleted == false) {
                    viewModel.downloadFile(stickerFile)
                } else {
                    stickerPath = stickerFile?.local?.path
                }
            }

            LaunchedEffect(downloadedFiles.values) {
                val downloadedFile = downloadedFiles[stickerFile?.id]
                if (downloadedFile?.local?.isDownloadingCompleted == true) {
                    stickerPath = downloadedFile.local?.path
                }
            }

            if (stickerPath != null) {
                when (animatedEmoji.sticker?.format) {
                    is TdApi.StickerFormatTgs -> {
                        val tgsJson = viewModel.decompressTgs(stickerPath!!)
                        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))

                        LottieAnimation(
                            composition = composition,
                            iterations = Int.MAX_VALUE,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                    is TdApi.StickerFormatWebp -> {
                        AsyncImage(
                            model = stickerPath,
                            contentDescription = "Стикер",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                    is TdApi.StickerFormatWebm -> {
                        StickerPlayer(
                            stickerPath!!,
                            Modifier
                                .padding(16.dp)
                                .size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        is MessageSticker -> {
            val sticker = content.sticker
            val stickerFile = sticker?.sticker
            var stickerPath by remember { mutableStateOf<String?>(null) }

            LaunchedEffect (stickerFile) {
                if (stickerFile?.local?.isDownloadingCompleted == false) {
                    viewModel.addFileToDownloads(stickerFile, chatId, messageId)
                } else {
                    stickerPath = stickerFile?.local?.path
                }
            }

            LaunchedEffect(downloadedFiles.values) {
                val downloadedFile = downloadedFiles[stickerFile?.id]
                if (downloadedFile?.local?.isDownloadingCompleted == true) {
                    stickerPath = downloadedFile.local?.path
                }
            }

            if (stickerPath != null) {
                when(sticker.format) {
                    is TdApi.StickerFormatWebp -> {
                        AsyncImage(
                            model = stickerPath,
                            contentDescription = "Стикер",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                    is TdApi.StickerFormatWebm -> {
                        StickerPlayer(
                            stickerPath!!,
                            Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                    is TdApi.StickerFormatTgs -> {
                        val tgsJson = viewModel.decompressTgs(stickerPath!!)
                        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))

                        LottieAnimation(
                            composition = composition,
                            iterations = Int.MAX_VALUE,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        is MessageVoiceNote -> {
            var isListened by remember { mutableStateOf(false) }

            LaunchedEffect(isVoicePlaying) {
                isListened = content.isListened
            }
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onTogglePlay(messageId, content, chatId) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (idMessageOfVoiceNote == messageId && isVoicePlaying)
                                R.drawable.baseline_pause_24
                            else
                                R.drawable.baseline_play_arrow_24
                        ),
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVoiceIndicator(isPlaying = idMessageOfVoiceNote == messageId && isVoicePlaying)
                }

                val seconds = content.voiceNote.duration
                Text(
                    text = formatDuration(seconds),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Используем состояние из актуального сообщения
                if (!isListened) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

        }
        else -> {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.unsupportedMessage),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun RepliedMessage(replyTo: TdApi.MessageReplyTo, viewModel: MainViewModel, onClick: () -> Unit) {
    var messageToReply by remember { mutableStateOf<TdApi.Message?>(null) }
    var messageContent by remember { mutableStateOf<MessageContent?>(null) }
    var messageTextToReply by remember { mutableStateOf<String?>(null) }
    var replyTitle by remember { mutableStateOf("") }

    LaunchedEffect(replyTo) {
        if (replyTo is MessageReplyToMessage) {
            try {
                val chatId = replyTo.chatId
                val messageId = replyTo.messageId
                messageToReply = viewModel.getMessageById(replyTo)

                if (messageToReply != null) {
                    // Получаем контент сообщения
                    messageContent = getMessageContent(chatId, messageId, viewModel)
                    messageTextToReply = messageContent?.textForReply

                    // Получаем имя отправителя
                    when (val sender = messageToReply!!.senderId) {
                        is TdApi.MessageSenderChat -> {
                            val chatReply = api.getChat(sender.chatId)
                            replyTitle = chatReply.title
                        }
                        is TdApi.MessageSenderUser -> {
                            val user = api.getUser(sender.userId)
                            replyTitle = "${user.firstName} ${user.lastName}".trim()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RepliedMessage", "Error loading reply message", e)
            }
        }
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(R.drawable.baseline_reply_24),
            contentDescription = "Reply"
        )

        Spacer(modifier = Modifier.width(4.dp))

        if (replyTitle.isNotEmpty()) {
            Text(
                text = replyTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier
                .clip(
                    CircleShape
                )
                .background(MaterialTheme.colorScheme.onSurface)
                .size(4.dp))
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Text(
                text = "Сообщение удалено",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier
                .clip(
                    CircleShape
                )
                .background(MaterialTheme.colorScheme.onSurface)
                .size(4.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }

        messageContent?.thumbnail?.let {
            ByteArrayImage(
                imageData = it,
                contentDescription = "Медиа в ответе",
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = messageTextToReply ?: "Контент недоступен",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class MediaAlbum(val messages: List<TdApi.Message>, val isOutgoing: Boolean, val replyTo: TdApi.MessageReplyTo?, val date: Int, val id: Long)

@Composable
fun DisplayAlbum(
    messages: List<TdApi.Message>,
    onMediaClick: (Long) -> Unit,
    viewModel: MainViewModel,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    if (messages.isEmpty()) return

    val album = MediaAlbum(
        messages = messages,
        isOutgoing = messages[0].isOutgoing,
        replyTo = messages.firstOrNull { it.replyTo != null }?.replyTo,
        date = messages[0].date,
        id = messages[0].id
    )

    MediaCarousel(
        album = album,
        viewModel = viewModel,
        onMediaClick = { message -> onMediaClick(message.id) },
        downloadedFiles = downloadedFiles
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCarousel(
    album: MediaAlbum,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onMediaClick: (TdApi.Message) -> Unit,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    val carouselState = rememberCarouselState { album.messages.size }

    Column(modifier = modifier) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            preferredItemWidth = 200.dp,
            minSmallItemWidth = 20.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(16.dp)
        ) { index ->
            val message = album.messages[index]
            when (message.content) {
                is MessagePhoto -> {
                    Box(
                        modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                    ) {
                        PhotoContent(message = message, viewModel = viewModel, onMediaClick = onMediaClick, downloadedFiles = downloadedFiles)
                    }
                }
                is MessageVideo -> {
                    Box(
                        modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                    ) {
                        VideoContent(message = message, viewModel = viewModel, onMediaClick = onMediaClick, downloadedFiles = downloadedFiles)
                    }
                }
            }
        }
        val customEmojiMap by viewModel.customEmojiMapFlow.collectAsState() // customEmojiId -> path

        // Caption and page indicators
        val formattedText = album.messages.firstNotNullOfOrNull { message ->
            when (val content = message.content) {
                is MessagePhoto -> content.caption.takeIf { !it.text.isNullOrEmpty() }
                is MessageVideo -> content.caption.takeIf { !it.text.isNullOrEmpty() }
                else -> null
            }
        }

        LaunchedEffect(formattedText) {
            if (formattedText != null) {
                viewModel.loadCustomEmojis(formattedText)
            }
        }
        // Собираем inlineContent для всех кастомных эмодзи
        val inlineContent = remember(customEmojiMap, formattedText) {
            val map = mutableMapOf<String, InlineTextContent>()
            formattedText?.entities?.forEach { entity ->
                if (entity.type is TdApi.TextEntityTypeCustomEmoji) {
                    val emojiId = (entity.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId
                    val path = customEmojiMap[emojiId]
                    if (path != null) {
                        map[emojiId.toString()] = InlineTextContent(
                            Placeholder(24.sp, 24.sp, PlaceholderVerticalAlign.Center)
                        ) {
                            when {
                                path.endsWith(".tgs") -> {
                                    val tgsJson = viewModel.decompressTgs(path)
                                    val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(tgsJson))
                                    LottieAnimation(
                                        composition = composition,
                                        iterations = Int.MAX_VALUE,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                path.endsWith(".webp") -> {
                                    AsyncImage(
                                        model = path,
                                        contentDescription = "Custom emoji",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                path.endsWith(".webm") -> {
                                    StickerPlayer(
                                        path,
                                        Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            map
        }



        if (formattedText != null) {
            Text(
                text = getAnnotatedString(formattedText),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                inlineContent = inlineContent
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PhotoContent(
    message: TdApi.Message,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    downloadedFiles: MutableMap<Int, TdApi. File?>,
    onMediaClick: (TdApi.Message) -> Unit
) {
    val photo = (message.content as MessagePhoto).photo.sizes.lastOrNull()?.photo
    var photoPath by remember { mutableStateOf(photo?.local?.path) }

    LaunchedEffect(photo) {
        if (photo?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(photo)
        } else {
            photoPath = photo?.local?.path
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[photo?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            photoPath = downloadedFile.local?.path
        }
    }


    AsyncImage(
        model = photoPath,
        contentDescription = "Фото сообщения",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .height(320.dp)
            .clickable(onClick = { onMediaClick(message) })
    )
}

@Composable
fun VideoContent(
    message: TdApi.Message,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onMediaClick: (TdApi.Message) -> Unit,
    downloadedFiles: MutableMap<Int, TdApi.File?>
) {
    val videoContent = (message.content as MessageVideo).video
    val thumbnailFile = (videoContent as Video).thumbnail?.file
    var thumbnailPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(thumbnailFile) {
        if (thumbnailFile?.local?.isDownloadingCompleted == false) {
            viewModel.downloadFile(thumbnailFile)
        } else {
            thumbnailPath = thumbnailFile?.local?.path
        }
    }

    LaunchedEffect(downloadedFiles.values) {
        val downloadedFile = downloadedFiles[thumbnailFile?.id]
        if (downloadedFile?.local?.isDownloadingCompleted == true) {
            thumbnailPath = downloadedFile.local?.path
        }
    }

    Box(
        modifier = modifier
            .height(320.dp)
    ) {
        AsyncImage(
            model = thumbnailPath,
            contentDescription = "Видео превью",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = { onMediaClick(message) },
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_play_arrow_24),
                contentDescription = "Запустить видео",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun ByteArrayImage(
    imageData: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(imageData) {
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
    }

    Image(
        bitmap = bitmap?.asImageBitmap() ?: ImageBitmap(1, 1),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}


@Composable
fun DraggableBox(
    modifier: Modifier = Modifier,
    onDragComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 20.dp.toPx() }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "dragAnimation"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX <= -swipeThreshold) {
                            onDragComplete()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        // Restrict drag to left only (negative values)
                        offsetX = newOffset.coerceIn(-swipeThreshold, 0f)
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun getAnnotatedString(formattedText: TdApi.FormattedText?): AnnotatedString {
    return buildAnnotatedString {
        val text = formattedText?.text ?: ""
        val entities = formattedText?.entities ?: emptyArray()
        var lastIndex = 0

        for (entity in entities) {
            val start = entity.offset
            val end = start + entity.length

            // Добавляем обычный текст между entity
            if (lastIndex < start) {
                append(text.substring(lastIndex, start))
            }

            val entityText = text.substring(start, end)
            when (val type = entity.type) {
                is TdApi.TextEntityTypeMention,
                is TdApi.TextEntityTypeHashtag,
                is TdApi.TextEntityTypeCashtag,
                is TdApi.TextEntityTypeBotCommand,
                is TdApi.TextEntityTypeEmailAddress,
                is TdApi.TextEntityTypePhoneNumber,
                is TdApi.TextEntityTypeBankCardNumber,
                is TdApi.TextEntityTypeMentionName -> {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeUrl -> {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(entityText)
                    }
                    addLink(
                        url = LinkAnnotation.Url(entityText),
                        start = start,
                        end = end
                    )
                }
                is TdApi.TextEntityTypeTextUrl -> {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(entityText)
                    }
                    addLink(
                        url = LinkAnnotation.Url(type.url),
                        start = start,
                        end = end
                    )
                }
                is TdApi.TextEntityTypeBold -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeItalic -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeUnderline -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeStrikethrough -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeSpoiler -> {
                    withStyle(
                        SpanStyle(
                            background = MaterialTheme.colorScheme.onSurface,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeCode,
                is TdApi.TextEntityTypePre,
                is TdApi.TextEntityTypePreCode -> {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeBlockQuote,
                is TdApi.TextEntityTypeExpandableBlockQuote -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(entityText)
                    }
                }
                is TdApi.TextEntityTypeCustomEmoji -> {
                    appendInlineContent(type.customEmojiId.toString())
                }
                is TdApi.TextEntityTypeMediaTimestamp -> {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.secondary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(entityText)
                    }
                }
                else -> {
                    append(entityText)
                }
            }
            lastIndex = end
        }

        // Добавляем оставшийся текст после последнего entity
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatDuration(seconds: Int): String {
    return when {
        seconds < 3600 -> String.format("%02d:%02d", seconds / 60, seconds % 60)
        else -> String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    }
}

@Composable
fun AnimatedVoiceIndicator(isPlaying: Boolean) {
    if (isPlaying) {
        // При воспроизведении – три полоски, высота которых изменяется случайным образом
        val bar1 = remember { Animatable(10f) }
        val bar2 = remember { Animatable(15f) }
        val bar3 = remember { Animatable(20f) }
        LaunchedEffect(isPlaying) {
            // Параллельные анимации для трёх полосок
            launch {
                while (true) {
                    bar1.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                    )
                }
            }
            launch {
                while (true) {
                    bar2.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 350, easing = LinearEasing)
                    )
                }
            }
            launch {
                while (true) {
                    bar3.animateTo(
                        targetValue = (10..30).random().toFloat(),
                        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(bar1.value.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(bar2.value.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(bar3.value.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
            )
        }
    } else {
        // При паузе – три маленьких круга 4.dp
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }
        }
    }
}
