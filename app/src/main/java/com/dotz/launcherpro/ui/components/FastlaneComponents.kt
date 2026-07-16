package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.data.FastlaneEvent
import com.dotz.launcherpro.data.FastlaneType
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FastlaneTypographyCard(
    event: FastlaneEvent,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onAction() },
        verticalAlignment = Alignment.Top
    ) {
        // Time Metadata
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp)),
            style = DotzType.dateStyle().copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = DotzTheme.colors.text.copy(alpha = 0.3f),
            modifier = Modifier.width(52.dp).padding(top = 4.dp)
        )

        Spacer(Modifier.width(12.dp))

        // Event Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getMonochromeIcon(event.type),
                    contentDescription = null,
                    tint = DotzTheme.colors.text.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = event.title.uppercase(),
                    style = DotzType.timeStyle().copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = DotzTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (event.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = event.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DotzTheme.colors.text.copy(alpha = 0.5f),
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AISummaryCard(summary: String) {
    if (summary.isBlank()) return
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        color = DotzTheme.colors.text.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DotzTheme.colors.text.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "DAILY REFLECTION",
                style = DotzType.dateStyle().copy(fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = DotzTheme.colors.accent.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = DotzTheme.colors.text.copy(alpha = 0.9f),
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun UpcomingEventCard(event: com.dotz.launcherpro.manager.CalendarEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(DotzTheme.colors.accent)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyLarge,
            color = DotzTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.startTime)),
            style = DotzType.dateStyle().copy(fontSize = 12.sp),
            color = DotzTheme.colors.text.copy(alpha = 0.5f)
        )
    }
}

fun getMonochromeIcon(type: FastlaneType): ImageVector {
    return when (type) {
        FastlaneType.APP_OPEN -> Icons.Default.RocketLaunch
        FastlaneType.CALL_MISSED -> Icons.Default.CallMissed
        FastlaneType.CALL_INCOMING -> Icons.Default.CallReceived
        FastlaneType.CALL_OUTGOING -> Icons.Default.CallMade
        FastlaneType.SMS_RECEIVED -> Icons.Default.Message
        FastlaneType.MUSIC_TRACK -> Icons.Default.MusicNote
        FastlaneType.CAMERA_PHOTO -> Icons.Default.CameraAlt
        FastlaneType.CAMERA_SCREENSHOT -> Icons.Default.Screenshot
        FastlaneType.WIFI_STATUS -> Icons.Default.Wifi
        FastlaneType.BT_STATUS -> Icons.Default.Bluetooth
        FastlaneType.BATTERY_STATUS -> Icons.Default.BatteryChargingFull
        FastlaneType.NOTIF_HISTORY -> Icons.Default.NotificationsNone
        FastlaneType.CALENDAR -> Icons.Default.CalendarToday
        FastlaneType.SPONSORED -> Icons.Default.Campaign
        FastlaneType.FOCUS_SUMMARY -> Icons.Default.Psychology
    }
}
