package io.homeassistant.deep

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.request.CachePolicy

class DeepApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        setupCoil()
    }

    private fun setupCoil() {
        // Create Coil ImageLoader with caching enabled
        val imageLoader = ImageLoader.Builder(applicationContext)
            .memoryCachePolicy(CachePolicy.ENABLED) // Enable memory caching
            .diskCachePolicy(CachePolicy.ENABLED)   // Enable disk caching
            // .crossfade(true) // Optional: Enable image crossfade
            .build()

        // Set Coil ImageLoader as the global instance
        Coil.setImageLoader(imageLoader)
    }
}