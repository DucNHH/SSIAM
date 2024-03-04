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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ssiam.ui.theme.SSIAMTheme

class MainActivity : ComponentActivity() {
    val money = mutableLongStateOf(0)
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == "com.example.ssiam.MONEY_UPDATE") {
                money.longValue = intent.getLongExtra("money", 0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        money.longValue = getSharedPreferences("com.example.ssiam", MODE_PRIVATE).getLong("money", 0)
        setContent {
            SSIAMTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Text(text = String.format("%,d", money.longValue), modifier = Modifier.weight(1f))
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
        ContextCompat.registerReceiver(this, receiver, IntentFilter("com.example.ssiam.MONEY_UPDATE"), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}