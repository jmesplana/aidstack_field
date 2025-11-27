package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.gdacs.Disaster
import com.example.myapplication.gdacs.DisasterSeverity
import com.example.myapplication.fieldreport.FieldReport
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow

class DisasterInfoWindow(mapView: MapView, private val disaster: Disaster, private val context: Context) :
    InfoWindow(R.layout.disaster_info_window, mapView) {

    override fun onOpen(item: Any?) {
        val titleView = mView.findViewById<TextView>(R.id.disaster_title)
        val severityView = mView.findViewById<TextView>(R.id.disaster_severity)
        val countryView = mView.findViewById<TextView>(R.id.disaster_country)
        val descriptionView = mView.findViewById<TextView>(R.id.disaster_description)
        val moreInfoLink = mView.findViewById<TextView>(R.id.more_info_link)

        titleView.text = "${disaster.type.icon} ${disaster.name}"

        // Set severity with color
        severityView.text = disaster.severity.displayName
        val severityColor = when (disaster.severity) {
            DisasterSeverity.RED -> android.graphics.Color.parseColor("#F44336")
            DisasterSeverity.ORANGE -> android.graphics.Color.parseColor("#FF9800")
            DisasterSeverity.GREEN -> android.graphics.Color.parseColor("#4CAF50")
            else -> android.graphics.Color.parseColor("#9E9E9E")
        }
        severityView.setTextColor(severityColor)

        countryView.text = "📍 ${disaster.country}"

        // Show description if available
        if (disaster.description.isNotBlank() && disaster.description != "No description available") {
            descriptionView.text = disaster.description
            descriptionView.visibility = View.VISIBLE
        } else {
            descriptionView.visibility = View.GONE
        }

        // Set up click listener for "More info" link
        moreInfoLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(disaster.getGdacsUrl()))
            context.startActivity(intent)
        }
    }

    override fun onClose() {
        // Cleanup if needed
    }
}

