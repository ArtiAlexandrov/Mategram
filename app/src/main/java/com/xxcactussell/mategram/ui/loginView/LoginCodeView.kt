package com.xxcactussell.mategram.ui.loginView

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R
import com.xxcactussell.mategram.kotlinx.telegram.core.TelegramRepository.api
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Composable
fun LoginCodeView() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var codeAuth by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    Scaffold (snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
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
                        painter = painterResource(R.drawable.ic_baseline_sms),
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
                        modifier = Modifier.width(272.dp),
                        text = "Введите код авторизации",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center
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
                    Row {
                        Card(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                value = "${
                                    try {
                                        codeAuth[0]
                                    } catch (e: IndexOutOfBoundsException) {
                                        ""
                                    }
                                }",
                                onValueChange = { },
                                readOnly = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 1,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 24.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                value = "${
                                    try {
                                        codeAuth[1]
                                    } catch (e: IndexOutOfBoundsException) {
                                        ""
                                    }
                                }",
                                onValueChange = { },
                                readOnly = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 1,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 24.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                value = "${
                                    try {
                                        codeAuth[2]
                                    } catch (e: IndexOutOfBoundsException) {
                                        ""
                                    }
                                }",
                                onValueChange = { },
                                readOnly = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 1,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 24.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                value = "${
                                    try {
                                        codeAuth[3]
                                    } catch (e: IndexOutOfBoundsException) {
                                        ""
                                    }
                                }",
                                onValueChange = { },
                                readOnly = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 1,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 24.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(36.dp)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                value = "${
                                    try {
                                        codeAuth[4]
                                    } catch (e: IndexOutOfBoundsException) {
                                        ""
                                    }
                                }",
                                onValueChange = { },
                                readOnly = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                maxLines = 1,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 24.sp
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 900)) +
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(1000, delayMillis = 900, easing = EaseOutQuint)
                            )
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "1" }
                            ) {
                                Text("1", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "2" }
                            ) {
                                Text("2", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "3" }
                            ) {
                                Text("3", style = MaterialTheme.typography.displaySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "4" }
                            ) {
                                Text("4", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "5" }
                            ) {
                                Text("5", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "6" }
                            ) {
                                Text("6", style = MaterialTheme.typography.displaySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "7" }
                            ) {
                                Text("7", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "8" }
                            ) {
                                Text("8", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "9" }
                            ) {
                                Text("9", style = MaterialTheme.typography.displaySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            FilledIconButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth = codeAuth.dropLast(1) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_backspace_24),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    contentDescription = ""
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                modifier = Modifier.size(80.dp),
                                onClick = { codeAuth += "0" }
                            ) {
                                Text("0", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                modifier = Modifier.size(80.dp),
                                onClick = {
                                    api.client?.send(TdApi.CheckAuthenticationCode(codeAuth)) { result ->
                                        when (result) {
                                            is TdApi.Error -> {
                                                Log.d("AUTH", "$result")
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Неверно указан код авторизации",
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
}