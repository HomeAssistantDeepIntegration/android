package io.homeassistant.deep

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cheonjaeung.compose.grid.GridScope
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import io.homeassistant.deep.ui.theme.AppTheme

class AssistActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

            val url = sharedPreferences.getString(
                "url", ""
            )
            val token = sharedPreferences.getString(
                "auth_token", ""
            )
            val conversationId = sharedPreferences.getString(
                "conversation_id", ""
            )
            val deviceId = sharedPreferences.getString(
                "device_id", null
            )

            if (url.isNullOrEmpty() || token.isNullOrEmpty() || conversationId.isNullOrEmpty()) {
                // Navigate to the configuration activity
                startActivity(
                    Intent(
                        this@AssistActivity, ConfigActivity::class.java
                    )
                )
            } else {
                val tts = TextToSpeech(context) {}
                DisposableEffect(Unit) {
                    onDispose {
                        tts.shutdown()
                    }
                }

                val repository = HomeAssistantRepository(url, token)
                val viewModel: HomeAssistantViewModel = viewModel(
                    factory = HomeAssistantViewModelFactory(repository)
                )
                val conversationViewModel: ConversationViewModel =
                    viewModel(factory = ConversationViewModelFactory(
                        repository, deviceId
                    ) { response, voice ->
                        viewModel.loadStates()
                        if (voice) {
                            tts.speak(
                                response, TextToSpeech.QUEUE_FLUSH, null, "utteranceId"
                            )
                        }
                    })

                AppTheme {
                    val states by viewModel.statesLiveData.observeAsState()

                    LaunchedEffect(Unit) {
                        viewModel.loadStates()
                    }

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
                            var agent by remember { mutableStateOf<EntityState?>(null) }
                            LaunchedEffect(states) {
                                if (agent == null) agent =
                                    states?.find { it.entityId == conversationId }
                            }

                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .verticalScroll(
                                        state = scrollState, enabled = true, reverseScrolling = true
                                    )
                                    .weight(1f)
                            ) {
//                                val conversationEntities by remember {
//                                    derivedStateOf {
//                                        states?.filter { it.entityId.startsWith("conversation.") }
//                                    }
//                                }
//                                LazyRow {
//                                    item {
//                                        Spacer(
//                                            modifier = Modifier.width(24.dp)
//                                        )
//                                    }
//                                    if (conversationEntities != null) {
//                                        items(conversationEntities!!) {
//                                            FilterChip(
//                                                selected = it.entityId == agent?.entityId,
//                                                onClick = {
//                                                    agent = it
//                                                },
//                                                leadingIcon = {
//                                                    Icon(
//                                                        rememberIconicsPainter(
//                                                            getStateIcon(it)
//                                                        ),
//                                                        it.attributes["friendly_name"] as String?
//                                                            ?: it.entityId
//                                                    )
//                                                },
//                                                label = {
//                                                    Text(
//                                                        text = it.attributes["friendly_name"] as String?
//                                                            ?: it.entityId
//                                                    )
//                                                },
//                                                modifier = Modifier.padding(
//                                                    horizontal = 8.dp,
//                                                )
//                                            )
//                                        }
//                                    }
//                                }

                                if (!states.isNullOrEmpty()) {
                                    conversationViewModel.messages.forEach { message ->
                                        MessageBubble(viewModel, states!!, message)
                                    }
                                }
                                if (conversationViewModel.responding) {
                                    RespondingMessageBubble(agent)
                                }
                                LaunchedEffect(conversationViewModel.messages) {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }

                            InputRow(onSend = { message, voice ->
                                agent?.let { conversationViewModel.sendMessage(message, voice, it) }
                            })
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

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun LoadingPreview() {
    AppTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            Loading()
        }
    }
}

@Composable
fun InputRow(
    onSend: (String, Boolean) -> Unit, modifier: Modifier = Modifier
) {
    RequiresPermission(permission = Manifest.permission.RECORD_AUDIO) { isMicrophoneGranted, requestMicrophonePermission ->
        var voice by remember { mutableStateOf(isMicrophoneGranted) }

        if (voice) {
            VoiceInputRow(isMicrophoneGranted, onSend, modifier, deactivateVoice = {
                voice = false
            }, requestMicrophonePermission)
        } else {
            TextInputRow(modifier, onSend, activateVoice = {
                voice = true
            })
        }
    }
}

@Composable
fun VoiceInputRowContent(
    recognizedText: String,
    listening: Boolean,
    startListening: () -> Unit,
    stopListening: () -> Unit,
    onDeactivateVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
        ) {
            Text(
                recognizedText,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .imePadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            if (listening) {
                PulsingIconButton(onClick = stopListening) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                }
            } else {
                FilledTonalIconButton(onClick = startListening) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                }
            }
            IconButton(onClick = onDeactivateVoice) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }
    }
}

