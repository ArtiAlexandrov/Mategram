package com.xxcactussell.mategram.ui.chat

import android.annotation.SuppressLint
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import kotlin.math.abs

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatImageViewer(
    chatId: Long,
    messageId: Long,
    window: Window,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
        offset += offsetChange
    }
    val dismissThreshold = 400f
    var message by remember { mutableStateOf<TdApi.Message?>(null) }
    val backgroundAlpha by remember(offset.y) {
        derivedStateOf {
            (1 - (abs(offset.y) / dismissThreshold)).coerceIn(0f, 1f) * 0.9f
        }
    }
    LaunchedEffect(messageId) {
        message = api.getMessage(chatId, messageId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.graphicsLayer {
                    alpha = backgroundAlpha
                },
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painterResource(R.drawable.baseline_close_24),
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) {
        BackHandler(enabled = true) {
            onDismiss()
        }
//        val systemBars =
//            WindowInsetsCompat.toWindowInsetsCompat(window.decorView.rootWindowInsets)
//        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
//            false
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
        ) {
            message?.let { msg ->
                when (msg.content) {
                    is TdApi.MessagePhoto -> {
                        val photo = (msg.content as TdApi.MessagePhoto)
                            .photo?.sizes?.maxByOrNull { it.width * it.height }?.photo
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

                        SubcomposeAsyncImage(
                            model = photoPath,
                            contentDescription = "Full size photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale == 1f) {
                                                scale = 2f
                                            } else {
                                                scale = 1f
                                                offset = Offset.Zero
                                            }
                                        }
                                    )
                                }
                                .transformable(state = state)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = offset.y
                                    translationX = offset.x
                                    if (scale <= 1f && abs(offset.y) > dismissThreshold) {
                                        onDismiss()
                                    }
                                },
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        )
                    }

                    is TdApi.MessageVideo -> {
                        val video = (msg.content as TdApi.MessageVideo).video
                        var isPlaying by remember { mutableStateOf(false) }
                        var isControlsVisible by remember { mutableStateOf(true) }
                        val controlsTimer = remember { mutableStateOf<Job?>(null) }
                        val scope = rememberCoroutineScope()
                        val context = LocalContext.current
                        val downloadedSize =
                            downloadedFiles[video.video.id]?.local?.downloadedSize ?: 0L
                        val downloadProgress =
                            downloadedSize.toFloat() / video.video.expectedSize.toFloat()

                        val exoPlayer = remember {
                            ExoPlayer.Builder(context).build().apply {
                                playWhenReady = false
                                repeatMode = Player.REPEAT_MODE_ONE
                                addListener(object : Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                    }
                                })
                            }
                        }

                        val hideControlsAfterDelay = {
                            controlsTimer.value?.cancel()
                            controlsTimer.value = scope.launch {
                                delay(3000)
                                isControlsVisible = false
                            }
                        }

                        // Handle initial setup and download
                        LaunchedEffect(video) {
                            if (!video.video.local.isDownloadingCompleted) {
                                viewModel.addFileToDownloads(video.video, chatId, messageId)
                            } else {
                                exoPlayer.setMediaItem(MediaItem.fromUri(video.video.local.path))
                                exoPlayer.prepare()
                            }
                        }

                        // Handle downloaded file updates
                        LaunchedEffect(downloadedFiles.values) {
                            val downloadedFile = downloadedFiles[video.video.id]
                            if (downloadedFile?.local?.isDownloadingCompleted == true) {
                                exoPlayer.setMediaItem(MediaItem.fromUri(downloadedFile.local.path))
                                exoPlayer.prepare()
                            }
                        }

                        // Handle play/pause state
                        LaunchedEffect(isPlaying) {
                            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                        }


                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            isControlsVisible = !isControlsVisible
                                            if (isControlsVisible) {
                                                hideControlsAfterDelay()
                                            }
                                        }
                                    )
                                }
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setBackgroundColor(Color.Transparent.toArgb())
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (!video.video.local.isDownloadingCompleted &&
                                downloadedFiles[video.video.id]?.local?.isDownloadingCompleted != true
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.Center),
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.3f),
                                    )
                                }
                            } else {
                                VideoControls(
                                    exoPlayer = exoPlayer,
                                    isPlaying = isPlaying,
                                    isControlsVisible = isControlsVisible,
                                    onPlayPauseClick = {
                                        isPlaying = !isPlaying
                                        if (isControlsVisible) {
                                            hideControlsAfterDelay()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                controlsTimer.value?.cancel()
                                exoPlayer.release()
                            }
                        }
                    }
                }
            }
        }
    }

}

@SuppressLint("DefaultLocale")
private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun VideoControls(
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    isControlsVisible: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isControlsVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // Play/Pause button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.baseline_pause_24
                        else R.drawable.baseline_play_arrow_24
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Progress bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val duration = exoPlayer.duration.coerceAtLeast(0)
                val position = exoPlayer.currentPosition.coerceAtLeast(0)

                Slider(
                    value = position.toFloat(),
                    onValueChange = { exoPlayer.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(position),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatDuration(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}