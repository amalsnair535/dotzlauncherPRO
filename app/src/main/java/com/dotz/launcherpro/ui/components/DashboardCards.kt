package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.dotz.launcherpro.data.TimelineItem
import com.dotz.launcherpro.data.TimelineType
import com.dotz.launcherpro.ui.theme.DotzTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MindfulnessInfo(
    val pkg: String,
    val label: String,
    val usageTime: String?,
    val launchCount: Int,
    val comp: String? = null
)

@Composable
fun FocusHistoryChart(
    history: List<Pair<String, Int>>,
    hasPermission: Boolean,
    onEnablePermission: () -> Unit
) {
    var selectedDate by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FOCUS HISTORY",
                style = MaterialTheme.typography.labelMedium,
                color = DotzTheme.colors.text.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (hasPermission && (history.isNotEmpty() || selectedDate != null)) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val displayDate = selectedDate ?: today
                val score = history.toMap()[displayDate] ?: if (displayDate == today) 100 else 0
                
                Text(
                    text = if (selectedDate != null && selectedDate != today) "$displayDate: $score%" else "$score%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (score >= 80) DotzTheme.colors.accent else DotzTheme.colors.text.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DotzTheme.colors.text.copy(alpha = 0.05f))
                    .clickable { onEnablePermission() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "TAP TO ENABLE ACCURATE STATS",
                    style = MaterialTheme.typography.labelSmall,
                    color = DotzTheme.colors.text.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }
            return@Column
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val calendar = Calendar.getInstance()
            // Find current week's Monday
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = if (currentDay == Calendar.SUNDAY) 6 else currentDay - Calendar.MONDAY
            calendar.add(Calendar.DATE, -daysToSubtract)
            
            val historyMap = history.toMap()
            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

            (0..6).forEach { i ->
                val day = calendar.clone() as Calendar
                day.add(Calendar.DATE, i)
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(day.time)
                val score = historyMap[dateStr] ?: 0
                val isToday = dateStr == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val isFuture = day.timeAfter(Date())

                val barHeight = (score.toFloat() / 100f).coerceIn(0.05f, 1f) * 80f
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (!isFuture) {
                            selectedDate = if (selectedDate == dateStr) null else dateStr
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(80.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = if (selectedDate == dateStr) DotzTheme.colors.text.copy(alpha = 0.1f)
                                            else DotzTheme.colors.text.copy(alpha = 0.03f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                        // Score bar
                        if (!isFuture) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight.dp)
                                    .background(
                                        color = if (selectedDate == dateStr) DotzTheme.colors.accent
                                                else if (score >= 80) DotzTheme.colors.accent
                                                else if (isToday) DotzTheme.colors.text.copy(alpha = 0.4f)
                                                else DotzTheme.colors.text.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = dayLabels[i],
                        fontSize = 10.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) DotzTheme.colors.text else DotzTheme.colors.text.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

private fun Calendar.timeAfter(date: Date): Boolean {
    val other = Calendar.getInstance()
    other.time = date
    if (get(Calendar.YEAR) > other.get(Calendar.YEAR)) return true
    if (get(Calendar.YEAR) < other.get(Calendar.YEAR)) return false
    return get(Calendar.DAY_OF_YEAR) > other.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun MindfulnessDialog(
    label: String,
    usageTime: String,
    launchCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Mindful Check",
        content = {
            Column {
                Text(
                    "You've opened $label $launchCount times today.",
                    color = textColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Total time spent: $usageTime",
                    color = textColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Do you really want to open it again?",
                    color = textColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButtonText = "YES, PROCEED",
        onConfirm = onConfirm,
        dismissButtonText = "NO, STAY FOCUSED",
        onDismiss = onDismiss
    )
}

@Composable
fun TimelineCard(
    item: TimelineItem, 
    onItemClick: (String?, String?) -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onReply: (String, String) -> Unit = { _, _ -> },
    onDeleteJournal: (String) -> Unit = {},
    onEditJournal: (String, String) -> Unit = { _, _ -> },
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val containerColor = if (isGlass) glassColor.copy(alpha = 0.05f) else DotzTheme.colors.tile

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (isGlass) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(glassColor.copy(alpha = 0.3f), Color.Transparent, glassColor.copy(alpha = 0.1f)),
                                start = androidx.compose.ui.geometry.Offset.Zero,
                                end = androidx.compose.ui.geometry.Offset.Infinite
                            ),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = if (item.type == TimelineType.MUSIC) Alignment.Top else Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DotzTheme.colors.text.copy(alpha = 0.03f))
                        .clickable { onItemClick(item.packageName, item.componentName) },
                    contentAlignment = Alignment.Center
                ) {
                    AppIconOrGeneric(item)
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onItemClick(item.packageName, item.componentName) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.title,
                            color = DotzTheme.colors.text,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                            color = DotzTheme.colors.text.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    if (item.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = item.subtitle,
                                color = DotzTheme.colors.text.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                maxLines = if (item.type == TimelineType.JOURNAL) 10 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (item.type == TimelineType.JOURNAL) {
                                            Modifier.clickable { onEditJournal(item.id, item.subtitle) }
                                        } else Modifier
                                    )
                            )
                            
                            if (item.type == TimelineType.JOURNAL) {
                                IconButton(
                                    onClick = { onDeleteJournal(item.id) },
                                    modifier = Modifier.size(32.dp).padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Note",
                                        tint = DotzTheme.colors.text.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (item.type == TimelineType.MUSIC) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, "Previous Track", tint = DotzTheme.colors.text, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = DotzTheme.colors.text,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, "Next Track", tint = DotzTheme.colors.text, modifier = Modifier.size(24.dp))
                    }
                }
            }

            if (item.canReply && item.notificationKey != null) {
                Spacer(Modifier.height(12.dp))
                if (!showReplyInput) {
                    TextButton(
                        onClick = { showReplyInput = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("REPLY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Write a reply...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (replyText.isNotBlank()) {
                                    onReply(item.notificationKey, replyText)
                                    replyText = ""
                                    showReplyInput = false
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.3f),
                                unfocusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.1f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            if (replyText.isNotBlank()) {
                                onReply(item.notificationKey, replyText)
                                replyText = ""
                                showReplyInput = false
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send Reply", tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconOrGeneric(item: TimelineItem) {
    val context = LocalContext.current
    val painter = remember(item.packageName) {
        try {
            if (item.packageName != null) {
                val icon = context.packageManager.getApplicationIcon(item.packageName)
                val target = 128
                val width = if (icon.intrinsicWidth > 0) icon.intrinsicWidth.coerceAtMost(target) else target
                val height = if (icon.intrinsicHeight > 0) icon.intrinsicHeight.coerceAtMost(target) else target
                BitmapPainter(icon.toBitmap(width, height).asImageBitmap())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    if (painter != null) {
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    } else {
        Icon(
            imageVector = getTimelineIcon(item.type),
            contentDescription = null,
            tint = DotzTheme.colors.accent,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "PRO", 
            color = Color.White.copy(alpha = 0.7f), 
            fontSize = 7.sp, 
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), 
            fontWeight = FontWeight.Black
        )
    }
}

fun getTimelineIcon(type: TimelineType): ImageVector {
    return when (type) {
        TimelineType.CALL -> Icons.Default.Call
        TimelineType.MESSAGE -> Icons.AutoMirrored.Filled.Chat
        TimelineType.PHOTO -> Icons.Default.Photo
        TimelineType.MUSIC -> Icons.Default.MusicNote
        TimelineType.APP_LAUNCH -> Icons.Default.RocketLaunch
        TimelineType.CALENDAR -> Icons.Default.CalendarToday
        TimelineType.SPONSORED -> Icons.Default.Campaign
        TimelineType.JOURNAL -> Icons.Default.EditNote
    }
}
