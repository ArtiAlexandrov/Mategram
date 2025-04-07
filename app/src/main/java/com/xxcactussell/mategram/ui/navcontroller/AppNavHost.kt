package com.xxcactussell.mategram.ui.navcontroller

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.xxcactussell.mategram.ui.ChatListView
import com.xxcactussell.mategram.ui.chat.ChatView

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "chat_list"
    ) {
        // Экран списка чатов
        composable("chat_list") {
            ChatListView(navController = navController)
        }

        // Экран деталей чата с передачей chatId в аргументах
        composable(
            route = "chat_detail/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: return@composable
            ChatView(chatId = chatId, navController = navController)
        }
    }
}