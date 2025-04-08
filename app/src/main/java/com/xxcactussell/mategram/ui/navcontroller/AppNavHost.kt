package com.xxcactussell.mategram.ui.navcontroller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.ui.ChatListView
import com.xxcactussell.mategram.ui.chat.ChatDetailPane
import com.xxcactussell.mategram.ui.chat.ChatView