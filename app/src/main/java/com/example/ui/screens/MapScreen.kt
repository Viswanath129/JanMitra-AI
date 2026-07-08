package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CitizenReport
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel

@Composable
fun MapScreen(
    viewModel: JanMitraViewModel,
    onNavigateToTrack: (reportId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    val stats by viewModel.allVillageStats.collectAsStateWithLifecycle()

    var mapMode by remember { mutableStateOf("Satellite") } // Satellite, Terrain, Default
    var selectedLayer by remember { mutableStateOf("Heatmap") } // Heatmap, Boundaries, Gaps

    // State for selected issue from map click
    var selectedReportOnMap by remember { mutableStateOf<CitizenReport?>(null) }

    // Map coordinates mock container sizes
    var mapWidth by remember { mutableStateOf(1f) }
    var mapHeight by remember { mutableStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // Map Toolbar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode selector (Satellite vs Standard)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                listOf("Satellite", "Terrain").forEach { mode ->
                    val isActive = mapMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) PrimaryBlue else Color.Transparent)
                            .clickable { mapMode = mode }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else SecondaryText
                        )
                    }
                }
            }

            // Layer Selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                listOf("Heatmap", "Boundaries", "Gaps").forEach { layer ->
                    val isActive = selectedLayer == layer
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) SecondaryTeal else Color.Transparent)
                            .clickable { selectedLayer = layer }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = layer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else SecondaryText
                        )
                    }
                }
            }
        }

        // Live GIS Map Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, DividerGray, RoundedCornerShape(20.dp))
        ) {
            // Map Draw Canvas
            val mapBaseColor = if (mapMode == "Satellite") Color(0xFF0F172A) else Color(0xFFE2E8F0)
            
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(mapBaseColor)
                    .pointerInput(reports) {
                        detectTapGestures { offset ->
                            // Look for clicked nodes
                            val width = size.width
                            val height = size.height
                            
                            val clicked = reports.firstOrNull { report ->
                                // Project reports locations onto canvas relative offsets
                                val xOffsetMultiplier = ((report.locationLatitude - 28.5) * 5.0).coerceIn(0.1, 0.9)
                                val yOffsetMultiplier = ((report.locationLongitude - 77.1) * 5.0).coerceIn(0.1, 0.9)
                                
                                val reportX = (xOffsetMultiplier * width).toFloat()
                                val reportY = (yOffsetMultiplier * height).toFloat()
                                
                                val distance = Math.sqrt(
                                    Math.pow((offset.x - reportX).toDouble(), 2.0) +
                                    Math.pow((offset.y - reportY).toDouble(), 2.0)
                                )
                                distance < 35.0 // Click radius threshold
                            }
                            selectedReportOnMap = clicked
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                // Draw heat brushes if Heatmap is enabled
                if (selectedLayer == "Heatmap") {
                    // Draw soft dynamic circles representing regional cluster heat
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(DangerRed.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(width * 0.45f, height * 0.4f),
                            radius = width * 0.35f
                        ),
                        radius = width * 0.35f,
                        center = Offset(width * 0.45f, height * 0.4f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(WarningOrange.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(width * 0.7f, height * 0.65f),
                            radius = width * 0.25f
                        ),
                        radius = width * 0.25f,
                        center = Offset(width * 0.7f, height * 0.65f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryBlue.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(width * 0.25f, height * 0.7f),
                            radius = width * 0.28f
                        ),
                        radius = width * 0.28f,
                        center = Offset(width * 0.25f, height * 0.7f)
                    )
                }

                // Draw mock village boundary vectors if Boundaries is enabled
                if (selectedLayer == "Boundaries") {
                    val boundaryColor = if (mapMode == "Satellite") Color.White.copy(alpha = 0.4f) else Color.DarkGray.copy(alpha = 0.4f)
                    
                    // Bhola Village polygon
                    drawRect(
                        color = boundaryColor,
                        topLeft = Offset(width * 0.1f, height * 0.15f),
                        size = androidx.compose.ui.geometry.Size(width * 0.35f, height * 0.35f),
                        style = Stroke(width = 2f)
                    )
                    // Rampur Village polygon
                    drawCircle(
                        color = boundaryColor,
                        center = Offset(width * 0.65f, height * 0.35f),
                        radius = width * 0.2f,
                        style = Stroke(width = 2f)
                    )
                    // Seva Village polygon
                    drawCircle(
                        color = boundaryColor,
                        center = Offset(width * 0.4f, height * 0.75f),
                        radius = width * 0.22f,
                        style = Stroke(width = 2f)
                    )
                }

                // Draw Citizen issue coordinate points as beautiful cluster dots
                reports.forEach { report ->
                    val xMultiplier = ((report.locationLatitude - 28.5) * 5.0).coerceIn(0.1, 0.9)
                    val yMultiplier = ((report.locationLongitude - 77.1) * 5.0).coerceIn(0.1, 0.9)
                    
                    val pxX = (xMultiplier * width).toFloat()
                    val pxY = (yMultiplier * height).toFloat()

                    val dotColor = when (report.category) {
                        "Water" -> PrimaryBlue
                        "Road" -> SecondaryTeal
                        "School" -> AccentAmber
                        "Women Safety" -> DangerRed
                        else -> WarningOrange
                    }

                    // Outer glowing transparency
                    drawCircle(
                        color = dotColor.copy(alpha = 0.35f),
                        radius = 20f,
                        center = Offset(pxX, pxY)
                    )
                    // Mid circle
                    drawCircle(
                        color = dotColor.copy(alpha = 0.7f),
                        radius = 12f,
                        center = Offset(pxX, pxY)
                    )
                    // Center solid core
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = Offset(pxX, pxY)
                    )
                }
            }

            // Floating Map Compass & Legend Indicator
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Legend", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                LegendItem(label = "Water Deficits", color = PrimaryBlue)
                LegendItem(label = "Road Damage", color = SecondaryTeal)
                LegendItem(label = "School Upgrades", color = AccentAmber)
                LegendItem(label = "Safety Concerns", color = DangerRed)
            }

            // Quick instruction badge at bottom
            if (selectedReportOnMap == null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap any highlighted cluster nodes on the map",
                            fontSize = 12.sp,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Expanded Issue details drawer from map selection
        selectedReportOnMap?.let { report ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DividerGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = report.category,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Text(
                                    text = report.locationName,
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )
                            }
                        }
                        
                        IconButton(onClick = { selectedReportOnMap = null }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Detail"
                            )
                        }
                    }

                    Text(
                        text = report.description,
                        fontSize = 13.sp,
                        color = PrimaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityBadge(score = report.priorityScore)
                        StatusBadge(status = report.status)
                    }

                    Button(
                        onClick = { onNavigateToTrack(report.issueId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Track Development Journey", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium
        )
    }
}