@Composable
fun VoiceInputRow(
    isMicrophoneGranted: Boolean,
    onSend: (String, Boolean) -> Unit,
    modifier: Modifier,
    deactivateVoice: () -> Unit,
    requestMicrophonePermission: () -> Unit
) {
    var listening by remember { mutableStateOf(false) }

    if (isMicrophoneGranted) {
        var recognizedText by remember { mutableStateOf("") }
        val context = LocalContext.current
        val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
        val intent = remember {
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        DisposableEffect(speechRecognizer) {
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onBeginningOfSpeech() {
                    recognizedText = ""
                }

                override fun onRmsChanged(rmsdB: Float) {}
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
                        onSend(recognizedText, true)
                        recognizedText = ""
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    recognizedText = matches?.get(0) ?: ""
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d("RecognitionListener", "onEvent: $eventType")
                }
            }

            speechRecognizer.setRecognitionListener(listener)
            speechRecognizer.startListening(intent)

            onDispose {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            }
        }

        VoiceInputRowContent(
            recognizedText = recognizedText,
            listening = listening,
            startListening = { speechRecognizer.startListening(intent) },
            stopListening = { speechRecognizer.stopListening() },
            onDeactivateVoice = {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
                deactivateVoice()
            },
            modifier = modifier
        )
    } else {
        Column {
            Text("Microphone permission required for this feature to be available. Please grant the permission.")
            Button(onClick = requestMicrophonePermission) {
                Text("Request permission")
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun VoiceInputRowPreview() {
    AppTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            VoiceInputRowContent(recognizedText = "",
                listening = false,
                startListening = {},
                stopListening = {},
                onDeactivateVoice = {})
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun VoiceInputRowListeningPreview() {
    AppTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            VoiceInputRowContent(recognizedText = "Hello",
                listening = true,
                startListening = {},
                stopListening = {},
                onDeactivateVoice = {})
        }
    }
}

// @Preview(uiMode = UI_MODE_NIGHT_YES)
// @Composable
// fun VoiceInputRowWithoutPermissionPreview() {
//     AppTheme {
//         Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
//             VoiceInputRow(isMicrophoneGranted = false,
//                 onSend = {},
//                 modifier = Modifier,
//                 deactivateVoice = {},
//                 requestMicrophonePermission = {})
//         }
//     }
// }

@Composable
private fun TextInputRow(
    modifier: Modifier,
    onSend: (String, Boolean) -> Unit,
    activateVoice: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

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
                onSend(message, false)
            })
        )
        if (input.isNotEmpty()) {
            FilledIconButton(onClick = {
                if (input.isEmpty()) return@FilledIconButton
                val message = input
                input = ""
                onSend(message, false)
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            }
        } else {
            FilledIconButton(modifier = Modifier.padding(bottom = 4.dp), onClick = activateVoice) {
                Icon(Icons.Default.Mic, contentDescription = null)
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TextInputRowPreview() {
    AppTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            TextInputRow(modifier = Modifier, onSend = { _, _ -> }, activateVoice = {})
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
    val containerColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = MaterialTheme.colorScheme.tertiary,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500), repeatMode = RepeatMode.Reverse
        ),
        label = "ColorPulse"
    )
    val contentColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.onPrimary,
        targetValue = MaterialTheme.colorScheme.onTertiary,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500), repeatMode = RepeatMode.Reverse
        ),
        label = "ColorPulse"
    )

    FilledIconButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor, contentColor = contentColor
        )
    ) {
        content()
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PulsingIconButtonPreview() {
    AppTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            PulsingIconButton(onClick = {}) {
                Icon(Icons.Default.Mic, contentDescription = null)
            }
        }
    }
}

