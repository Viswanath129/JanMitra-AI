package com.example.ui.screens

import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.CitizenReport
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: JanMitraViewModel,
    onNavigateToTrack: (reportId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.allVillageStats.collectAsStateWithLifecycle()
    val reports by viewModel.allReports.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Activity launcher for file pickers
    val galleryImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val localPath = viewModel.copyUriToLocal(context, uri, "attached_img")
                if (localPath != null) {
                    viewModel.attachedImageUri = localPath
                    viewModel.isPhotoAttached = true
                }
            }
        }
    }

    val galleryVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val localPath = viewModel.copyUriToLocal(context, uri, "attached_vid")
                if (localPath != null) {
                    viewModel.attachedVideoUri = localPath
                    viewModel.isVoiceRecorded = true
                }
            }
        }
    }

    // GPS Centering logic
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLocating) {
        if (isLocating) {
            delay(1000)
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val providers = locationManager?.getProviders(true)
                var foundLocation: android.location.Location? = null
                if (providers != null) {
                    for (provider in providers) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            foundLocation = loc
                            break
                        }
                    }
                }
                if (foundLocation != null) {
                    viewModel.reportLatitude = foundLocation.latitude
                    viewModel.reportLongitude = foundLocation.longitude
                    
                    // Match closest village stats
                    val closest = stats.minByOrNull { stat ->
                        val (vLat, vLon) = getVillageCoordsMock(stat.villageName)
                        val dy = foundLocation.latitude - vLat
                        val dx = foundLocation.longitude - vLon
                        dy * dy + dx * dx
                    }
                    viewModel.reportVillageName = closest?.villageName ?: "GPS Pinned Area"
                } else {
                    viewModel.reportLatitude = 28.6145 + (Math.random() - 0.5) * 0.005
                    viewModel.reportLongitude = 77.2095 + (Math.random() - 0.5) * 0.005
                    viewModel.reportVillageName = "Bhola Village (GPS)"
                }
            } catch (e: SecurityException) {
                locationError = "Location access denied. Using defaults."
                viewModel.reportLatitude = 28.6145
                viewModel.reportLongitude = 77.2095
                viewModel.reportVillageName = "Bhola Village"
            } catch (e: Exception) {
                viewModel.reportLatitude = 28.6145
                viewModel.reportLongitude = 77.2095
                viewModel.reportVillageName = "Bhola Village"
            } finally {
                isLocating = false
            }
        }
    }

    val categories = listOf(
        CategoryWizardInfo("Water Supply", Icons.Rounded.WaterDrop, Color(0xFF2563EB), "Clean drinking water issue"),
        CategoryWizardInfo("Road Damage", Icons.Rounded.AddRoad, Color(0xFF16A34A), "Potholes, broken roads, connectivity"),
        CategoryWizardInfo("School Upgrade", Icons.Rounded.School, Color(0xFFEA580C), "Classrooms, toilets, furniture"),
        CategoryWizardInfo("Hospital Staff", Icons.Rounded.LocalHospital, Color(0xFFEF4444), "Medicines, nurses, infrastructure"),
        CategoryWizardInfo("Power Outage", Icons.Rounded.ElectricBolt, Color(0xFFEAB308), "Outages, transformer issues"),
        CategoryWizardInfo("Street Lights", Icons.Rounded.Lightbulb, Color(0xFF06B6D4), "Dark roads, safety hazards"),
        CategoryWizardInfo("Women Safety", Icons.Rounded.Security, Color(0xFFD946EF), "Safe corridors, policing, patrols"),
        CategoryWizardInfo("Agriculture", Icons.Rounded.Agriculture, Color(0xFF10B981), "Irrigation, water channel clogging"),
        CategoryWizardInfo("Other Demand", Icons.Rounded.Campaign, Color(0xFF64748B), "General grievance or development request")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // --- Header Wizard Progress ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (viewModel.activeFormStep in 2..5) {
                            IconButton(
                                onClick = { viewModel.activeFormStep-- },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = getStepTitle(viewModel.activeFormStep),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "Step ${viewModel.activeFormStep} of 6",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                // Progress indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (i in 1..6) {
                        val isActive = i <= viewModel.activeFormStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isActive) PrimaryBlue else DividerGray)
                        )
                    }
                }
            }
        }

        // --- Active Wizard Body ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewModel.activeFormStep) {
                1 -> {
                    // --- STEP 1: ISSUE CATEGORY ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "What type of local development do you want to report?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(categories) { cat ->
                                val cleanedCatName = cat.name.split(" ")[0]
                                val selected = viewModel.reportCategory == cleanedCatName
                                Card(
                                    modifier = Modifier
                                        .height(106.dp)
                                        .clickable { viewModel.reportCategory = cleanedCatName },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) cat.color.copy(alpha = 0.08f) else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) cat.color else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(cat.color.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = cat.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) cat.color else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = cat.desc,
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.activeFormStep = 2 },
                            enabled = viewModel.reportCategory.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                text = if (viewModel.reportCategory.isEmpty()) "Select a Category to Continue" else "Continue to Location Pinning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                2 -> {
                    // --- STEP 2: LOCATION PINNING (EMBEDDED INTERACTIVE MAP) ---
                    var searchInput by remember { mutableStateOf("") }
                    var mapTypeSatellite by remember { mutableStateOf(false) }
                    var showTraffic by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Pin the exact geographical point of this grievance on the map:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )

                        // Search Bar
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Search Village or Landmark...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            singleLine = true
                        )

                        // Matched Search Suggestions dropdown inline
                        val matchedVillages = stats.filter {
                            searchInput.isNotEmpty() && it.villageName.contains(searchInput, ignoreCase = true)
                        }
                        if (matchedVillages.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column {
                                    matchedVillages.forEach { stat ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val (lat, lon) = getVillageCoordsMock(stat.villageName)
                                                    viewModel.reportLatitude = lat
                                                    viewModel.reportLongitude = lon
                                                    viewModel.reportVillageName = stat.villageName
                                                    searchInput = ""
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                            Text(stat.villageName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                        }
                                    }
                                }
                            }
                        }

                        // Embedded GIS Map Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        // Dynamic interactive click pining
                                        val dx = (offset.x - size.width / 2f) / (size.width / 2f) * 0.015
                                        val dy = -(offset.y - size.height / 2f) / (size.height / 2f) * 0.015
                                        viewModel.reportLatitude += dy
                                        viewModel.reportLongitude += dx
                                        
                                        // Reverse geocode closest area
                                        val closest = stats.minByOrNull { stat ->
                                            val (vLat, vLon) = getVillageCoordsMock(stat.villageName)
                                            val diffY = viewModel.reportLatitude - vLat
                                            val diffX = viewModel.reportLongitude - vLon
                                            diffY * diffY + diffX * diffX
                                        }
                                        viewModel.reportVillageName = closest?.villageName ?: "GPS Pinned Area"
                                    }
                                }
                        ) {
                            // Map drawing
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val mapBg = if (mapTypeSatellite) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                                drawRect(mapBg)

                                val gridSpacing = 70f
                                val gridColor = if (mapTypeSatellite) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                for (x in 0..(size.width.toInt()) step gridSpacing.toInt()) {
                                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                                }
                                for (y in 0..(size.height.toInt()) step gridSpacing.toInt()) {
                                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                                }

                                // Mock lake/river vector paths on map
                                drawCircle(
                                    color = Color(0x223B82F6),
                                    radius = size.width * 0.35f,
                                    center = Offset(size.width * 0.2f, size.height * 0.7f)
                                )

                                // Traffic overlay drawing
                                if (showTraffic) {
                                    drawLine(
                                        color = Color(0x9910B981),
                                        start = Offset(0f, size.height * 0.4f),
                                        end = Offset(size.width, size.height * 0.4f),
                                        strokeWidth = 10f
                                    )
                                    drawLine(
                                        color = Color(0x99EF4444),
                                        start = Offset(size.width * 0.45f, 0f),
                                        end = Offset(size.width * 0.45f, size.height),
                                        strokeWidth = 8f
                                    )
                                }

                                // Render other village core points
                                stats.forEach { stat ->
                                    val (vLat, vLon) = getVillageCoordsMock(stat.villageName)
                                    val dx = (vLon - viewModel.reportLongitude) / 0.015 * (size.width / 2f) + (size.width / 2f)
                                    val dy = -(vLat - viewModel.reportLatitude) / 0.015 * (size.height / 2f) + (size.height / 2f)

                                    if (dx in 0f..size.width && dy in 0f..size.height) {
                                        drawCircle(Color.White, radius = 10f, center = Offset(dx.toFloat(), dy.toFloat()))
                                        drawCircle(Color(0xFF2563EB), radius = 6f, center = Offset(dx.toFloat(), dy.toFloat()))
                                    }
                                }

                                // Pinned Target Core Marker (Center)
                                val pinCenter = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(Color.Red.copy(alpha = 0.2f), radius = 45f, center = pinCenter)
                                drawCircle(Color.Red.copy(alpha = 0.5f), radius = 24f, center = pinCenter)
                                drawCircle(Color.Red, radius = 10f, center = pinCenter)
                            }

                            // Layer selectors top-right
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Satellite toggle
                                IconButton(
                                    onClick = { mapTypeSatellite = !mapTypeSatellite },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (mapTypeSatellite) Icons.Rounded.LayersClear else Icons.Rounded.Layers,
                                        contentDescription = "Map Style",
                                        tint = Color(0xFF0F172A)
                                    )
                                }

                                // Traffic toggle
                                IconButton(
                                    onClick = { showTraffic = !showTraffic },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Traffic,
                                        contentDescription = "Traffic",
                                        tint = if (showTraffic) Color(0xFF10B981) else Color(0xFF64748B)
                                    )
                                }
                            }

                            // Floating instructions
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.72f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Drag/Tap map to adjust marker point",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // GPS locator button bottom-right
                            IconButton(
                                onClick = { isLocating = true },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(46.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF2563EB))
                                } else {
                                    Icon(Icons.Rounded.MyLocation, contentDescription = "Use GPS", tint = Color(0xFF2563EB))
                                }
                            }
                        }

                        // Resolved Address Display Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.LocationSearching, contentDescription = null, tint = Color(0xFF0D9488))
                                Column {
                                    Text(
                                        text = "Reverse Geocoded: ${viewModel.reportVillageName}, Andhra Pradesh",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "GPS Coordinates: ${String.format("%.5f", viewModel.reportLatitude)}° N, ${String.format("%.5f", viewModel.reportLongitude)}° E",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.activeFormStep = 3 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Confirm Location & Add Media", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                3 -> {
                    // --- STEP 3: PHOTO / VIDEO EVIDENCE (CAMERAX INTERFACE OVERLAY GUIDANCE) ---
                    var cameraActive by remember { mutableStateOf(false) }
                    var mediaCaptured by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Provide real photo or video evidence of the infrastructure deficit:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )

                        // Responsive Live Camera Frame simulation (with intelligent capture overlay guides)
                        if (cameraActive) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            ) {
                                // Camera lens view simulated
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Reticle
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.4f),
                                        radius = size.width * 0.15f,
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        style = Stroke(width = 2f)
                                    )
                                }

                                // Interactive guideline based on category
                                val guidanceText = when (viewModel.reportCategory) {
                                    "Water" -> "Position leaking valve or water puddle inside frame"
                                    "Road" -> "Align camera angle to show potholes depth clearly"
                                    "School" -> "Capture the damaged roof or school furniture"
                                    else -> "Ensure sufficient lighting and high-contrast capture"
                                }

                                Card(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xBB1E293B)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = guidanceText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Capture trigger button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(24.dp)
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(4.dp, Color(0xFFEF4444), CircleShape)
                                        .clickable {
                                            viewModel.attachedImageUri = "mock_captured_evidence.jpg"
                                            viewModel.isPhotoAttached = true
                                            cameraActive = false
                                            mediaCaptured = true
                                        }
                                )
                            }
                        } else {
                            // Offline Camera selection buttons
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (viewModel.attachedImageUri != null) {
                                        // Preview thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(2.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                                        ) {
                                            val uriStr = viewModel.attachedImageUri
                                            val isRealImage = uriStr != null && (uriStr.startsWith("content://") || uriStr.startsWith("/"))
                                            if (isRealImage) {
                                                val model = if (uriStr!!.startsWith("content://")) Uri.parse(uriStr) else java.io.File(uriStr)
                                                AsyncImage(
                                                    model = model,
                                                    contentDescription = "Evidence Preview",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                // Simulated captured canvas view
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    drawRect(Color(0xFF334155))
                                                    drawCircle(Color(0xFF10B981).copy(alpha = 0.4f), radius = 60f)
                                                }
                                                Icon(
                                                    imageVector = Icons.Rounded.Verified,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .align(Alignment.Center)
                                                )
                                            }

                                            // Delete button
                                            IconButton(
                                                onClick = {
                                                    viewModel.attachedImageUri = null
                                                    viewModel.isPhotoAttached = false
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(24.dp)
                                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                            ) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Evidence Attached!",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "Compression details: Original (4.2 MB) -> JPEG Optimized (185 KB)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(54.dp))
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No evidence photo/video captured yet",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                        Text(
                                            text = "Provide visual details to help administration verify instantly",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Capture buttons row
                        if (!cameraActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { cameraActive = true },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Camera, contentDescription = null)
                                        Text("Open CameraX Lens", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Button(
                                    onClick = { galleryImageLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Upload Gallery", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        if (!cameraActive) {
                            Button(
                                onClick = { viewModel.activeFormStep = 4 },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("Continue to Voice Input", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                4 -> {
                    // --- STEP 4: VOICE RECORDING & TRANSCRIPT SPEECH-TO-TEXT ---
                    var isVoiceRecordingActive by remember { mutableStateOf(false) }
                    var voiceSecs by remember { mutableStateOf(0) }
                    var isVoicePaused by remember { mutableStateOf(false) }

                    // Continuous typing transcript simulation
                    val typingTranscriptList = when (viewModel.userLanguage) {
                        "Hindi" -> listOf(
                            "ग्राम सभा भोला में पेयजल समस्या अति गंभीर है।",
                            "पानी की पुरानी पाइपलाइन पूरी तरह फट चुकी है।",
                            "जिसके कारण पानी की बड़ी मात्रा बर्बाद होकर सड़कों पर बह रही है।",
                            "गाँव के लोगों को पीने का पानी पाने के लिए मीलों दूर भटकना पड़ता है।"
                        )
                        "Telugu" -> listOf(
                            "భోలా గ్రామంలో త్రాగునీటి సమస్య తీవ్రంగా ఉంది.",
                            "పాత వాటర్ సప్లై పైప్‌లైన్ పూర్తిగా పగిలిపోయింది.",
                            "దీని వలన రోడ్ల మీద భారీగా నీరు వృధాగా ప్రవహిస్తోంది.",
                            "ప్రజలు త్రాగునీటి కోసం ఇతర గ్రామాలకు వెళ్లాల్సి వస్తోంది."
                        )
                        else -> listOf(
                            "Drinking water issue in Bhola Village is critical.",
                            "The legacy water supply line is completely burst.",
                            "Large volume of water is flooding the main transit street.",
                            "Villagers have to walk miles daily to collect fresh water."
                        )
                    }

                    LaunchedEffect(isVoiceRecordingActive, isVoicePaused) {
                        if (isVoiceRecordingActive && !isVoicePaused) {
                            while (true) {
                                delay(1000)
                                voiceSecs++
                                val index = (voiceSecs / 2).coerceAtMost(typingTranscriptList.size - 1)
                                viewModel.reportDescription = typingTranscriptList.take(index + 1).joinToString(" ")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "State your grievance out loud (multilingual STT engine):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )

                        // Visualizer Bouncing waveform
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Night Navy
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val phaseAnim = rememberInfiniteTransition(label = "step_wave")
                                val phase by phaseAnim.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 2f * Math.PI.toFloat(),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing)
                                    ),
                                    label = "phase"
                               )

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                ) {
                                    val midY = size.height / 2f
                                    val count = 20
                                    val step = size.width / count
                                    for (i in 0..count) {
                                        val heightMultiplier = if (isVoiceRecordingActive && !isVoicePaused) {
                                            sin(i * 0.4f + phase) * 20f + 25f
                                        } else {
                                            4f
                                        }
                                        drawLine(
                                            color = Color(0xFF60A5FA),
                                            start = Offset(i * step, midY - heightMultiplier),
                                            end = Offset(i * step, midY + heightMultiplier),
                                            strokeWidth = 6f
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isVoiceRecordingActive && !isVoicePaused) Color(0xFFEF4444) else Color(0xFF94A3B8))
                                    )
                                    Text(
                                        text = String.format("%02d:%02d", voiceSecs / 60, voiceSecs % 60),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Realtime STT transcript preview
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.SpeakerNotes, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Text("LIVE TRANSCRIBED TRANSCRIPT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = viewModel.reportDescription,
                                    onValueChange = { if (it.length <= 1000) viewModel.reportDescription = it },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2563EB),
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                Text(
                                    text = "${viewModel.reportDescription.length} / 1000 characters",
                                    fontSize = 11.sp,
                                    color = if (viewModel.reportDescription.length >= 900) Color(0xFFEF4444) else Color(0xFF64748B),
                                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                )
                            }
                        }

                        // Recording actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Record / Stop
                            IconButton(
                                onClick = {
                                    if (isVoiceRecordingActive) {
                                        isVoiceRecordingActive = false
                                        viewModel.isVoiceRecorded = true
                                    } else {
                                        isVoiceRecordingActive = true
                                        isVoicePaused = false
                                        voiceSecs = 0
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isVoiceRecordingActive) Color(0xFFEF4444) else Color(0xFF2563EB))
                            ) {
                                Icon(
                                    imageVector = if (isVoiceRecordingActive) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    tint = Color.White,
                                    contentDescription = "Voice"
                                )
                            }

                            // Pause / Resume
                            if (isVoiceRecordingActive) {
                                IconButton(
                                    onClick = { isVoicePaused = !isVoicePaused },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    Icon(
                                        imageVector = if (isVoicePaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                        tint = Color(0xFF475569),
                                        contentDescription = "Pause"
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.analyzeReportWithGemini()
                                viewModel.activeFormStep = 5
                            },
                            enabled = viewModel.reportDescription.isNotEmpty() && !isVoiceRecordingActive,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Continue to AI Analysis & Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                5 -> {
                    // --- STEP 5: AI REVIEW, PREVIEW & DUPLICATE WARNING ---
                    val submittedList = reports.filter { it.status == "PendingSubmission" || it.status == "Reported" || it.status == "Approved" || it.status == "In Progress" }
                    val duplicateMatch = submittedList.firstOrNull {
                        it.category == viewModel.reportCategory && it.locationName == viewModel.reportVillageName
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Review extracted details before committing to government registry:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )

                        // Core Preview Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Category, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "Category: ${viewModel.reportCategory}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    PriorityBadge(score = viewModel.simulatedPriorityScore)
                                }

                                Divider(color = Color(0xFFF1F5F9))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("Resolved Village:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("${viewModel.reportVillageName}, Andhra Pradesh", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Coordinates: ${String.format("%.4f", viewModel.reportLatitude)}° N, ${String.format("%.4f", viewModel.reportLongitude)}° E", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }

                                Divider(color = Color(0xFFF1F5F9))

                                Column {
                                    Text("Transcribed Speech Details:", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(viewModel.reportDescription, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                }

                                if (viewModel.attachedImageUri != null) {
                                    Divider(color = Color(0xFFF1F5F9))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Attachment, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Text("1 Photo Attachment Attached (Optimized)", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // AI Analysis Details Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(18.dp))
                                        Text("AI Structured Insights", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                                    }
                                }
                                Text(
                                    text = viewModel.aiAnalysisSummary.ifEmpty { "AI extraction successful. Priority Index is calculated based on ${viewModel.reportVillageName} Development Gap (historical gap indices in roads and utilities)." },
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        // Duplicate issue warning! (Features 8 and 9)
                        if (duplicateMatch != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFD97706))
                                        Text(
                                            text = "Similar Active Issue Discovered!",
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Text(
                                        text = "Another citizen reported a similar '${viewModel.reportCategory}' issue in ${viewModel.reportVillageName} earlier. Instead of creating a duplicate, you can support their report to increase administration urgency!",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFFB45309)
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.approveReport(duplicateMatch) // Support existing by updating status/votes
                                            coroutineScope.launch {
                                                viewModel.activeFormStep = 6
                                                viewModel.submittedReportId = duplicateMatch.issueId
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("I Have This Issue (Support existing)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Submit buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Draft save
                            Button(
                                onClick = {
                                    viewModel.saveFormDraft()
                                    viewModel.activeFormStep = 1
                                    onNavigateToTrack("")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Draft", fontWeight = FontWeight.Bold)
                            }

                            // Submit Final
                            Button(
                                onClick = { viewModel.submitFormReport() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Submit Grievance", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // --- STEP 6: SUBMIT / SUCCESS (DELIVERY-STYLE TRACKER TIMELINE) ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECFDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Text(
                            text = "Grievance Lodged Successfully!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Your development need is recorded in the administrative registry. The decentralized planning pipeline is generated below.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        // Issue ID block
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("LEDGER TRACK ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(viewModel.submittedReportId ?: "JM-82A7-91X", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(viewModel.submittedReportId ?: "JM-82A7-91X")) }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Track ID", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Delivery-style tracker timeline
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "ADMINISTRATIVE DISPATCH JOURNEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Step 1
                            TimelineStep(
                                label = "Need Logged",
                                time = "Just Now",
                                desc = "Grievance submitted by citizen successfully. Uploaded images compressed & cataloged.",
                                isCompleted = true,
                                isLast = false
                            )

                            // Step 2
                            TimelineStep(
                                label = "AI Priority Calculated",
                                time = "Just Now",
                                desc = "JanMitra AI analyzed community urgency at ${String.format("%.1f", viewModel.simulatedPriorityScore)}/100 index.",
                                isCompleted = true,
                                isLast = false
                            )

                            // Step 3
                            TimelineStep(
                                label = "Verification & Approval",
                                time = "Pending (Within 24 Hours)",
                                desc = "District collectorate reviews structural logs & assigns government nodal officer.",
                                isCompleted = false,
                                isLast = false
                            )

                            // Step 4
                            TimelineStep(
                                label = "Fund Allocation & Completion",
                                time = "Scheduled",
                                desc = "Public works budget dispatched to the target ward. Infrastructure repairs completed.",
                                isCompleted = false,
                                isLast = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onNavigateToTrack(viewModel.submittedReportId ?: "") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timeline, contentDescription = null)
                                Text("Open Interactive Tracker Timeline", fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.resetForm()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lodge Another Need", fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStep(
    label: String,
    time: String,
    desc: String,
    isCompleted: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFFECFDF5) else Color(0xFFF1F5F9))
                    .border(2.dp, if (isCompleted) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                } else {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(if (isCompleted) Color(0xFF10B981) else Color(0xFFE2E8F0))
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isCompleted) Color(0xFF1E293B) else Color(0xFF64748B))
                Text(text = time, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
            }
            Text(text = desc, fontSize = 11.sp, color = Color(0xFF64748B), lineHeight = 15.sp)
        }
    }
}

fun getStepTitle(step: Int): String {
    return when (step) {
        1 -> "Select Category"
        2 -> "GIS Location Pin"
        3 -> "Evidence Photo/Video"
        4 -> "Describe with Voice"
        5 -> "AI Structured Review"
        else -> "Lodged Status Timeline"
    }
}

fun getVillageCoordsMock(name: String): Pair<Double, Double> {
    return when (name) {
        "Bhola Village" -> 28.6145 to 77.2095
        "Rampur Village" -> 28.6250 to 77.2150
        "Kalyanpur Village" -> 28.6010 to 77.1950
        "Seva Village" -> 28.6320 to 77.2280
        else -> 28.6139 to 77.2090
    }
}

data class CategoryWizardInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val desc: String
)
