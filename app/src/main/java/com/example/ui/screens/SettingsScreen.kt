package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DateUtils
import com.example.domain.MoneyFormatter
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.LedgerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Composable
fun SettingsScreen(
    uiState: LedgerUiState,
    onSaveExchangeRates: (usdRate: BigDecimal, eurRate: BigDecimal) -> Unit,
    onToggleAlwaysShowDecimals: (Boolean) -> Unit,
    onToggleWeekStartsMonday: (Boolean) -> Unit,
    onGenerateBackupJson: suspend () -> String,
    onRestoreBackupJson: (json: String, replaceExisting: Boolean) -> Unit,
    onClearAllTransactions: () -> Unit,
    onNotifyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val financeColors = LocalFinanceColors.current

    var usdRateText by remember { mutableStateOf(MoneyFormatter.formatRateInput(uiState.settings.defaultUsdRate)) }
    var eurRateText by remember { mutableStateOf(MoneyFormatter.formatRateInput(uiState.settings.defaultEurRate)) }
    var rateError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.settings.defaultUsdRate, uiState.settings.defaultEurRate) {
        usdRateText = MoneyFormatter.formatRateInput(uiState.settings.defaultUsdRate)
        eurRateText = MoneyFormatter.formatRateInput(uiState.settings.defaultEurRate)
    }

    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = onGenerateBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Unable to open output file")
                    }
                }.onSuccess {
                    onNotifyMessage("Backup exported successfully")
                }.onFailure { err ->
                    onNotifyMessage("Export failed: ${err.message ?: "Unknown error"}")
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                            it.readText()
                        } ?: error("Unable to read backup file")
                    }
                }.onSuccess { content ->
                    pendingRestoreJson = content
                }.onFailure { err ->
                    onNotifyMessage("Could not read file: ${err.message ?: "Unknown error"}")
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Default Exchange Rates
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, financeColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_exchange_rates),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Pre-fills the rate when adding foreign ad expenses. Past transactions keep their recorded rates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = usdRateText,
                                onValueChange = {
                                    usdRateText = it
                                    rateError = null
                                },
                                label = { Text(stringResource(R.string.settings_usd_rate)) },
                                suffix = { Text("MAD") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_usd_rate_input")
                            )
                            OutlinedTextField(
                                value = eurRateText,
                                onValueChange = {
                                    eurRateText = it
                                    rateError = null
                                },
                                label = { Text(stringResource(R.string.settings_eur_rate)) },
                                suffix = { Text("MAD") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_eur_rate_input")
                            )
                        }

                        if (rateError != null) {
                            Text(
                                text = rateError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        val invalidRateMsg = stringResource(R.string.error_invalid_rate)
                        Button(
                            onClick = {
                                val usd = MoneyFormatter.parseBigDecimal(usdRateText)
                                val eur = MoneyFormatter.parseBigDecimal(eurRateText)
                                if (usd == null || usd <= BigDecimal.ZERO || eur == null || eur <= BigDecimal.ZERO) {
                                    rateError = invalidRateMsg
                                } else {
                                    onSaveExchangeRates(usd, eur)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_rates_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }

            // Display & Calendar Preferences
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, financeColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_preferences),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_always_decimals),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Format amounts as 830.00 DH instead of 830 DH",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.alwaysShowDecimals,
                                onCheckedChange = onToggleAlwaysShowDecimals,
                                modifier = Modifier.testTag("switch_always_decimals")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_week_monday),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Used for weekly summaries and automatic breakdowns",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.settings.weekStartsOnMonday,
                                onCheckedChange = onToggleWeekStartsMonday,
                                modifier = Modifier.testTag("switch_week_monday")
                            )
                        }
                    }
                }
            }

            // Data Safety: Backup / Restore / Clear
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, financeColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_data_safety),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "All ${uiState.allTransactions.size} transaction(s) are saved offline on this device. Export a JSON backup anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                val fileName = "safiprofit-backup-${DateUtils.todayDateString()}.json"
                                exportLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_backup_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_export_backup))
                        }

                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("restore_backup_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_restore_backup))
                        }

                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            enabled = uiState.allTransactions.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("clear_all_data_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_clear_data))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text(stringResource(R.string.restore_mode_title)) },
            text = { Text(stringResource(R.string.restore_mode_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val json = pendingRestoreJson
                        pendingRestoreJson = null
                        if (json != null) onRestoreBackupJson(json, false)
                    }
                ) {
                    Text(stringResource(R.string.restore_merge))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingRestoreJson = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    TextButton(
                        onClick = {
                            val json = pendingRestoreJson
                            pendingRestoreJson = null
                            if (json != null) onRestoreBackupJson(json, true)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.restore_replace),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAllTransactions()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
