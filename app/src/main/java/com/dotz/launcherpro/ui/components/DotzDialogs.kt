package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF121212), // Deep dark for modern look
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
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
                        containerColor = Color.White,
                        contentColor = Color.Black
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
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Text(
                            text = dismissButtonText.uppercase(),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        dismissButton = null // We handle both buttons in confirmButton for vertical layout
    )
}
