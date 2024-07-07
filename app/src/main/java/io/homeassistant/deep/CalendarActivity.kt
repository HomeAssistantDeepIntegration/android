package io.homeassistant.deep

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Color
import android.icu.util.TimeZone
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import io.homeassistant.deep.ui.theme.AppTheme

class CalendarActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Calendar") }, navigationIcon = {
                            IconButton(onClick = {
                                finish()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        })
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        RequiresPermission(permission = Manifest.permission.READ_CALENDAR) { isReadCalendarGranted, requestReadCalendarPermission ->
                            RequiresPermission(permission = Manifest.permission.WRITE_CALENDAR) { isWriteCalendarGranted, requestWriteCalendarPermission ->
                                if (isReadCalendarGranted && isWriteCalendarGranted) {
                                    val calendarViewModel = CalendarViewModel()
                                    CalendarScreen(calendarViewModel)
                                }
                                if (!isWriteCalendarGranted) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Write Calendar permission required for this feature to be available. Please grant the permission.")
                                        Button(onClick = { requestWriteCalendarPermission() }) {
                                            Text("Request permission")
                                        }
                                    }
                                }
                                if (!isReadCalendarGranted) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Read Calendar permission required for this feature to be available. Please grant the permission.")
                                        Button(onClick = { requestReadCalendarPermission() }) {
                                            Text("Request permission")
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

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = CalendarViewModel()) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { viewModel.addCustomCalendar(contentResolver) }) {
            Text(text = "Add Custom Calendar")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.insertEventToCustomCalendar(contentResolver) }) {
            Text(text = "Insert Event")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.queryEventsFromCustomCalendar(contentResolver) }) {
            Text(text = "Query Events")
        }
        Spacer(modifier = Modifier.height(16.dp))
        EventList(events = viewModel.events)
    }
}

@Composable
fun EventList(events: List<Event>) {
    LazyColumn {
        items(events) { event ->
            EventItem(event = event)
        }
    }
}

@Composable
fun EventItem(event: Event) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "Title: ${event.title}")
        Text(text = "Start: ${event.dtStart}")
        Text(text = "End: ${event.dtEnd}")
    }
}

data class Event(val id: Long, val title: String, val dtStart: Long, val dtEnd: Long, val description: String?)

class CalendarViewModel : ViewModel() {
    private val _events = mutableStateListOf<Event>()
    val events: List<Event> get() = _events

    fun addCustomCalendar(contentResolver: ContentResolver) {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "HomeAssistant")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "Custom Calendar")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "My Custom Calendar")
            put(CalendarContract.Calendars.CALENDAR_COLOR, Color.BLUE)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "custom_account_name")
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }

        val uri = asSyncAdapter(
            CalendarContract.Calendars.CONTENT_URI,
            "custom_account_name",
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        val newUri = contentResolver.insert(uri, values)
        if (newUri != null) {
            Log.d("CustomCalendar", "Custom calendar added: $newUri")
        } else {
            Log.e("CustomCalendar", "Failed to add custom calendar")
        }
    }

    private fun asSyncAdapter(uri: Uri, account: String, accountType: String): Uri {
        return uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType).build()
    }

    fun insertEventToCustomCalendar(contentResolver: ContentResolver) {
        val calendarId = getCalendarId(contentResolver) ?: return

        val values = ContentValues().apply {
            put(
                CalendarContract.Events.DTSTART, System.currentTimeMillis() + 60 * 60 * 1000
            ) // Event start time
            put(
                CalendarContract.Events.DTEND, System.currentTimeMillis() + 2 * 60 * 60 * 1000
            ) // Event end time
            put(CalendarContract.Events.TITLE, "Sample Event")
            put(CalendarContract.Events.DESCRIPTION, "Description of the event")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri != null) {
            Log.d("CustomCalendarEvent", "Event inserted with URI: $uri")
        } else {
            Log.e("CustomCalendarEvent", "Failed to insert event")
        }
    }

    fun queryEventsFromCustomCalendar(contentResolver: ContentResolver) {
        val calendarId = getCalendarId(contentResolver) ?: return

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, null
        )

        _events.clear()
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val eventID = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID))
                    val title =
                        cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE))
                    val dtStart =
                        cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART))
                    val dtEnd = cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTEND))

                    _events.add(Event(eventID, title, dtStart, dtEnd, null))

                } while (cursor.moveToNext())
            }
        }
    }

    private fun getCalendarId(contentResolver: ContentResolver): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection =
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf("custom_account_name", CalendarContract.ACCOUNT_TYPE_LOCAL)

        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, selection, selectionArgs, null
        )

        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(CalendarContract.Calendars._ID))
            }
        }
        return null
    }
}
