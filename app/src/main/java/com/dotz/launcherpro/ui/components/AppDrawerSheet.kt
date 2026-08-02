package com.dotz.launcherpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.dotz.launcherpro.data.DrawerApp
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.ui.theme.DotzType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSheet(
    apps: List<DrawerApp>,
    suggestedApps: List<DrawerApp> = emptyList(),
    onDismiss: () -> Unit,
    onLaunch: (String, String?) -> Unit
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    val containerColor = if (isGlass) Color.Transparent else DotzTheme.colors.solidBackground

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = glassColor.copy(alpha = 0.2f))
        },
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .then(
                if (isGlass) {
                    Modifier.drawBehind {
                        // Blurred glass effect logic is handled by background brush in theme
                        // Here we just add the border and slightly tint the sheet
                        drawRect(color = glassColor.copy(alpha = 0.05f))
                        
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(glassColor.copy(alpha = 0.3f), Color.Transparent, glassColor.copy(alpha = 0.1f)),
                                start = androidx.compose.ui.geometry.Offset.Zero,
                                end = androidx.compose.ui.geometry.Offset.Infinite
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            strokeWidth = strokeWidth
                        )
                    }
                } else Modifier
            )
    ) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, apps) {
            if (query.isBlank()) apps
            else apps.filter { it.label.contains(query, ignoreCase = true) }
        }

        val grouped = remember(filtered) {
            filtered
                .sortedBy { it.label.uppercase() }
                .groupBy { it.label.trim().firstOrNull()?.uppercaseChar() ?: '#' }
                .toSortedMap()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "ALL APPS",
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = glassColor.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Minimal Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Search tools...", color = glassColor.copy(alpha = 0.2f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = glassColor.copy(alpha = 0.3f)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = glassColor.copy(alpha = 0.3f),
                    unfocusedBorderColor = glassColor.copy(alpha = 0.1f),
                    focusedTextColor = glassColor,
                    unfocusedTextColor = glassColor,
                    cursorColor = glassColor
                )
            )

            Spacer(Modifier.height(8.dp))

            if (query.isBlank()) {
                if (suggestedApps.isNotEmpty()) {
                    Text(
                        text = "SUGGESTED",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColor.copy(alpha = 0.3f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        suggestedApps.forEach { app ->
                            PredictiveAppItem(app, glassColor, onLaunch)
                        }
                        // Fill space if less than 3
                        repeat(3 - suggestedApps.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TYPE TO SEARCH",
                            color = glassColor.copy(alpha = 0.2f),
                            style = DotzType.timeStyle().copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 4.sp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mindful access only",
                            color = glassColor.copy(alpha = 0.1f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Grouped Text List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    grouped.forEach { (initial, appsInGroup) ->
                        item {
                            Text(
                                text = initial.toString(),
                                color = glassColor.copy(alpha = 0.2f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }
                        items(appsInGroup) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLaunch(app.packageName, app.componentName) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.label.uppercase(),
                                    color = glassColor,
                                    style = DotzType.timeStyle().copy(fontSize = 18.sp, fontWeight = FontWeight.Light),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (app.usageTime != null) {
                                    Text(
                                        text = app.usageTime,
                                        color = glassColor.copy(alpha = 0.3f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            // Optional subtle divider
                            HorizontalDivider(color = glassColor.copy(alpha = 0.05f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictiveAppItem(
    app: DrawerApp,
    textColor: Color,
    onClick: (String, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(app.packageName, app.componentName) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var icon by remember(app.packageName) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
        
        LaunchedEffect(app.packageName) {
            withContext(Dispatchers.IO) {
                try {
                    icon = context.packageManager.getApplicationIcon(app.packageName)
                } catch (_: Exception) {}
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(textColor.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon?.let { drawable ->
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.graphics.painter.BitmapPainter(drawable.toBitmap(128, 128).asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
                )
            } ?: Icon(Icons.Default.Apps, null, tint = textColor.copy(alpha = 0.2f))
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = app.label.lowercase(),
            color = textColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
