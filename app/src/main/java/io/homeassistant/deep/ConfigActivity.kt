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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import io.homeassistant.deep.ui.theme.AppTheme

class ConfigActivity : ComponentActivity() {
    private fun saveAndSendSettings(
        url: String, authToken: String, conversationId: String, deviceId: String?
    ) {
        val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("url", url)
            putString("auth_token", authToken)
            putString("conversation_id", conversationId)
            if (deviceId.isNullOrEmpty()) putString("device_id", null)
            else putString("device_id", deviceId)
            apply()
        }

        val dataClient = Wearable.getDataClient(this)
        with(PutDataMapRequest.create("/config")) {
            setUrgent()
            dataMap.putString("url", url)
            dataMap.putString("auth_token", authToken)
            dataMap.putString("conversation_id", conversationId)
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
                    var conversationId by remember {
                        mutableStateOf(
                            sharedPreferences.getString("conversation_id", "") ?: ""
                        )
                    }
                    var deviceId by remember {
                        mutableStateOf(
                            sharedPreferences.getString("device_id", "") ?: ""
                        )
                    }
                    val focusManager = LocalFocusManager.current

                    Column(modifier = Modifier.padding(innerPadding)) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text("URL") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            saveAndSendSettings(
                                                url, authToken, conversationId, deviceId
                                            )
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    saveAndSendSettings(url, authToken, conversationId, deviceId)
                                    focusManager.clearFocus()
                                })
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = authToken,
                                onValueChange = { authToken = it },
                                label = { Text("Authentication Token") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused) {
                                            saveAndSendSettings(
                                                url, authToken, conversationId, deviceId
                                            )
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    saveAndSendSettings(url, authToken, conversationId, deviceId)
                                    focusManager.clearFocus()
                                })
                            )
                        }

                        if (url.isNotEmpty() && authToken.isNotEmpty()) {
                            val repository by remember { derivedStateOf { HomeAssistantRepository(url, authToken) } }
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                val viewModel: HomeAssistantViewModel = viewModel(
                                    factory = HomeAssistantViewModelFactory(repository)
                                )
                                LaunchedEffect(repository) {
                                    viewModel.repository = repository
                                    viewModel.loadStates()
                                }
                                val states by viewModel.statesLiveData.observeAsState()
                                val conversationEntities = states?.filter { state ->
                                    state.entityId.startsWith("conversation.")
                                }
                                if (conversationEntities.isNullOrEmpty()) {
                                    Loading("")
                                } else {
                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expanded,
                                        onExpandedChange = { expanded = !expanded }) {
                                        OutlinedTextField(
                                            leadingIcon = {
                                                conversationEntities.find {
                                                    it.entityId == conversationId
                                                }?.let {
                                                    Icon(
                                                        rememberIconicsPainter(
                                                            getStateIcon(
                                                                it
                                                            )
                                                        ),
                                                        contentDescription = it.attributes["friendly_name"] as String?
                                                    )
                                                }
                                            },
                                            value = if (conversationId.isNotEmpty()) {
                                                conversationEntities.find { it.entityId == conversationId }?.attributes?.get(
                                                    "friendly_name"
                                                ) as String? ?: conversationId
                                            } else {
                                                ""
                                            },
                                            onValueChange = {
                                                saveAndSendSettings(
                                                    url, authToken, conversationId, deviceId
                                                )
                                            },
                                            readOnly = true,
                                            label = { Text("Default Conversation Entity") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expanded
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(expanded = expanded,
                                            onDismissRequest = { expanded = false }) {
                                            conversationEntities.forEach { entity ->
                                                DropdownMenuItem(leadingIcon = {
                                                    Icon(
                                                        rememberIconicsPainter(
                                                            getStateIcon(
                                                                entity
                                                            )
                                                        ),
                                                        contentDescription = entity.attributes["friendly_name"] as String?
                                                    )
                                                }, text = {
                                                    Text(
                                                        entity.attributes["friendly_name"] as String?
                                                            ?: entity.entityId
                                                    )
                                                }, onClick = {
                                                    conversationId = entity.entityId
                                                    expanded = false
                                                    saveAndSendSettings(
                                                        url, authToken, conversationId, deviceId
                                                    )
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                val viewModel: DevicesViewModel = viewModel(
                                    factory = DevicesViewModelFactory(repository)
                                )
                                LaunchedEffect(repository) {
                                    viewModel.repository = repository
                                    viewModel.loadDevices()
                                }
                                val devices by viewModel.devicesLiveData.observeAsState()
                                val mobileAppDevices = devices?.filter { device ->
                                    device.identifiers.any { it.first == "mobile_app" }
                                }
                                if (mobileAppDevices.isNullOrEmpty()) {
                                    Loading("")
                                } else {
                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expanded,
                                        onExpandedChange = { expanded = !expanded }) {
                                        OutlinedTextField(
                                            value = if (deviceId.isNotEmpty()) {
                                                mobileAppDevices.find { it.deviceId == deviceId }?.name
                                                    ?: deviceId
                                            } else {
                                                ""
                                            },
                                            onValueChange = {
                                                saveAndSendSettings(
                                                    url, authToken, conversationId, deviceId
                                                )
                                            },
                                            readOnly = true,
                                            label = { Text("Device") },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expanded
                                                )
                                            },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(expanded = expanded,
                                            onDismissRequest = { expanded = false }) {
                                            mobileAppDevices.forEach { device ->
                                                DropdownMenuItem(text = {
                                                    Text(
                                                        device.name
                                                    )
                                                }, onClick = {
                                                    deviceId = device.deviceId
                                                    expanded = false
                                                    saveAndSendSettings(
                                                        url, authToken, conversationId, deviceId
                                                    )
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
