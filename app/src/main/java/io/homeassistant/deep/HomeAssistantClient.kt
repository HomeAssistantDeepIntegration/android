package io.homeassistant.deep

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Date

interface HomeAssistantApi {

    @GET("api/states")
    fun getStates(@Header("Authorization") token: String): Call<List<EntityState>>

    @GET("api/config/device_registry/list")
    fun getDevices(@Header("Authorization") token: String): Call<List<Device>>

    @POST("api/services/{domain}/{service}")
    fun callService(
        @Header("Authorization") token: String,
        @Path("domain") domain: String,
        @Path("service") service: String,
        @Body requestBody: RequestBody
    ): Call<List<EntityState>>

    @POST("api/services/{domain}/toggle")
    fun toggleEntity(
        @Header("Authorization") token: String,
        @Path("domain") domain: String,
        @Body requestBody: ToggleEntity
    ): Call<List<EntityState>>

    // Calendar

    @GET("api/calendars")
    fun getCalendars(@Header("Authorization") token: String): Call<List<Calendar>>

    @POST("api/calendars/{calendar_id}/events")
    fun createEvent(
        @Header("Authorization") token: String,
        @Path("calendar_id") calendarId: String,
        @Body event: CalendarEvent
    ): Call<Unit>

    @GET("api/calendars/{calendar_id}/events")
    fun getEvents(
        @Header("Authorization") token: String, @Path("calendar_id") calendarId: String
    ): Call<List<CalendarEvent>>

    // Todos

    @GET("api/todos/{todo_id}/items")
    fun getItems(
        @Header("Authorization") token: String, @Path("todo_id") todoId: String
    ): Call<List<TodoItem>>

    @POST("api/services/todo/add_item")
    fun addItem(
        @Header("Authorization") token: String, @Body item: AddTodoItemData
    ): Call<Unit>

    @POST("api/services/todo/update_item")
    fun updateItem(
        @Header("Authorization") token: String, @Body item: UpdateTodoItemData
    ): Call<Unit>

    @POST("api/services/todo/delete_item")
    fun removeItem(
        @Header("Authorization") token: String, @Body item: RemoveTodoItemData
    ): Call<Unit>

    // Conversation

    @POST("api/conversation/{conversation_id}/process")
    fun processConversation(
        @Header("Authorization") token: String,
        @Path("conversation_id") conversationId: String,
        @Body requestBody: ConversationInput
    ): Call<ConversationResult>
}

@JsonClass(generateAdapter = true)
data class EntityState(
    @Json(name = "entity_id") val entityId: String,
    val state: String,
    @Json(name = "last_changed") val lastChanged: Date,
    val attributes: Map<String, Any>
)

@JsonClass(generateAdapter = true)
data class Device(
    val name: String,
    @Json(name = "id") val deviceId: String,
    val identifiers: List<Pair<String, String>>
)


class PairAdapter<A, B>(
    private val aAdapter: JsonAdapter<A>, private val bAdapter: JsonAdapter<B>
) : JsonAdapter<Pair<A, B>>() {

    @ToJson
    override fun toJson(writer: JsonWriter, value: Pair<A, B>?) {
        writer.beginArray()
        aAdapter.toJson(writer, value?.first)
        bAdapter.toJson(writer, value?.second)
        writer.endArray()
    }

    @FromJson
    override fun fromJson(reader: JsonReader): Pair<A, B>? {
        var first: A? = null
        var second: B? = null

        reader.beginArray()
        if (reader.hasNext()) {
            first = aAdapter.fromJson(reader)
        }
        if (reader.hasNext()) {
            second = bAdapter.fromJson(reader)
        }
        reader.endArray()

        return if (first != null && second != null) {
            Pair(first, second)
        } else {
            null
        }
    }
}

class PairAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi
    ): JsonAdapter<*>? {
        if (type is ParameterizedType && type.rawType == Pair::class.java) {
            val aType = type.actualTypeArguments[0]
            val bType = type.actualTypeArguments[1]
            val aAdapter = moshi.adapter<Any>(aType)
            val bAdapter = moshi.adapter<Any>(bType)
            return PairAdapter(aAdapter, bAdapter)
        }
        return null
    }
}

@JsonClass(generateAdapter = true)
data class ToggleEntity(
    @Json(name = "entity_id") val entityId: String
)

@JsonClass(generateAdapter = true)
data class Calendar(
    val id: String, val name: String
)

@JsonClass(generateAdapter = true)
data class CalendarEvent(
    val summary: String,
    val start: String,
    val end: String,
    val description: String?,
    val location: String?
)

@JsonClass(generateAdapter = true)
data class TodoItem(
    val uid: String,
    val summary: String,
    val status: String,
    val due: Date?,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class AddTodoItemData(
    @Json(name = "entity_id") val entityId: String,
    val item: String,
    @Json(name = "due_date") val dueDate: Date?,
    @Json(name = "due_date_time") val dueDateTime: Date?,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class UpdateTodoItemData(
    @Json(name = "entity_id") val entityId: String,
    val item: String,
    val rename: String?,
    val status: String,
    @Json(name = "due_date") val dueDate: Date?,
    @Json(name = "due_date_time") val dueDateTime: Date?,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class RemoveTodoItemData(
    @Json(name = "entity_id") val entityId: String, val item: String
)

@JsonClass(generateAdapter = true)
data class ConversationInput(
    val text: String,
    @Json(name = "conversation_id") val conversationId: String? = null,
    @Json(name = "device_id") val deviceId: String? = null,
    val language: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationResult(
    val response: ConversationResponse, @Json(name = "conversation_id") val conversationId: String?
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    val speech: ConversationSpeech,
    val language: String,
    @Json(name = "response_type") val responseType: ConversationResponseType?,
    val data: ConversationData
)


enum class ConversationResponseType(val value: String) {
    ACTION_DONE("action_done"), QUERY_ANSWER("query_answer"), ERROR("error");

    companion object {
        @Suppress("unused")
        fun fromValueOrNull(value: String): ConversationResponseType? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

class ConversationResponseTypeAdapter : JsonAdapter<ConversationResponseType?>() {
    @FromJson
    override fun fromJson(reader: JsonReader): ConversationResponseType? {
        val value = reader.nextString()
        return ConversationResponseType.fromValueOrNull(value)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ConversationResponseType?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.value)
        }
    }
}

@JsonClass(generateAdapter = true)
data class ConversationSpeech(
    val plain: ConversationPlain
)

@JsonClass(generateAdapter = true)
data class ConversationPlain(
    val speech: String, @Json(name = "extra_data") val extraData: Any?
)

@JsonClass(generateAdapter = true)
data class ConversationData(
    val code: ConversationErrorCode?,
    val targets: List<ConversationTarget>?,
    val success: List<ConversationTarget>?,
    val failed: List<ConversationTarget>?
)

enum class ConversationErrorCode(val value: String) {
    NO_INTENT_MATCH("no_intent_match"), NO_VALID_TARGETS("no_valid_targets"), FAILED_TO_HANDLE(
        "failed_to_handle"
    ),
    TIMER_NOT_FOUND_RESPONSE("timer_not_found"), MULTIPLE_TIMERS_MATCHED_RESPONSE("multiple_timers_matched"), NO_TIMER_SUPPORT_RESPONSE(
        "no_timer_support"
    );

    companion object {
        @Suppress("unused")
        fun fromValueOrNull(value: String): ConversationErrorCode? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

class ConversationErrorCodeAdapter : JsonAdapter<ConversationErrorCode?>() {
    @FromJson
    override fun fromJson(reader: JsonReader): ConversationErrorCode? {
        val value = reader.nextString()
        return ConversationErrorCode.fromValueOrNull(value)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ConversationErrorCode?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.value)
        }
    }
}

@JsonClass(generateAdapter = true)
data class ConversationTarget(
    val name: String, val type: ConversationTargetType?, val id: String?
)

enum class ConversationTargetType(val value: String) {
    AREA("area"), FLOOR("floor"), DEVICE("device"), ENTITY("entity"), DOMAIN("domain"), DEVICE_CLASS(
        "device_class"
    ),
    CUSTOM("custom");

    companion object {
        @Suppress("unused")
        fun fromValueOrNull(value: String): ConversationTargetType? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

class ConversationTargetTypeAdapter : JsonAdapter<ConversationTargetType?>() {
    @FromJson
    override fun fromJson(reader: JsonReader): ConversationTargetType? {
        val value = reader.nextString()
        return ConversationTargetType.fromValueOrNull(value)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ConversationTargetType?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.value)
        }
    }
}

class LoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Log the raw request
        val rawJsonReq = request.body?.toString() ?: "No request body"
        Log.d("LoggingInterceptor", "Raw JSON request: $rawJsonReq")

        val response = chain.proceed(request)

        // Log the raw response
        val rawJsonRes = response.body?.string() ?: "No response body"
        Log.d("LoggingInterceptor", "Raw JSON response: $rawJsonRes")

        // Create a new response with the same body so that it can be consumed by Moshi
        return response.newBuilder().body(rawJsonRes.toResponseBody(response.body?.contentType()))
            .build()
    }
}

class HomeAssistantClient(url: String) {
    val okHttpClient = OkHttpClient.Builder().addInterceptor(LoggingInterceptor()).build()
    private val retrofit: Retrofit =
        Retrofit.Builder().baseUrl(url).client(okHttpClient).addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder().add(ConversationResponseTypeAdapter())
                    .add(ConversationErrorCodeAdapter()).add(ConversationTargetTypeAdapter())
                    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                    .add(PairAdapterFactory()).build()
            )
        ).build()

    val api: HomeAssistantApi = retrofit.create(HomeAssistantApi::class.java)
}

