@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.fieldreport

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ReportView(
    viewModel: FieldReportViewModel,
    currentLatitude: Double,
    currentLongitude: Double,
    onReportSubmitted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(ReportCategory.DAMAGE_ASSESSMENT) }
    var selectedSeverity by remember { mutableStateOf(ReportSeverity.MEDIUM) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSeverityDropdown by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled in camera launcher
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        photoUris = photoUris + uris
    }

    // Camera launcher
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            photoUris = photoUris + tempPhotoUri!!
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "Create Field Report",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A365D)
        )

        Text(
            "Document field observations and share critical information",
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Location Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF1A365D)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Current Location", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(
                        "%.6f°, %.6f°".format(currentLatitude, currentLongitude),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Category Selector
        Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        ExposedDropdownMenuBox(
            expanded = showCategoryDropdown,
            onExpandedChange = { showCategoryDropdown = it }
        ) {
            OutlinedTextField(
                value = "${selectedCategory.icon} ${selectedCategory.displayName}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors()
            )
            ExposedDropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                ReportCategory.values().forEach { category ->
                    DropdownMenuItem(
                        text = { Text("${category.icon} ${category.displayName}") },
                        onClick = {
                            selectedCategory = category
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }

        // Severity Selector
        Text("Severity", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        ExposedDropdownMenuBox(
            expanded = showSeverityDropdown,
            onExpandedChange = { showSeverityDropdown = it }
        ) {
            OutlinedTextField(
                value = selectedSeverity.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSeverityDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors()
            )
            ExposedDropdownMenu(
                expanded = showSeverityDropdown,
                onDismissRequest = { showSeverityDropdown = false }
            ) {
                ReportSeverity.values().forEach { severity ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            Color(severity.color),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(severity.displayName)
                            }
                        },
                        onClick = {
                            selectedSeverity = severity
                            showSeverityDropdown = false
                        }
                    )
                }
            }
        }

        // Title Input
        Text("Title", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Brief summary...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Description Input
        Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Detailed description of the situation...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        // Photos Section
        Text("Photos", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add from Camera button
            OutlinedButton(
                onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    // Create temp file for camera
                    val photoFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                    tempPhotoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    cameraLauncher.launch(tempPhotoUri!!)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Camera")
            }

            // Add from Gallery button
            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gallery")
            }
        }

        // Display selected photos
        if (photoUris.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photoUris) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { photoUris = photoUris.filter { it != uri } },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Submit Button
        Button(
            onClick = {
                if (title.isNotBlank()) {
                    viewModel.createReport(
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        category = selectedCategory,
                        severity = selectedSeverity,
                        title = title,
                        description = description,
                        photoUris = photoUris
                    )
                    // Reset form
                    title = ""
                    description = ""
                    photoUris = emptyList()
                    showSuccessDialog = true
                    onReportSubmitted?.invoke()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            )
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit Report", fontSize = 16.sp)
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
            title = { Text("Report Submitted") },
            text = { Text("Your field report has been saved locally and will be synced when connected.") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
