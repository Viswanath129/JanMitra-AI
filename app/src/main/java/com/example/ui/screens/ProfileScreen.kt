package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel
import android.content.ClipboardManager
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.network.SessionManager
import com.example.data.network.NetworkModule
import com.example.data.network.NetworkTestNotificationRequest
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: JanMitraViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf("English") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Clean Slate-50 background
    ) {
        // App Bar with Title & Settings Gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            IconButton(
                onClick = { showAboutDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF1E293B)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Card: Avatar and Details (KW - Kasi Viswanath)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle matching design style with initials
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .border(1.5.dp, Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val displayName = viewModel.currentUserDisplayName.ifEmpty { "Guest User" }
                    val initials = displayName.split(" ")
                        .filter { it.isNotEmpty() }
                        .map { it.first().uppercase() }
                        .joinToString("")
                        .take(2)
                    Text(
                        text = if (initials.isNotEmpty()) initials else "GU",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = viewModel.currentUserDisplayName.ifEmpty { "Guest Advocate" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = viewModel.currentUserRole.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Advocate",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = viewModel.currentUserPhone.ifEmpty { "No Verified Phone" },
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = viewModel.currentUserEmail.ifEmpty { "anonymous@janmitra.ai" },
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Menu Items Layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Rounded.Description,
                    label = "My Reports",
                    onClick = onNavigateToReports
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Rounded.Forum,
                    label = "My Conversations",
                    onClick = onNavigateToChat
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Rounded.Language,
                    label = "Language",
                    valueLabel = currentLanguage,
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Rounded.Notifications,
                    label = "Notifications",
                    onClick = { showNotificationsDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Rounded.Help,
                    label = "Help & Support",
                    onClick = { showHelpDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileMenuItem(
                    icon = Icons.Rounded.Info,
                    label = "About JanMitra AI",
                    onClick = { showAboutDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                
                // Logout Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.logout() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = "Logout",
                        tint = Color(0xFFEF4444), // Crimson Red
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Logout & Secure Session",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Interface Language", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "Hindi (हिंदी)", "Telugu (తెలుగు)", "Tamil (தமிழ்)").forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    currentLanguage = lang.split(" ")[0]
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lang, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            if (currentLanguage == lang.split(" ")[0]) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = PrimaryBlue)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showNotificationsDialog) {
        val context = LocalContext.current
        val sessionManager = SessionManager.getInstance(context)
        val fcmToken = sessionManager.fcmToken ?: "No token generated yet"
        val coroutineScope = rememberCoroutineScope()
        var testStatus by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("FCM Notification Panel", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Verify production-grade Firebase Cloud Messaging (FCM) on your hardware:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    // Display active token
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Active Registration Token:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                fcmToken,
                                fontSize = 11.sp,
                                maxLines = 3,
                                color = Color.DarkGray
                            )
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("FCM Token", fcmToken)
                                    clipboard.setPrimaryClip(clip)
                                    testStatus = "Token copied to clipboard!"
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Copy Token", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Test local and remote actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Trigger Local Alert
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    val pendingIntent = PendingIntent.getActivity(
                                        context, 101, intent,
                                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                    val channelId = "janmitra_alerts_channel"
                                    val notificationBuilder = NotificationCompat.Builder(context, channelId)
                                        .setSmallIcon(context.applicationInfo.icon)
                                        .setContentTitle("JanMitra Local Alert")
                                        .setContentText("Local rendering test completed successfully!")
                                        .setAutoCancel(true)
                                        .setSound(defaultSoundUri)
                                        .setContentIntent(pendingIntent)
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)

                                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val channel = NotificationChannel(
                                            channelId,
                                            "JanMitra Governance Alerts",
                                            NotificationManager.IMPORTANCE_HIGH
                                        )
                                        notificationManager.createNotificationChannel(channel)
                                    }
                                    notificationManager.notify(101, notificationBuilder.build())
                                    testStatus = "Local test notification rendered!"
                                } catch (e: Exception) {
                                    testStatus = "Local test failed: ${e.message}"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Trigger Local", fontSize = 12.sp, color = Color.White)
                        }

                        // Action 2: Trigger Backend FCM
                        Button(
                            onClick = {
                                if (fcmToken.contains("No token")) {
                                    testStatus = "Cannot trigger: no FCM Token"
                                    return@Button
                                }
                                testStatus = "Sending request to FastAPI server..."
                                coroutineScope.launch {
                                    try {
                                        val req = NetworkTestNotificationRequest(
                                            fcmToken = fcmToken,
                                            title = "JanMitra Server Push",
                                            message = "Real hardware push notification test from FastAPI!"
                                        )
                                        val response = NetworkModule.backendApiService.sendTestNotification(req)
                                        testStatus = "Server response: ${response["status"] ?: "OK"}"
                                    } catch (e: Exception) {
                                        testStatus = "Server dispatch failed: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Server Push", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    if (testStatus != null) {
                        Text(
                            testStatus!!,
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Support Center", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How do I file a local development request?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Navigate to the 'Report' wizard step in the center of the bottom navigation bar. Choose a category, pin your location on the GIS Map, and describe the need (voice or text).", fontSize = 12.sp, color = SecondaryText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("How does the Priority Index work?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("JanMitra AI calculates priority metrics dynamically by assessing population density, historic ward neglect, current infrastructure water/road deficits, and vulnerable community percentages.", fontSize = 12.sp, color = SecondaryText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About JanMitra AI", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("JanMitra AI - People's Voice. Intelligent Development.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Version 1.1.0-Release", fontSize = 12.sp, color = SecondaryText)
                    Text("An advanced, state-of-the-art decision-intelligence platform bridging citizen-reported infrastructure gaps directly with legislative planners, backed by local SQLite persistence, on-device voice processing, and Gemini-based explainable prioritization engines.", fontSize = 13.sp, lineHeight = 18.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    valueLabel: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF475569),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )
        if (valueLabel != null) {
            Text(
                text = valueLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}
