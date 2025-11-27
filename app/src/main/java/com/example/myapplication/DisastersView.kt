package com.example.myapplication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DisastersView(
    disasterState: DisasterState,
    userLat: Double,
    userLon: Double,
    useMetric: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Global Disaster Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${disasterState.disasters.size}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Active", fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${disasterState.nearbyDisasters.size}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (disasterState.nearbyDisasters.isNotEmpty())
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("Nearby", fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Refresh Button
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !disasterState.isLoading
        ) {
            if (disasterState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (disasterState.isLoading) "Refreshing..." else "Refresh Data")
        }

        // Error message
        if (disasterState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    disasterState.error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nearby Disasters (if any)
        if (disasterState.nearbyDisasters.isNotEmpty()) {
            Text(
                "⚠️ Disasters Near You (within 500km)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            disasterState.nearbyDisasters.forEach { disaster ->
                DisasterCard(
                    disaster = disaster,
                    userLat = userLat,
                    userLon = userLon,
                    useMetric = useMetric,
                    isNearby = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // All Disasters
        Text(
            "All Active Disasters",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (disasterState.disasters.isEmpty() && !disasterState.isLoading) {
            Text(
                "No active disasters reported",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            disasterState.disasters.sortedByDescending { it.alertScore }.forEach { disaster ->
                DisasterCard(
                    disaster = disaster,
                    userLat = userLat,
                    userLon = userLon,
                    useMetric = useMetric,
                    isNearby = false
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DisasterCard(
    disaster: com.example.myapplication.gdacs.Disaster,
    userLat: Double,
    userLon: Double,
    useMetric: Boolean,
    isNearby: Boolean
) {
    val context = LocalContext.current
    val distance = disaster.getDistanceFrom(userLat, userLon)
    val distanceText = if (useMetric) {
        "%.0f km away".format(distance)
    } else {
        "%.0f mi away".format(distance * 0.621371)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Open GDACS website for this disaster
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(disaster.getGdacsUrl()))
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isNearby)
                androidx.compose.ui.graphics.Color(disaster.severity.color).copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        disaster.type.icon,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            disaster.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        Text(
                            disaster.type.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = androidx.compose.ui.graphics.Color(disaster.severity.color)
                ) {
                    Text(
                        disaster.severity.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    disaster.country,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    distanceText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (disaster.description.isNotEmpty() && disaster.description != "No description available") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    disaster.description,
                    fontSize = 13.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
