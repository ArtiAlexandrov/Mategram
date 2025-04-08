package com.xxcactussell.mategram.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xxcactussell.mategram.R

@Composable
fun LoginView(
    onLoginClick: () -> Unit
) {
    Scaffold { paddingValues ->
        Box (modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(
                    modifier = Modifier,
                    text = "Добро пожаловать в Mategram",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                ExtendedFloatingActionButton(
                    onClick = { onLoginClick() },
                    icon = { Icon(painterResource(R.drawable.baseline_arrow_right_24), "Extended floating action button.") },
                    text = { Text(text = "Войти в аккаунт") },
                )
            }
        }
    }
}
