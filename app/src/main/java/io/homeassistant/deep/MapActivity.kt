package io.homeassistant.deep

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.size.Size
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import io.homeassistant.deep.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MapActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

            val url = sharedPreferences.getString(
                "url", ""
            )
            val token = sharedPreferences.getString(
                "auth_token", ""
            )

            if (url.isNullOrEmpty() || token.isNullOrEmpty()) {
                // Navigate to the configuration activity
                startActivity(
                    Intent(
                        this@MapActivity, ConfigActivity::class.java
                    )
                )
            } else {
                val repository = HomeAssistantRepository(url, token)
                val viewModel: HomeAssistantViewModel = viewModel(
                    factory = HomeAssistantViewModelFactory(repository)
                )

                AppTheme {
                    val states by viewModel.statesLiveData.observeAsState()
                    LaunchedEffect(Unit) {
                        viewModel.loadStates()
                    }

                    Scaffold(topBar = {
                        TopAppBar(title = { Text("Map") }, navigationIcon = {
                            IconButton(onClick = {
                                finish()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }, actions = {
                            IconButton(onClick = {
                                viewModel.loadStates()
                            }) {
                                Icon(Icons.Filled.Refresh, "Refresh")
                            }
                        })
                    }, content = { padding ->
                        if (states.isNullOrEmpty()) {
                            Column(modifier = Modifier.padding(padding)) {
                                Loading("")
                            }
                        } else {
                            Map(url, states)
                        }
                    })
                }
            }
        }
    }
}


