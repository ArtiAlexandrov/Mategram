package com.xxcactussell.mategram.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.drinkless.tdlib.TdApi

@Composable
fun ChatInfoPane(
    chat: TdApi.Chat,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Содержимое информации о чате
    }
}