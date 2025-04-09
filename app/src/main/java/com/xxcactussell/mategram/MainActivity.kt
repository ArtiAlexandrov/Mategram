package com.xxcactussell.mategram

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.initialize
import com.google.firebase.messaging.FirebaseMessaging
import com.xxcactussell.mategram.TelegramRepository.api
import com.xxcactussell.mategram.domain.entity.AuthState
import com.xxcactussell.mategram.ui.ChatListView
import com.xxcactussell.mategram.ui.FcmManager
import com.xxcactussell.mategram.ui.Login2FAView
import com.xxcactussell.mategram.ui.LoginCodeView
import com.xxcactussell.mategram.ui.LoginPhoneView
import com.xxcactussell.mategram.ui.LoginView
import com.xxcactussell.mategram.ui.NotificationManager.createNotificationChannels
import com.xxcactussell.mategram.ui.chat.ChatView
import com.xxcactussell.mategram.ui.theme.MategramTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi


class MainActivity : ComponentActivity() {
    lateinit var fcmManager: FcmManager
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TelegramRepository.initialize(this)
        TelegramRepository.checkAuthState()
        FirebaseApp.initializeApp(this)
        createNotificationChannels(this)
        fcmManager = FcmManager.getInstance(this)
        val requiredPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                1
            )
        }
        enableEdgeToEdge()
        setContent {
            MategramTheme {
                val viewModel: MainViewModel = viewModel()
                val authState by viewModel.authState.collectAsState()
                println("AUTHSTATE: $authState")
                LaunchedEffect(authState) {
                    println("Состояние в UI изменилось: $authState")
                }
                var isOvercome by rememberSaveable { mutableStateOf(false) }

                when (authState) {
                    AuthState.WaitCode -> {
                        LoginCodeView()
                    }
                    AuthState.WaitPhone -> {
                        if (isOvercome) {
                            LoginPhoneView()
                        } else {
                            LoginView { isOvercome = true }
                        }
                    }
                    AuthState.WaitPassword -> {
                        Login2FAView()
                    }
                    AuthState.NoAuth -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(64.dp)
                                    .align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                    AuthState.Initial -> {
                        Text(text = "Init")
                        viewModel.performAuthResult()
                    }

                    AuthState.WaitTdlibParameters -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(64.dp)
                                    .align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }

                    AuthState.Ready -> {
                        ChatListView()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MategramTheme {
        Greeting("Android")
    }
}