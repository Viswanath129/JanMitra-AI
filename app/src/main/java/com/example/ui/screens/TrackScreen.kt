package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CitizenReport
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    viewModel: JanMitraViewModel,
    initialSearchId: String = "",
    onNavigateToMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    var searchIdInput by remember { mutableStateOf(initialSearchId) }
    var selectedReportForDetails by remember { mutableStateOf<CitizenReport?>(null) }

    // Sync initial search if passed
    LaunchedEffect(initialSearchId, reports) {
        if (initialSearchId.isNotEmpty() && reports.isNotEmpty()) {
            searchIdInput = initialSearchId
            val found = reports.firstOrNull { it.issueId.lowercase() == initialSearchId.lowercase() }
            if (found != null) {
                selectedReportForDetails = found
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Match Clean Slate-50 background from design style
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { 
                    if (selectedReportForDetails != null) {
                        selectedReportForDetails = null
                        searchIdInput = ""
                    } else {
                        onNavigateToMap()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Track Progress",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        if (selectedReportForDetails != null) {
            val report = selectedReportForDetails!!
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Metadata Header Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Report ID: ${report.issueId}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.description,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 26.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Format Timestamp
                    val dateStr = try {
                        val sdf = SimpleDateFormat("d MMM yyyy • hh:mm a", Locale.getDefault())
                        sdf.format(Date(report.timestamp))
                    } catch (e: Exception) {
                        "6 Jul 2026 • 09:15 AM"
                    }

                    Text(
                        text = "Reported on $dateStr",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Timeline List Area
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    val currentStatusIndex = getStatusIndex(report.status)

                    // Phase 1: Reported
                    item {
                        TimelineStepCard(
                            title = "Reported",
                            time = formatOffsetTime(report.timestamp, 0),
                            desc = "Your report has been submitted successfully.",
                            icon = Icons.Rounded.Edit,
                            iconColor = Color(0xFF2563EB), // Primary Blue
                            bgColor = Color(0xFFEFF6FF),
                            isActive = currentStatusIndex >= 1,
                            isLineActive = currentStatusIndex > 1
                        )
                    }

                    // Phase 2: AI Analyzed
                    item {
                        val urgencyText = report.urgency.uppercase()
                        TimelineStepCard(
                            title = "AI Analyzed",
                            time = formatOffsetTime(report.timestamp, 47 * 60000), // ~47 min later
                            desc = "AI classified the issue and analyzed the gap. Priority: $urgencyText",
                            icon = Icons.Rounded.AutoAwesome,
                            iconColor = Color(0xFF0D9488), // Beautiful Teal
                            bgColor = Color(0xFFF0FDF4),
                            isActive = currentStatusIndex >= 2,
                            isLineActive = currentStatusIndex > 2
                        )
                    }

                    // Phase 3: Approved
                    item {
                        TimelineStepCard(
                            title = "Approved",
                            time = formatOffsetTime(report.timestamp, 26 * 3600000), // ~26 hours later
                            desc = "Approved by MP. Budget recommendation finalized.",
                            icon = Icons.Rounded.CheckCircle,
                            iconColor = Color(0xFFD97706), // Amber/Orange
                            bgColor = Color(0xFFFFFBEB),
                            isActive = currentStatusIndex >= 3,
                            isLineActive = currentStatusIndex > 3
                        )
                    }

                    // Phase 4: Work Started
                    item {
                        TimelineStepCard(
                            title = "Work Started",
                            time = formatOffsetTime(report.timestamp, 72 * 3600000), // ~3 days later
                            desc = "Work order issued to Engineering Department.",
                            icon = Icons.Rounded.Engineering,
                            iconColor = Color(0xFF6D28D9), // Violet/Purple
                            bgColor = Color(0xFFF5F3FF),
                            isActive = currentStatusIndex >= 4,
                            isLineActive = currentStatusIndex > 4
                        )
                    }

                    // Phase 5: Completed
                    item {
                        TimelineStepCard(
                            title = "Completed",
                            time = formatOffsetTime(report.timestamp, 216 * 3600000), // ~9 days later
                            desc = "Work completed successfully. Thank you!",
                            icon = Icons.Rounded.TaskAlt,
                            iconColor = Color(0xFF059669), // Emerald Green
                            bgColor = Color(0xFFECFDF5),
                            isActive = currentStatusIndex >= 5,
                            isLineActive = false,
                            isLast = true
                        )
                    }
                }

                // "View on Map" Full Width Button
                Button(
                    onClick = { onNavigateToMap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "View on Map",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            // General Track Search Landing Screen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchIdInput,
                    onValueChange = { input ->
                        searchIdInput = input
                        val found = reports.firstOrNull { it.issueId.lowercase() == input.trim().lowercase() }
                        if (found != null) {
                            selectedReportForDetails = found
                        }
                    },
                    placeholder = { Text("Enter Issue ID (e.g. JM-2026-0001)", fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
                    trailingIcon = {
                        if (searchIdInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchIdInput = ""
                                selectedReportForDetails = null
                            }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1D4ED8),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = Color(0xFF1D4ED8),
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                Text(
                    text = "Public Transparency Board",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    val filteredList = reports.filter {
                        searchIdInput.isEmpty() || it.issueId.contains(searchIdInput, ignoreCase = true)
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No records match your search ID.",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(filteredList) { report ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedReportForDetails = report
                                        searchIdInput = report.issueId
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = report.description,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "ID: ${report.issueId} • ${report.locationName.removeSuffix(" Village")}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    StatusBadge(status = report.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStepCard(
    title: String,
    time: String,
    desc: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    isActive: Boolean,
    isLineActive: Boolean,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left timeline node with active/inactive connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Circle Node
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isActive) iconColor else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Connector Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(60.dp)
                        .background(if (isLineActive) iconColor else Color(0xFFE2E8F0))
                )
            }
        }

        // Timeline Step Info Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isActive) Color.White else Color(0xFFF8FAFC)),
            border = BorderStroke(
                width = 1.dp,
                color = if (isActive) Color(0xFFE2E8F0) else Color(0xFFF1F5F9)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF1E293B) else Color(0xFF94A3B8)
                    )
                    Text(
                        text = time,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = if (isActive) Color(0xFF475569) else Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Helpers
private fun getStatusIndex(status: String): Int {
    return when (status) {
        "Reported" -> 1
        "Analyzed" -> 2
        "Approved" -> 3
        "In Progress" -> 4
        "Completed" -> 5
        else -> 1
    }
}

private fun formatOffsetTime(baseTime: Long, offsetMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(baseTime + offsetMs))
}
