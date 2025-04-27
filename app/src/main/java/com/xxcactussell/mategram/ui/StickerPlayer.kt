package com.xxcactussell.mategram.ui

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun StickerPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            val webView = WebView(ctx)
            webView.apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.mediaPlaybackRequiresUserGesture = false

                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webChromeClient = WebChromeClient()

                // Создаем AssetLoader с доменом, указанным явно
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain("appassets.androidplatform.net")
                    .addPathHandler("/stickers/", object : WebViewAssetLoader.PathHandler {
                        override fun handle(path: String): WebResourceResponse? {
                            // Логирование для отладки
                            Log.d("AssetLoader", "Requested path: $path")
                            val expectedFilename = File(videoPath).name
                            // Используем endsWith для более гибкого сравнения
                            if (path.endsWith(expectedFilename)) {
                                val file = File(videoPath)
                                if (file.exists()) {
                                    val inputStream = FileInputStream(file)
                                    return WebResourceResponse(
                                        "video/webm",
                                        "UTF-8",
                                        inputStream
                                    )
                                } else {
                                    Log.e("AssetLoader", "File does not exist: $videoPath")
                                }
                            }
                            return null
                        }
                    })
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }
                }

                val filename = File(videoPath).name
                // Формируем URL, который будет обработан assetLoader
                val secureUrl = "https://appassets.androidplatform.net/stickers/$filename"

                val html = """
                <html>
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                      body {
                        margin: 0;
                        padding: 0;
                        background: transparent;
                        overflow: hidden;
                      }
                      video {
                        display: block;
                        width: 100%;       /* видео займет всю ширину контейнера */
                        height: auto;      /* высота автоматически подстроится */
                        object-fit: contain; /* покажет весь контент видео без обрезки */
                      }
                    </style>
                  </head>
                  <body>
                    <video autoplay loop muted playsinline name="media">
                      <source src="$secureUrl" type="video/webm">
                    </video>
                  </body>
                </html>

                """.trimIndent()
                loadDataWithBaseURL(
                    "https://appassets.androidplatform.net",
                    html,
                    "text/html",
                    "utf-8",
                    null
                )
            }
            webView
        },
        modifier = modifier,
    )
}
