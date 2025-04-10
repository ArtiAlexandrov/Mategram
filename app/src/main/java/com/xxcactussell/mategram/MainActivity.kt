package com.xxcactussell.mategram

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.domain.entity.AuthState
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.getAuthorizationState
import com.xxcactussell.mategram.kotlinx.telegram.coroutines.setOption
import com.xxcactussell.mategram.ui.ChatListView
import com.xxcactussell.mategram.ui.FcmManager
import com.xxcactussell.mategram.ui.Login2FAView
import com.xxcactussell.mategram.ui.LoginCodeView
import com.xxcactussell.mategram.ui.LoginPhoneView
import com.xxcactussell.mategram.ui.LoginView
import com.xxcactussell.mategram.ui.theme.MategramTheme
import org.drinkless.tdlib.TdApi


class MainActivity : ComponentActivity() {
    lateinit var fcmManager: FcmManager
        private set
    private var pendingChatId: Long? = null
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TelegramRepository.initialize(this)
        FirebaseApp.initializeApp(this)
        fcmManager = FcmManager.getInstance(this)
        requestNotificationPermissions()
        createNotificationChannel()
        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            MategramTheme {
                val viewModel: MainViewModel = viewModel()
                val authState by viewModel.authState.collectAsState()
                var initialChatId by remember {
                    mutableStateOf(
                        intent?.getLongExtra("chat_id", 0L)?.takeIf { it != 0L }
                    )
                }

                api.attachClient()

                LaunchedEffect(Unit) {
                    try {
                        when (val state = api.getAuthorizationState()) {
                            is TdApi.AuthorizationStateReady -> {
                                viewModel.setAuthState(AuthState.Ready)
                                initialChatId?.let { chatId ->
                                    viewModel.handleNotificationOpen(chatId)
                                    initialChatId = null
                                }
                            }
                            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                                viewModel.setAuthState(AuthState.WaitTdlibParameters)
                            }
                            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                                viewModel.setAuthState(AuthState.WaitPhone)
                            }
                            is TdApi.AuthorizationStateWaitCode -> {
                                viewModel.setAuthState(AuthState.WaitCode)
                            }
                            is TdApi.AuthorizationStateWaitPassword -> {
                                viewModel.setAuthState(AuthState.WaitPassword)
                            }
                            else -> {
                                viewModel.setAuthState(AuthState.NoAuth)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error checking auth state", e)
                        viewModel.setAuthState(AuthState.NoAuth)
                    }
                }

                when (authState) {
                    AuthState.Ready -> {
                        viewModel.setOnline(true)
                        ChatListView(
                            viewModel = viewModel
                        )
                    }
                    else -> {
                        AuthStateContent(authState, viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Important: update the stored intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("from_notification", false) == true) {
            val chatId = intent.getLongExtra("chat_id", 0L)
            if (chatId != 0L) {
                pendingChatId = chatId
                // If already authorized, open chat immediately
                if (viewModel.authState.value == AuthState.Ready) {
                    viewModel.openChat(chatId)
                }
            }
        }
    }

    private fun requestNotificationPermissions() {
        val requiredPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                1
            )
        }
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("telegram_messages", name, importance).apply {
            description = descriptionText
            enableLights(true)
            lightColor = Color.BLUE
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setOnline(false)
    }
}

@Composable
private fun AuthStateContent(
    authState: AuthState,
    viewModel: MainViewModel
) {
    var isOvercome by rememberSaveable { mutableStateOf(false) }

    when (authState) {
        AuthState.WaitCode -> LoginCodeView()
        AuthState.WaitPhone -> {
            if (isOvercome) LoginPhoneView()
            else LoginView { isOvercome = true }
        }
        AuthState.WaitPassword -> Login2FAView()
        AuthState.NoAuth, AuthState.WaitTdlibParameters -> LoadingIndicator()
        AuthState.Initial -> {
            Log.d("AUTHSTATE", authState.toString())
            LoadingIndicator()
            LaunchedEffect(Unit) {
                viewModel.performAuthResult()
            }
        }
        AuthState.Ready -> {
            TdApi.SetOption("online", TdApi.OptionValueBoolean(true))
            ChatListView()
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}