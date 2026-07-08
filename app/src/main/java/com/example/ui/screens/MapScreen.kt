package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.sqrt

@Composable
fun MapScreen(
    viewModel: JanMitraViewModel,
    onNavigateToTrack: (reportId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    val stats by viewModel.allVillageStats.collectAsStateWithLifecycle()

    var mapMode by remember { mutableStateOf("Satellite") } // Satellite, Terrain, Normal
    var selectedLayer by remember { mutableStateOf("Heatmap") } // Heatmap, Boundaries, Gaps
    
    // Live Filters state
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedUrgencyFilter by remember { mutableStateOf("All") } // All, High (>70), Med (40-70), Low (<40)

    // State for selected issue from map click
    var selectedReportOnMap by remember { mutableStateOf<CitizenReport?>(null) }
    
    // Tap to calculate distance coordinates
    var userCustomPinLat by remember { mutableStateOf<Double?>(null) }
    var userCustomPinLon by remember { mutableStateOf<Double?>(null) }

    // Map coordinates mock center
    val centerLat = 28.6139
    val centerLon = 77.2090

    // Filter reports based on live user selections
    val filteredReports = reports.filter { report ->
        val categoryMatches = selectedCategoryFilter == "All" || report.category.equals(selectedCategoryFilter, ignoreCase = true)
        val urgencyMatches = when (selectedUrgencyFilter) {
            "High Priority" -> report.priorityScore >= 70.0
            "Medium" -> report.priorityScore >= 40.0 && report.priorityScore < 70.0
            "Low" -> report.priorityScore < 40.0
            else -> true
        }
        categoryMatches && urgencyMatches && report.status != "Draft"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // --- 1. Map Options & Type Selector Toolbar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Map Type Buttons
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                listOf("Satellite", "Terrain", "Normal").forEach { mode ->
                    val isActive = mapMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { mapMode = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Overlays Layer Selection
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                listOf("Heatmap", "Boundaries").forEach { layer ->
                    val isActive = selectedLayer == layer
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) Color(0xFF0D9488) else Color.Transparent)
                            .clickable { selectedLayer = layer }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = layer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // --- 2. Live Filters Bar (Category & Urgency level) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 10.dp)
        ) {
            // Category filter row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filterCats = listOf("All", "Water", "Road", "School", "Hospital", "Electricity", "Safety")
                items(filterCats) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (cat == "All") "All Categories" else cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Urgency Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val urgencies = listOf("All", "High Priority", "Medium", "Low")
                items(urgencies) { urg ->
                    val isSelected = selectedUrgencyFilter == urg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFEF4444) else Color(0xFFF1F5F9))
                            .clickable { selectedUrgencyFilter = urg }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = urg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // --- 3. Live GIS Interactive Map ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
        ) {
            val mapBaseColor = when (mapMode) {
                "Satellite" -> Color(0xFF0F172A)
                "Terrain" -> Color(0xFFFEF3C7).copy(alpha = 0.4f)
                else -> Color(0xFFF8FAFC)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(mapBaseColor)
                    .pointerInput(filteredReports) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val height = size.height

                            // 1. Detect if tapped close to any report dot
                            var tappedReport: CitizenReport? = null
                            for (report in filteredReports) {
                                val xMultiplier = ((report.locationLongitude - 77.1) * 5.0).coerceIn(0.1, 0.9)
                                val yMultiplier = (1.0 - (report.locationLatitude - 28.5) * 5.0).coerceIn(0.1, 0.9)
                                val rx = (xMultiplier * width).toFloat()
                                val ry = (yMultiplier * height).toFloat()

                                val dist = sqrt((offset.x - rx) * (offset.x - rx) + (offset.y - ry) * (offset.y - ry))
                                if (dist < 40f) {
                                    tappedReport = report
                                    break
                                }
                            }

                            if (tappedReport != null) {
                                selectedReportOnMap = tappedReport
                            } else {
                                // Drop custom distance calculator pin on coordinates
                                val rawX = offset.x / width
                                val rawY = offset.y / height
                                userCustomPinLon = 77.1 + (rawX * 0.2)
                                userCustomPinLat = 28.5 + ((1.0 - rawY) * 0.2)
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                // Draw heat grid overlay if Heatmap is enabled
                if (selectedLayer == "Heatmap") {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x55EF4444), Color.Transparent),
                            center = Offset(width * 0.4f, height * 0.35f),
                            radius = width * 0.45f
                        ),
                        radius = width * 0.45f,
                        center = Offset(width * 0.4f, height * 0.35f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x44FBBF24), Color.Transparent),
                            center = Offset(width * 0.72f, height * 0.62f),
                            radius = width * 0.3f
                        ),
                        radius = width * 0.3f,
                        center = Offset(width * 0.72f, height * 0.62f)
                    )
                }

                // Draw mock village administrative borders if Boundaries layer is active
                if (selectedLayer == "Boundaries") {
                    val strokeStyle = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    val boundaryColor = if (mapMode == "Satellite") Color.White.copy(alpha = 0.5f) else Color.DarkGray.copy(alpha = 0.5f)
                    
                    // Bhola Village sector
                    drawCircle(
                        color = boundaryColor,
                        center = Offset(width * 0.3f, height * 0.3f),
                        radius = width * 0.22f,
                        style = strokeStyle
                    )
                    // Rampur sector
                    drawCircle(
                        color = boundaryColor,
                        center = Offset(width * 0.7f, height * 0.45f),
                        radius = width * 0.25f,
                        style = strokeStyle
                    )
                }

                // Draw standard river paths
                drawLine(
                    color = Color(0x333B82F6),
                    start = Offset(0f, height * 0.2f),
                    end = Offset(width, height * 0.8f),
                    strokeWidth = 24f
                )

                // Render dynamic filtered issues pins
                // Group by location coordinates to draw clustered item counts (+3 items)
                val coordGroups = filteredReports.groupBy { String.format("%.3f,%.3f", it.locationLatitude, it.locationLongitude) }

                coordGroups.forEach { (_, groupReports) ->
                    val sample = groupReports.first()
                    val count = groupReports.size

                    val xMultiplier = ((sample.locationLongitude - 77.1) * 5.0).coerceIn(0.1, 0.9)
                    val yMultiplier = (1.0 - (sample.locationLatitude - 28.5) * 5.0).coerceIn(0.1, 0.9)
                    val pxX = (xMultiplier * width).toFloat()
                    val pxY = (yMultiplier * height).toFloat()

                    val pinColor = when (sample.category) {
                        "Water" -> Color(0xFF2563EB)
                        "Road" -> Color(0xFF16A34A)
                        "School" -> Color(0xFFEA580C)
                        "Women Safety" -> Color(0xFFD946EF)
                        else -> Color(0xFFEAB308)
                    }

                    // Pulse outer halo
                    drawCircle(
                        color = pinColor.copy(alpha = 0.25f),
                        radius = if (count > 1) 32f else 22f,
                        center = Offset(pxX, pxY)
                    )

                    // Core marker dot
                    drawCircle(
                        color = pinColor,
                        radius = if (count > 1) 16f else 11f,
                        center = Offset(pxX, pxY)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = if (count > 1) 6f else 4f,
                        center = Offset(pxX, pxY)
                    )
                }

                // Render User's custom distance calculator pin if active
                userCustomPinLat?.let { uLat ->
                    userCustomPinLon?.let { uLon ->
                        // Calculate canvas position
                        val uX = (((uLon - 77.1) / 0.2) * width).toFloat()
                        val uY = ((1.0 - (uLat - 28.5) / 0.2) * height).toFloat()

                        // Draw Custom Flag/Marker
                        drawCircle(Color(0xFFEF4444).copy(alpha = 0.3f), radius = 28f, center = Offset(uX, uY))
                        drawCircle(Color(0xFFEF4444), radius = 8f, center = Offset(uX, uY))

                        // Distance calculator - Find closest issue and draw a dashed distance calculation line
                        val closestIssue = filteredReports.minByOrNull {
                            val dy = it.locationLatitude - uLat
                            val dx = it.locationLongitude - uLon
                            dy * dy + dx * dx
                        }

                        closestIssue?.let { issue ->
                            val ixMultiplier = ((issue.locationLongitude - 77.1) * 5.0).coerceIn(0.1, 0.9)
                            val iyMultiplier = (1.0 - (issue.locationLatitude - 28.5) * 5.0).coerceIn(0.1, 0.9)
                            val idxX = (ixMultiplier * width).toFloat()
                            val idyY = (iyMultiplier * height).toFloat()

                            // Draw dashed distance geodesic vector line on map canvas!
                            drawLine(
                                color = Color(0xFFF59E0B),
                                start = Offset(uX, uY),
                                end = Offset(idxX, idyY),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        }
                    }
                }
            }

            // Legend Overlay Box
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("Map Legend", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                MapLegendItem(label = "Water Deficit", color = Color(0xFF2563EB))
                MapLegendItem(label = "Road repairs", color = Color(0xFF16A34A))
                MapLegendItem(label = "School Needs", color = Color(0xFFEA580C))
                MapLegendItem(label = "Women Safety", color = Color(0xFFD946EF))
            }

            // Interactive Distance Calculator Display Badge
            userCustomPinLat?.let { uLat ->
                userCustomPinLon?.let { uLon ->
                    val closestIssue = filteredReports.minByOrNull {
                        val dy = it.locationLatitude - uLat
                        val dx = it.locationLongitude - uLon
                        dy * dy + dx * dx
                    }

                    val kmDistance = closestIssue?.let {
                        val dy = it.locationLatitude - uLat
                        val dx = it.locationLongitude - uLon
                        sqrt(dy * dy + dx * dx) * 111.0 // 1 degree lat is approx 111 km
                    } ?: 1.84

                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.SpaceDashboard, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Text(
                                text = String.format("Calculated Distance: %.2f KM to closest issue", kmDistance),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                userCustomPinLat = null
                                userCustomPinLon = null
                            }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Quick instruction bar at bottom
            if (selectedReportOnMap == null && userCustomPinLat == null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Tap nodes to see details • Tap anywhere to calculate distances",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // --- 4. Slid-up Expandable Detail Drawer ---
        AnimatedVisibility(
            visible = selectedReportOnMap != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedReportOnMap?.let { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationSearching,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = report.category,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${report.locationName}, Andhra Pradesh",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            IconButton(onClick = { selectedReportOnMap = null }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close Detail")
                            }
                        }

                        Text(
                            text = report.description,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Track Development Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapLegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold
        )
    }
}
