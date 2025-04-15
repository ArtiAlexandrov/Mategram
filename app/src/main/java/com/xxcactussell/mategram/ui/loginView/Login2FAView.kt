package com.xxcactussell.mategram.ui.loginView

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Composable
fun Login2FAView() {
    val viewModel: MainViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    var passwordLogin by remember { mutableStateOf("") }
    Scaffold (snackbarHost = { SnackbarHost(snackbarHostState) })  { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000)) +
                            slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = tween(1000, easing = EaseOutQuint)
                            ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        modifier = Modifier.size(56.dp),
                        painter = painterResource(R.drawable.ic_password),
                        contentDescription = "SIM",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(32.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) +
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(1000, delayMillis = 300, easing = EaseOutQuint)
                            )
                ) {
                    Text(
                        modifier = Modifier,
                        text = "Введите пароль 2FA",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) +
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(1000, delayMillis = 600, easing = EaseOutQuint)
                            )
                ) {
                    Row(
                        modifier = Modifier.widthIn(max = 420.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Card(
                            modifier = Modifier.height(72.dp).weight(1f),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxSize(),
                                value = passwordLogin,
                                onValueChange = { passwordLogin = it },
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                visualTransformation = PasswordVisualTransformation('*'),
                                maxLines = 1,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 36.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        FilledIconButton(
                            modifier = Modifier.size(72.dp),
                            onClick = {
                                api.client?.send(
                                    TdApi.CheckAuthenticationPassword(
                                        passwordLogin
                                    )
                                ) { result ->
                                    when (result) {
                                        is TdApi.Error -> {
                                            Log.d("AUTH", "$result")
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Неверно указан пароль",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_arrow_forward_24),
                                contentDescription = "Extended floating action button."
                            )
                        }
                    }
                }
            }
        }
    }
}