package io.homeassistant.deep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.homeassistant.deep.ui.theme.AppTheme

class EntitiesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

            val url =
                sharedPreferences.getString(
                    "url", ""
                )
            val token =
                sharedPreferences.getString(
                    "auth_token", ""
                )

            if (url.isNullOrEmpty() || token.isNullOrEmpty()) {
                // Navigate to the configuration activity
                startActivity(
                    Intent(
                        this@EntitiesActivity, ConfigActivity::class.java
                    )
                )
            } else {
                val repository = HomeAssistantRepository(url, token)
                val viewModel: HomeAssistantViewModel = viewModel(
                    factory = HomeAssistantViewModelFactory(repository)
                )

                AppTheme {

                    val states by viewModel.statesLiveData.observeAsState()
                    LaunchedEffect(Unit) {
                        viewModel.loadStates()
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(title = { Text("Entities") }, navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            })
                        },
                        content = { padding ->
                            Box(
                                modifier = Modifier
                                    .padding(padding)
                                    .fillMaxSize()
                            ) {
                                if (states.isNullOrEmpty()) {
                                    Loading("")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        items(states!!) { state ->
                                            EntityStateItem(state)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EntityStateItem(state: EntityState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val name = state.attributes["friendly_name"] as String?
            Icon(
                rememberIconicsPainter(
                    getStateIcon(state)
                ), name
            )
            if (name is String) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "Entity ID: ${state.entityId}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(text = "State: ${state.state}", style = MaterialTheme.typography.bodySmall)

            for ((key, value) in state.attributes) {
                Text(
                    text = "$key: $value",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
