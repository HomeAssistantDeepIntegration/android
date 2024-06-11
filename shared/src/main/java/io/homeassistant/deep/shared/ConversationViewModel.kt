package io.homeassistant.deep.shared

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tinder.scarlet.WebSocket

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
                is io.homeassistant.deep.shared.AuthRequiredSocketMessage -> {
                    service.sendMessage(io.homeassistant.deep.shared.AuthSocketMessage(this@ConversationViewModel.authToken))
                }

                is io.homeassistant.deep.shared.AuthOkSocketMessage -> {
                    _connectionStatus = ConnectionStatus.OPENED
                }

                is io.homeassistant.deep.shared.AuthInvalidSocketMessage -> {
                    _connectionStatus = ConnectionStatus.AUTHENTICATION_FAILED
                }

                is io.homeassistant.deep.shared.EventSocketMessage -> {
                    when (message.event) {
                        is io.homeassistant.deep.shared.IntentEndEvent -> {
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

                        is io.homeassistant.deep.shared.UnknownEvent -> {
                            Log.w(TAG, "Unknown event received")
                        }
                    }
                }

                is io.homeassistant.deep.shared.UnknownSocketMessage -> {
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
            io.homeassistant.deep.shared.AssistPipelineRunSocketMessage(
                id = id++,
                startStage = "intent",
                endStage = "intent",
                input = io.homeassistant.deep.shared.AssistPipelineRunSocketMessage.Input(text = content),
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