@Composable
fun Map(
    url: String,
    states: List<EntityState>?,
    modifier: Modifier = Modifier,
    focusedState: EntityState? = null
) {
    val currentStates by rememberUpdatedState(states)
    val focusedStateState by remember {
        derivedStateOf { focusedState?.let { currentStates!!.find { it.entityId == focusedState.entityId } } }
    }

    val coroutineScope = rememberCoroutineScope()

    fun MapView.updateMarkers() {
        val personLayer = mutableListOf<Marker>()
        val deviceTrackerLayer = mutableListOf<Marker>()
        val zoneLayer = mutableListOf<Marker>()

        currentStates!!.forEach { state ->
            if (state.entityId.startsWith("person.")) {
                val latitude = state.attributes["latitude"] as Double?
                val longitude = state.attributes["longitude"] as Double?
                val name = state.attributes["friendly_name"] as String?

                if (latitude != null && longitude != null && (latitude != .0 || longitude != .0)) {
                    val marker = Marker(this)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.image = IconicsDrawable(context, getStateIcon(state))
                    marker.icon = BitmapDrawable(
                        context.resources,
                        IconicsDrawable(context, getStateIcon(state)).toBitmap()
                    )
                    val image = state.attributes["entity_picture"] as String?
                    if (image != null) {
                        coroutineScope.launch {
                            marker.icon = getDrawableFromUrl(context, "$url$image", marker.icon, size = Size(48.dp.Int(context), 48.dp.Int(context)), circleCrop = true)
                        }
                    }
                    //                                                    marker.icon = AppCompatResources.getDrawable(context, org.osmdroid.shape.R.drawable.person)
                    marker.title = name
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    personLayer.add(marker)
                }
            } else if (state.entityId.startsWith("device_tracker.") && state.attributes.containsKey(
                    "icon"
                )
            ) {
                val latitude = state.attributes["latitude"] as Double?
                val longitude = state.attributes["longitude"] as Double?
                val name = state.attributes["friendly_name"] as String?

                if (latitude != null && longitude != null && (latitude != .0 || longitude != .0)) {
                    val marker = Marker(this)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.image = IconicsDrawable(context, getStateIcon(state))
                    marker.icon = BitmapDrawable(
                        context.resources, IconicsDrawable(
                            context, getStateIcon(state)
                        ).apply {
                            colorInt = Color.RED
                        }.toBitmap()
                    )
                    //                                                        marker.icon =
                    //                                                            AppCompatResources.getDrawable(
                    //                                                                context,
                    //                                                                org.osmdroid.shape.R.drawable.marker_default
                    //                                                            )
                    marker.title = name
                    marker.setAnchor(
                        Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER
                    )
                    deviceTrackerLayer.add(marker)
                }
            } else if (state.entityId.startsWith("zone.")) {
                val latitude = state.attributes["latitude"] as Double
                val longitude = state.attributes["longitude"] as Double
                val name = state.attributes["friendly_name"] as String?

                if (latitude != .0 || longitude != .0) {
                    val marker = Marker(this)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.image = IconicsDrawable(context, getStateIcon(state))
                    marker.icon = BitmapDrawable(
                        context.resources, IconicsDrawable(
                            context, getStateIcon(state)
                        ).apply { colorInt = Color.GRAY }.toBitmap()
                    )
                    //                                                    marker.icon =
                    //                                                        AppCompatResources.getDrawable(
                    //                                                            context,
                    //                                                            org.osmdroid.shape.R.drawable.osm_ic_center_map
                    //                                                        )
                    marker.title = name
                    marker.setAnchor(
                        Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER
                    )
                    zoneLayer.add(marker)
                }
            }
        }

        overlays.clear()
        overlays.addAll(zoneLayer)
        val iFocusedStateState = focusedStateState
        if (iFocusedStateState != null) {
            if (iFocusedStateState.entityId.startsWith("person.")) {
                val latitude = iFocusedStateState.attributes["latitude"] as Double?
                val longitude = iFocusedStateState.attributes["longitude"] as Double?
                val name = iFocusedStateState.attributes["friendly_name"] as String?

                if (latitude != null && longitude != null && (latitude != .0 || longitude != .0)) {
                    val marker = Marker(this)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.image =
                        IconicsDrawable(context, getStateIcon(iFocusedStateState))
                    marker.icon = BitmapDrawable(
                        context.resources,
                        IconicsDrawable(
                            context,
                            getStateIcon(iFocusedStateState)
                        ).toBitmap()
                    )
                    val image = iFocusedStateState.attributes["entity_picture"] as String?
                    if (image != null) {
                        coroutineScope.launch {
                            marker.icon = getDrawableFromUrl(context, "$url$image", marker.icon, size = Size(48.dp.Int(context), 48.dp.Int(context)), circleCrop = true)
                        }
                    }
                    //                                                    marker.icon = AppCompatResources.getDrawable(context, org.osmdroid.shape.R.drawable.person)
                    marker.title = name
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    overlays.add(marker)
                    controller.animateTo(marker.position, 18.0, 1)
                }
            } else if (iFocusedStateState.entityId.startsWith("device_tracker.") && iFocusedStateState.attributes.containsKey(
                    "icon"
                )
            ) {
                val latitude = iFocusedStateState.attributes["latitude"] as Double?
                val longitude = iFocusedStateState.attributes["longitude"] as Double?
                val name = iFocusedStateState.attributes["friendly_name"] as String?

                if (latitude != null && longitude != null && (latitude != .0 || longitude != .0)) {
                    val marker = Marker(this)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.image = IconicsDrawable(context, getStateIcon(iFocusedStateState))
                    marker.icon = BitmapDrawable(
                        context.resources, IconicsDrawable(
                            context, getStateIcon(iFocusedStateState)
                        ).apply {
                            colorInt = Color.RED
                        }.toBitmap()
                    )
                    //                                                        marker.icon =
                    //                                                            AppCompatResources.getDrawable(
                    //                                                                context,
                    //                                                                org.osmdroid.shape.R.drawable.marker_default
                    //                                                            )
                    marker.title = name
                    marker.setAnchor(
                        Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER
                    )
                    overlays.add(marker)
                    controller.animateTo(marker.position, 18.0, 1)
                }
            }
        } else {
            overlays.addAll(deviceTrackerLayer)
            overlays.addAll(personLayer)
            val boundingBox =
                BoundingBox.fromGeoPoints(/* deviceTrackerLayer.map { it.position } + */ personLayer.map { it.position })
            controller.animateTo(boundingBox.centerWithDateLine)
            controller.zoomToSpan(
                boundingBox.latitudeSpan * 1.5,
                boundingBox.longitudeSpanWithDateLine * 1.5
            )
        }

        invalidate()
    }

    AndroidView(modifier = modifier.fillMaxSize(), factory = { context ->
        MapView(context).apply {
            Configuration.getInstance().userAgentValue = "HomeAssistantDeepIntegration"
            setTileSource(TileSourceFactory.MAPNIK)
            minZoomLevel = 1.0
            setMultiTouchControls(true)

            updateMarkers()
        }
    }, update = { mapView ->
        mapView.apply {
            updateMarkers()
        }
    })
}
