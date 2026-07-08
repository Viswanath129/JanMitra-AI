package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CitizenReport
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JanMitraViewModel,
    onNavigateToReport: (voiceFirst: Boolean) -> Unit,
    onNavigateToTrack: (reportId: String) -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val drafts = reports.filter { it.status == "Draft" }
    val submittedReports = reports.filter { it.status != "Draft" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Clean Slate-50 background
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Voice-First Reporting Card (Mockup Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToReport(true) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Golden Circle with Yellow/Gold Mic
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED)) // Very light amber
                            .border(1.5.dp, Color(0xFFFBBF24), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B)), // Amber-500
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Voice Reporting",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice-First Reporting",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Tap the mic and speak about your issue in any local language",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Navigate",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. What would you like to report? Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What would you like to report?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "View all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.clickable { onNavigateToMap() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryCircleItem(
                        title = "Water Issue",
                        icon = Icons.Rounded.WaterDrop,
                        color = Color(0xFF2563EB), // Blue
                        onClick = {
                            viewModel.reportCategory = "Water"
                            onNavigateToReport(false)
                        }
                    )
                    CategoryCircleItem(
                        title = "Road / Drainage",
                        icon = Icons.Rounded.AddRoad,
                        color = Color(0xFF16A34A), // Green
                        onClick = {
                            viewModel.reportCategory = "Road"
                            onNavigateToReport(false)
                        }
                    )
                    CategoryCircleItem(
                        title = "Education",
                        icon = Icons.Rounded.School,
                        color = Color(0xFFEA580C), // Orange
                        onClick = {
                            viewModel.reportCategory = "School"
                            onNavigateToReport(false)
                        }
                    )
                    CategoryCircleItem(
                        title = "More",
                        icon = Icons.Rounded.MoreHoriz,
                        color = Color(0xFF7C3AED), // Purple
                        onClick = {
                            viewModel.reportCategory = "Other"
                            onNavigateToReport(false)
                        }
                    )
                }
            }

            // Saved Drafts Section
            if (drafts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Saved Drafts (${drafts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    
                    drafts.forEach { draft ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.loadDraft(draft) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // Warm Amber tint
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = "Draft",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draft.description.ifEmpty { "[No description yet]" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Category: ${draft.category} • Location: ${draft.locationName}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Button(
                                    onClick = { viewModel.loadDraft(draft) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Resume", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. AI Decision Insight Card with Native glowing Heatmap
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMap() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep Slate Navy
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "AI Decision Insight",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Water supply issues in your area are high. 112 similar reports found.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { onNavigateToMap() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("View Details", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Native glowing heatmap graphic
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw some abstract green network/landmass paths
                            val pathColor = Color(0xFF0F172A)
                            drawCircle(color = pathColor, radius = 45f, center = Offset(x = size.width * 0.3f, y = size.height * 0.4f))
                            drawCircle(color = pathColor, radius = 55f, center = Offset(x = size.width * 0.7f, y = size.height * 0.6f))

                            // Draw glowing heatmap circles with radial gradients
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x99EF4444), Color(0x33EF4444), Color.Transparent),
                                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                                    radius = 70f
                                ),
                                radius = 70f,
                                center = Offset(size.width * 0.5f, size.height * 0.4f)
                            )

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x88F59E0B), Color(0x22F59E0B), Color.Transparent),
                                    center = Offset(size.width * 0.35f, size.height * 0.6f),
                                    radius = 50f
                                ),
                                radius = 50f,
                                center = Offset(size.width * 0.35f, size.height * 0.6f)
                            )

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x7710B981), Color(0x1110B981), Color.Transparent),
                                    center = Offset(size.width * 0.7f, size.height * 0.3f),
                                    radius = 60f
                                ),
                                radius = 60f,
                                center = Offset(size.width * 0.7f, size.height * 0.3f)
                            )
                        }
                    }
                }
            }

            // 4. Recent Reports Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "View all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.clickable { onNavigateToMap() }
                    )
                }

                if (submittedReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                    }
                } else {
                    submittedReports.take(4).forEach { report ->
                        PolishedIssueCard(
                            report = report,
                            onClick = { onNavigateToTrack(report.issueId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCircleItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PolishedIssueCard(
    report: CitizenReport,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with Circle Backdrop
            val icon = when (report.category) {
                "Water" -> Icons.Rounded.WaterDrop
                "Road" -> Icons.Rounded.AddRoad
                "School" -> Icons.Rounded.School
                "Hospital" -> Icons.Rounded.LocalHospital
                "Women Safety" -> Icons.Rounded.Security
                else -> Icons.Rounded.Campaign
            }
            val backdropColor = when (report.category) {
                "Water" -> Color(0xFFEFF6FF)
                "Road" -> Color(0xFFF0FDF4)
                "School" -> Color(0xFFFFF7ED)
                else -> Color(0xFFFAF5FF)
            }
            val iconTint = when (report.category) {
                "Water" -> Color(0xFF2563EB)
                "Road" -> Color(0xFF16A34A)
                "School" -> Color(0xFFEA580C)
                else -> Color(0xFF7C3AED)
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(backdropColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Report info text
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = report.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${report.locationName.removeSuffix(" Village")}, Andhra Pradesh",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Reported on 6 Jul 2026 • ID: ${report.issueId}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Badge on the Right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFCCFBF1)) // Light Teal
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI Analyzed",
                    color = Color(0xFF0F766E), // Dark Teal
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
