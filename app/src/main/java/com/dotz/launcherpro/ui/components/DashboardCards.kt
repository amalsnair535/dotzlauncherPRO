package com.dotz.launcherpro.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.ui.theme.DotzColors
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherUiState

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DotzTheme.colors.tile),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DotzTheme.colors.text,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = DotzTheme.colors.text,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun LockedDashboardCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = title,
        icon = icon,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = DotzTheme.colors.text.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "PRO",
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AppUsageCard(uiState: LauncherUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DashboardCard(
        title = "APP USAGE",
        icon = Icons.Default.BarChart,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.topApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Grant usage access to see stats",
                        color = DotzTheme.colors.text.copy(alpha = 0.3f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                uiState.topApps.take(5).forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label.uppercase(),
                            color = DotzTheme.colors.text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.usageTime ?: "0m",
                            color = DotzTheme.colors.text.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            HorizontalDivider(color = DotzTheme.colors.text.copy(alpha = 0.05f), thickness = 1.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SCREEN TIME", 
                    color = DotzTheme.colors.text.copy(alpha = 0.4f), 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    uiState.focusTimeToday, 
                    color = DotzTheme.colors.accent, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun AiSummaryCard(uiState: LauncherUiState, modifier: Modifier = Modifier) {
    DashboardCard(
        title = "AI SUMMARY",
        icon = Icons.Default.AutoAwesome,
        modifier = modifier
    ) {
        val messageCount = uiState.activeNotifications.count { it.packageName.contains("message") || it.packageName.contains("whatsapp") }
        val missedCalls = uiState.activeNotifications.count { it.packageName.contains("dialer") || it.packageName.contains("telecom") }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SummaryItem(messageCount.toString(), "Important messages")
            SummaryItem(missedCalls.toString(), "Missed calls")
            SummaryItem(uiState.activeNotifications.size.toString(), "Total updates")
        }
        
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = DotzTheme.colors.text.copy(alpha = 0.05f), thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = DotzTheme.colors.text,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if (uiState.activeNotifications.isEmpty()) "Everything looks calm." else "Stay focused.",
                    color = DotzTheme.colors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.activeNotifications.isEmpty()) "You're all good." else "Check later.",
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(count: String, label: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = count, color = DotzTheme.colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = label, 
            color = DotzTheme.colors.text.copy(alpha = 0.3f), 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NowPlayingCard(
    uiState: LauncherUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = "NOW PLAYING",
        icon = Icons.Default.MusicNote,
        modifier = modifier.heightIn(min = 180.dp)
    ) {
        if (uiState.nowPlayingTitle == "Not Playing" || uiState.nowPlayingTitle.isBlank()) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No music playing",
                    color = DotzTheme.colors.text.copy(alpha = 0.2f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DotzTheme.colors.text.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = DotzTheme.colors.text.copy(alpha = 0.2f), modifier = Modifier.size(24.dp))
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.nowPlayingTitle,
                            color = DotzTheme.colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = uiState.nowPlayingArtist,
                            color = DotzTheme.colors.text.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Progress Bar
                val progress = if (uiState.playbackDuration > 0) uiState.playbackPosition.toFloat() / uiState.playbackDuration else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = DotzTheme.colors.accent,
                    trackColor = DotzTheme.colors.text.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(Modifier.height(12.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onSkipPrevious) { 
                        Icon(Icons.Default.SkipPrevious, null, tint = DotzTheme.colors.text, modifier = Modifier.size(32.dp)) 
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DotzTheme.colors.text.copy(alpha = 0.05f))
                    ) { 
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = DotzTheme.colors.text,
                            modifier = Modifier.size(36.dp)
                        ) 
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = onSkipNext) { 
                        Icon(Icons.Default.SkipNext, null, tint = DotzTheme.colors.text, modifier = Modifier.size(32.dp)) 
                    }
                }
            }
        }
    }
}

@Composable
fun AiAssistantCard(
    uiState: LauncherUiState,
    onAsk: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    DashboardCard(
        title = "DOTZ AI",
        icon = Icons.Default.AutoAwesome,
        modifier = modifier.heightIn(min = 120.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (uiState.aiResponse != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DotzTheme.colors.text.copy(alpha = 0.03f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = uiState.aiResponse,
                            color = DotzTheme.colors.text.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        
                        if (!uiState.isAiLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.aiResponse))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, null, tint = DotzTheme.colors.text.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = {
                                    Toast.makeText(context, "Response reported. Thank you for your feedback.", Toast.LENGTH_LONG).show()
                                    onClear()
                                }) {
                                    Icon(Icons.Default.Flag, null, tint = DotzTheme.colors.text.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }
                                TextButton(onClick = onClear) {
                                    Text("CLEAR", color = DotzTheme.colors.text.copy(alpha = 0.3f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "AI may provide inaccurate info. Report inappropriate responses.",
                    color = DotzTheme.colors.text.copy(alpha = 0.2f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask anything...", color = DotzTheme.colors.text.copy(alpha = 0.2f), fontSize = 14.sp) },
                trailingIcon = {
                    if (uiState.isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = DotzTheme.colors.text)
                    } else {
                        IconButton(onClick = {
                            if (inputText.isNotBlank()) {
                                onAsk(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = DotzTheme.colors.text)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.1f),
                    focusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.3f),
                    cursorColor = DotzTheme.colors.text,
                    focusedTextColor = DotzTheme.colors.text,
                    unfocusedTextColor = DotzTheme.colors.text
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        onAsk(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                })
            )
        }
    }
}
