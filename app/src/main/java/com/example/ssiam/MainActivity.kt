package com.example.ssiam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.ssiam.ui.theme.SSIAMTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val money = mutableLongStateOf(0)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == Constant.MONEY_UPDATE) {
                money.longValue = intent.getLongExtra("money", 0)
            }
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        money.longValue =
            getSharedPreferences(Constant.PREF_NAME, MODE_PRIVATE).getLong(Constant.PREF_MONEY, 0)
        val isUpdateAvailable = mutableStateOf(false)
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
                            text = String.format("%,d", money.longValue),
                            modifier = Modifier.weight(1f)
                        )
                        if(isUpdateAvailable.value) {
                            AlertDialog(
                                onDismissRequest = { isUpdateAvailable.value = false },
                                title = { Text("Update available") },
                                text = { Text("Do you want to update the app?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            stopService(Intent(this@MainActivity, MyService::class.java))
                                            AppUpdater.downloadAndInstallApk(this@MainActivity)
                                            isUpdateAvailable.value = false
                                        }
                                    ) {
                                        Text("Update")
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { isUpdateAvailable.value = false }
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
                                val intent = Intent(this@MainActivity, MyService::class.java)
                                if (MyService.isRunning.value) {
                                    stopService(intent)
                                } else {
                                    startService(intent)
                                }
                            }) {
                                Text(if (MyService.isRunning.value) "Stop" else "Start")
                            }
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter("com.example.ssiam.MONEY_UPDATE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        scope.launch {
            if (AppUpdater.checkForUpdates()) {
                isUpdateAvailable.value = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        scope.cancel()
    }

    inner class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            // app moved to foreground
            MyService.delayTime = Constant.FOREGROUND_DELAY // 10 seconds
        }

        override fun onStop(owner: LifecycleOwner) {
            // app moved to background
            MyService.delayTime = Constant.BACKGROUND_DELAY // 6 hours
        }
    }
}