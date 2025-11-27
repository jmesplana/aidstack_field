package com.example.myapplication.gdacs

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GdacsResponse(
    @Json(name = "features")
    val features: List<DisasterFeature>
)

@JsonClass(generateAdapter = true)
data class DisasterFeature(
    @Json(name = "properties")
    val properties: DisasterProperties,
    @Json(name = "geometry")
    val geometry: DisasterGeometry
)

@JsonClass(generateAdapter = true)
data class DisasterProperties(
    @Json(name = "eventid")
    val eventId: String?,
    @Json(name = "eventtype")
    val eventType: String?,
    @Json(name = "eventname")
    val eventName: String?,
    @Json(name = "alertlevel")
    val alertLevel: String?,
    @Json(name = "alertscore")
    val alertScore: Double?,
    @Json(name = "episodealertlevel")
    val episodeAlertLevel: String?,
    @Json(name = "episodealertscore")
    val episodeAlertScore: Double?,
    @Json(name = "country")
    val country: String?,
    @Json(name = "fromdate")
    val fromDate: String?,
    @Json(name = "todate")
    val toDate: String?,
    @Json(name = "description")
    val description: String?,
    @Json(name = "htmldescription")
    val htmlDescription: String?
    // url field removed - it comes as an object in GDACS API
)

@JsonClass(generateAdapter = true)
data class DisasterGeometry(
    @Json(name = "type")
    val type: String?,
    @Json(name = "coordinates")
    val coordinates: List<Any> // Can be List<Double> for Point or nested List for Polygon
)

data class Disaster(
    val id: String,
    val type: DisasterType,
    val name: String,
    val severity: DisasterSeverity,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val date: String,
    val description: String,
    val url: String,
    val alertScore: Double,
    val geometryType: String?,
    val affectedAreaCoordinates: List<List<List<Double>>>? // For Polygon geometry
) {
    fun getDistanceFrom(latitude: Double, longitude: Double): Double {
        // Haversine formula for distance calculation
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(this.latitude - latitude)
        val dLon = Math.toRadians(this.longitude - longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(this.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    fun getGdacsUrl(): String {
        // Build GDACS event URL
        val eventType = when (type) {
            DisasterType.EARTHQUAKE -> "EQ"
            DisasterType.TROPICAL_CYCLONE -> "TC"
            DisasterType.FLOOD -> "FL"
            DisasterType.VOLCANO -> "VO"
            DisasterType.DROUGHT -> "DR"
            DisasterType.WILDFIRE -> "WF"
            DisasterType.TSUNAMI -> "TS"
            else -> "EQ"
        }
        return "https://www.gdacs.org/report.aspx?eventid=$id&eventtype=$eventType"
    }
}

enum class DisasterType(val displayName: String, val icon: String) {
    EARTHQUAKE("Earthquake", "🌍"),
    TSUNAMI("Tsunami", "🌊"),
    TROPICAL_CYCLONE("Tropical Cyclone", "🌀"),
    FLOOD("Flood", "💧"),
    VOLCANO("Volcano", "🌋"),
    WILDFIRE("Wildfire", "🔥"),
    DROUGHT("Drought", "☀️"),
    UNKNOWN("Unknown", "⚠️");

    companion object {
        fun fromString(type: String): DisasterType {
            return when (type.uppercase()) {
                "EQ" -> EARTHQUAKE
                "TC" -> TROPICAL_CYCLONE
                "FL" -> FLOOD
                "VO" -> VOLCANO
                "DR" -> DROUGHT
                "WF" -> WILDFIRE
                "TS" -> TSUNAMI
                else -> UNKNOWN
            }
        }
    }
}

enum class DisasterSeverity(val color: Long, val displayName: String) {
    GREEN(0xFF4CAF50, "Minor"),
    ORANGE(0xFFFF9800, "Moderate"),
    RED(0xFFF44336, "Severe"),
    UNKNOWN(0xFF9E9E9E, "Unknown");

    companion object {
        fun fromString(level: String): DisasterSeverity {
            return when (level.uppercase()) {
                "GREEN" -> GREEN
                "ORANGE" -> ORANGE
                "RED" -> RED
                else -> UNKNOWN
            }
        }
    }
}

fun DisasterFeature.toDisaster(): Disaster {
    // Parse coordinates based on geometry type
    var lat = 0.0
    var lon = 0.0
    var affectedArea: List<List<List<Double>>>? = null

    when (geometry.type?.uppercase()) {
        "POINT" -> {
            // Point geometry: [longitude, latitude]
            if (geometry.coordinates.size >= 2) {
                lon = (geometry.coordinates[0] as? Double) ?: 0.0
                lat = (geometry.coordinates[1] as? Double) ?: 0.0
            }
        }
        "POLYGON" -> {
            // Polygon geometry: [[[lon, lat], [lon, lat], ...]]
            try {
                @Suppress("UNCHECKED_CAST")
                val polygonCoords = geometry.coordinates as? List<List<List<Double>>>
                affectedArea = polygonCoords

                // Use first point as center
                polygonCoords?.firstOrNull()?.firstOrNull()?.let { firstPoint ->
                    if (firstPoint.size >= 2) {
                        lon = firstPoint[0]
                        lat = firstPoint[1]
                    }
                }
            } catch (e: Exception) {
                // Fallback to 0,0
            }
        }
        else -> {
            // Try to parse as point
            if (geometry.coordinates.size >= 2) {
                try {
                    lon = (geometry.coordinates[0] as? Double) ?: 0.0
                    lat = (geometry.coordinates[1] as? Double) ?: 0.0
                } catch (e: Exception) {
                    // Keep as 0,0
                }
            }
        }
    }

    return Disaster(
        id = properties.eventId ?: "unknown",
        type = DisasterType.fromString(properties.eventType ?: ""),
        name = properties.eventName ?: "Unknown Event",
        severity = DisasterSeverity.fromString(properties.alertLevel ?: ""),
        latitude = lat,
        longitude = lon,
        country = properties.country ?: "Unknown",
        date = properties.fromDate ?: "",
        description = properties.description ?: "No description available",
        url = "", // URL not used - GDACS returns it as an object
        alertScore = properties.alertScore ?: 0.0,
        geometryType = geometry.type,
        affectedAreaCoordinates = affectedArea
    )
}
