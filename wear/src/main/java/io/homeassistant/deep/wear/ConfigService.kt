package io.homeassistant.deep.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class ConfigService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        Log.d("ConfigService", "Received data event")
        for (event in dataEvents) {
            Log.d("ConfigService", "Received data event: ${event.type} ${event.dataItem.uri} ${event.dataItem.data?.toString()}")
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/config") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val url = dataMapItem.dataMap.getString("url")
                val authToken = dataMapItem.dataMap.getString("auth_token")
                val assistPipeline = dataMapItem.dataMap.getString("assist_pipeline")

                // Save to SharedPreferences
                val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("url", url)
                    putString("auth_token", authToken)
                    putString("assist_pipeline", assistPipeline)
                    apply()
                }
            }
        }
    }
}