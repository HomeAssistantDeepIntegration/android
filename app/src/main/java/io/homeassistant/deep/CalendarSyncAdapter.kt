package io.homeassistant.deep

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import java.util.TimeZone

class CalendarSyncAdapter(
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean = false
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?
    ) {
        // Synchronize data between Home Assistant and the custom calendar here
        Log.d("CalendarSyncAdapter", "Performing sync")


        val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val url =
            sharedPreferences.getString(
                "url", ""
            ) ?: ""
        val token =
            sharedPreferences.getString(
                "auth_token", ""
            ) ?: ""
        val repository = HomeAssistantRepository(url, token)

        repository.getEvents("calendar.joseph_s_diary") { events ->
            if (events != null) {
                for (event in events) {
                    Log.d("CalendarSyncAdapter", "Event: $event")

                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, event.start)
                        put(CalendarContract.Events.DTEND, event.end)
                        put(CalendarContract.Events.TITLE, event.summary)
                        put(CalendarContract.Events.DESCRIPTION, event.description)
                        put(CalendarContract.Events.CALENDAR_ID, getCustomCalendarId())
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                        put(CalendarContract.Events.EVENT_LOCATION, event.location)
                    }
                    context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                }
            }
        }
    }

    private fun getCustomCalendarId(): Long {
        // Retrieve the ID of your custom calendar
        return 1L
    }
}
