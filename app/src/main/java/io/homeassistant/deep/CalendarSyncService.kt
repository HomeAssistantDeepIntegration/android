package io.homeassistant.deep

import android.app.Service
import android.content.Intent
import android.os.IBinder

class CalendarSyncService : Service() {
    private lateinit var syncAdapter: CalendarSyncAdapter

    override fun onCreate() {
        synchronized(this) {
            if (!::syncAdapter.isInitialized) {
                syncAdapter = CalendarSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return syncAdapter.syncAdapterBinder
    }
}
