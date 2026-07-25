package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import java.text.SimpleDateFormat
import java.util.*

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
