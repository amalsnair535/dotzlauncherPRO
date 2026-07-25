package com.dotz.launcherpro.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotz.launcherpro.ui.theme.DotzTheme
import com.dotz.launcherpro.viewmodel.LauncherViewModel

class UltraFocusAppSelectionActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val selectionSettings = remember(uiState.settings) {
                uiState.settings.copy(showWallpaper = false)
            }

            DotzTheme(settings = selectionSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DotzTheme.colors.background
                ) {
                    UltraFocusAppSelectionScreen(
                        allApps = viewModel.getInstalledApps().sortedBy { it.label.lowercase() },
                        selectedPackages = uiState.settings.ultraFocusAppPackages,
                        onBack = { finish() },
                        onSave = { packages ->
                            viewModel.setUltraFocusApps(packages)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UltraFocusAppSelectionScreen(
    allApps: List<com.dotz.launcherpro.data.DrawerApp>,
    selectedPackages: List<String>,
    onBack: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(selectedPackages.toSet()) }
    var searchQuery by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ULTRA FOCUS APPS",
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = DotzTheme.colors.text,
                        )
                        Text(
                            text = "${selected.size}/7 SELECTED",
                            fontSize = 10.sp,
                            color = DotzTheme.colors.text.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DotzTheme.colors.text)
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(selected.toList()) }) {
                        Text("SAVE", color = DotzTheme.colors.accent, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DotzTheme.colors.background),
            )
        },
        containerColor = DotzTheme.colors.background,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                placeholder = { Text("Search apps…", color = DotzTheme.colors.text.copy(alpha = 0.3f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = DotzTheme.colors.text.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.4f),
                    unfocusedBorderColor = DotzTheme.colors.text.copy(alpha = 0.15f),
                    focusedTextColor = DotzTheme.colors.text,
                    unfocusedTextColor = DotzTheme.colors.text,
                    cursorColor = DotzTheme.colors.text,
                    focusedContainerColor = DotzTheme.colors.tile.copy(alpha = 0.05f),
                    unfocusedContainerColor = DotzTheme.colors.tile.copy(alpha = 0.05f)
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = selected.contains(app.packageName)
                    AppSelectionRow(
                        label = app.label,
                        pkg = app.packageName,
                        isChecked = isChecked,
                        onClick = {
                            if (isChecked) {
                                selected = selected - app.packageName
                            } else {
                                if (selected.size < 7) {
                                    selected = selected + app.packageName
                                } else {
                                    Toast.makeText(context, "Maximum 7 apps allowed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSelectionRow(label: String, pkg: String, isChecked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DotzTheme.colors.tile.copy(alpha = if (isChecked) 0.1f else 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = DotzTheme.colors.text, fontSize = 14.sp, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal)
            Text(
                pkg,
                color = DotzTheme.colors.text.copy(alpha = 0.35f),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isChecked) DotzTheme.colors.accent else DotzTheme.colors.text.copy(alpha = 0.2f)
        )
    }
}
