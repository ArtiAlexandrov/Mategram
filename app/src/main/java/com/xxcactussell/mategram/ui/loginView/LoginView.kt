package com.xxcactussell.mategram.ui.loginView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInElastic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutElastic
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xxcactussell.mategram.R

@Composable
fun LoginView(
    onLoginClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold { paddingValues ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Logo animation
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000)) +
                        slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(1000, easing = EaseOutQuint)
                        ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    modifier = Modifier.size(256.dp),
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.weight(1f))
            Column(modifier = Modifier
                    .padding(24.dp).wrapContentSize()) {
                // Content animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) +
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(1000, delayMillis = 300, easing = EaseOutQuint)
                            )
                ) {
                    Text(
                        text = "Добро пожаловать",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 600)) +
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(1000, delayMillis = 600, easing = EaseOutQuint)
                            )
                ) {
                    Text(
                        text = "в Mategram",
                        style = MaterialTheme.typography.displayLarge
                    )
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
                    ExtendedFloatingActionButton(
                        onClick = { onLoginClick() },
                        icon = {
                            Icon(
                                painterResource(R.drawable.baseline_arrow_forward_line_24),
                                contentDescription = "Login button icon"
                            )
                        },
                        text = { Text(text = "Войти в аккаунт") },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    )
                }
            }
        }

    }
}