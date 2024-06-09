package io.homeassistant.deep

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.homeassistant.deep.ui.theme.HomeAssistantDeepIntegrationTheme

class ConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HomeAssistantDeepIntegrationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("My Screen") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        )
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val context = LocalContext.current
                    val sharedPreferences =
                        context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

                    var url by remember {
                        mutableStateOf(
                            sharedPreferences.getString("url", "") ?: ""
                        )
                    }
                    var authToken by remember {
                        mutableStateOf(
                            sharedPreferences.getString("auth_token", "") ?: ""
                        )
                    }
                    var assistPipeline by remember {
                        mutableStateOf(
                            sharedPreferences.getString("assist_pipeline", "") ?: ""
                        )
                    }
                    val focusManager = LocalFocusManager.current

                    Column(modifier = Modifier.padding(innerPadding)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text("URL") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            with(sharedPreferences.edit()) {
                                                putString("url", url)
                                                apply()
                                            }
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        with(sharedPreferences.edit()) {
                                            putString("url", url)
                                            apply()
                                        }
                                        focusManager.clearFocus()
                                    }
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = authToken,
                                onValueChange = { authToken = it },
                                label = { Text("Authentication Token") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            with(sharedPreferences.edit()) {
                                                putString("auth_token", authToken)
                                                apply()
                                            }
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        with(sharedPreferences.edit()) {
                                            putString("auth_token", authToken)
                                            apply()
                                        }
                                        focusManager.clearFocus()
                                    }
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = assistPipeline,
                                onValueChange = { assistPipeline = it },
                                label = { Text("Assist Pipeline") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            with(sharedPreferences.edit()) {
                                                putString("assist_pipeline", assistPipeline)
                                                apply()
                                            }
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        with(sharedPreferences.edit()) {
                                            putString("assist_pipeline", assistPipeline)
                                            apply()
                                        }
                                        focusManager.clearFocus()
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