class HomeAssistantRepository(val url: String, private val token: String) {
    private val api = HomeAssistantClient(url).api

    fun getStates(callback: (List<EntityState>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getStates("Bearer $token").execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    fun getDevices(callback: (List<Device>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getDevices("Bearer $token").execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                        Log.d("HomeAssistantRepository", "Loaded ${response.body()?.size} devices")
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    fun callService(
        domain: String, service: String, data: RequestBody, callback: (Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.callService("Bearer $token", domain, service, data).execute()
                withContext(Dispatchers.Main) {
                    callback(response.isSuccessful)
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            }
        }
    }

    fun toggleEntity(domain: String, data: ToggleEntity, callback: (List<EntityState>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.toggleEntity("Bearer $token", domain, data).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    fun getCalendars(callback: (List<Calendar>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getCalendars("Bearer $token").execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    fun getEvents(calendarId: String, callback: (List<CalendarEvent>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getEvents("Bearer $token", calendarId).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    fun getItems(todoId: String, callback: (List<TodoItem>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getItems("Bearer $token", todoId).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    fun addItem(item: AddTodoItemData, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.addItem("Bearer $token", item).execute()
                withContext(Dispatchers.Main) {
                    callback(response.isSuccessful)
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            }
        }
    }

    fun updateItem(item: UpdateTodoItemData, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.updateItem("Bearer $token", item).execute()
                withContext(Dispatchers.Main) {
                    callback(response.isSuccessful)
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            }
        }
    }

    fun removeItem(item: RemoveTodoItemData, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.removeItem("Bearer $token", item).execute()
                withContext(Dispatchers.Main) {
                    callback(response.isSuccessful)
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(false) }
            }
        }
    }

    fun processConversation(
        conversationId: String, input: ConversationInput, callback: (ConversationResult?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response =
                    api.processConversation("Bearer $token", conversationId, input).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(response.body())
                    } else {
                        callback(null)
                    }
                }
            } catch (e: HttpException) {
                // Handle the HTTP error
                Log.e("API Error", "HttpException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonDataException) {
                // Handle the JSON parsing error
                Log.e("API Error", "JsonDataException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: JsonEncodingException) {
                // Handle the JSON encoding error
                Log.e("API Error", "JsonEncodingException: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            } catch (e: Exception) {
                // Handle any other exceptions
                Log.e("API Error", "Exception: ${e.message}")
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }
}


class HomeAssistantViewModel(var repository: HomeAssistantRepository) : ViewModel() {
    private val _statesLiveData = MutableLiveData<List<EntityState>>()
    val statesLiveData: LiveData<List<EntityState>> = _statesLiveData

    fun loadStates() {
        viewModelScope.launch {
            repository.getStates { states ->
                if (!states.isNullOrEmpty()) {
                    _statesLiveData.postValue(states!!)
                }
            }
        }
    }

    fun callService(domain: String, service: String, data: RequestBody) {
        viewModelScope.launch {
            repository.callService(domain, service, data) { success ->
                // Handle the result
            }
        }
    }

    fun toggleEntity(entityId: String) {
        viewModelScope.launch {
            repository.toggleEntity(
                domain = entityId.split(".").first(), data = ToggleEntity(entityId)
            ) { states ->
                if (!states.isNullOrEmpty()) {
                    // Merge into the existing state
                    val cur = _statesLiveData.value!!.toMutableList()
                    states.forEach { state ->
                        _statesLiveData.value?.indexOfFirst { it.entityId == state.entityId }?.let {
                            cur[it] = state
                        }
                    }
                    _statesLiveData.postValue(cur)
                }
            }
            // Optimistic update
            _statesLiveData.value?.indexOfFirst { it.entityId == entityId }?.let {
                val cur = _statesLiveData.value!!.toMutableList()
                cur[it] = cur[it].copy(
                    state = if (cur[it].state == "off") "on" else "off"
                )
                _statesLiveData.postValue(cur)
            }
        }
    }
}

class HomeAssistantViewModelFactory(private val repository: HomeAssistantRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeAssistantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return HomeAssistantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DevicesViewModel(var repository: HomeAssistantRepository) : ViewModel() {
    private val _devicesLiveData = MutableLiveData<List<Device>>()
    val devicesLiveData: LiveData<List<Device>> = _devicesLiveData

    fun loadDevices() {
        viewModelScope.launch {
            repository.getDevices { devices ->
                if (!devices.isNullOrEmpty()) {
                    _devicesLiveData.postValue(devices!!)
                }
            }
        }
    }
}

class DevicesViewModelFactory(private val repository: HomeAssistantRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DevicesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return DevicesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class TodoViewModel(private val repository: HomeAssistantRepository) : ViewModel() {
    // Map todoId to items
    private val _itemsLiveData = MutableLiveData<Map<String, List<TodoItem>>>()
    val itemsLiveData: LiveData<Map<String, List<TodoItem>>> = _itemsLiveData

    private val _loading = mutableIntStateOf(0)
    val loading by derivedStateOf { _loading.intValue > 0 }

    fun loadItems(todoId: String) {
        _loading.intValue++
        viewModelScope.launch {
            repository.getItems(todoId) { items ->
                if (!items.isNullOrEmpty()) {
                    val cur = _itemsLiveData.value?.toMutableMap() ?: mutableMapOf()
                    cur[todoId] = items
                    _itemsLiveData.postValue(cur)
                }
                _loading.intValue--
            }
        }
    }

    fun addItem(
        todoId: String,
        item: String,
        dueDate: Date? = null,
        dueDateTime: Date? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            val data = AddTodoItemData(
                entityId = todoId,
                item = item,
                dueDate = dueDate,
                dueDateTime = dueDateTime,
                description = description
            )
            repository.addItem(data) { success ->
                loadItems(todoId)
            }
            // Optimistic update
            val cur = _itemsLiveData.value?.toMutableMap() ?: mutableMapOf()
            cur[todoId] = cur[todoId].orEmpty() + TodoItem(
                uid = "",
                summary = item,
                status = "needs_action",
                due = dueDate,
                description = description
            )
            _itemsLiveData.postValue(cur)
        }
    }

    fun updateItem(
        todoId: String,
        item: String,
        rename: String? = null,
        status: String,
        dueDate: Date? = null,
        dueDateTime: Date? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            val data = UpdateTodoItemData(
                entityId = todoId,
                item = item,
                rename = rename,
                status = status,
                dueDate = dueDate,
                dueDateTime = dueDateTime,
                description = description
            )
            repository.updateItem(data) { success ->
                loadItems(todoId)
            }
            // Optimistic update
            val cur = _itemsLiveData.value?.toMutableMap() ?: mutableMapOf()
            cur[todoId] = cur[todoId].orEmpty().map {
                if (it.summary == item) {
                    it.copy(
                        summary = rename ?: it.summary,
                        status = status,
                        due = dueDate,
                        description = description
                    )
                } else {
                    it
                }
            }
            _itemsLiveData.postValue(cur)
        }
    }

    fun removeItem(
        todoId: String, item: String
    ) {
        viewModelScope.launch {
            val data = RemoveTodoItemData(
                entityId = todoId, item = item
            )
            repository.removeItem(data) { success ->
                loadItems(todoId)
            }
            // Optimistic update
            val cur = _itemsLiveData.value?.toMutableMap() ?: mutableMapOf()
            cur[todoId] = cur[todoId].orEmpty().filter { it.summary != item }
            _itemsLiveData.postValue(cur)
        }
    }
}

class TodoViewModelFactory(private val repository: HomeAssistantRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class AssistMessage(
    val content: String,
    val isUserMessage: Boolean = false,
    val isError: Boolean = false,
    val data: ConversationData? = null,
    val agent: EntityState? = null
)

class ConversationViewModel(
    private val repository: HomeAssistantRepository,
    private val deviceId: String?,
    private val onResponse: (String, Boolean) -> Unit
) : ViewModel() {
    private val TAG = "ConversationViewModel"

    private var conversation: String? = null

    private val _messages = mutableStateListOf<AssistMessage>()
    val messages: List<AssistMessage>
        get() = _messages

    private var _responding by mutableIntStateOf(0)
    val responding by derivedStateOf { _responding > 0 }

    fun sendMessage(content: String, voice: Boolean, agent: EntityState) {
        _messages.add(AssistMessage(content, true))
        _responding++
        repository.processConversation(
            agent.entityId, ConversationInput(
                text = content,
                conversationId = conversation,
                deviceId = deviceId,
                language = "en-GB"
            )
        ) { result ->
            if (result != null) {
                conversation = result.conversationId
                when (result.response.responseType) {
                    ConversationResponseType.ACTION_DONE, ConversationResponseType.QUERY_ANSWER, null -> {
                        _messages.add(
                            AssistMessage(
                                result.response.speech.plain.speech,
                                false,
                                data = result.response.data,
                                agent = agent
                            )
                        )
                    }

                    ConversationResponseType.ERROR -> {
                        _messages.add(
                            AssistMessage(
                                result.response.speech.plain.speech,
                                false,
                                isError = true,
                                data = result.response.data,
                                agent = agent
                            )
                        )
                    }
                }
                _responding--
                onResponse(result.response.speech.plain.speech, voice)
            } else {
                _messages.add(
                    AssistMessage(
                        "An unknown error occurred.", isError = true
                    )
                )
                _responding--
            }
        }

        Log.d(TAG, "Sent message: $content")
    }
}

class ConversationViewModelFactory(
    private val repository: HomeAssistantRepository,
    private val deviceId: String?,
    private val onResponse: (String, Boolean) -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ConversationViewModel(
                repository, deviceId, onResponse
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
