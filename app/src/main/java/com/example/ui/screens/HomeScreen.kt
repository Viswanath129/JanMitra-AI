package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CitizenReport
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

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
    val coroutineScope = rememberCoroutineScope()

    val drafts = reports.filter { it.status == "Draft" }
    val submittedReports = reports.filter { it.status != "Draft" }

    // State for interactive voice overlay
    var showVoiceOverlay by remember { mutableStateOf(false) }

    // Native language options
    val languages = listOf(
        "हिन्दी" to "Hindi",
        "తెలుగు" to "Telugu",
        "தமிழ்" to "Tamil",
        "ಕನ್ನಡ" to "Kannada",
        "മലയാളം" to "Malayalam",
        "বাংলা" to "Bengali",
        "मराठी" to "Marathi",
        "ગુજરાતી" to "Gujarati",
        "ਪੰਜਾਬੀ" to "Punjabi",
        "English" to "English"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Premium Slate background
    ) {
        // --- 1. Custom Responsive Header & Language Selector ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 18.dp)
            ) {
                // Branded row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "JanMitra AI",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Text(
                            text = "Direct Citizen-to-Government Link",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Network status / online pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "District Online",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Native Language Quick Selection row
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Preferred Language / भाषा / భాష",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(languages) { (nativeName, engName) ->
                            val isSelected = viewModel.userLanguage == engName
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF1E40AF) else Color(0xFFF1F5F9))
                                    .clickable { viewModel.updateUserLanguage(engName) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = nativeName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Main Content Column (Scrollable) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // --- 2. Voice-First Pulse Microphone Button (Central Action) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, DividerGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Voice-First Grievance Reporting",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Report any local issue instantly by speaking in your language",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Pulse Circles Microphone
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .clickable { showVoiceOverlay = true }
                    ) {
                        // Bouncing ring 1
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha))
                        )
                        // Bouncing ring 2
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale * 0.8f)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha * 1.2f))
                        )
                        
                        // Solid Center Button
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                    )
                                )
                                .border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Tap to Record Voice Report",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TAP TO REPORT WITH VOICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2563EB),
                        letterSpacing = 1.sp
                    )
                }
            }

            // --- 3. Offline Draft Recovery Panel ---
            if (drafts.isNotEmpty()) {
                val draft = drafts.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // Warm Amber tint
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = "Draft Icon",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unfinished Grievance Draft Found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = draft.description.ifEmpty { "Grievance details in progress..." },
                                fontSize = 12.sp,
                                color = Color(0xFFB45309),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.loadDraft(draft) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Resume", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { viewModel.deleteReport(draft.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Discard Draft",
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. Icon-First Categories Grid (Large Touch Targets) ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Grievance Categories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                val categoriesList = listOf(
                    CategoryInfo("Water Supply", Icons.Rounded.WaterDrop, PrimaryBlue, "Deficit or poor quality water"),
                    CategoryInfo("Roads & Transit", Icons.Rounded.AddRoad, SecondaryTeal, "Potholes, broken roads, blocks"),
                    CategoryInfo("School Quality", Icons.Rounded.School, SecondaryTeal, "School infra upgrades & facilities"),
                    CategoryInfo("Hospital Staff", Icons.Rounded.LocalHospital, DangerRed, "Medicines, staff, equipment"),
                    CategoryInfo("Power Lines", Icons.Rounded.ElectricBolt, AccentAmber, "Outages or dangling wires"),
                    CategoryInfo("Street Lights", Icons.Rounded.Lightbulb, SecondaryTeal, "Non-functional neighborhood lights"),
                    CategoryInfo("Women Safety", Icons.Rounded.Security, PrimaryBlue, "Safe corridors, patrols, lights"),
                    CategoryInfo("Agriculture", Icons.Rounded.Agriculture, SuccessGreen, "Irrigation, canal, seed needs"),
                    CategoryInfo("Other Request", Icons.Rounded.Campaign, SecondaryText, "Any other district support needs")
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp) // Large explicit height to avoid infinite scroll conflicts
                ) {
                    items(categoriesList) { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                                .clickable {
                                    viewModel.resetForm()
                                    viewModel.reportCategory = cat.name.split(" ")[0]
                                    onNavigateToReport(false)
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, DividerGray),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(cat.color.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = cat.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cat.desc,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        lineHeight = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. AI Insight Banner ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMap() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep Slate
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "AI Constituency Insights",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "A total of ${submittedReports.size} active development needs have been logged this week across 4 villages. Dynamic heatmap overlays are available.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Explore Live GIS Map →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x99EF4444), Color.Transparent),
                                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                                    radius = 80f
                                ),
                                radius = 80f,
                                center = Offset(size.width * 0.5f, size.height * 0.5f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.8f),
                                radius = 6f,
                                center = Offset(size.width * 0.5f, size.height * 0.5f)
                            )
                        }
                    }
                }
            }

            // --- 6. Recent Reports Section ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Grievance Submissions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "View Map",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable { onNavigateToMap() }
                    )
                }

                if (submittedReports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No reports submitted yet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }
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

    // --- Interactive Voice Overlay Dialog ---
    if (showVoiceOverlay) {
        VoiceCaptureSimulationOverlay(
            userLanguage = viewModel.userLanguage,
            onClose = { showVoiceOverlay = false },
            onSaveAndProceed = { text ->
                viewModel.resetForm()
                viewModel.reportDescription = text
                viewModel.isVoiceRecorded = true
                viewModel.attachedVideoUri = "mock_voice_recording.aac" // Save mock voice path
                viewModel.activeFormStep = 2 // Move directly to Location / Details!
                showVoiceOverlay = false
                onNavigateToReport(true)
            }
        )
    }
}

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val desc: String
)

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
            fontSize = 11.sp,
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, DividerGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = report.description,
                    fontSize = 14.sp,
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
                    text = "Report ID: ${report.issueId} • ${report.status}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Priority score badge
            PriorityBadge(score = report.priorityScore)
        }
    }
}

