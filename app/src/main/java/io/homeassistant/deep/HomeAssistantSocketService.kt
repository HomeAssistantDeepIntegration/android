package io.homeassistant.deep

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable
import okhttp3.OkHttpClient

enum class ConnectionStatus {
    NOT_STARTED, OPENED, CLOSED, CONNECTING, CLOSING, FAILED, AUTHENTICATING, AUTHENTICATION_FAILED
}

enum class MessageType(val value: String) {
    UNKNOWN("unknown"), AUTH_REQUIRED("auth_required"), AUTH("auth"), AUTH_OK("auth_ok"), AUTH_INVALID(
        "auth_invalid"
    ),
    ASSIST_PIPELINE_RUN("assist_pipeline/run"),
    EVENT("event");

    companion object {
        @Suppress("unused")
        fun fromValueOrNull(value: String): MessageType? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

class MessageTypeAdapter : JsonAdapter<MessageType>() {
    @FromJson
    override fun fromJson(reader: JsonReader): MessageType? {
        val value = reader.nextString()
        return MessageType.fromValueOrNull(value)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: MessageType?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.value)
        }
    }
}

sealed class SocketMessage(
    @Suppress("unused") val type: MessageType
)

data object UnknownSocketMessage : SocketMessage(MessageType.UNKNOWN)

@JsonClass(generateAdapter = true)
data class AuthRequiredSocketMessage(
    @Json(name = "ha_version") val haVersion: String
) : SocketMessage(MessageType.AUTH_REQUIRED)

@JsonClass(generateAdapter = true)
data class AuthSocketMessage(
    @Json(name = "access_token") val accessToken: String
) : SocketMessage(MessageType.AUTH)

@JsonClass(generateAdapter = true)
data class AuthOkSocketMessage(
    @Json(name = "ha_version") val haVersion: String
) : SocketMessage(MessageType.AUTH_OK)

@JsonClass(generateAdapter = true)
data class AuthInvalidSocketMessage(
    val message: String
) : SocketMessage(MessageType.AUTH_INVALID)

@JsonClass(generateAdapter = true)
data class AssistPipelineRunSocketMessage(
    val id: Int,
    @Json(name = "start_stage") val startStage: String,
    val input: Input,
    @Json(name = "end_stage") val endStage: String,
    val pipeline: String,
    @Json(name = "conversation_id") val conversationId: String?,
) : SocketMessage(MessageType.ASSIST_PIPELINE_RUN) {
    @JsonClass(generateAdapter = true)
    data class Input(
        val text: String?
    )
}

@JsonClass(generateAdapter = true)
data class EventSocketMessage(
    val id: Int,
    val event: EventSocketMessageEvent
) : SocketMessage(MessageType.EVENT)


enum class EventType(val value: String) {
    UNKNOWN("unknown"), INTENT_END("intent-end");

    companion object {
        @Suppress("unused")
        fun fromValueOrNull(value: String): EventType? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

class EventTypeAdapter : JsonAdapter<EventType>() {
    @FromJson
    override fun fromJson(reader: JsonReader): EventType? {
        val value = reader.nextString()
        return EventType.fromValueOrNull(value)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: EventType?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.value)
        }
    }
}

sealed class EventSocketMessageEvent(
    @Suppress("unused") val type: EventType
)

data object UnknownEvent : EventSocketMessageEvent(EventType.UNKNOWN)

@JsonClass(generateAdapter = true)
data class IntentEndEvent(
    val data: IntentEndEventData,
    val timestamp: String
) : EventSocketMessageEvent(EventType.INTENT_END) {
    @JsonClass(generateAdapter = true)
    class IntentEndEventData(
        @Json(name = "intent_output")
        val intentOutput: IntentOutput
    )
    @JsonClass(generateAdapter = true)
    class IntentOutput(
        val response: Response,
        @Json(name = "conversation_id")
        val conversationId: String?
    )
    @JsonClass(generateAdapter = true)
    class Response(
        val speech: Speech
    )
    @JsonClass(generateAdapter = true)
    class Speech(
        @Json(name = "plain")
        val plain: Plain
    )
    @JsonClass(generateAdapter = true)
    class Plain(
        val speech: String
    )
}

interface HomeAssistantSocketService {
    @Receive
    fun observeConnection(): Flowable<WebSocket.Event>

    @Receive
    fun observeMessages(): Flowable<SocketMessage>

    @Send
    fun sendMessage(message: SocketMessage)
}

val moshiFactory: Moshi =
    Moshi.Builder().add(MessageTypeAdapter()).add(EventTypeAdapter()).add(
        PolymorphicJsonAdapterFactory.of(SocketMessage::class.java, "type").withSubtype(
            AuthRequiredSocketMessage::class.java, MessageType.AUTH_REQUIRED.value
        ).withSubtype(AuthSocketMessage::class.java, MessageType.AUTH.value)
            .withSubtype(AuthOkSocketMessage::class.java, MessageType.AUTH_OK.value)
            .withSubtype(
                AuthInvalidSocketMessage::class.java, MessageType.AUTH_INVALID.value
            ).withSubtype(
                AssistPipelineRunSocketMessage::class.java,
                MessageType.ASSIST_PIPELINE_RUN.value
            ).withSubtype(
                EventSocketMessage::class.java, MessageType.EVENT.value
            ).withDefaultValue(UnknownSocketMessage)
    ).add(
        PolymorphicJsonAdapterFactory.of(EventSocketMessageEvent::class.java, "type")
            .withSubtype(IntentEndEvent::class.java, EventType.INTENT_END.value)
            .withDefaultValue(UnknownEvent)
    ).build()

fun socketServiceFactory(url: String): HomeAssistantSocketService {
    return Scarlet.Builder().webSocketFactory(
        OkHttpClient.Builder().build().newWebSocketFactory("$url/api/websocket")
    ).addMessageAdapterFactory(
        MoshiMessageAdapter.Factory(
            moshiFactory
        )
    )
        .addStreamAdapterFactory(RxJava2StreamAdapterFactory()).build()
        .create<HomeAssistantSocketService>()
}
