package com.xxcactussell.mategram.ui

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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxcactussell.mategram.MainViewModel
import com.xxcactussell.mategram.R

@Composable
fun Login2FAView() {
    val viewModel: MainViewModel = viewModel()

    var passwordLogin by remember { mutableStateOf("") }
    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(
                    modifier = Modifier,
                    text = "Введите пароль 2FA",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = passwordLogin,
                        onValueChange = { passwordLogin = it },
                        label = { Text("Пароль") },
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                    FilledIconButton(
                        modifier = Modifier.size(56.dp),
                        onClick = {
                            viewModel.sendPassword(passwordLogin)
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