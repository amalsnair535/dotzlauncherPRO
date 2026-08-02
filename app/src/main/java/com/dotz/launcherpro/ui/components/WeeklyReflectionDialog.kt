package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.WeeklyReflection

@Composable
fun WeeklyReflectionDialog(
    reflection: WeeklyReflection,
    onDismiss: () -> Unit
) {
    val textColor = DotzTheme.colors.text
    
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Weekly Reflection",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReflectionRow(
                    label = "Focus Score",
                    value = reflection.focusScore.toString(),
                    deltaText = "${if (reflection.focusScoreDelta >= 0) "+" else ""}${reflection.focusScoreDelta}",
                    isPositive = reflection.focusScoreDelta >= 0,
                    icon = if (reflection.focusScoreDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
                )
                
                ReflectionRow(
                    label = "Unlocks",
                    value = reflection.unlocks.toString(),
                    deltaText = "${reflection.unlocksDeltaPercent}%",
                    isPositive = reflection.unlocksDeltaPercent < 0, // Less unlocks is positive
                    icon = if (reflection.unlocksDeltaPercent < 0) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp
                )

                HorizontalDivider()

                StatsGrid(reflection)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DotzTheme.colors.accent.copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Digital Wellness", fontSize = 10.sp, color = textColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        Text(reflection.wellnessRating.uppercase(), fontSize = 18.sp, color = DotzTheme.colors.accent, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButtonText = "DISMISS",
        onConfirm = onDismiss
    )
}

@Composable
private fun ReflectionRow(
    label: String,
    value: String,
    deltaText: String,
    isPositive: Boolean,
    icon: ImageVector
) {
    val textColor = DotzTheme.colors.text
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isPositive) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                deltaText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
private fun StatsGrid(reflection: WeeklyReflection) {
    val textColor = DotzTheme.colors.text
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(label = "Notifications", value = reflection.notifications.toString(), modifier = Modifier.weight(1f))
            StatItem(label = "Ignored", value = reflection.ignored.toString(), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(label = "Longest Focus", value = reflection.longestFocus, modifier = Modifier.weight(1f))
            StatItem(label = "Most Productive Day", value = reflection.mostProductiveDay, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = DotzTheme.colors.text.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 14.sp, color = DotzTheme.colors.text, fontWeight = FontWeight.Medium)
    }
}
