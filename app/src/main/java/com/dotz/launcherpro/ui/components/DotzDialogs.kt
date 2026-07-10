package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun DotzAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor = containerColor
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
