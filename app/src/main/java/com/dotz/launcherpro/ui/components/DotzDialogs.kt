package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.ui.theme.DotzTheme

@Composable
fun UltraFocusExitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val textColor = DotzTheme.colors.text
    
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Leaving Focus Mode?",
        content = {
            Column {
                Text(
                    "You're about to exit your deep work session. Why are you stopping?",
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Reason for exiting...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = textColor.copy(alpha = 0.3f),
                        unfocusedBorderColor = textColor.copy(alpha = 0.1f),
                        cursorColor = textColor
                    )
                )
            }
        },
        confirmButtonText = "CONFIRM EXIT",
        confirmButtonEnabled = reason.isNotBlank(),
        onConfirm = { onConfirm(reason) },
        dismissButtonText = "STAY FOCUSED",
        onDismiss = onDismiss
    )
}

@Composable
fun AppDrawerConfirmDialog(
    openCount: Int, 
    totalAppOpens: Int,
    onDismiss: () -> Unit, 
    onConfirm: () -> Unit,
    onEmergencyConfirm: () -> Unit
) {
    val remaining = (5 - openCount).coerceAtLeast(0)
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = if (remaining > 0) "Open All Apps?" else "App Drawer Locked",
        content = {
            Column {
                if (remaining > 0) {
                    Text("Are you sure you want to open all apps?", color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("This gesture is for emergency access only.", color = textColor.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("You have used all 5 daily app drawer opens.", color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Opening it now will penalize your Focus Score by 10 points.", color = Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("REMAINING TODAY: $remaining/5", color = if (remaining > 0) textColor else Color.Red.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (remaining == 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("TOTAL APP OPENS: $totalAppOpens", color = textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButtonText = if (remaining > 0) "OPEN" else "EMERGENCY ACCESS",
        onConfirm = { if (remaining > 0) onConfirm() else onEmergencyConfirm() },
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}

@Composable
fun UsageStatsPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Mindful Usage Disclosure",
        content = { 
            Text(
                "Dotz Launcher uses anonymized usage statistics to track your screen time and device unlocks. " +
                "This information is processed only on your device to calculate your Focus Score and enable app usage limits. " +
                "No usage data is ever collected or transmitted.", 
                color = textColor.copy(alpha = 0.7f)
            ) 
        },
        confirmButtonText = "ENABLE",
        onConfirm = onGoToSettings,
        dismissButtonText = "NOT NOW",
        onDismiss = onDismiss
    )
}

@Composable
fun NotificationPermissionDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Enable Notifications",
        content = { Text("Allow Dotz to read notifications.", color = textColor.copy(alpha = 0.7f)) },
        confirmButtonText = "ENABLE",
        onConfirm = onGoToSettings,
        dismissButtonText = "SKIP",
        onDismiss = onDismiss
    )
}

@Composable
fun DefaultLauncherDialog(onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Set as Default Launcher",
        content = { Text("Use Dotz as your main home screen.", color = textColor.copy(alpha = 0.7f)) },
        confirmButtonText = "SET DEFAULT",
        onConfirm = onGoToSettings,
        dismissButtonText = "SKIP",
        onDismiss = onDismiss
    )
}

@Composable
fun AppAccessDisclosureDialog(onAccept: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = { },
        title = "App Visibility Disclosure",
        content = { 
            Text(
                "To function as a home screen, Dotz Launcher requires access to your list of installed applications. " +
                "This allows you to assign apps to tiles and use the App Drawer. " +
                "This data is used only to provide core launcher functionality and is never collected or shared.", 
                color = textColor.copy(alpha = 0.7f)
            ) 
        },
        confirmButtonText = "I UNDERSTAND",
        onConfirm = onAccept
    )
}

@Composable
fun UnassignedTileDialog(tileLabel: String, onDismiss: () -> Unit, onSelectApp: () -> Unit) {
    val textColor = DotzTheme.colors.text
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = "Unassigned Tile",
        content = { Text("Assign an app to $tileLabel?", color = textColor.copy(alpha = 0.7f)) },
        confirmButtonText = "SELECT APP",
        onConfirm = onSelectApp,
        dismissButtonText = "CANCEL",
        onDismiss = onDismiss
    )
}

@Composable
fun DotzAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    confirmButtonEnabled: Boolean = true,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    val isGlass = DotzTheme.colors.isGlass
    val textColor = DotzTheme.colors.text
    val containerColor = if (isGlass) {
        DotzTheme.colors.solidBackground.copy(alpha = 0.95f)
    } else {
        DotzTheme.colors.solidBackground
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = containerColor,
        modifier = if (isGlass) {
            Modifier.drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(textColor.copy(alpha = 0.3f), Color.Transparent, textColor.copy(alpha = 0.1f)),
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset.Infinite
                    ),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
        } else Modifier,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = confirmButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor = containerColor,
                        disabledContainerColor = textColor.copy(alpha = 0.3f),
                        disabledContentColor = containerColor.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Text(
                        text = confirmButtonText.uppercase(),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (dismissButtonText != null && onDismiss != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Text(
                            text = dismissButtonText.uppercase(),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        dismissButton = null // We handle both buttons in confirmButton for vertical layout
    )
}
