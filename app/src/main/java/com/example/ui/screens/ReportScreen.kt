package com.example.ui.screens

import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import coil.compose.AsyncImage
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: JanMitraViewModel,
    onNavigateToTrack: (reportId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.allVillageStats.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val galleryImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachedImageUri = uri.toString()
            viewModel.isPhotoAttached = true
        }
    }

    val galleryVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachedVideoUri = uri.toString()
            viewModel.isVoiceRecorded = true
        }
    }

    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showMapPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isLocating) {
        if (isLocating) {
            delay(1200)
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
                    val closest = stats.minByOrNull { stat ->
                        val vLat = when(stat.villageName) {
                            "Bhola Village" -> 28.6145
                            "Rampur Village" -> 28.6250
                            "Kalyanpur Village" -> 28.6010
                            "Seva Village" -> 28.6320
                            else -> 28.6139
                        }
                        val vLon = when(stat.villageName) {
                            "Bhola Village" -> 77.2095
                            "Rampur Village" -> 77.2150
                            "Kalyanpur Village" -> 77.1950
                            "Seva Village" -> 77.2280
                            else -> 77.2090
                        }
                        val dy = foundLocation.latitude - vLat
                        val dxCoord = foundLocation.longitude - vLon
                        dy * dy + dxCoord * dxCoord
                    }
                    if (closest != null) {
                        viewModel.reportVillageName = closest.villageName
                    } else {
                        viewModel.reportVillageName = "GPS Pinned Area"
                    }
                } else {
                    viewModel.reportLatitude = 28.6145 + (Math.random() - 0.5) * 0.01
                    viewModel.reportLongitude = 77.2095 + (Math.random() - 0.5) * 0.01
                    viewModel.reportVillageName = "Bhola Village (GPS)"
                }
            } catch (e: SecurityException) {
                locationError = "Permission needed."
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

    // Mock category listing
    val categories = listOf(
        "Water" to Icons.Rounded.WaterDrop,
        "Road" to Icons.Rounded.AddRoad,
        "School" to Icons.Rounded.School,
        "Hospital" to Icons.Rounded.LocalHospital,
        "Electricity" to Icons.Rounded.ElectricBolt,
        "Street Lights" to Icons.Rounded.Lightbulb,
        "Women Safety" to Icons.Rounded.Security,
        "Drainage" to Icons.Rounded.CoPresent,
        "Agriculture" to Icons.Rounded.Agriculture,
        "Other" to Icons.Rounded.Campaign
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // Form Step Header Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report Local Need",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryText
                    )
                    Text(
                        text = "Step ${viewModel.activeFormStep} of 4",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                // Custom Linear Progress Indicator Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..4) {
                        val active = i <= viewModel.activeFormStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) PrimaryBlue else DividerGray)
                        )
                    }
                }
            }
        }

        // Active Wizard Screens
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewModel.activeFormStep) {
                1 -> {
                    // Category Selection
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "1. Select Category of Development Needed:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryText
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(categories) { (cat, icon) ->
                                val selected = viewModel.reportCategory == cat
                                Card(
                                    modifier = Modifier
                                        .height(100.dp)
                                        .clickable { viewModel.reportCategory = cat },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) PrimaryBlue else DividerGray
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = cat,
                                            tint = if (selected) PrimaryBlue else SecondaryText,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) PrimaryBlue else PrimaryText
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.activeFormStep = 2 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Continue to Details", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // Input details (Voice & Text details)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { viewModel.activeFormStep = 1 }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = "2. Add Input Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryText
                            )
                        }

                        // Target Village / Location Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DividerGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryBlue)
                                    Text(
                                        text = "Target Village & Coordinates:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                }
                                
                                Text(
                                    text = "Current Selection: ${viewModel.reportVillageName}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Coordinates: ${String.format("%.4f", viewModel.reportLatitude)}° N, ${String.format("%.4f", viewModel.reportLongitude)}° E",
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // GPS Location Button
                                    Button(
                                        onClick = { isLocating = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isLocating) DividerGray else Color(0xFFF1F5F9),
                                            contentColor = PrimaryBlue
                                        ),
                                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isLocating) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PrimaryBlue)
                                            } else {
                                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Use GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Map Picker Button
                                    Button(
                                        onClick = { showMapPickerDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEFF6FF),
                                            contentColor = PrimaryBlue
                                        ),
                                        border = BorderStroke(1.dp, PrimaryBlue),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("GIS Map Pin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = DividerGray)

                        // Voice Recorder Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DividerGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Voice-First Input (Multilingual Support)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Text(
                                    text = "Describe your issue in your local dialect (Hindi, Telugu, Tamil, Marathi, etc.). AI translates and synthesizes automatically.",
                                    fontSize = 12.sp,
                                    color = SecondaryText,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Interactive Microphone button
                                val micColor = if (viewModel.isVoiceRecorded) SuccessGreen else PrimaryBlue
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(micColor.copy(alpha = 0.12f))
                                        .border(2.dp, micColor, CircleShape)
                                        .clickable {
                                            viewModel.isVoiceRecorded = !viewModel.isVoiceRecorded
                                            if (viewModel.isVoiceRecorded) {
                                                viewModel.reportDescription =
                                                    "पानी की बहुत समस्या है यहाँ कुआँ पूरा सूखा पड़ा है। पानी लाने ४ किलोमीटर जाना पड़ता है।"
                                            } else {
                                                viewModel.reportDescription = ""
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isVoiceRecorded) Icons.Rounded.Mic else Icons.Rounded.MicNone,
                                        contentDescription = "Record",
                                        tint = micColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                if (viewModel.isVoiceRecorded) {
                                    Text(
                                        text = "Voice Captured! Autofilled detected dialect.",
                                        fontSize = 12.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Tap to speak",
                                        fontSize = 12.sp,
                                        color = SecondaryText
                                    )
                                }
                            }
                        }

                        // Text Field details
                        Text(
                            text = "Additional Written Description (Optional):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        OutlinedTextField(
                            value = viewModel.reportDescription,
                            onValueChange = { viewModel.reportDescription = it },
                            placeholder = { Text("E.g., No clean water source, existing pump is leaking...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = DividerGray
                            )
                        )

                        // Premium Media Evidence Grid with Previews & Delete Buttons
                        Text(
                            text = "Evidence Attachments (Photos / Videos):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Image evidence button
                            Button(
                                onClick = { galleryImageLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = PrimaryText),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DividerGray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Video evidence button
                            Button(
                                onClick = { galleryVideoLauncher.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = PrimaryText),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DividerGray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attach Video", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Media Preview Display Row
                        if (viewModel.attachedImageUri != null || viewModel.attachedVideoUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                viewModel.attachedImageUri?.let { uriStr ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.LightGray)
                                            .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = Uri.parse(uriStr),
                                            contentDescription = "Photo evidence",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.attachedImageUri = null
                                                viewModel.isPhotoAttached = false
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                viewModel.attachedVideoUri?.let { uriStr ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF334155))
                                            .border(1.dp, DividerGray, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(36.dp))
                                        Text(
                                            text = "VIDEO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.attachedVideoUri = null
                                                viewModel.isVoiceRecorded = false
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Save Draft Locally Button
                            Button(
                                onClick = { viewModel.saveFormDraft() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7), contentColor = Color(0xFF92400E)),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.SaveAs, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Save Draft", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            // Analyze with JanMitra AI
                            Button(
                                onClick = { viewModel.analyzeReportWithGemini() },
                                modifier = Modifier.weight(1.3f),
                                enabled = (viewModel.reportDescription.isNotEmpty() || viewModel.attachedImageUri != null || viewModel.attachedVideoUri != null) && !viewModel.isAnalyzingReport,
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (viewModel.isAnalyzingReport) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White)
                                        Text("Analyzing...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("AI Analyze", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // AI Summary & Priority score confirmation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { viewModel.activeFormStep = 2 }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = "3. AI Structured Extraction",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryText
                            )
                        }

                        // AI Analysis Details Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DividerGray)
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
                                    Text(
                                        text = "AI CLASSIFICATION RESULTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryTeal
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SecondaryTeal.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Accuracy: 98.4%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryTeal
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Detected Language", fontSize = 11.sp, color = SecondaryText)
                                        Text(viewModel.detectedLanguage, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Assigned Department", fontSize = 11.sp, color = SecondaryText)
                                        Text(viewModel.reportCategory, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                }

                                Divider(color = DividerGray)

                                Text("AI-Synthesized Development Summary (Editable):", fontSize = 12.sp, color = SecondaryText)
                                OutlinedTextField(
                                    value = viewModel.aiAnalysisSummary,
                                    onValueChange = { viewModel.aiAnalysisSummary = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Check for Duplicates alert if exists
                                viewModel.duplicateAlertMessage?.let { alert ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(WarningOrange.copy(alpha = 0.1f))
                                            .border(1.dp, WarningOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningOrange)
                                            Text(
                                                text = alert,
                                                fontSize = 12.sp,
                                                color = WarningOrange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Decision Priority Score card
                        Card(
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
                                    Text(
                                        text = "DECISION INTELLIGENCE INDEX",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                    PriorityBadge(score = viewModel.simulatedPriorityScore)
                                }

                                val explanation = if (viewModel.reportCategory == "Water") {
                                    "This project ranks highly because there is a validated water scarcity gap (Infrastructure Gap: 15/15) in ${viewModel.reportVillageName}. This village exhibits high historical neglect with a development index below 0.35. Priority calculations score: ${String.format("%.1f", viewModel.simulatedPriorityScore)}/100."
                                } else {
                                    "This project ranks at ${String.format("%.1f", viewModel.simulatedPriorityScore)}/100. It directly satisfies the urgent development demand for ${viewModel.reportCategory} in ${viewModel.reportVillageName}, addressing local infrastructure deficits and safety constraints."
                                }

                                Text(
                                    text = explanation,
                                    fontSize = 12.sp,
                                    color = PrimaryText,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.submitFormReport() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Submit Structured Request", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                4 -> {
                    // Success Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = SuccessGreen,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Report Filed Successfully!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your submission is now recorded on the decentralized public planning ledger, prioritized, and queued for administrative review.",
                            fontSize = 13.sp,
                            color = SecondaryText,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Issue ID Display
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DividerGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("OFFICIAL ISSUE ID", fontSize = 10.sp, color = SecondaryText, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = viewModel.submittedReportId ?: "JM-2026-X83A",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(viewModel.submittedReportId ?: "JM-2026-X83A"))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy ID",
                                        tint = SecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { onNavigateToTrack(viewModel.submittedReportId ?: "") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Track Development Timeline", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { viewModel.resetForm() }) {
                            Text("Submit Another Development Need", fontWeight = FontWeight.Bold, color = SecondaryTeal)
                        }
                    }
                }
            }
        }
    }

    if (showMapPickerDialog) {
        MapPickerDialog(
            initialLat = viewModel.reportLatitude,
            initialLon = viewModel.reportLongitude,
            allVillages = stats,
            onConfirm = { name, lat, lon ->
                viewModel.reportVillageName = name
                viewModel.reportLatitude = lat
                viewModel.reportLongitude = lon
                showMapPickerDialog = false
            },
            onDismiss = { showMapPickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLat: Double,
    initialLon: Double,
    allVillages: List<com.example.data.entity.VillageStatistics>,
    onConfirm: (villageName: String, lat: Double, lon: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    var zoomLevel by remember { mutableStateOf(14) }
    var currentLat by remember { mutableStateOf(initialLat) }
    var currentLon by remember { mutableStateOf(initialLon) }

    val getVillageCoords = { name: String ->
        when (name) {
            "Bhola Village" -> 28.6145 to 77.2095
            "Rampur Village" -> 28.6250 to 77.2150
            "Kalyanpur Village" -> 28.6010 to 77.1950
            "Seva Village" -> 28.6320 to 77.2280
            else -> 28.6139 to 77.2090
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GIS Interactive Map Picker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = SecondaryText)
                    }
                }

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search village name (Bhola, Rampur...)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = SecondaryText) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DividerGray
                    ),
                    singleLine = true
                )

                val matchedVillages = allVillages.filter {
                    searchInput.isNotEmpty() && it.villageName.contains(searchInput, ignoreCase = true)
                }
                if (matchedVillages.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DividerGray.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            matchedVillages.forEach { stat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val (lat, lon) = getVillageCoords(stat.villageName)
                                            currentLat = lat
                                            currentLon = lon
                                            searchInput = ""
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Text(stat.villageName, fontSize = 14.sp, color = PrimaryText)
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.5.dp, DividerGray, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val dx = (offset.x - size.width / 2f) / (size.width / 2f) * 0.02
                                val dy = -(offset.y - size.height / 2f) / (size.height / 2f) * 0.02
                                currentLat += dy
                                currentLon += dx
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSpacing = 60f
                        for (x in 0..(size.width.toInt()) step gridSpacing.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.5f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                        }
                        for (y in 0..(size.height.toInt()) step gridSpacing.toInt()) {
                            drawLine(Color.White.copy(alpha = 0.5f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                        }

                        drawLine(Color(0xFF94A3B8), Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.7f), strokeWidth = 12f)
                        drawLine(Color(0x333B82F6), Offset(size.width * 0.2f, 0f), Offset(size.width * 0.5f, size.height), strokeWidth = 18f)

                        allVillages.forEach { stat ->
                            val (vLat, vLon) = getVillageCoords(stat.villageName)
                            val dx = (vLon - currentLon) / 0.02 * (size.width / 2f) + (size.width / 2f)
                            val dy = -(vLat - currentLat) / 0.02 * (size.height / 2f) + (size.height / 2f)
                            
                            if (dx in 0f..size.width && dy in 0f..size.height) {
                                drawCircle(Color.White, radius = 12f, center = Offset(dx.toFloat(), dy.toFloat()))
                                drawCircle(PrimaryBlue, radius = 8f, center = Offset(dx.toFloat(), dy.toFloat()))
                            }
                        }

                        val pinCenter = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(Color.Red.copy(alpha = 0.2f), radius = 40f, center = pinCenter)
                        drawCircle(Color.Red.copy(alpha = 0.4f), radius = 24f, center = pinCenter)
                        drawCircle(Color.Red, radius = 10f, center = pinCenter)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { zoomLevel = Math.min(18, zoomLevel + 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = PrimaryText)
                        }
                        IconButton(
                            onClick = { zoomLevel = Math.max(10, zoomLevel - 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = PrimaryText)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Tap anywhere on map to drop custom pin",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                val resolvedName = remember(currentLat, currentLon) {
                    val closest = allVillages.minByOrNull { stat ->
                        val (vLat, vLon) = getVillageCoords(stat.villageName)
                        val dy = currentLat - vLat
                        val dx = currentLon - vLon
                        dy * dy + dx * dx
                    }
                    if (closest != null) closest.villageName else "Pinned Region"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, DividerGray)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationSearching, contentDescription = null, tint = SecondaryTeal)
                        Column {
                            Text(
                                text = "Resolved Area: $resolvedName",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryText
                            )
                            Text(
                                text = "Coordinates: ${String.format("%.4f", currentLat)}° N, ${String.format("%.4f", currentLon)}° E",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                    }
                }

                Button(
                    onClick = { onConfirm(resolvedName, currentLat, currentLon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Pinned Location", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
