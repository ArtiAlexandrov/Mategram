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
import androidx.compose.material3.FilledTonalButton
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
fun LoginPhoneView() {
    val viewModel: MainViewModel = viewModel()

    var phoneNumber by remember { mutableStateOf("") }
    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(
                    modifier = Modifier,
                    text = "Введите номер телефона",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Номер телефона") },
                        readOnly = true
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                    FilledIconButton(
                        modifier = Modifier.size(56.dp),
                        onClick = {
                            viewModel.sendPhone(phoneNumber)
                        },
                    ) {
                        Icon(painterResource(R.drawable.baseline_arrow_forward_24), contentDescription = "Extended floating action button.")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "1" }
                    ) {
                        Text("1")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "2" }
                    ) {
                        Text("2")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "3" }
                    ) {
                        Text("3")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "4" }
                    ) {
                        Text("4")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "5" }
                    ) {
                        Text("5")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "6" }
                    ) {
                        Text("6")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "7" }
                    ) {
                        Text("7")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "8" }
                    ) {
                        Text("8")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "9" }
                    ) {
                        Text("9")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber += "0" }
                    ) {
                        Text("0")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton (
                        modifier = Modifier.weight(1f),
                        onClick = { phoneNumber = phoneNumber.dropLast(1) }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_backspace_24),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
}