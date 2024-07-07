package io.homeassistant.deep

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.homeassistant.deep.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            var remoteMediaPlayer by remember {
                mutableStateOf(
                    sharedPreferences.getBoolean(
                        "remote_media_player",
                        false
                    )
                )
            }
            var localMediaPlayer by remember {
                mutableStateOf(
                    sharedPreferences.getBoolean(
                        "local_media_player",
                        false
                    )
                )
            }
            var localCalendar by remember {
                mutableStateOf(
                    sharedPreferences.getBoolean(
                        "local_calendar",
                        false
                    )
                )
            }

            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    "url" -> url = sharedPreferences.getString("url", "")
                    "auth_token" -> authToken = sharedPreferences.getString("auth_token", "")
                    "assist_pipeline" -> assistPipeline =
                        sharedPreferences.getString("assist_pipeline", "")

                    "remote_media_player" -> remoteMediaPlayer = sharedPreferences.getBoolean(
                        "remote_media_player", false
                    )

                    "local_media_player" -> localMediaPlayer = sharedPreferences.getBoolean(
                        "local_media_player", false
                    )

                    "local_calendar" -> localCalendar = sharedPreferences.getBoolean(
                        "local_calendar", false
                    )
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
                Scaffold(
                    topBar = {
                        TopAppBar(title = {
                            Text("HomeAssistant Deep Integration")
                        })
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Button(modifier = Modifier.weight(1f), onClick = {
                                startActivity(
                                    Intent(
                                        this@MainActivity, ConfigActivity::class.java
                                    )
                                )
                            }) {
                                Text("Configure")
                            }
                        }

                        if (url != "" && authToken != "") {
                            ListItem(headlineContent = { Text("Entities") }, leadingContent = {
                                Icon(
                                    Icons.Default.Favorite, contentDescription = null
                                )
                            }, modifier = Modifier.clickable {
                                startActivity(
                                    Intent(
                                        this@MainActivity, EntitiesActivity::class.java
                                    )
                                )
                            }, trailingContent = {
                                FilledIconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity, EntitiesActivity::class.java
                                        )
                                    )
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            })
                            ListItem(headlineContent = { Text("Map") }, leadingContent = {
                                Icon(
                                    Icons.Default.LocationOn, contentDescription = null
                                )
                            }, modifier = Modifier.clickable {
                                startActivity(
                                    Intent(
                                        this@MainActivity, MapActivity::class.java
                                    )
                                )
                            }, trailingContent = {
                                FilledIconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity, MapActivity::class.java
                                        )
                                    )
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            })
                            ListItem(headlineContent = { Text("Todos") }, leadingContent = {
                                Icon(
                                    Icons.Default.CheckCircle, contentDescription = null
                                )
                            }, modifier = Modifier.clickable {
                                startActivity(
                                    Intent(
                                        this@MainActivity, TodosActivity::class.java
                                    )
                                )
                            }, trailingContent = {
                                FilledIconButton(onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity, TodosActivity::class.java
                                        )
                                    )
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            })
                            SettingsItem(headlineContent = { Text("Create remote media player") },
                                overlineContent = { Text("Create a media player on HomeAssistant") },
                                supportingContent = { Text("This will allow HomeAssistant to control this device's media playback") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Favorite, contentDescription = null
                                    )
                                },
                                checked = remoteMediaPlayer,
                                onCheckedChange = {
                                    remoteMediaPlayer = it
                                    with(sharedPreferences.edit()) {
                                        putBoolean("remote_media_player", it)
                                        apply()
                                    }
                                })
                            SettingsItem(headlineContent = { Text("Create local media player") },
                                overlineContent = { Text("Create a media player on this device") },
                                supportingContent = { Text("This will allow this device to control HomeAssistant media playback") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Favorite, contentDescription = null
                                    )
                                },
                                checked = localMediaPlayer,
                                onCheckedChange = {
                                    localMediaPlayer = it
                                    with(sharedPreferences.edit()) {
                                        putBoolean("local_media_player", it)
                                        apply()
                                    }
                                    // Create the service when the user enables the setting
                                    if (it) {
                                        // Start the service
                                        startService(
                                            Intent(
                                                this@MainActivity,
                                                LocalMediaPlayerService::class.java
                                            )
                                        )
                                    } else {
                                        // Stop the service
                                        stopService(
                                            Intent(
                                                this@MainActivity,
                                                LocalMediaPlayerService::class.java
                                            )
                                        )
                                    }
                                })
                            SettingsItem(headlineContent = { Text("Create local calendar") },
                                overlineContent = { Text("Create a calendar on this device") },
                                supportingContent = { Text("This will allow this device to interact with HomeAssistant calendars") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.DateRange, contentDescription = null
                                    )
                                },
                                checked = localCalendar,
                                onCheckedChange = {
                                    localCalendar = it
                                    with(sharedPreferences.edit()) {
                                        putBoolean("local_calendar", it)
                                        apply()
                                    }
                                })
                            if (localCalendar) {
                                ListItem(headlineContent = { Text("Calendar") }, leadingContent = {
                                    Icon(
                                        Icons.Default.DateRange, contentDescription = null
                                    )
                                }, modifier = Modifier.clickable {
                                    startActivity(
                                        Intent(
                                            this@MainActivity, CalendarActivity::class.java
                                        )
                                    )
                                }, trailingContent = {
                                    FilledIconButton(onClick = {
                                        startActivity(
                                            Intent(
                                                this@MainActivity, CalendarActivity::class.java
                                            )
                                        )
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null
                                        )
                                    }
                                })
                            }

                            if (assistPipeline != "") {
                                ListItem(headlineContent = { Text("Assist") }, leadingContent = {
                                    Icon(
                                        Icons.Default.Face, contentDescription = null
                                    )
                                }, modifier = Modifier.clickable {
                                    startActivity(
                                        Intent(
                                            this@MainActivity, AssistActivity::class.java
                                        )
                                    )
                                }, trailingContent = {
                                    FilledIconButton(onClick = {
                                        startActivity(
                                            Intent(
                                                this@MainActivity, AssistActivity::class.java
                                            )
                                        )
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null
                                        )
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable() (() -> Unit)? = null,
    supportingContent: @Composable() (() -> Unit)? = null,
    leadingContent: @Composable() (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,

    enabled: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(headlineContent = headlineContent,
        modifier = modifier.clickable(enabled = enabled, role = Role.Switch) {
            onCheckedChange(!checked)
        },
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        trailingContent = {
            Switch(
                checked = checked, onCheckedChange = { onCheckedChange(it) }, enabled = enabled
            )
        })
}
