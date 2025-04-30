package com.xxcactussell.mategram.ui

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun StickerPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                isOpaque = false

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    // Локальная ссылка на плеер
                    var player: Vp9Player? = null

                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        // Создаем Surface из SurfaceTexture для передачи в нативный плеер
                        val surface = Surface(surfaceTexture)
                        player = Vp9Player(videoPath, surface)
                        player?.start()
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        // При изменении размеров можно добавить адаптацию вывода
                    }

                    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                        player?.stop()
                        player?.destroy()
                        player = null
                        // Возвращаем true, если мы самостоятельно освобождаем ресурсы SurfaceTexture
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                        // Здесь можно отреагировать на изменения в SurfaceTexture (необязательно)
                    }
                }
            }
        },
        modifier = modifier
    )
}

