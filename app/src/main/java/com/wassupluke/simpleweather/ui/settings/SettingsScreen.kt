package com.wassupluke.simpleweather.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wassupluke.simpleweather.R
import com.wassupluke.simpleweather.data.parseColorSafe
import com.wassupluke.simpleweather.ui.theme.SimpleWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap?)

@Composable
private fun SetButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp).padding(end = 8.dp)
    ) {
        Text(stringResource(R.string.action_set))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onRequestDeviceLocation: () -> Unit,
    onDisableDeviceLocation: () -> Unit,
    onSetLocation: (String) -> Unit,
    onSetTempUnit: (String) -> Unit,
    onSetUpdateInterval: (Int) -> Unit,
    onSetWidgetTextColor: (String) -> Unit,
    onSetWidgetTapPackage: (String) -> Unit,
    onSetWidgetDynamicColor: (Boolean) -> Unit,
) {
    var locationInput by remember { mutableStateOf("") }
    var locationInputInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.locationQuery) {
        if (!locationInputInitialized && uiState.locationQuery.isNotEmpty()) {
            locationInput = uiState.locationQuery
            locationInputInitialized = true
        }
    }

    var showAppPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val installedApps by produceState(emptyList()) {
        value = withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            apps.sortedBy { it.loadLabel(context.packageManager).toString() }
                .map { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.applicationInfo.packageName
                    AppEntry(
                        pkg = pkg,
                        label = resolveInfo.loadLabel(context.packageManager).toString(),
                        icon = runCatching {
                            context.packageManager.getApplicationIcon(pkg).toBitmap().asImageBitmap()
                        }.getOrNull()
                    )
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.title_location), style = MaterialTheme.typography.titleSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!uiState.useDeviceLocation) onRequestDeviceLocation()
                        else onDisableDeviceLocation()
                    }
            ) {
                Text(stringResource(R.string.label_use_device_location), modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.useDeviceLocation,
                    onCheckedChange = { use ->
                        if (use) onRequestDeviceLocation() else onDisableDeviceLocation()
                    }
                )
            }

            if (!uiState.useDeviceLocation) {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text(stringResource(R.string.hint_location_input)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (locationInput.isNotBlank()) onSetLocation(locationInput)
                    }),
                    trailingIcon = {
                        if (locationInput.isNotBlank()) {
                            SetButton { onSetLocation(locationInput) }
                        }
                    }
                )
            }
            if (uiState.locationDisplayName.isNotEmpty()) {
                Text(
                    text = "Current: ${uiState.locationDisplayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.title_temperature_unit), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("C", "F").forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = uiState.tempUnit == unit,
                        onClick = { onSetTempUnit(unit) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        label = { Text("°$unit") }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.title_update_interval), style = MaterialTheme.typography.titleSmall)

            val intervalOptions = listOf(
                15 to stringResource(R.string.interval_15min),
                30 to stringResource(R.string.interval_30min),
                60 to stringResource(R.string.interval_1hr),
                180 to stringResource(R.string.interval_3hr),
                360 to stringResource(R.string.interval_6hr)
            )
            var intervalExpanded by remember { mutableStateOf(false) }
            val selectedLabel = intervalOptions.firstOrNull { it.first == uiState.updateIntervalMinutes }?.second ?: stringResource(R.string.interval_1hr)

            ExposedDropdownMenuBox(
                expanded = intervalExpanded,
                onExpandedChange = { intervalExpanded = !intervalExpanded }
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_interval)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = intervalExpanded,
                    onDismissRequest = { intervalExpanded = false }
                ) {
                    intervalOptions.forEach { (minutes, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSetUpdateInterval(minutes)
                                intervalExpanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetWidgetDynamicColor(!uiState.widgetDynamicColor) }
                ) {
                    Text(stringResource(R.string.label_dynamic_color), modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.widgetDynamicColor,
                        onCheckedChange = { onSetWidgetDynamicColor(it) }
                    )
                }
            }

            if (!uiState.widgetDynamicColor) {
                Text(stringResource(R.string.title_widget_text_color), style = MaterialTheme.typography.titleSmall)

                var colorInput by remember { mutableStateOf(uiState.widgetTextColor) }
                LaunchedEffect(uiState.widgetTextColor) { colorInput = uiState.widgetTextColor }

                val previewColor = remember(uiState.widgetTextColor) {
                    parseColorSafe(uiState.widgetTextColor)?.let { argb ->
                        Color(
                            red = android.graphics.Color.red(argb) / 255f,
                            green = android.graphics.Color.green(argb) / 255f,
                            blue = android.graphics.Color.blue(argb) / 255f,
                            alpha = android.graphics.Color.alpha(argb) / 255f
                        )
                    }
                }

                OutlinedTextField(
                    value = colorInput,
                    onValueChange = { colorInput = it },
                    label = { Text(stringResource(R.string.label_text_color)) },
                    placeholder = { Text(stringResource(R.string.hint_color_input)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onSetWidgetTextColor(colorInput.trim())
                    }),
                    trailingIcon = {
                        if (colorInput.isNotBlank()) {
                            SetButton { onSetWidgetTextColor(colorInput.trim()) }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(previewColor ?: MaterialTheme.colorScheme.errorContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                )

                if (previewColor == null && uiState.widgetTextColor.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.error_invalid_color),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                stringResource(R.string.title_widget_tap_action),
                style = MaterialTheme.typography.titleSmall
            )

            val selectedAppInfo = remember(uiState.widgetTapPackage) {
                if (uiState.widgetTapPackage.isEmpty()) null
                else runCatching {
                    context.packageManager.getApplicationInfo(uiState.widgetTapPackage, 0)
                }.getOrNull()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppPicker = true }
                    .padding(vertical = 8.dp)
            ) {
                if (selectedAppInfo != null) {
                    val icon by produceState<ImageBitmap?>(null, uiState.widgetTapPackage) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                context.packageManager
                                    .getApplicationIcon(uiState.widgetTapPackage)
                                    .toBitmap()
                                    .asImageBitmap()
                            }.getOrNull()
                        }
                    }
                    if (icon != null) {
                        Image(
                            bitmap = icon!!,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).padding(end = 8.dp)
                        )
                    } else {
                        Spacer(Modifier.size(40.dp).padding(end = 8.dp))
                    }
                    Text(
                        text = selectedAppInfo.loadLabel(context.packageManager).toString(),
                        modifier = Modifier.weight(1f)
                    )
                } else if (uiState.widgetTapPackage.isNotEmpty()) {
                    Spacer(Modifier.size(40.dp).padding(end = 8.dp))
                    Text(
                        text = stringResource(R.string.label_selected_app_not_found),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Spacer(Modifier.size(40.dp).padding(end = 8.dp))
                    Text(
                        text = stringResource(R.string.label_widget_tap_none),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showAppPicker) {
                ModalBottomSheet(onDismissRequest = { showAppPicker = false }) {
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSetWidgetTapPackage("")
                                        showAppPicker = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Spacer(Modifier.size(40.dp).padding(end = 12.dp))
                                Text(
                                    text = stringResource(R.string.label_widget_tap_none),
                                    modifier = Modifier.weight(1f)
                                )
                                if (uiState.widgetTapPackage.isEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        items(installedApps) { entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSetWidgetTapPackage(entry.pkg)
                                        showAppPicker = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                if (entry.icon != null) {
                                    Image(
                                        bitmap = entry.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).padding(end = 12.dp)
                                    )
                                } else {
                                    Spacer(Modifier.size(40.dp).padding(end = 12.dp))
                                }
                                Text(entry.label, modifier = Modifier.weight(1f))
                                if (entry.pkg == uiState.widgetTapPackage) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLocationPermissionGranted: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setUseDeviceLocation(true)
            onLocationPermissionGranted?.invoke()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        onRequestDeviceLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        onDisableDeviceLocation = { viewModel.setUseDeviceLocation(false) },
        onSetLocation = { viewModel.resolveAndSaveLocation(it) },
        onSetTempUnit = { viewModel.setTempUnit(it) },
        onSetUpdateInterval = { viewModel.setUpdateInterval(it) },
        onSetWidgetTextColor = { viewModel.setWidgetTextColor(it) },
        onSetWidgetTapPackage = { viewModel.setWidgetTapPackage(it) },
        onSetWidgetDynamicColor = { viewModel.setWidgetDynamicColor(it) },
    )
}

// --- Previews ---

@Preview(name = "Set Button", showBackground = true)
@Composable
private fun SetButtonPreview() {
    SimpleWeatherTheme {
        SetButton(onClick = {})
    }
}

@Preview(name = "No location set", showBackground = true)
@Composable
private fun SettingsScreenEmptyPreview() {
    SimpleWeatherTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onRequestDeviceLocation = {},
            onDisableDeviceLocation = {},
            onSetLocation = {},
            onSetTempUnit = {},
            onSetUpdateInterval = {},
            onSetWidgetTextColor = {},
            onSetWidgetTapPackage = {},
            onSetWidgetDynamicColor = {},
        )
    }
}

@Preview(name = "Device location enabled", showBackground = true)
@Composable
private fun SettingsScreenDeviceLocationPreview() {
    SimpleWeatherTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                useDeviceLocation = true,
                locationDisplayName = "San Francisco, California",
            ),
            onRequestDeviceLocation = {},
            onDisableDeviceLocation = {},
            onSetLocation = {},
            onSetTempUnit = {},
            onSetUpdateInterval = {},
            onSetWidgetTextColor = {},
            onSetWidgetTapPackage = {},
            onSetWidgetDynamicColor = {},
        )
    }
}

@Preview(name = "Manual location, Fahrenheit", showBackground = true)
@Composable
private fun SettingsScreenManualLocationPreview() {
    SimpleWeatherTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                useDeviceLocation = false,
                locationQuery = "Austin, TX",
                locationDisplayName = "Austin, Texas",
                tempUnit = "F",
                updateIntervalMinutes = 30,
                widgetTextColor = "white",
            ),
            onRequestDeviceLocation = {},
            onDisableDeviceLocation = {},
            onSetLocation = {},
            onSetTempUnit = {},
            onSetUpdateInterval = {},
            onSetWidgetTextColor = {},
            onSetWidgetTapPackage = {},
            onSetWidgetDynamicColor = {},
        )
    }
}
