package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.Helpers
import java.util.Calendar

@Composable
fun ReportDateDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reportDate by remember { mutableStateOf(currentDate) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعيين تاريخ وطباعة التقرير 📅",
                color = PalWhitePure,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "قم بتعيين أو كتابة التاريخ والوقت المراد إثباته وطباعته على المستند أو كشف التقرير بالكامل وبشكل صحيح (يمكنك كتابة أي تاريخ أو فترة مخصصة):",
                    color = PalWhiteMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = reportDate,
                    onValueChange = { reportDate = it },
                    label = { Text("تاريخ وطباعة التقرير والعمليات", color = PalWhiteMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PalGreenLight,
                        unfocusedBorderColor = PalBlackLight,
                        focusedTextColor = PalWhitePure,
                        unfocusedTextColor = PalWhiteSoft,
                        focusedContainerColor = PalBlackDark,
                        unfocusedContainerColor = PalBlackDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PalGreenLight)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reportDate) },
                colors = ButtonDefaults.buttonColors(containerColor = PalGreenNormal)
            ) {
                Text("تأكيد واعتماد التاريخ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        },
        containerColor = PalBlackNormal,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * A beautiful, highly-interactive unified report period and date picker selector component.
 * Supports: Specific Date (تحديد تاريخ), Specific Month (اختيار شهر), and Specific Year (اختيار سنة).
 */
@Composable
fun UnifiedPeriodSelector(
    modifier: Modifier = Modifier,
    initialType: String = "يوم", // "يوم", "شهر", "سنة"
    initialValue: String = Helpers.getCurrentDate(), // e.g. "2026-06-01"
    onPeriodChanged: (type: String, formattedValue: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) } // "يوم", "شهر", "سنة"
    
    // Day Selection State
    var selectedDay by remember { mutableStateOf(Helpers.getCurrentDate()) }
    // Month Selection State ("2026-06")
    val defaultMonth = if (initialValue.length >= 7) initialValue.substring(5, 7) else "06"
    val defaultYear = if (initialValue.length >= 4) initialValue.substring(0, 4) else "2026"
    
    var selectedMonth by remember { mutableStateOf(defaultMonth) }
    var selectedYearForMonth by remember { mutableStateOf(defaultYear) }
    // Year Selection State ("2026")
    var selectedYearOnly by remember { mutableStateOf(defaultYear) }

    // Broadcaster
    val triggerChange = {
        val finalVal = when (selectedType) {
            "يوم" -> selectedDay
            "شهر" -> "$selectedYearForMonth-$selectedMonth"
            "سنة" -> selectedYearOnly
            else -> selectedDay
        }
        onPeriodChanged(selectedType, finalVal)
    }

    LaunchedEffect(selectedType, selectedDay, selectedMonth, selectedYearForMonth, selectedYearOnly) {
        triggerChange()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PalBlackDark)
            .border(BorderStroke(1.dp, PalBlackLight), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "تصفية الفترات وتقويم الجرائد 📆",
            color = PalWhitePure,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )

        // Triple Segmented Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("يوم", "شهر", "سنة").forEach { type ->
                val isSel = selectedType == type
                val label = when (type) {
                    "يوم" -> "تحديد تاريخ اليوم"
                    "شهر" -> "حساب شهر محدد"
                    "سنة" -> "حساب سنة كاملة"
                    else -> type
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) PalGreenNormal else PalBlackLight)
                        .clickable { selectedType = type }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSel) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Divider(color = PalBlackLight, thickness = 0.5.dp)

        // Sub-interfaces based on selection
        when (selectedType) {
            "يوم" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تقويم اليوم المالي (السنة-الشهر-اليوم):", color = PalWhiteSoft, fontSize = 10.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            // Decrement day
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                val date = sdf.parse(selectedDay) ?: java.util.Date()
                                val cal = Calendar.getInstance()
                                cal.time = date
                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                selectedDay = sdf.format(cal.time)
                            } catch (e: Exception) {}
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "السابق", tint = PalGreenLight)
                        }

                        OutlinedTextField(
                            value = selectedDay,
                            onValueChange = { selectedDay = it },
                            placeholder = { Text("yyyy-MM-dd", color = PalWhiteMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PalGreenLight,
                                unfocusedBorderColor = PalBlackLight,
                                focusedTextColor = PalWhitePure,
                                unfocusedTextColor = PalWhiteSoft
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        )

                        IconButton(onClick = {
                            // Increment day
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                val date = sdf.parse(selectedDay) ?: java.util.Date()
                                val cal = Calendar.getInstance()
                                cal.time = date
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                selectedDay = sdf.format(cal.time)
                            } catch (e: Exception) {}
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "التالي", tint = PalGreenLight)
                        }
                    }
                }
            }
            "شهر" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تعيين الشهر والسنة للبحث:", color = PalWhiteSoft, fontSize = 10.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Month Selector
                        OutlinedTextField(
                            value = selectedMonth,
                            onValueChange = { selectedMonth = it.take(2).filter { c -> c.isDigit() } },
                            label = { Text("الشهر (01-12)", color = PalWhiteMuted, fontSize = 9.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PalGreenLight,
                                unfocusedBorderColor = PalBlackLight,
                                focusedTextColor = PalWhitePure,
                                unfocusedTextColor = PalWhiteSoft
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        )

                        // Year Selector
                        OutlinedTextField(
                            value = selectedYearForMonth,
                            onValueChange = { selectedYearForMonth = it.take(4).filter { c -> c.isDigit() } },
                            label = { Text("السنة (yyyy)", color = PalWhiteMuted, fontSize = 9.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PalGreenLight,
                                unfocusedBorderColor = PalBlackLight,
                                focusedTextColor = PalWhitePure,
                                unfocusedTextColor = PalWhiteSoft
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }
            "سنة" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تعيين السنة (مثال: 2026):", color = PalWhiteSoft, fontSize = 10.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            val cur = selectedYearOnly.toIntOrNull() ?: 2026
                            selectedYearOnly = (cur - 1).toString()
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "السنة السابقة", tint = PalGreenLight)
                        }

                        OutlinedTextField(
                            value = selectedYearOnly,
                            onValueChange = { selectedYearOnly = it.take(4).filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PalGreenLight,
                                unfocusedBorderColor = PalBlackLight,
                                focusedTextColor = PalWhitePure,
                                unfocusedTextColor = PalWhiteSoft
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        )

                        IconButton(onClick = {
                            val cur = selectedYearOnly.toIntOrNull() ?: 2026
                            selectedYearOnly = (cur + 1).toString()
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "السنة التالية", tint = PalGreenLight)
                        }
                    }
                }
            }
        }
    }
}
