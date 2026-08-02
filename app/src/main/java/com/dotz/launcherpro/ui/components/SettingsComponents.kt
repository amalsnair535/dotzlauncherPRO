package com.dotz.launcherpro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.R
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.ThemeMode

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.ExtraBold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
    )
}

@Composable
fun ModernSettingsActionRow(
    label: String,
    icon: ImageVector,
    subtitle: String? = null,
    isPremium: Boolean = false,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !isLocked,
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isLocked) 0.3f else 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isPremium && isLocked) {
                        Spacer(Modifier.width(8.dp))
                        PremiumBadge()
                    }
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ModernSettingsToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    subtitle: String? = null,
    isPremium: Boolean = false,
    isLocked: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        onClick = { onToggle(!checked) },
        enabled = !isLocked,
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconContainerColor by animateColorAsState(
                if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                label = "iconContainer"
            )
            val iconTint by animateColorAsState(
                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isLocked) 0.3f else 0.8f),
                label = "iconTint"
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isPremium && isLocked) {
                        Spacer(Modifier.width(8.dp))
                        PremiumBadge()
                    }
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                enabled = !isLocked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun SettingsPreviewCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun MiniAppTile(
    label: String,
    icon: ImageVector,
    transparency: Float,
    isList: Boolean,
    modifier: Modifier = Modifier
) {
    val isGlass = DotzTheme.colors.isGlass
    val glassColor = DotzTheme.colors.text
    
    if (isList) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isGlass) glassColor.copy(alpha = 0.05f) 
                    else DotzTheme.colors.tile.copy(alpha = transparency)
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = DotzTheme.colors.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .width(40.dp)
                    .height(8.dp)
                    .background(DotzTheme.colors.text.copy(alpha = 0.2f), CircleShape))
        }
    } else {
        Column(
            modifier = modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isGlass) glassColor.copy(alpha = 0.05f) 
                    else DotzTheme.colors.tile.copy(alpha = transparency)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = DotzTheme.colors.text, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .background(DotzTheme.colors.text.copy(alpha = 0.2f), CircleShape))
        }
    }
}

