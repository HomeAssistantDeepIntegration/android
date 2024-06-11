package io.homeassistant.deep

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import io.homeassistant.deep.ui.theme.AppTheme

class ConfigActivity : ComponentActivity() {
    private fun saveAndSendSettings(url: String, authToken: String, assistPipeline: String) {
        val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("url", url)
            putString("auth_token", authToken)
            putString("assist_pipeline", assistPipeline)
            apply()
        }

        val dataClient = Wearable.getDataClient(this)
        with(PutDataMapRequest.create("/config")) {
            setUrgent()
            dataMap.putString("url", url)
            dataMap.putString("auth_token", authToken)
            dataMap.putString("assist_pipeline", assistPipeline)
            dataClient.putDataItem(asPutDataRequest()).addOnSuccessListener {
                Log.d("ConfigActivity", "Successfully sent config")
            }.addOnFailureListener {
                Log.d("ConfigActivity", "Failed to send config")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Configure") }, navigationIcon = {
                            IconButton(onClick = {
                                finish()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        })
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
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(value = url,
                                onValueChange = { url = it },
                                label = { Text("URL") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            saveAndSendSettings(url, authToken, assistPipeline)
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    saveAndSendSettings(url, authToken, assistPipeline)
                                    focusManager.clearFocus()
                                }))
                        }
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(value = authToken,
                                onValueChange = { authToken = it },
                                label = { Text("Authentication Token") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            saveAndSendSettings(url, authToken, assistPipeline)
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    saveAndSendSettings(url, authToken, assistPipeline)
                                    focusManager.clearFocus()
                                }))
                        }
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(value = assistPipeline,
                                onValueChange = { assistPipeline = it },
                                label = { Text("Assist Pipeline") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            saveAndSendSettings(url, authToken, assistPipeline)
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    saveAndSendSettings(url, authToken, assistPipeline)
                                    focusManager.clearFocus()
                                }))
                        }
                    }
                }
            }
        }
    }
}
