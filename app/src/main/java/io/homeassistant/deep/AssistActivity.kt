package io.homeassistant.deep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.tinder.scarlet.WebSocket
import io.homeassistant.deep.ui.theme.HomeAssistantDeepIntegrationTheme

data class AssistMessage(val content: String, val isUserMessage: Boolean)
class ConversationViewModel(
    url: String,
    private val authToken: String,
    private val assistPipeline: String,
    private val onResponse: (String) -> Unit
) {
    private val TAG = "ConversationViewModel"

    private var service = socketServiceFactory(url)

    private var id = 1
    private var conversationId: String? = null

    fun observeConnection() {
        _connectionStatus = ConnectionStatus.CONNECTING
        service.observeConnection().subscribe({ response ->
            when (response) {
                is WebSocket.Event.OnConnectionOpened<*> -> _connectionStatus =
                    (ConnectionStatus.AUTHENTICATING)

                is WebSocket.Event.OnConnectionClosed -> _connectionStatus =
                    (ConnectionStatus.CLOSED)

                is WebSocket.Event.OnConnectionClosing -> _connectionStatus =
                    (ConnectionStatus.CLOSING)

                is WebSocket.Event.OnConnectionFailed -> _connectionStatus =
                    (ConnectionStatus.FAILED)

                is WebSocket.Event.OnMessageReceived -> {
                    Log.d(TAG, response.message.toString())
                }
            }
        }, { error ->
            error.localizedMessage?.let { Log.e(TAG, it) }
        })
        service.observeMessages().subscribe({ message ->
            Log.d(TAG, message.toString())

            when (message) {
                is AuthRequiredSocketMessage -> {
                    service.sendMessage(AuthSocketMessage(this@ConversationViewModel.authToken))
                }

                is AuthOkSocketMessage -> {
                    _connectionStatus = ConnectionStatus.OPENED
                }

                is AuthInvalidSocketMessage -> {
                    _connectionStatus = ConnectionStatus.AUTHENTICATION_FAILED
                }

                is EventSocketMessage -> {
                    when (message.event) {
                        is IntentEndEvent -> {
                            conversationId = message.event.data.intentOutput.conversationId
                            _messages.add(
                                AssistMessage(
                                    message.event.data.intentOutput.response.speech.plain.speech,
                                    false
                                )
                            )
                            _responding--
                            onResponse(message.event.data.intentOutput.response.speech.plain.speech)
                        }

                        is UnknownEvent -> {
                            Log.w(TAG, "Unknown event received")
                        }
                    }
                }

                is UnknownSocketMessage -> {
                    Log.w(TAG, "Unknown message type received")
                }

                else -> {}
            }
        }, { error ->
            error.localizedMessage?.let { Log.e(TAG, it) }
        })
    }

    private var _connectionStatus by mutableStateOf(ConnectionStatus.NOT_STARTED)
    val connectionStatus: ConnectionStatus
        get() = _connectionStatus

    private val _messages = mutableStateListOf<AssistMessage>()
    val messages: List<AssistMessage>
        get() = _messages

    private var _responding by mutableStateOf(0)
    val responding: Int
        get() = _responding

    fun sendMessage(content: String) {
        _messages.add(AssistMessage(content, true))
        _responding++
        service.sendMessage(
            AssistPipelineRunSocketMessage(
                id = id++,
                startStage = "intent",
                endStage = "intent",
                input = AssistPipelineRunSocketMessage.Input(text = content),
                pipeline = assistPipeline,
                conversationId = conversationId
            )
        )
        Log.d(TAG, "Sent message: $content")
    }

    init {
        observeConnection()
    }
}

class AssistActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HomeAssistantDeepIntegrationTheme {
                ModalBottomSheet(
                    onDismissRequest = { this.finish() },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    windowInsets = BottomSheetDefaults.windowInsets,
                    modifier = Modifier.imePadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = with(LocalConfiguration.current) { screenHeightDp.dp * 0.1f },
                                max = with(LocalConfiguration.current) { screenHeightDp.dp * 0.6f })
                            .padding(16.dp), verticalArrangement = Arrangement.Bottom
                    ) {
                        val context = LocalContext.current
                        val sharedPreferences =
                            context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

                        val tts = TextToSpeech(context) {}
                        // DisposableEffect(Unit) {
                        //     onDispose {
                        //         tts.shutdown()
                        //     }
                        // }

                        val conversation = remember {
                            ConversationViewModel(
                                sharedPreferences.getString("url", "") ?: "",
                                sharedPreferences.getString("auth_token", "") ?: "",
                                sharedPreferences.getString("assist_pipeline", "") ?: "",
                                onResponse = { response ->
                                    tts.speak(
                                        response,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "utteranceId"
                                    )
                                }
                            )
                        }

                        when (conversation.connectionStatus) {
                            ConnectionStatus.NOT_STARTED -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Not started")
                                }
                            }

                            ConnectionStatus.OPENED -> {
                                val scrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(
                                            state = scrollState,
                                            enabled = true,
                                            reverseScrolling = true
                                        )
                                        .weight(1f)
                                ) {
                                    conversation.messages.forEach { message ->
                                        MessageBubble(message)
                                    }
                                    if (conversation.responding > 0) {
                                        RespondingMessageBubble()
                                    }
                                    LaunchedEffect(conversation.messages) {
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                }

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
}

