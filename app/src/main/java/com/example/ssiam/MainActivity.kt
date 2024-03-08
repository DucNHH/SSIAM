package com.example.ssiam

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ssiam.ui.theme.SSIAMTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appUpdater = AppUpdater(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        setContent {
            SSIAMTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Text(
                            text = String.format(
                                "%,d",
                                MoneyManager.getInstance(this@MainActivity).money.longValue
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (appUpdater.isUpdateAvailable.value) {
                            AlertDialog(
                                onDismissRequest = { appUpdater.isUpdateAvailable.value = false },
                                title = { Text("Update available") },
                                text = { Text("Do you want to update the app?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            stopService(
                                                Intent(
                                                    this@MainActivity,
                                                    MoneyUpdateService::class.java
                                                )
                                            )
                                            appUpdater.downloadApk()
                                            appUpdater.isUpdateAvailable.value = false
                                        }
                                    ) {
                                        Text("Update")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { appUpdater.isUpdateAvailable.value = false }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 0.dp, 0.dp, 10.dp)
                        ) {
                            Button(onClick = {
                                val intent = Intent(this@MainActivity, MoneyUpdateService::class.java)
                                if (MoneyUpdateService.isRunning.value) {
                                    stopService(intent)
                                } else {
                                    startService(intent)
                                }
                            }) {
                                Text(if (MoneyUpdateService.isRunning.value) "Stop" else "Start")
                            }
                        }
                    }
                }
            }
        }
        appUpdater.checkForUpdates()
    }

    inner class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            // app moved to foreground
            MoneyUpdateService.delayTime = Constant.FOREGROUND_DELAY // 10 seconds
        }

        override fun onStop(owner: LifecycleOwner) {
            // app moved to background
            MoneyUpdateService.delayTime = Constant.BACKGROUND_DELAY // 6 hours
        }
    }
}