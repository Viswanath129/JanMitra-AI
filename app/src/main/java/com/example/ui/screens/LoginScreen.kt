package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.JanMitraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: JanMitraViewModel,
    activity: Activity,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Email, 1 = Phone OTP
    var isRegistering by remember { mutableStateOf(false) }

    // Email fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Phone fields
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Clean Slate-50 background
    ) {
        // Decorative background top banner with modern gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Deep Blue Slate
                            Color(0xFF3B82F6).copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // App Logo / Icon Header
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Logo",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Title and Subtitle
            Text(
                text = "JanMitra AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Secure Democratic Governance Portal",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auth Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TAB SELECTOR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TabButton(
                            label = "Email Portal",
                            selected = selectedTab == 0 && !isRegistering,
                            onClick = {
                                selectedTab = 0
                                isRegistering = false
                                viewModel.authErrorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            label = "Phone OTP",
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                isRegistering = false
                                viewModel.authErrorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Banner
                    viewModel.authErrorMessage?.let { errMsg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Error,
                                    contentDescription = "Error",
                                    tint = Color(0xFFDC2626)
                                )
                                Text(
                                    text = errMsg,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Success Notification
                    viewModel.otpSentMessage?.let { successMsg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF059669)
                                )
                                Text(
                                    text = successMsg,
                                    fontSize = 13.sp,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // INPUT FORMS
                    AnimatedContent(
                        targetState = if (selectedTab == 1) "Phone" else if (isRegistering) "Register" else "Login",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "form_animation"
                    ) { formType ->
                        when (formType) {
                            "Login" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("email_input")
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Lock") },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = "Toggle password"
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("password_input")
                                    )

                                    Button(
                                        onClick = { viewModel.loginWithEmailPassword(email, password) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        enabled = !viewModel.isAuthenticating,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("login_button")
                                    ) {
                                        if (viewModel.isAuthenticating) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("Sign In Securely", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("New to JanMitra? ", color = Color(0xFF64748B), fontSize = 14.sp)
                                        Text(
                                            text = "Create Account",
                                            color = Color(0xFF2563EB),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.clickable { isRegistering = true }
                                        )
                                    }
                                }
                            }

                            "Register" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = "Name") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_name_input")
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_email_input")
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Lock") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_password_input")
                                    )

                                    Button(
                                        onClick = { viewModel.registerWithEmailPassword(email, password, name) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        enabled = !viewModel.isAuthenticating,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("register_button")
                                    ) {
                                        if (viewModel.isAuthenticating) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("Register & Synchronize", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("Have an account? ", color = Color(0xFF64748B), fontSize = 14.sp)
                                        Text(
                                            text = "Sign In",
                                            color = Color(0xFF2563EB),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.clickable { isRegistering = false }
                                        )
                                    }
                                }
                            }

                            "Phone" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text("Phone Number (+91...)") },
                                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = "Phone") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("phone_input")
                                    )

                                    if (viewModel.isOtpSent) {
                                        OutlinedTextField(
                                            value = otpCode,
                                            onValueChange = { otpCode = it },
                                            label = { Text("Enter 6-Digit OTP") },
                                            leadingIcon = { Icon(Icons.Rounded.Pin, contentDescription = "Pin") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("otp_input")
                                        )

                                        Button(
                                            onClick = { viewModel.verifyPhoneOtp(otpCode) },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            enabled = !viewModel.isAuthenticating,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .testTag("verify_otp_button")
                                        ) {
                                            if (viewModel.isAuthenticating) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                            } else {
                                                Text("Verify OTP & Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.sendPhoneOtp(phoneNumber, activity) },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                            enabled = !viewModel.isAuthenticating,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .testTag("send_otp_button")
                                        ) {
                                            if (viewModel.isAuthenticating) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                            } else {
                                                Text("Send Verification Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Spacer(modifier = Modifier.height(20.dp))

                    // GOOGLE SIGN IN BUTTON
                    OutlinedButton(
                        onClick = { viewModel.signInWithGoogle("mock_google_id_token") },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("google_login_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = "Google Icon",
                                tint = Color(0xFFEA4335),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "Continue with Google",
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ANONYMOUS GUEST OPTION
            TextButton(
                onClick = { viewModel.signInAnonymously() },
                modifier = Modifier.testTag("anonymous_login_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Continue as Anonymous Citizen Advocate",
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "Arrow",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color.White else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        ),
        modifier = modifier
            .padding(2.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF1E293B) else Color(0xFF64748B)
            )
        }
    }
}
