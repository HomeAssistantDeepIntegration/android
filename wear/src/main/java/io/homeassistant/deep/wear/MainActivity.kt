package io.homeassistant.deep.wear

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.remote.interactions.RemoteActivityHelper
import io.homeassistant.deep.wear.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

            var url by remember {
                mutableStateOf(
                    sharedPreferences.getString(
                        "url", ""
                    )
                )
            }
            var authToken by remember {
                mutableStateOf(
                    sharedPreferences.getString(
                        "auth_token", ""
                    )
                )
            }
            var assistPipeline by remember {
                mutableStateOf(
                    sharedPreferences.getString(
                        "assist_pipeline", ""
                    )
                )
            }

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    "url" -> url = sharedPreferences.getString("url", "")
                    "auth_token" -> authToken = sharedPreferences.getString("auth_token", "")

                    "assist_pipeline" -> assistPipeline =
                        sharedPreferences.getString("assist_pipeline", "")
                }
            }
            DisposableEffect(Unit) {
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(
                        listener
                    )
                }
            }

            AppTheme {
                val listState = rememberScalingLazyListState()
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    autoCentering = AutoCenteringParams(itemIndex = 0),
                    state = listState
                ) {
                    item {
                        Chip(icon = {
                            Icon(
                                Icons.Default.Build, contentDescription = null
                            )
                        },
                            label = { Text("Configure") },
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                // Launch ConfigActivity on connected phone
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setClassName(
                                        "io.homeassistant.deep",
                                        "io.homeassistant.deep.ConfigActivity"
                                    )
                                }

                                // Use RemoteIntentHelper to start the activity on the phone
                                RemoteActivityHelper(this@MainActivity).startRemoteActivity(
                                    intent
                                )
                            })
                    }

                    item {
                        Chip(icon = {
                            Icon(
                                Icons.Default.Face, contentDescription = null
                            )
                        },
                            label = { Text("Assist") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = url != "" && authToken != "" && assistPipeline != "",
                            onClick = {
                                startActivity(
                                    this@MainActivity, Intent(
                                        this@MainActivity, AssistActivity::class.java
                                    ), null
                                )
                            })
                    }
                }
            }
        }
    }
}