@Composable
fun Loading(text: String = "Loading...") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.heightIn(16.dp))
        Text(text)
    }
}

@Composable
fun Failed(
    text: String = "Failed to connect", conversation: ConversationViewModel, onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text)
        Button(onClick = { conversation.observeConnection() }) {
            Text("Retry")
        }
        Button(onClick = { onCancel() }) {
            Text("Cancel")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InputRow(
    onSend: (String) -> Unit, modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val microphonePermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )
    var voice by remember { mutableStateOf(microphonePermissionState.status.isGranted) }

    if (voice) {
        var listening by remember { mutableStateOf(false) }

        if (microphonePermissionState.status.isGranted) {
            var recognizedText by remember { mutableStateOf("") }
            val context = LocalContext.current
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_GB")
            }
            val recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onBeginningOfSpeech() {
                    recognizedText = ""
                }

                override fun onRmsChanged(p0: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    recognizedText = matches?.get(0) ?: ""
                    listening = false
                    if (recognizedText.isNotEmpty()) {
                        onSend(recognizedText)
                        recognizedText = ""
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    recognizedText = matches?.get(0) ?: ""
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            speechRecognizer.setRecognitionListener(recognitionListener)

            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
            ) {
                Text(
                    recognizedText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .imePadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                if (listening) {
                    PulsingIconButton(onClick = {
                        speechRecognizer.stopListening()
                    }) {
                        Icon(Icons.Default.Face, contentDescription = null)
                    }
                } else {
                    FilledTonalIconButton(onClick = {
                        speechRecognizer.startListening(intent)
                    }) {
                        Icon(Icons.Default.Face, contentDescription = null)
                    }
                }
                IconButton(onClick = {
                    speechRecognizer.stopListening()
                    speechRecognizer.destroy()
                    voice = false
                }) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }


            LaunchedEffect(Unit) {
                speechRecognizer.startListening(intent)
            }
            // DisposableEffect(Unit) {
            //     onDispose {
            //         speechRecognizer.stopListening()
            //         speechRecognizer.destroy()
            //     }
            // }
        } else {
            Column {
                val textToShow = if (microphonePermissionState.status.shouldShowRationale) {
                    // If the user has denied the permission but the rationale can be shown,
                    // then gently explain why the app requires this permission
                    "The microphone is important for this app. Please grant the permission."
                } else {
                    // If it's the first time the user lands on this feature, or the user
                    // doesn't want to be asked again for this permission, explain that the
                    // permission is required
                    "Microphone permission required for this feature to be available. " + "Please grant the permission"
                }
                Text(textToShow)
                Button(onClick = { microphonePermissionState.launchPermissionRequest() }) {
                    Text("Request permission")
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                label = { Text("Enter a request") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isEmpty()) return@KeyboardActions
                    val message = input
                    input = ""
                    onSend(message)
                })
            )
            if (input.isNotEmpty()) {
                FilledIconButton(onClick = {
                    if (input.isEmpty()) return@FilledIconButton
                    val message = input
                    input = ""
                    onSend(message)
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                }
            } else {
                FilledIconButton(modifier = Modifier.padding(bottom = 4.dp), onClick = {
                    voice = true
                }) {
                    Icon(Icons.Default.Face, contentDescription = null)
                }
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun PulsingIconButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
        ), label = "Pulse"
    )

    FilledIconButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
    ) {
        content()
    }
}

@Composable
fun MessageBubble(message: AssistMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isUserMessage) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = if (message.isUserMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(
                topStart = if (message.isUserMessage) 16.dp else 0.dp,
                topEnd = if (message.isUserMessage) 0.dp else 16.dp,
                bottomEnd = 16.dp,
                bottomStart = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(text = message.content)
            }
        }
    }
}

@Composable
fun RespondingMessageBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(
                topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    HomeAssistantDeepIntegrationTheme {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            MessageBubble(AssistMessage(content = "Hello", isUserMessage = true))
            MessageBubble(AssistMessage(content = "How can I help you?", isUserMessage = false))
            RespondingMessageBubble()
        }
    }
}