@Composable
fun TonalSegmentedControl(
    options: List<Pair<String, String>>, 
    selected: String, 
    proOptions: Set<String> = emptySet(),
    onSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            val isPro = proOptions.contains(mode)
            
            Surface(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(mode) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        label, 
                        style = MaterialTheme.typography.labelLarge, 
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isPro) {
                        Spacer(Modifier.width(6.dp))
                        PremiumBadge()
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeModeSelectionRow(currentMode: ThemeMode, isPremium: Boolean, isUpgradeAvailable: Boolean, isLiteVersion: Boolean, onModeChange: (ThemeMode) -> Unit, onShowPremiumDialog: () -> Unit) {
    val modes = if (isLiteVersion) listOf(ThemeMode.LIGHT, ThemeMode.DARK) else ThemeMode.entries
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.forEach { mode ->
            val isSelected = currentMode == mode
            val isPro = mode == ThemeMode.CIRCADIAN || mode == ThemeMode.TRANSPARENT || mode == ThemeMode.CUSTOM
            val locked = isPro && isUpgradeAvailable && !isPremium

            Surface(
                modifier = Modifier
                    .width(84.dp)
                    .height(72.dp)
                    .clickable { if (locked) onShowPremiumDialog() else onModeChange(mode) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when(mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.CIRCADIAN -> Icons.Default.Schedule
                                ThemeMode.TRANSPARENT -> Icons.Default.Wallpaper
                                ThemeMode.CUSTOM -> Icons.Default.ColorLens
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            mode.name, 
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                        if (locked) {
                            PremiumBadge(Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransparencySlider(value: Float, isPremium: Boolean, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Opacity, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.label_tile_transparency), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    PremiumBadge()
                }
            }
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun TileLayoutSelection(current: String, isPremium: Boolean, onChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.label_tile_layout), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (!isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("classic" to stringResource(R.string.layout_grid), "list" to stringResource(R.string.layout_list)).forEach { (style, label) ->
                val selected = current == style
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { onChange(style) }, 
                    shape = RoundedCornerShape(16.dp), 
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label, 
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FontSelectionRow(currentFontId: String, isPremium: Boolean, isUpgradeAvailable: Boolean, onFontChange: (String) -> Unit, onShowPremiumDialog: () -> Unit) {
    val fonts = listOf(
        "default" to "Default",
        "serif" to "Serif",
        "monospace" to "Mono",
        "sans-serif" to "Sans"
    )
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FontDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Typography", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            if (!isPremium && isUpgradeAvailable) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
        Spacer(Modifier.height(12.dp))
        TonalSegmentedControl(
            options = fonts,
            selected = currentFontId,
            proOptions = if (!isPremium && isUpgradeAvailable) fonts.map { it.first }.filter { it != "default" }.toSet() else emptySet(),
            onSelect = { 
                if (it == "default" || isPremium || !isUpgradeAvailable) onFontChange(it)
                else onShowPremiumDialog()
            }
        )
    }
}

@Composable
fun ColorPaletteRow(
    selectedColor: Int?,
    onColorSelect: (Int) -> Unit
) {
    val colors = listOf(
        0xFFFFFFFF.toInt(), // White
        0xFF88C0D0.toInt(), // Nordic Blue
        0xFF8DA37E.toInt(), // Forest Green
        0xFFE9967A.toInt(), // Sunset Glow
        0xFF38BDF8.toInt(), // Electric Blue
        0xFFA78BFA.toInt(), // Purple
        0xFFF472B6.toInt(), // Pink
        0xFFFBBF24.toInt(), // Amber
        0xFFEF4444.toInt(), // Red
        0xFF10B981.toInt()  // Emerald
    )

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        colors.forEach { colorInt ->
            val color = Color(colorInt)
            val isSelected = selectedColor == colorInt
            val colorName = when(colorInt) {
                0xFFFFFFFF.toInt() -> "White"
                0xFF88C0D0.toInt() -> "Nordic Blue"
                0xFF8DA37E.toInt() -> "Forest Green"
                0xFFE9967A.toInt() -> "Sunset Glow"
                0xFF38BDF8.toInt() -> "Electric Blue"
                0xFFA78BFA.toInt() -> "Purple"
                0xFFF472B6.toInt() -> "Pink"
                0xFFFBBF24.toInt() -> "Amber"
                0xFFEF4444.toInt() -> "Red"
                0xFF10B981.toInt() -> "Emerald"
                else -> "Custom Color"
            }
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
                    .clickable(
                        onClickLabel = "Select $colorName theme color",
                        onClick = { onColorSelect(colorInt) }
                    )
                    .semantics { 
                        contentDescription = if (isSelected) "Selected color: $colorName" else "Color: $colorName" 
                    }
            )
        }
    }
}

@Composable
fun ProfileManagementCard(activeId: String, profiles: List<com.dotz.launcherpro.data.LauncherProfile>, isPremium: Boolean, isUpgradeAvailable: Boolean, onSwitch: (String) -> Unit, onDelete: (String) -> Unit, onAddClick: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        ProfileItem(stringResource(R.string.label_default), activeId == "default", false, { onSwitch("default") }, null)
        profiles.filter { it.id != "default" }.forEach { profile ->
            ProfileItem(
                name = profile.name,
                active = activeId == profile.id,
                locked = isUpgradeAvailable && !isPremium,
                onClick = { if (!isUpgradeAvailable || isPremium) onSwitch(profile.id) },
                onDelete = { onDelete(profile.id) }
            )
        }
        TextButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_item_add_profile))
            if (isUpgradeAvailable && !isPremium) {
                Spacer(Modifier.width(8.dp))
                PremiumBadge()
            }
        }
    }
}

@Composable
fun ProfileItem(name: String, active: Boolean, locked: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth().padding(4.dp).clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Transparent).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = active, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface))
            Spacer(Modifier.width(8.dp))
            Text(name, color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (locked) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            } else if (onDelete != null && !active) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun PremiumPromotionCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, 
        shape = RoundedCornerShape(28.dp), 
        color = MaterialTheme.colorScheme.onSurface
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.premium_promo_title), 
                        style = MaterialTheme.typography.headlineSmall, 
                        color = MaterialTheme.colorScheme.surface, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "SALE 50% OFF", 
                            color = MaterialTheme.colorScheme.onPrimary, 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Text(
                    stringResource(R.string.premium_promo_subtitle), 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            }
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun IconPackDialog(currentIconPack: String?, iconPacks: List<Pair<String, String>>, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    DotzAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_select_icon_pack),
        content = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    Text(stringResource(R.string.label_default), color = if (currentIconPack == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
                items(iconPacks.size) { index ->
                    val (pkg, name) = iconPacks[index]
                    Text(name, color = if (currentIconPack == pkg) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth().clickable { onSelect(pkg) }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButtonText = stringResource(R.string.btn_cancel),
        onConfirm = onDismiss
    )
}