@Composable
fun GridScope.EntityTile(
    viewModel: HomeAssistantViewModel, states: List<EntityState>, target: ConversationTarget
) {
    val entity = states.find { it.entityId == target.id }

    if (entity != null) {
        when (entity.entityId.split(".").first()) {
            "light", "switch" -> {
                Card(
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
                        containerColor = if (entity.state != "on") MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (entity.state != "on") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onPrimaryContainer,
                    ), onClick = {
                        viewModel.toggleEntity(entity.entityId)
                    }, enabled = entity.state != "unavailable", modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                rememberIconicsPainter(
                                    getStateIcon(entity)
                                ), target.name, modifier = Modifier.fillMaxHeight()
                            )

                            Switch(
                                checked = entity.state == "on", onCheckedChange = {
                                    viewModel.toggleEntity(entity.entityId)
                                }, enabled = entity.state != "unavailable"
                            )
                        }
                        Text(text = target.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            "binary_sensor" -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (entity.state != "on") MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (entity.state != "on") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    onClick = { },
                    enabled = entity.state != "unavailable",
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                rememberIconicsPainter(
                                    getStateIcon(entity)
                                ), target.name
                            )
                        }
                        Text(text = target.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            "sensor" -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    onClick = { },
                    enabled = entity.state != "unavailable",
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                rememberIconicsPainter(
                                    getStateIcon(entity)
                                ), target.name
                            )
                        }
                        Text(text = target.name, modifier = Modifier.fillMaxWidth())

                        val unit = entity.attributes["unit_of_measurement"] as String?
                        if (unit.isNullOrBlank()) {
                            Text(text = entity.state, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(text = "${entity.state} $unit", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            "person", "device_tracker" -> {
                Card(
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
                        containerColor = if (entity.state != "on") MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (entity.state != "on") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onPrimaryContainer,
                    ), modifier = Modifier
                        .span(2)
                        .aspectRatio(1f)
                ) {
                    Map(
                        viewModel.repository.url,
                        states,
                        focusedState = entity,
                    )
                }
            }

            "conversation" -> {
                Card(
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ), modifier = Modifier.span(2)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 2.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                rememberIconicsPainter(
                                    getStateIcon(entity)
                                ),
                                target.name,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                        Text(text = target.name, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
fun ErrorCard(
    code: ConversationErrorCode?
) {
    Card(
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ), modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = when (code) {
                    null -> "Unknown error"
                    ConversationErrorCode.NO_INTENT_MATCH -> "No intent matched"
                    ConversationErrorCode.NO_VALID_TARGETS -> "No valid targets"
                    ConversationErrorCode.FAILED_TO_HANDLE -> "Failed to handle"
                    ConversationErrorCode.TIMER_NOT_FOUND_RESPONSE -> "Timer not found"
                    ConversationErrorCode.MULTIPLE_TIMERS_MATCHED_RESPONSE -> "Multiple timers matched"
                    ConversationErrorCode.NO_TIMER_SUPPORT_RESPONSE -> "No timer support"
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    viewModel: HomeAssistantViewModel, states: List<EntityState>, message: AssistMessage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isUserMessage) Arrangement.End else Arrangement.Start
    ) {
//        if (!message.isUserMessage && message.agent != null) {
//            Column(
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .padding(end = 8.dp),
//                verticalArrangement = Arrangement.Bottom
//            ) {
//                Surface(
//                    shape = CircleShape,
//                    tonalElevation = 2.dp
//                ) {
//                    Icon(
//                        rememberIconicsPainter(
//                            getStateIcon(message.agent)
//                        ),
//                        message.agent.attributes["friendly_name"] as String?,
//                        modifier = Modifier.padding(8.dp),
//                    )
//                }
//            }
//        }
        Surface(
            color = if (message.isError) MaterialTheme.colorScheme.errorContainer else if (message.isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else if (message.isUserMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
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
                if (message.isError && message.data != null) {
                    Spacer(modifier = Modifier.heightIn(8.dp))
                    ErrorCard(message.data.code)
                }
                if (!message.data?.success.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.heightIn(8.dp))
                    VerticalGrid(
                        columns = SimpleGridCells.Adaptive(minSize = 128.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        message.data!!.success!!.forEach {
                            when (it.type) {
                                ConversationTargetType.ENTITY -> {
                                    EntityTile(viewModel, states, it)
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RespondingMessageBubble(agent: EntityState? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
//        if (agent != null) {
//            Column(
//                modifier = Modifier
//                    .fillMaxHeight()
//                    .padding(end = 8.dp),
//                verticalArrangement = Arrangement.Bottom
//            ) {
//                Surface(
//                    shape = CircleShape,
//                    tonalElevation = 2.dp
//                ) {
//                    Icon(
//                        rememberIconicsPainter(
//                            getStateIcon(agent)
//                        ),
//                        agent.attributes["friendly_name"] as String?,
//                        modifier = Modifier.padding(8.dp),
//                    )
//                }
//            }
//        }
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

//@Preview(uiMode = UI_MODE_NIGHT_YES)
//@Composable
//fun MessageBubblesPreview() {
//    AppTheme {
//        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
//            Column(
//                modifier = Modifier.padding(vertical = 4.dp)
//            ) {
//                MessageBubble(AssistMessage(content = "Hello", isUserMessage = true))
//                MessageBubble(AssistMessage(content = "How can I help you?", isUserMessage = false))
//                RespondingMessageBubble()
//            }
//        }
//    }
//}

