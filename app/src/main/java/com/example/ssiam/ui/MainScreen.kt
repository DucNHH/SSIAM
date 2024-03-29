package com.example.ssiam.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MoneyScreen(moneyViewModel: MoneyViewModel = viewModel()) {
    val moneyUiState by moneyViewModel.uiState.collectAsState()
    Column {
        Text(
            text = String.format("%,d", moneyUiState.money),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(0.dp, 10.dp, 0.dp, 10.dp)
        )
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 10.dp, 0.dp, 10.dp)
        ) {
            Button(onClick = { }) {
                Text("Start")
            }
        }
    }
}

@Composable
fun UpdaterScreen(updaterViewModel: UpdaterViewModel = viewModel()) {
    val updaterUiState by updaterViewModel.uiState.collectAsState()
    if  (updaterUiState.isUpdateAvailable) {
        AlertDialog(
            onDismissRequest = {  },
            title = { Text("Version ${updaterUiState.version} available") },
            text = { Text("Do you want to update the app?") },
            confirmButton = {
                Button(
                    onClick = { updaterViewModel.startUpdate() }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                Button(
                    onClick = { updaterViewModel.ignoreUpdate() }
                ) {
                    Text("Ignore")
                }
            }
        )
    }
}
