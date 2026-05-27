package com.kail.location.views.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kail.location.R
import com.kail.location.utils.KailLog
import com.kail.location.viewmodels.SettingsViewModel

/**
 * 设置屏幕主界面
 * 展示所有可配置的应用选项，按类别分组显示。
 *
 * @param viewModel 设置界面的 ViewModel，用于读取和更新偏好设置
 * @param onBackClick 返回按钮点击回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var logCacheSize by remember { mutableStateOf(KailLog.getLogCacheSizeBytes(context)) }
    var showClearLogDialog by remember { mutableStateOf(false) }
    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val ok = KailLog.exportLogs(context, uri)
            android.widget.Toast.makeText(
                context,
                if (ok) context.getString(R.string.setting_export_log_success) else context.getString(R.string.setting_export_log_failed),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        logCacheSize = KailLog.getLogCacheSizeBytes(context)
    }

    // State observation
    val joystickType by viewModel.joystickType.collectAsState()
    val walkSpeed by viewModel.walkSpeed.collectAsState()
    val runSpeed by viewModel.runSpeed.collectAsState()
    val bikeSpeed by viewModel.bikeSpeed.collectAsState()
    val altitude by viewModel.altitude.collectAsState()
    val mockSpeed by viewModel.mockSpeed.collectAsState()
    val accuracy by viewModel.accuracy.collectAsState()
    val minSatellites by viewModel.minSatellites.collectAsState()
    val reportInterval by viewModel.reportInterval.collectAsState()
    val randomOffset by viewModel.randomOffset.collectAsState()
    val latOffset by viewModel.latOffset.collectAsState()
    val lonOffset by viewModel.lonOffset.collectAsState()
    val logEnabled by viewModel.logEnabled.collectAsState()
    val debugLogEnabled by viewModel.debugLogEnabled.collectAsState()
    val historyExpiration by viewModel.historyExpiration.collectAsState()
    val baiduMapKey by viewModel.baiduMapKey.collectAsState()
    val mapZoom by viewModel.mapZoom.collectAsState()
    val gpsSatelliteSim by viewModel.gpsSatelliteSim.collectAsState()
    val naturalJitter by viewModel.naturalJitter.collectAsState()
    val stepSimEnabled by viewModel.stepSimEnabled.collectAsState()
    val simScheme by viewModel.simScheme.collectAsState()
    val opencellidApiKey by viewModel.opencellidApiKey.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // ===== Group: 移动参数 =====
            PreferenceCategory(title = stringResource(R.string.setting_group_move))

            ListPreference(
                title = stringResource(R.string.setting_joystick_type),
                currentValue = joystickType,
                entries = stringArrayResource(R.array.array_joystick_type),
                entryValues = stringArrayResource(R.array.array_joystick_type_values),
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_JOYSTICK_TYPE, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_walk),
                value = walkSpeed,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_WALK_SPEED, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_run),
                value = runSpeed,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_RUN_SPEED, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_bike),
                value = bikeSpeed,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_BIKE_SPEED, it) }
            )

            // ===== Group: 位置模拟参数 =====
            PreferenceCategory(title = stringResource(R.string.setting_group_move))

            EditTextPreference(
                title = stringResource(R.string.setting_altitude),
                value = altitude,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_ALTITUDE, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_mock_speed),
                value = mockSpeed,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_MOCK_SPEED, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_accuracy_title),
                value = accuracy,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_ACCURACY, it) },
                description = stringResource(R.string.setting_accuracy_summary)
            )

            EditTextPreference(
                title = stringResource(R.string.setting_min_satellites),
                value = minSatellites,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_MIN_SATELLITES, it) },
                description = stringResource(R.string.setting_min_satellites_summary)
            )

            EditTextPreference(
                title = stringResource(R.string.setting_report_interval),
                value = reportInterval,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_REPORT_INTERVAL, it) },
                description = stringResource(R.string.setting_report_interval_summary)
            )

            // ===== Group: 位置偏移 =====
            PreferenceCategory(title = stringResource(R.string.setting_group_location_offset))

            SwitchPreference(
                title = stringResource(R.string.setting_random_offset),
                checked = randomOffset,
                onCheckedChange = { viewModel.updateBooleanPreference(SettingsViewModel.KEY_RANDOM_OFFSET, it) },
                summary = stringResource(R.string.setting_random_offset_summary)
            )

            EditTextPreference(
                title = stringResource(R.string.setting_lat_max_offset),
                value = latOffset,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_LAT_OFFSET, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_lon_max_offset),
                value = lonOffset,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_LON_OFFSET, it) }
            )

            // ===== Group: 卫星与信号 =====
            PreferenceCategory(title = stringResource(R.string.setting_group_satellite_and_signal))

            SwitchPreference(
                title = stringResource(R.string.setting_gps_satellite_title),
                checked = gpsSatelliteSim,
                onCheckedChange = { viewModel.updateBooleanPreference(SettingsViewModel.KEY_GPS_SATELLITE_SIM, it) },
                summary = stringResource(R.string.setting_gps_satellite_summary)
            )

            // ===== Group: 步频模拟 =====
            PreferenceCategory(title = "步频模拟")

            SwitchPreference(
                title = "启用步频模拟",
                checked = stepSimEnabled,
                onCheckedChange = { viewModel.updateBooleanPreference(SettingsViewModel.KEY_STEP_SIM_ENABLED, it) },
                summary = "模拟步频数据用于计步类应用"
            )

            ListPreference(
                title = "步频数据类型",
                currentValue = simScheme,
                entries = stringArrayResource(R.array.array_sim_scheme),
                entryValues = stringArrayResource(R.array.array_sim_scheme_values),
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_SIM_SCHEME, it) }
            )

            SwitchPreference(
                title = stringResource(R.string.setting_natural_jitter),
                checked = naturalJitter,
                onCheckedChange = { viewModel.updateBooleanPreference(SettingsViewModel.KEY_NATURAL_JITTER, it) },
                summary = stringResource(R.string.setting_natural_jitter_summary)
            )

            // ===== Group: 日志/其他 =====
            PreferenceCategory(title = stringResource(R.string.setting_group_other))

            EditTextPreference(
                title = stringResource(R.string.setting_baidu_key),
                value = baiduMapKey,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_BAIDU_MAP_KEY, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_opencellid_key),
                value = opencellidApiKey,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_OPENCELLID_API_KEY, it) },
                description = stringResource(R.string.setting_opencellid_key_summary)
            )

            SwitchPreference(
                title = stringResource(R.string.setting_enable_log),
                checked = logEnabled,
                onCheckedChange = { viewModel.updateBooleanPreference(SettingsViewModel.KEY_LOG_ENABLED, it) },
                summary = stringResource(R.string.setting_enable_log_summary)
            )

            SwitchPreference(
                title = stringResource(R.string.setting_enable_debug_log),
                checked = debugLogEnabled,
                onCheckedChange = { viewModel.updateBooleanPreference(SettingsViewModel.KEY_DEBUG_LOG_ENABLED, it) },
                summary = stringResource(R.string.setting_enable_debug_log_summary)
            )

            ActionPreference(
                title = stringResource(R.string.setting_export_log),
                summary = stringResource(R.string.setting_export_log_summary),
                onClick = { exportLogLauncher.launch("kail_location_logs.txt") }
            )

            ActionPreference(
                title = stringResource(R.string.setting_clear_log_cache),
                summary = stringResource(R.string.setting_clear_log_cache_summary, formatBytes(logCacheSize)),
                onClick = {
                    logCacheSize = KailLog.getLogCacheSizeBytes(context)
                    showClearLogDialog = true
                }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_history_expiration),
                value = historyExpiration,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_HISTORY_EXPIRATION, it) }
            )

            EditTextPreference(
                title = stringResource(R.string.setting_map_zoom),
                value = mapZoom,
                onValueChange = { viewModel.updateStringPreference(SettingsViewModel.KEY_MAP_ZOOM, it) },
                description = stringResource(R.string.setting_map_zoom_summary)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.setting_current_version)) },
                supportingContent = { Text(viewModel.appVersion) }
            )
        }
    }

    if (showClearLogDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogDialog = false },
            title = { Text(stringResource(R.string.setting_clear_log_cache)) },
            text = { Text(stringResource(R.string.setting_clear_log_cache_confirm, formatBytes(logCacheSize))) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = KailLog.clearLogCache(context)
                        logCacheSize = KailLog.getLogCacheSizeBytes(context)
                        showClearLogDialog = false
                        android.widget.Toast.makeText(
                            context,
                            if (ok) context.getString(R.string.setting_clear_log_cache_success) else context.getString(R.string.setting_clear_log_cache_failed),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                ) { Text(stringResource(R.string.setting_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogDialog = false }) {
                    Text(stringResource(R.string.setting_cancel))
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(java.util.Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(java.util.Locale.US, "%.2f MB", mb)
}

/**
 * 设置类别标题组件
 */
@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * 开关类设置项组件
 */
@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun ActionPreference(
    title: String,
    summary: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        modifier = Modifier.clickable { onClick() }
    )
}

/**
 * 文本编辑类设置项组件
 */
@Composable
fun EditTextPreference(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    description: String = ""
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text(value.ifEmpty { stringResource(R.string.setting_not_set) })
                if (description.isNotEmpty()) {
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        var tempValue by remember { mutableStateOf(value) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    if (description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempValue.isNotBlank()) {
                            onValueChange(tempValue)
                        }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.setting_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.setting_cancel))
                }
            }
        )
    }
}

/**
 * 列表选择类设置项组件
 */
@Composable
fun ListPreference(
    title: String,
    currentValue: String,
    entries: Array<String>,
    entryValues: Array<String>,
    onValueChange: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val index = entryValues.indexOf(currentValue)
    val displayValue = if (index >= 0 && index < entries.size) entries[index] else currentValue

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(displayValue) },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    entries.forEachIndexed { i, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(entryValues[i])
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (entryValues[i] == currentValue),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = entry)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.setting_cancel))
                }
            }
        )
    }
}