class FieldReportInfoWindow(mapView: MapView, private val report: FieldReport, private val context: Context) :
    InfoWindow(R.layout.field_report_info_window, mapView) {

    override fun onOpen(item: Any?) {
        val titleView = mView.findViewById<TextView>(R.id.report_title)
        val severityView = mView.findViewById<TextView>(R.id.report_severity)
        val categoryView = mView.findViewById<TextView>(R.id.report_category)
        val locationView = mView.findViewById<TextView>(R.id.report_location)
        val descriptionView = mView.findViewById<TextView>(R.id.report_description)
        val timestampView = mView.findViewById<TextView>(R.id.report_timestamp)
        val photoCountView = mView.findViewById<TextView>(R.id.photo_count)
        val navigateButton = mView.findViewById<Button>(R.id.navigate_button)

        titleView.text = "${report.category.icon} ${report.title}"

        // Set severity with color
        severityView.text = report.severity.displayName
        severityView.setTextColor(report.severity.color.toInt())

        categoryView.text = report.category.displayName

        locationView.text = "📍 %.6f°, %.6f°".format(report.latitude, report.longitude)

        // Show description
        if (report.description.isNotBlank()) {
            descriptionView.text = report.description
            descriptionView.visibility = View.VISIBLE
        } else {
            descriptionView.visibility = View.GONE
        }

        // Show timestamp
        val timeAgo = getTimeAgo(report.timestamp)
        timestampView.text = "🕐 $timeAgo"

        // Show photo count
        if (report.photoUris.isNotEmpty()) {
            photoCountView.text = "📷 ${report.photoUris.size} photo${if (report.photoUris.size > 1) "s" else ""}"
            photoCountView.visibility = View.VISIBLE
        } else {
            photoCountView.visibility = View.GONE
        }

        // Set up navigate button click listener
        navigateButton.setOnClickListener {
            // Create geo URI for navigation
            val geoUri = Uri.parse("google.navigation:q=${report.latitude},${report.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            // Try Google Maps first, fall back to generic map intent
            try {
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                // Fallback to generic geo intent
                val fallbackUri = Uri.parse("geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${report.title})")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                try {
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    // Could show a toast here if needed
                }
            }
        }
    }

    override fun onClose() {
        // Cleanup if needed
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}

@Composable
fun MapViewComposable(
    latitude: Double,
    longitude: Double,
    disasters: List<Disaster>,
    fieldReports: List<FieldReport> = emptyList(),
    modifier: Modifier = Modifier,
    recenterTrigger: Int = 0,
    targetLatitude: Double? = null,
    targetLongitude: Double? = null,
    navigationTrigger: Int = 0
) {
    val context = LocalContext.current
    var currentMapView by remember { mutableStateOf<MapView?>(null) }
    var isFirstLocation by remember { mutableStateOf(true) }
    var lastRecenterTrigger by remember { mutableStateOf(0) }
    var lastNavigationTrigger by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(6.0)
                minZoomLevel = 2.0
                maxZoomLevel = 20.0

                // Prevent vertical scrolling beyond map bounds
                isVerticalMapRepetitionEnabled = false
                isHorizontalMapRepetitionEnabled = true
                setScrollableAreaLimitDouble(null)

                // Request parent to not intercept touch events when user is interacting with map
                setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }

                // Close info windows when map is tapped
                overlays.add(org.osmdroid.views.overlay.MapEventsOverlay(object : org.osmdroid.events.MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        // Close all info windows when tapping on empty map space
                        overlays.forEach { overlay ->
                            if (overlay is Marker) {
                                overlay.closeInfoWindow()
                            }
                        }
                        return false
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        return false
                    }
                }))

                currentMapView = this
            }
        },
        update = { mapView ->
            currentMapView = mapView

            // Handle navigation to specific coordinates
            if (navigationTrigger != lastNavigationTrigger && targetLatitude != null && targetLongitude != null) {
                mapView.controller.setZoom(15.0) // Zoom in closer for specific location
                mapView.controller.animateTo(GeoPoint(targetLatitude, targetLongitude))
                lastNavigationTrigger = navigationTrigger
            }

            // Handle recenter button click
            if (recenterTrigger != lastRecenterTrigger && latitude != 0.0 && longitude != 0.0) {
                mapView.controller.setZoom(12.0) // Reset to default zoom
                mapView.controller.animateTo(GeoPoint(latitude, longitude))
                lastRecenterTrigger = recenterTrigger
            }

            // Remove old markers
            mapView.overlays.clear()

            // Add user location marker
            if (latitude != 0.0 && longitude != 0.0) {
                val userLocation = GeoPoint(latitude, longitude)

                // Only center on first location update
                if (isFirstLocation) {
                    mapView.controller.setCenter(userLocation)
                    isFirstLocation = false
                }

                val userMarker = Marker(mapView).apply {
                    position = userLocation
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You are here"
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
                }
                mapView.overlays.add(userMarker)
            }

            // Add disaster markers and affected areas
            disasters.forEach { disaster ->
                // Add affected area polygon if available
                disaster.affectedAreaCoordinates?.firstOrNull()?.let { ring ->
                    if (ring.isNotEmpty()) {
                        val polygon = Polygon(mapView).apply {
                            points = ring.map { point ->
                                GeoPoint(point.getOrNull(1) ?: 0.0, point.getOrNull(0) ?: 0.0)
                            }

                            // Style based on severity
                            val baseColor = when (disaster.severity) {
                                DisasterSeverity.RED -> android.graphics.Color.parseColor("#F44336")
                                DisasterSeverity.ORANGE -> android.graphics.Color.parseColor("#FF9800")
                                DisasterSeverity.GREEN -> android.graphics.Color.parseColor("#4CAF50")
                                else -> android.graphics.Color.parseColor("#9E9E9E")
                            }

                            fillPaint.color = android.graphics.Color.argb(50,
                                android.graphics.Color.red(baseColor),
                                android.graphics.Color.green(baseColor),
                                android.graphics.Color.blue(baseColor)
                            )
                            outlinePaint.color = baseColor
                            outlinePaint.strokeWidth = 3f

                            title = "Affected area: ${disaster.name}"
                        }
                        mapView.overlays.add(polygon)
                    }
                }

                // Add marker at disaster center
                val disasterMarker = Marker(mapView).apply {
                    position = GeoPoint(disaster.latitude, disaster.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "${disaster.type.icon} ${disaster.name}"
                    snippet = "${disaster.severity.displayName} - ${disaster.country}"

                    // Use custom marker with disaster icon and severity color
                    icon = MarkerUtils.createDisasterMarker(
                        context,
                        disaster.type,
                        disaster.severity
                    )

                    // Set custom info window
                    infoWindow = DisasterInfoWindow(mapView, disaster, context)

                    // Handle marker clicks - toggle info window
                    setOnMarkerClickListener { marker, _ ->
                        if (marker.isInfoWindowShown) {
                            // If already open, close it
                            marker.closeInfoWindow()
                        } else {
                            // Close all other info windows first
                            mapView.overlays.forEach { overlay ->
                                if (overlay is Marker && overlay != marker) {
                                    overlay.closeInfoWindow()
                                }
                            }
                            // Then open this one
                            marker.showInfoWindow()
                        }
                        true
                    }
                }
                mapView.overlays.add(disasterMarker)
            }

            // Add field report markers
            fieldReports.forEach { report ->
                val reportMarker = Marker(mapView).apply {
                    position = GeoPoint(report.latitude, report.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "${report.category.icon} ${report.title}"
                    snippet = "${report.severity.displayName} - ${report.category.displayName}"

                    // Use custom marker for field reports
                    icon = MarkerUtils.createFieldReportMarker(
                        context,
                        report.category,
                        report.severity
                    )

                    // Set custom info window
                    infoWindow = FieldReportInfoWindow(mapView, report, context)

                    // Handle marker clicks - toggle info window
                    setOnMarkerClickListener { marker, _ ->
                        if (marker.isInfoWindowShown) {
                            // If already open, close it
                            marker.closeInfoWindow()
                        } else {
                            // Close all other info windows first
                            mapView.overlays.forEach { overlay ->
                                if (overlay is Marker && overlay != marker) {
                                    overlay.closeInfoWindow()
                                }
                            }
                            // Then open this one
                            marker.showInfoWindow()
                        }
                        true
                    }
                }
                mapView.overlays.add(reportMarker)
            }

            mapView.invalidate()
        }
    )
}
