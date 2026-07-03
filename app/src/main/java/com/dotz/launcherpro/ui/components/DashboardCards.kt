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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Date
import java.util.Locale

data class MindfulnessInfo(
    val pkg: String,
    val label: String,
    val usageTime: String?,
    val launchCount: Int
)

@Composable
fun MindfulnessDialog(
    label: String,
    usageTime: String,
    launchCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Mindful Check",
        content = {
            Column {
                Text(
                    "You've opened $label $launchCount times today.",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Total time spent: $usageTime",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Do you really want to open it again?",
                    color = Color.White.copy(alpha = 0.6f),
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
    onItemClick: (String?) -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onReply: (String, String) -> Unit = { _, _ -> },
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DotzTheme.colors.tile),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DotzTheme.colors.text.copy(alpha = 0.05f))
                        .clickable { onItemClick(item.packageName) },
                    contentAlignment = Alignment.Center
                ) {
                    AppIconOrGeneric(item)
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onItemClick(item.packageName) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = DotzTheme.colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.type == TimelineType.SPONSORED) {
                            SponsoredBadge(Modifier.padding(start = 8.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                            color = DotzTheme.colors.text.copy(alpha = 0.3f),
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = item.subtitle,
                        color = DotzTheme.colors.text.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (item.type == TimelineType.MUSIC) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, null, tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = DotzTheme.colors.text,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, null, tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
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
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
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
                BitmapPainter(icon.toBitmap().asImageBitmap())
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

@Composable
fun SponsoredBadge(modifier: Modifier = Modifier) {
    Surface(
        color = DotzTheme.colors.accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "SPONSORED", 
            color = DotzTheme.colors.accent.copy(alpha = 0.8f), 
            fontSize = 6.sp, 
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), 
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

fun getTimelineIcon(type: TimelineType): ImageVector {
    return when (type) {
        TimelineType.CALL -> Icons.Default.Call
        TimelineType.MESSAGE -> Icons.Default.Chat
        TimelineType.PHOTO -> Icons.Default.Photo
        TimelineType.MUSIC -> Icons.Default.MusicNote
        TimelineType.APP_LAUNCH -> Icons.Default.RocketLaunch
        TimelineType.CALENDAR -> Icons.Default.CalendarToday
        TimelineType.SPONSORED -> Icons.Default.Campaign
    }
}
