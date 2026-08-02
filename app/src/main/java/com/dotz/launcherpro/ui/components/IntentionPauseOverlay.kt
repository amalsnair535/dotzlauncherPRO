package com.dotz.launcherpro.ui.components

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

@Composable
fun IntentionPauseOverlay(
    useBiometric: Boolean = false,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(3) }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        if (!useBiometric) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            onFinished()
        }
    }

    val authenticate = {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onFinished()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Intention Check")
            .setSubtitle("Confirm your intention to open this app")
            .setNegativeButtonText("Never mind")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Wait a moment...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Do you really need to open this app right now?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))
            
            if (useBiometric) {
                Button(
                    onClick = { authenticate() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(56.dp).fillMaxWidth(0.7f)
                ) {
                    Text("AUTHENTICATE TO OPEN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "$timeLeft",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(64.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = CircleShape
            ) {
                Text("NEVER MIND", color = Color.White)
            }
        }
    }
}