// --- VOICE RECORDING & WAVEFORM SIMULATION OVERLAY ---
@Composable
fun VoiceCaptureSimulationOverlay(
    userLanguage: String,
    onClose: () -> Unit,
    onSaveAndProceed: (String) -> Unit
) {
    var recordingDurationSec by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var detectedText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Mock transcript chunks mapped to the selected language to make it highly immersive!
    val transcriptList = when (userLanguage) {
        "Hindi" -> listOf(
            "सुनिए, हमारे गाँव भोला में...",
            "मुख्य सड़क पर जो पेयजल की पाइपलाइन है...",
            "वह पूरी तरह से टूट गई है और पानी बह रहा है...",
            "सड़क पर बहुत जलभराव हो गया है, बच्चे स्कूल नहीं जा पा रहे हैं।"
        )
        "Telugu" -> listOf(
            "నమస్కారం, మా భోలా గ్రామంలో...",
            "ప్రధాన రహదారి పక్కన త్రాగునీటి పైప్‌లైన్ పగిలిపోయింది...",
            "నీరంతా వృధాగా పోయి రోడ్డంతా నిండిపోయింది...",
            "స్కూల్ పిల్లలు మరియు వాహనదారులు వెళ్లడానికి చాలా ఇబ్బంది పడుతున్నారు."
        )
        "Tamil" -> listOf(
            "வணக்கம், எங்களது போலா கிராமத்தில்...",
            "முக்கிய சாலையில் உள்ள குடிநீர் குழாய் உடைந்துள்ளது...",
            "நீர் வீணாக வெளியேறி சாலை முழுவதும் தேங்கியுள்ளது...",
            "பள்ளிக்குச் செல்லும் குழந்தைகள் இதனால் பெரிதும் பாதிக்கப்படுகின்றனர்."
        )
        "Kannada" -> listOf(
            "ನಮಸ್ಕಾರ, ನಮ್ಮ ಭೋಲಾ ಗ್ರಾಮದಲ್ಲಿ...",
            "ಮುಖ್ಯ ರಸ್ತೆಯಲ್ಲಿರುವ ಕುಡಿಯುವ ನೀರಿನ ಪೈಪ್ ಒಡೆದು ಹೋಗಿದೆ...",
            "ನೀರೆಲ್ಲಾ ಪೋಲಾಗಿ ರಸ್ತೆಯ ತುಂಬೆಲ್ಲಾ ಹರಿಯುತ್ತಿದೆ...",
            "ಶಾಲಾ ಮಕ್ಕಳಿಗೆ ಮತ್ತು ಸಾರ್ವಜನಿಕರಿಗೆ ಓಡಾಡಲು ತುಂಬಾ ತೊಂದರೆಯಾಗಿದೆ."
        )
        else -> listOf(
            "Hello, in our Bhola Village...",
            "The drinking water pipeline beside the main road is broken...",
            "Water is leaking heavily and flooding the streets...",
            "It has created a major hazard. Kids can't walk to school."
        )
    }

    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000)
            recordingDurationSec++
            
            // Periodically dump a transcript line to simulate continuous Speech-to-Text!
            val chunkIndex = (recordingDurationSec / 2).coerceAtMost(transcriptList.size - 1)
            detectedText = transcriptList.take(chunkIndex + 1).joinToString(" ")
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A).copy(alpha = 0.95f)), // Full screen immersive dark
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JanMitra Voice Assistant",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Core Animation & Waveform
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isPaused) "Recording Paused" else "Listening in $userLanguage...",
                        color = if (isPaused) Color(0xFFFBBF24) else Color(0xFF60A5FA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Waveform lines animation drawn dynamically
                    val timeAnim = rememberInfiniteTransition(label = "waveform")
                    val phaseOffset by timeAnim.animateFloat(
                        initialValue = 0f,
                        targetValue = 2f * Math.PI.toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "phase"
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f
                        
                        // Draw 3 dynamic sine waves overlapping to look highly realistic and beautiful
                        val waves = listOf(
                            WaveParams(amplitude = 36f, frequency = 0.025f, color = Color(0xFF3B82F6), strokeWidth = 3f, alpha = 0.9f),
                            WaveParams(amplitude = 22f, frequency = 0.04f, color = Color(0xFF10B981), strokeWidth = 2f, alpha = 0.7f),
                            WaveParams(amplitude = 15f, frequency = 0.055f, color = Color(0xFFEC4899), strokeWidth = 1.5f, alpha = 0.5f)
                        )

                        waves.forEach { wave ->
                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, midY)
                            for (x in 0..width.toInt() step 5) {
                                // If paused, damp amplitude to flat line
                                val amp = if (isPaused) 2f else wave.amplitude
                                val y = midY + amp * sin(x * wave.frequency + phaseOffset)
                                path.lineTo(x.toFloat(), y)
                            }
                            drawPath(
                                path = path,
                                color = wave.color.copy(alpha = wave.alpha),
                                style = Stroke(width = wave.strokeWidth)
                            )
                        }
                    }

                    // Digital timer
                    val minutes = recordingDurationSec / 60
                    val seconds = recordingDurationSec % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Live continuous Speech-to-Text Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Spellcheck, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Text(
                                text = "LIVE SPEECH-TO-TEXT TRANSCRIPT",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = detectedText.ifEmpty { "Start speaking. The AI will convert your voice in real-time..." },
                            color = if (detectedText.isEmpty()) Color(0xFF64748B) else Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Audio Recorder Controls (Record, Pause, Resume, Stop, Playback)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Resume Button
                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = Color.White
                        )
                    }

                    // Submit & Proceed Check Button
                    Button(
                        onClick = {
                            val finalSpeechText = if (detectedText.isEmpty()) "Water pipes leaking on Bhola Village central street." else detectedText
                            onSaveAndProceed(finalSpeechText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Text("Use Voice Transcript", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

data class WaveParams(
    val amplitude: Float,
    val frequency: Float,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float
)
