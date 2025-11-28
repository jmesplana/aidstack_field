package com.example.myapplication

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import java.io.File

class OfflineMapManager(private val context: Context) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var cacheManager: CacheManager? = null

    init {
        val mapView = MapView(context)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        cacheManager = CacheManager(mapView)
    }

    // Predefined regions for quick selection
    companion object {
        val PREDEFINED_REGIONS = mapOf(
            "Jamaica" to Region(18.6, 17.6, -76.1, -78.6, "Jamaica"),
            "Haiti" to Region(20.1, 18.0, -71.6, -74.5, "Haiti"),
            "Dominican Republic" to Region(19.9, 17.5, -68.3, -72.0, "Dominican Republic"),
            "Puerto Rico" to Region(18.5, 17.9, -65.6, -67.3, "Puerto Rico"),
            "Cuba" to Region(23.3, 19.8, -74.1, -84.9, "Cuba"),
            "Philippines" to Region(19.5, 4.6, 126.6, 116.9, "Philippines"),
            "Indonesia" to Region(6.0, -11.0, 141.0, 95.0, "Indonesia"),
            "Bangladesh" to Region(26.6, 20.7, 92.7, 88.0, "Bangladesh"),
            "Nepal" to Region(30.4, 26.3, 88.2, 80.0, "Nepal"),
            "Myanmar" to Region(28.5, 9.8, 101.2, 92.2, "Myanmar")
        )

        // Zoom level ranges
        const val MIN_ZOOM_LEVEL = 6
        const val DEFAULT_MAX_ZOOM = 14  // Good balance
        const val MAX_ZOOM_LEVEL = 16     // More detail, larger size
    }

    /**
     * Download map tiles for a predefined region
     */
    suspend fun downloadRegion(
        regionName: String,
        maxZoom: Int = DEFAULT_MAX_ZOOM
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val region = PREDEFINED_REGIONS[regionName]
            ?: return@withContext Result.failure(IllegalArgumentException("Region not found"))

        downloadCustomRegion(
            north = region.north,
            south = region.south,
            east = region.east,
            west = region.west,
            maxZoom = maxZoom
        )
    }

    /**
     * Download map tiles for a custom bounding box
     */
    suspend fun downloadCustomRegion(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        maxZoom: Int = DEFAULT_MAX_ZOOM
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Preparing

            val boundingBox = BoundingBox(north, east, south, west)

            cacheManager?.let { manager ->
                // Calculate total tiles
                val tileCount = manager.possibleTilesInArea(
                    boundingBox,
                    MIN_ZOOM_LEVEL,
                    maxZoom.coerceIn(MIN_ZOOM_LEVEL, MAX_ZOOM_LEVEL)
                )

                _downloadState.value = DownloadState.Downloading(0, tileCount)

                // Download tiles
                manager.downloadAreaAsync(
                    context,
                    boundingBox,
                    MIN_ZOOM_LEVEL,
                    maxZoom,
                    object : CacheManager.CacheManagerCallback {
                        override fun onTaskComplete() {
                            CoroutineScope(Dispatchers.Main).launch {
                                _downloadState.value = DownloadState.Completed
                            }
                        }

                        override fun onTaskFailed(errors: Int) {
                            CoroutineScope(Dispatchers.Main).launch {
                                _downloadState.value = DownloadState.Error("Download failed with $errors errors")
                            }
                        }

                        override fun updateProgress(
                            progress: Int,
                            currentZoomLevel: Int,
                            zoomMin: Int,
                            zoomMax: Int
                        ) {
                            CoroutineScope(Dispatchers.Main).launch {
                                _downloadState.value = DownloadState.Downloading(progress, tileCount)
                            }
                        }

                        override fun downloadStarted() {
                            CoroutineScope(Dispatchers.Main).launch {
                                _downloadState.value = DownloadState.Downloading(0, tileCount)
                            }
                        }

                        override fun setPossibleTilesInArea(total: Int) {
                            // Total tiles calculated
                        }
                    }
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Download map tiles for current visible area + buffer
     */
    suspend fun downloadCurrentArea(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 50.0,
        maxZoom: Int = DEFAULT_MAX_ZOOM
    ): Result<Unit> {
        // Calculate bounding box based on center and radius
        val latDelta = radiusKm / 111.0  // 1 degree latitude ≈ 111 km
        val lonDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(centerLat)))

        return downloadCustomRegion(
            north = centerLat + latDelta,
            south = centerLat - latDelta,
            east = centerLon + lonDelta,
            west = centerLon - lonDelta,
            maxZoom = maxZoom
        )
    }

    /**
     * Get current cache size in bytes
     */
    fun getCacheSize(): Long {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        return calculateDirectorySize(cacheDir)
    }

    /**
     * Clear all cached tiles
     */
    fun clearCache() {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        cacheDir.deleteRecursively()
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Check if any tiles are cached
     */
    fun isCacheAvailable(): Boolean {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        return cacheDir.exists() && cacheDir.listFiles()?.isNotEmpty() == true
    }

    /**
     * Format file size for display
     */
    fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    /**
     * Estimate download size for a region
     */
    fun estimateDownloadSize(
        regionName: String,
        maxZoom: Int = DEFAULT_MAX_ZOOM
    ): String {
        val region = PREDEFINED_REGIONS[regionName] ?: return "Unknown"

        val boundingBox = BoundingBox(region.north, region.east, region.south, region.west)
        val tileCount = cacheManager?.possibleTilesInArea(
            boundingBox,
            MIN_ZOOM_LEVEL,
            maxZoom
        ) ?: 0

        // Average tile size is about 15-20 KB
        val estimatedBytes = tileCount * 17000L
        return formatCacheSize(estimatedBytes)
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size: Long = 0
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    fun cancelDownload() {
        // Note: OSMDroid's CacheManager doesn't have a built-in cancel method
        // This is a limitation we should document
        _downloadState.value = DownloadState.Idle
    }
}

/**
 * Represents a geographic region
 */
data class Region(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val name: String
)

/**
 * Download state for offline maps
 */
sealed class DownloadState {
    object Idle : DownloadState()
    object Preparing : DownloadState()
    data class Downloading(val progress: Int, val total: Int) : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}
