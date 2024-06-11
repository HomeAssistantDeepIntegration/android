package io.homeassistant.deep.wear

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import io.homeassistant.deep.shared.ConnectionStatus
import io.homeassistant.deep.shared.ConversationViewModel
import io.homeassistant.deep.wear.ui.theme.AppTheme

class AssistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    TimeText(modifier = Modifier.align(Alignment.TopCenter))

                    val context = LocalContext.current
                    val sharedPreferences = context.getSharedPreferences("my_prefs", MODE_PRIVATE)

                    val tts = TextToSpeech(context) {}
                    DisposableEffect(Unit) {
                        onDispose {
                            tts.shutdown()
                        }
                    }

                    val conversation = remember {
                        ConversationViewModel(sharedPreferences.getString("url", "") ?: "",
                            sharedPreferences.getString("auth_token", "") ?: "",
                            sharedPreferences.getString("assist_pipeline", "") ?: "",
                            onResponse = { response ->
                                tts.speak(
                                    response, TextToSpeech.QUEUE_FLUSH, null, "utteranceId"
                                )
                            })
                    }

                    when (conversation.connectionStatus) {
                        ConnectionStatus.NOT_STARTED -> {
                            Text("Not started")
                        }

                        ConnectionStatus.OPENED -> {
                            // val scrollState = rememberScrollState()
                            // Column(
                            //     modifier = Modifier
                            //         .verticalScroll(
                            //             state = scrollState,
                            //             enabled = true,
                            //             reverseScrolling = true
                            //         )
                            //         .weight(1f)
                            // ) {
                            //     conversation.messages.forEach { message ->
                            //         MessageBubble(message)
                            //     }
                            //     if (conversation.responding > 0) {
                            //         RespondingMessageBubble()
                            //     }
                            //     LaunchedEffect(conversation.messages) {
                            //         scrollState.animateScrollTo(scrollState.maxValue)
                            //     }
                            // }

                            InputRow(onSend = {
                                conversation.sendMessage(it)
                            })
                        }

                        ConnectionStatus.CLOSED -> Failed("Connection closed.",
                            conversation,
                            onCancel = { this@AssistActivity.finish() })

                        ConnectionStatus.CONNECTING -> Loading("Connecting...")

                        ConnectionStatus.CLOSING -> Loading("Closing...")

                        ConnectionStatus.FAILED -> Failed(conversation = conversation,
                            onCancel = { this@AssistActivity.finish() })

                        ConnectionStatus.AUTHENTICATING -> Loading("Authenticating...")

                        ConnectionStatus.AUTHENTICATION_FAILED -> Failed("Authentication failed.",
                            conversation,
                            onCancel = { this@AssistActivity.finish() })
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true, device = "id:wearos_small_round", showSystemUi = true
)
@Composable
fun MainScreenPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            TimeText(modifier = Modifier.align(Alignment.TopCenter))

            InputRow(onSend = {})
        }
    }
}

@Composable
fun Loading(text: String = "Loading...") {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}

@Preview(
    showBackground = true, device = "id:wearos_small_round", showSystemUi = true
)
@Composable
fun LoadingScreenPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            TimeText(modifier = Modifier.align(Alignment.TopCenter))

            Loading()
        }
    }
}

@Composable
fun Failed(
    text: String = "Failed to connect", conversation: ConversationViewModel, onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            FilledIconButton(onClick = { conversation.observeConnection() }) {
                Icon(Icons.Default.Refresh, "Retry")
            }
            Spacer(modifier = Modifier.width(16.dp))
            FilledIconButton(onClick = { onCancel() }) {
                Icon(Icons.Default.Close, "Cancel")
            }
        }
    }
}

@Preview(
    showBackground = true, device = "id:wearos_small_round", showSystemUi = true
)
@Composable
fun FailedScreenPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            TimeText(modifier = Modifier.align(Alignment.TopCenter))

            Failed(conversation = ConversationViewModel("", "", "", onResponse = {}), onCancel = {})
        }
    }
}

@Composable
fun InputRow(
    onSend: (String) -> Unit
) {
    val placeholder = "Enter a request"

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it.data?.let { data ->
                val results: Bundle = RemoteInput.getResultsFromIntent(data)
                val newValue: CharSequence? = results.getCharSequence(placeholder)
                onSend(newValue as String)
            }
        }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FilledIconButton(onClick = {
            val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent();
            val remoteInputs: List<RemoteInput> = listOf(
                RemoteInput.Builder(placeholder).setLabel(placeholder).wearableExtender {
                    setEmojisAllowed(false)
                    setInputActionType(EditorInfo.IME_ACTION_DONE)
                }.build()
            )

            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

            launcher.launch(intent)
        }) {
            Icon(Icons.Default.Face, contentDescription = null)
        }
    }
}

