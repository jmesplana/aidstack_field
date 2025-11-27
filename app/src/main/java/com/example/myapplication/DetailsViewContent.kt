package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetailsViewContent(locationData: LocationData, useMetric: Boolean) {
    if (locationData.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Waiting for GPS signal...")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Coordinates Card
            DetailCard(title = "Coordinates") {
                DetailRow(
                    icon = Icons.Default.Place,
                    label = "Latitude",
                    value = "%.6f°".format(locationData.latitude)
                )
                DetailRow(
                    icon = Icons.Default.Place,
                    label = "Longitude",
                    value = "%.6f°".format(locationData.longitude)
                )
            }

            // Altitude Card
            DetailCard(title = "Elevation") {
                DetailRow(
                    icon = Icons.Default.Terrain,
                    label = "Altitude",
                    value = if (useMetric)
                        "%.2f meters".format(locationData.altitude)
                    else
                        "%.2f feet".format(locationData.altitude * 3.281f)
                )
            }

            // Movement Card
            DetailCard(title = "Movement") {
                DetailRow(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = if (useMetric)
                        "%.2f km/h".format(locationData.speed * 3.6f)
                    else
                        "%.2f mph".format(locationData.speed * 2.237f)
                )
                DetailRow(
                    icon = Icons.Default.Navigation,
                    label = "Bearing",
                    value = "%.1f° (%s)".format(
                        locationData.bearing,
                        locationData.getBearingDirection()
                    )
                )
            }

            // Accuracy Card
            DetailCard(title = "Accuracy") {
                DetailRow(
                    icon = Icons.Default.GpsFixed,
                    label = "Precision",
                    value = if (useMetric)
                        "%.2f m (%s)".format(
                            locationData.accuracy,
                            locationData.getAccuracyQuality()
                        )
                    else
                        "%.2f ft (%s)".format(
                            locationData.accuracy * 3.281f,
                            locationData.getAccuracyQuality()
                        )
                )
            }

            // Timestamp Card
            DetailCard(title = "Last Update") {
                DetailRow(
                    icon = Icons.Default.AccessTime,
                    label = "Time",
                    value = locationData.getFormattedTime()
                )
                DetailRow(
                    icon = Icons.Default.DateRange,
                    label = "Date",
                    value = locationData.getFormattedDate()
                )
            }
        }
    }
}
