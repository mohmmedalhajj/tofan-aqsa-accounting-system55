package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AppViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun SplashScreen(viewModel: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PalBlackDark, PalGreenDark, PalBlackDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "flag_wave")
        val flagOffset by infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offset"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Premium styled launcher logo with animated floating offset
            AgencyPremiumLogo(
                logoSize = 170.dp,
                modifier = Modifier.offset(y = flagOffset.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "وكالة عاهد الصبري",
                color = PalGoldCalligraphy,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "لأجود أنواع القات الماوية والورزاني",
                color = PalWhiteSoft,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = PalRedLight,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "نظام محاسبي متكامل وإداري ذكي",
                color = PalGreenLight,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val isPassVisible by viewModel.isPasswordVisible.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PalBlackDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            // Premium styled logo badge
            AgencyPremiumLogo(
                logoSize = 125.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "تسجيل الدخول",
                color = PalWhitePure,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Text(
                text = "نظام وكالة عاهد الصبري المحاسبي والإداري",
                color = PalGreenLight,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PalBlackNormal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = PalRedLight,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Username field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم", color = PalWhiteMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PalGreenLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور", color = PalWhiteMuted) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PalGreenLight) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.isPasswordVisible.value = !isPassVisible }) {
                                Icon(
                                    imageVector = if (isPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = PalWhiteMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PalGreenLight,
                            unfocusedBorderColor = PalBlackLight,
                            focusedTextColor = PalWhitePure,
                            unfocusedTextColor = PalWhiteSoft
                        ),
                        singleLine = true,
                        visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = PalGreenLight)
                        )
                        Text(
                            text = "حفظ بيانات تسجيل الدخول",
                            color = PalWhiteSoft,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val success = viewModel.attemptLogin(username, password, rememberMe)
                            if (!success) {
                                errorMessage = "خطأ في اسم المستخدم أو كلمة المرور!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "دخول",
                            color = PalWhitePure,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AgencyPremiumLogo(
    modifier: Modifier = Modifier,
    logoSize: androidx.compose.ui.unit.Dp = 120.dp
) {
    Box(
        modifier = modifier
            .size(logoSize)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing radial light scheme matching Palestinian colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PalGreenDark.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Luxury custom geometric framed borders
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.5.dp, PalGoldCalligraphy, RoundedCornerShape(24.dp))
                .padding(4.dp)
                .border(1.dp, PalGreenLight.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_agency_logo_1780160497858),
                contentDescription = "شعار وكالة عاهد الصبري",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
