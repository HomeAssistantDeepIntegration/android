package io.homeassistant.deep

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.homeassistant.deep.ui.theme.HomeAssistantDeepIntegrationTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HomeAssistantDeepIntegrationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("HomeAssistant Deep Integration")
                            }
                        )
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        val context = LocalContext.current
                        val sharedPreferences =
                            context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            ConfigActivity::class.java
                                        )
                                    )
                                }
                            ) {
                                Text("Configure")
                            }
                        }

                        if (sharedPreferences.getString(
                                "url",
                                ""
                            ) != "" && sharedPreferences.getString("auth_token", "") != ""
                        ) {
                            var remoteMediaPlayer by remember { mutableStateOf(false) }
                            SettingsItem(
                                headlineContent = { Text("Create remote media player") },
                                overlineContent = { Text("Create a media player on HomeAssistant") },
                                supportingContent = { Text("This will allow HomeAssistant to control this device's media playback") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null
                                    )
                                },
                                checked = remoteMediaPlayer,
                                onCheckedChange = { remoteMediaPlayer = it }
                            )
                            var localMediaPlayer by remember { mutableStateOf(false) }
                            SettingsItem(
                                headlineContent = { Text("Create local media player") },
                                overlineContent = { Text("Create a media player on this device") },
                                supportingContent = { Text("This will allow this device to control HomeAssistant media playback") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null
                                    )
                                },
                                checked = localMediaPlayer,
                                onCheckedChange = {
                                    localMediaPlayer = it
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
                                }
                            )
                            var localCalendar by remember { mutableStateOf(false) }
                            SettingsItem(
                                headlineContent = { Text("Create local calendar") },
                                overlineContent = { Text("Create a calendar on this device") },
                                supportingContent = { Text("This will allow this device to interact with HomeAssistant calendars") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null
                                    )
                                },
                                checked = localCalendar,
                                onCheckedChange = { localCalendar = it }
                            )

                            if (sharedPreferences.getString("assist_pipeline", "") != "") {
                                ListItem(
                                    headlineContent = { Text("Assist") },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Face,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        startActivity(
                                            Intent(
                                                this@MainActivity,
                                                AssistActivity::class.java
                                            )
                                        )
                                    },
                                    trailingContent = {
                                        FilledIconButton(onClick = {
                                            startActivity(
                                                Intent(
                                                    this@MainActivity,
                                                    AssistActivity::class.java
                                                )
                                            )
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
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
    ListItem(
        headlineContent = headlineContent,
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
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                enabled = enabled
            )
        }
    )
}
