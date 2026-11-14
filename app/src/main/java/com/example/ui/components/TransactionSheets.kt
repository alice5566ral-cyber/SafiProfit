package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.CurrencyCode
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.domain.DateUtils
import com.example.domain.MoneyFormatter
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeModalSheet(
    existingRecord: TransactionRecord? = null,
    onDismiss: () -> Unit,
    onSave: (amountMad: BigDecimal, description: String, dateString: String, status: TransactionStatus) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current

    var amountText by remember(existingRecord) {
        mutableStateOf(existingRecord?.let { MoneyFormatter.formatPlainInput(it.amountMad) } ?: "")
    }
    var descriptionText by remember(existingRecord) {
        mutableStateOf(existingRecord?.description ?: "Affipartner payout")
    }
    var dateString by remember(existingRecord) {
        mutableStateOf(existingRecord?.dateString ?: DateUtils.todayDateString())
    }
    var status by remember(existingRecord) {
        mutableStateOf(existingRecord?.status ?: TransactionStatus.RECEIVED)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presets = listOf("Affipartner payout", "COD payout", "Cash on Delivery", "Store payout")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = if (existingRecord == null) {
                    stringResource(R.string.sheet_add_income_title)
                } else {
                    stringResource(R.string.sheet_edit_income_title)
                },
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("${stringResource(R.string.field_amount)} (DH)") },
                placeholder = { Text("830") },
                suffix = { Text("DH") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = descriptionText,
                onValueChange = {
                    descriptionText = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.field_description)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_description_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = descriptionText == preset,
                        onClick = { descriptionText = preset },
                        label = { Text(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .testTag("income_date_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.field_date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateUtils.formatRelativeHeader(dateString),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = stringResource(R.string.field_date),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.field_status),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusOptionCard(
                    title = stringResource(R.string.status_received),
                    subtitle = stringResource(R.string.status_received_desc),
                    selected = status == TransactionStatus.RECEIVED,
                    accentColor = financeColors.profit,
                    containerColor = financeColors.profitContainer,
                    icon = Icons.Rounded.CheckCircle,
                    onClick = { status = TransactionStatus.RECEIVED },
                    testTag = "income_status_received",
                    modifier = Modifier.weight(1f)
                )
                StatusOptionCard(
                    title = stringResource(R.string.status_pending),
                    subtitle = stringResource(R.string.status_pending_desc),
                    selected = status == TransactionStatus.PENDING,
                    accentColor = financeColors.pending,
                    containerColor = financeColors.pendingContainer,
                    icon = Icons.Rounded.Schedule,
                    onClick = { status = TransactionStatus.PENDING },
                    testTag = "income_status_pending",
                    modifier = Modifier.weight(1f)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val invalidAmountMsg = stringResource(R.string.error_invalid_amount)
            val emptyDescMsg = stringResource(R.string.error_empty_description)

            Button(
                onClick = {
                    val amount = MoneyFormatter.parseBigDecimal(amountText)
                    when {
                        amount == null || amount <= BigDecimal.ZERO -> errorMessage = invalidAmountMsg
                        descriptionText.trim().isEmpty() -> errorMessage = emptyDescMsg
                        else -> onSave(amount, descriptionText.trim(), dateString, status)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_income_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (existingRecord == null) {
                        stringResource(R.string.action_save)
                    } else {
                        stringResource(R.string.action_update)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (existingRecord != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sheet_delete_income_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }

    if (showDatePicker) {
        LedgerDatePickerDialog(
            initialIsoDate = dateString,
            onDateSelected = { selected ->
                dateString = selected
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdExpenseModalSheet(
    settings: AppSettings,
    existingRecord: TransactionRecord? = null,
    onDismiss: () -> Unit,
    onSave: (
        amountOriginal: BigDecimal,
        currency: CurrencyCode,
        exchangeRate: BigDecimal,
        description: String,
        dateString: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val financeColors = LocalFinanceColors.current

    var amountText by remember(existingRecord) {
        mutableStateOf(existingRecord?.let { MoneyFormatter.formatPlainInput(it.amountOriginal) } ?: "")
    }
    var currency by remember(existingRecord) {
        mutableStateOf(existingRecord?.currency ?: CurrencyCode.USD)
    }
    var rateText by remember(existingRecord) {
        val initialRate = when {
            existingRecord != null && existingRecord.currency != CurrencyCode.MAD ->
                MoneyFormatter.formatRateInput(existingRecord.exchangeRate)
            existingRecord != null && existingRecord.currency == CurrencyCode.MAD -> "1"
            else -> MoneyFormatter.formatRateInput(settings.defaultUsdRate)
        }
        mutableStateOf(initialRate)
    }
    var descriptionText by remember(existingRecord) {
        mutableStateOf(existingRecord?.description ?: "Meta Ads")
    }
    var dateString by remember(existingRecord) {
        mutableStateOf(existingRecord?.dateString ?: DateUtils.todayDateString())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmount = MoneyFormatter.parseBigDecimal(amountText)
    val parsedRate = if (currency == CurrencyCode.MAD) BigDecimal.ONE else MoneyFormatter.parseBigDecimal(rateText)
    val convertedMad = if (parsedAmount != null && parsedAmount > BigDecimal.ZERO && parsedRate != null && parsedRate > BigDecimal.ZERO) {
        if (currency == CurrencyCode.MAD) parsedAmount else parsedAmount.multiply(parsedRate)
    } else {
        null
    }

    val presets = listOf("Meta Ads", "TikTok Ads", "Google Ads", "Snapchat Ads")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = if (existingRecord == null) {
                    stringResource(R.string.sheet_add_ad_title)
                } else {
                    stringResource(R.string.sheet_edit_ad_title)
                },
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.field_currency),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(CurrencyCode.USD, CurrencyCode.MAD, CurrencyCode.EUR)
                options.forEachIndexed { index, code ->
                    SegmentedButton(
                        selected = currency == code,
                        onClick = {
                            currency = code
                            rateText = when (code) {
                                CurrencyCode.MAD -> "1"
                                CurrencyCode.USD -> MoneyFormatter.formatRateInput(settings.defaultUsdRate)
                                CurrencyCode.EUR -> MoneyFormatter.formatRateInput(settings.defaultEurRate)
                            }
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        modifier = Modifier.testTag("ad_currency_${code.name.lowercase()}")
                    ) {
                        Text(code.displayLabel)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("${stringResource(R.string.field_amount)} (${currency.symbol})") },
                placeholder = { Text(if (currency == CurrencyCode.MAD) "443" else "44.27") },
                prefix = {
                    if (currency != CurrencyCode.MAD) Text(currency.symbol)
                },
                suffix = {
                    if (currency == CurrencyCode.MAD) Text("DH")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ad_amount_input")
            )

            if (currency != CurrencyCode.MAD) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.field_exchange_rate)) },
                    placeholder = { Text("10") },
                    suffix = { Text("MAD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ad_exchange_rate_input")
                )
            }

            if (convertedMad != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.converted_mad_preview),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (currency != CurrencyCode.MAD && parsedAmount != null && parsedRate != null) {
                                Text(
                                    text = "${MoneyFormatter.formatForeignAmount(parsedAmount, currency)} × ${MoneyFormatter.formatRateInput(parsedRate)} MAD",
                                    style = MaterialTheme.typography.bodySmall.merge(TabularAmountStyle),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Text(
                            text = MoneyFormatter.formatMad(convertedMad, settings.alwaysShowDecimals),
                            style = MaterialTheme.typography.titleMedium.merge(TabularAmountStyle),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("ad_converted_mad_preview")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = descriptionText,
                onValueChange = {
                    descriptionText = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.field_description)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ad_description_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = descriptionText == preset,
                        onClick = { descriptionText = preset },
                        label = { Text(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .testTag("ad_date_selector")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.field_date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateUtils.formatRelativeHeader(dateString),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = stringResource(R.string.field_date),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val invalidAmountMsg = stringResource(R.string.error_invalid_amount)
            val invalidRateMsg = stringResource(R.string.error_invalid_rate)
            val emptyDescMsg = stringResource(R.string.error_empty_description)

            Button(
                onClick = {
                    val amount = MoneyFormatter.parseBigDecimal(amountText)
                    val rate = if (currency == CurrencyCode.MAD) BigDecimal.ONE else MoneyFormatter.parseBigDecimal(rateText)
                    when {
                        amount == null || amount <= BigDecimal.ZERO -> errorMessage = invalidAmountMsg
                        rate == null || rate <= BigDecimal.ZERO -> errorMessage = invalidRateMsg
                        descriptionText.trim().isEmpty() -> errorMessage = emptyDescMsg
                        else -> onSave(amount, currency, rate, descriptionText.trim(), dateString)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_ad_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = financeColors.loss,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (existingRecord == null) {
                        stringResource(R.string.action_save)
                    } else {
                        stringResource(R.string.action_update)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (existingRecord != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sheet_delete_ad_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }

    if (showDatePicker) {
        LedgerDatePickerDialog(
            initialIsoDate = dateString,
            onDateSelected = { selected ->
                dateString = selected
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun StatusOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) containerColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerDatePickerDialog(
    initialIsoDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.isoDateToUtcPickerMillis(initialIsoDate)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(DateUtils.utcPickerMillisToIsoDate(millis))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
