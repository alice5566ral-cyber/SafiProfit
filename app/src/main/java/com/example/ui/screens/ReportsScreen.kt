package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BreakdownGranularity
import com.example.data.model.ReportPeriod
import com.example.domain.DateUtils
import com.example.domain.MoneyFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LedgerDatePickerDialog
import com.example.ui.components.PeriodSummaryCard
import com.example.ui.components.SimpleProfitChart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.viewmodel.LedgerUiState
import java.math.BigDecimal

@Composable
fun ReportsScreen(
    uiState: LedgerUiState,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onCustomRangeChange: (start: String, end: String) -> Unit,
    onSelectGranularity: (BreakdownGranularity) -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val alwaysDecimals = uiState.settings.alwaysShowDecimals

    var editingStartDate by remember { mutableStateOf(false) }
    var editingEndDate by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.reports_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryPeriods = listOf(
                        ReportPeriod.TODAY,
                        ReportPeriod.THIS_WEEK,
                        ReportPeriod.THIS_MONTH,
                        ReportPeriod.CUSTOM,
                        ReportPeriod.THIS_YEAR,
                        ReportPeriod.ALL_TIME
                    )
                    primaryPeriods.forEach { period ->
                        FilterChip(
                            selected = uiState.reportPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            label = { Text(period.label) },
                            modifier = Modifier.testTag("report_period_${period.name.lowercase()}")
                        )
                    }
                }
            }

            if (uiState.reportPeriod == ReportPeriod.CUSTOM) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateRangeBox(
                            label = "Start Date",
                            dateString = uiState.customStartDate,
                            onClick = { editingStartDate = true },
                            testTag = "custom_start_date",
                            modifier = Modifier.weight(1f)
                        )
                        DateRangeBox(
                            label = "End Date",
                            dateString = uiState.customEndDate,
                            onClick = { editingEndDate = true },
                            testTag = "custom_end_date",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                val periodTitle = when (uiState.reportPeriod) {
                    ReportPeriod.CUSTOM -> DateUtils.formatWeekRangeLabel(
                        uiState.customStartDate,
                        uiState.customEndDate
                    )
                    else -> uiState.reportPeriod.label
                }
                PeriodSummaryCard(
                    title = periodTitle,
                    summary = uiState.reportSummary,
                    alwaysShowDecimals = alwaysDecimals,
                    testTag = "report_active_summary_card"
                )
            }

            // Weekly Summary + comparison with previous week
            item {
                val comp = uiState.weeklyComparison
                val isPositiveDelta = comp.profitDelta >= BigDecimal.ZERO
                val deltaColor = if (isPositiveDelta) financeColors.profit else financeColors.loss
                val deltaBg = if (isPositiveDelta) financeColors.profitContainer else financeColors.lossContainer

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_comparison_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, financeColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.weekly_summary_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                color = deltaBg,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPositiveDelta) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = deltaColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val compText = when {
                                        comp.percentChange != null -> {
                                            val sign = if (comp.percentChange > BigDecimal.ZERO) "+" else ""
                                            "$sign${MoneyFormatter.formatNumber(comp.percentChange, false)}% profit"
                                        }
                                        else -> {
                                            "${MoneyFormatter.formatMad(comp.profitDelta, alwaysDecimals, includePlusSign = true)} vs last wk"
                                        }
                                    }
                                    Text(
                                        text = compText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = deltaColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.compared_previous_week),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.label_income),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyFormatter.formatMad(comp.current.totalIncome, alwaysDecimals),
                                    style = MaterialTheme.typography.titleSmall.merge(TabularAmountStyle)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.label_ads),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyFormatter.formatMad(comp.current.totalAds, alwaysDecimals),
                                    style = MaterialTheme.typography.titleSmall.merge(TabularAmountStyle)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.label_net_profit),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyFormatter.formatMad(
                                        comp.current.netProfit,
                                        alwaysDecimals,
                                        includePlusSign = true
                                    ),
                                    style = MaterialTheme.typography.titleSmall.merge(TabularAmountStyle),
                                    color = if (comp.current.netProfit < BigDecimal.ZERO) financeColors.loss else financeColors.profit
                                )
                            }
                        }
                    }
                }
            }

            item {
                SimpleProfitChart(points = uiState.chartPoints)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Automatic Breakdown",
                        style = MaterialTheme.typography.titleMedium
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        BreakdownGranularity.entries.forEachIndexed { idx, gran ->
                            SegmentedButton(
                                selected = uiState.breakdownGranularity == gran,
                                onClick = { onSelectGranularity(gran) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = BreakdownGranularity.entries.size
                                ),
                                modifier = Modifier.testTag("granularity_${gran.name.lowercase()}")
                            ) {
                                Text(gran.label)
                            }
                        }
                    }
                }
            }

            if (uiState.breakdownBuckets.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No period data yet",
                        body = "Transactions are automatically organized by day, week, month, and year once added."
                    )
                }
            } else {
                items(
                    items = uiState.breakdownBuckets,
                    key = { it.key }
                ) { bucket ->
                    val isLoss = bucket.summary.netProfit < BigDecimal.ZERO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, financeColors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bucket.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "In: ${MoneyFormatter.formatMad(bucket.summary.totalIncome, alwaysDecimals)} • Ads: ${MoneyFormatter.formatMad(bucket.summary.totalAds, alwaysDecimals)}",
                                    style = MaterialTheme.typography.bodySmall.merge(TabularAmountStyle),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = MoneyFormatter.formatMad(
                                    bucket.summary.netProfit,
                                    alwaysDecimals,
                                    includePlusSign = true
                                ),
                                style = MaterialTheme.typography.titleMedium.merge(TabularAmountStyle),
                                color = if (isLoss) financeColors.loss else financeColors.profit
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (editingStartDate) {
        LedgerDatePickerDialog(
            initialIsoDate = uiState.customStartDate,
            onDateSelected = { selected ->
                onCustomRangeChange(selected, uiState.customEndDate)
                editingStartDate = false
            },
            onDismiss = { editingStartDate = false }
        )
    }

    if (editingEndDate) {
        LedgerDatePickerDialog(
            initialIsoDate = uiState.customEndDate,
            onDateSelected = { selected ->
                onCustomRangeChange(uiState.customStartDate, selected)
                editingEndDate = false
            },
            onDismiss = { editingEndDate = false }
        )
    }
}

@Composable
private fun DateRangeBox(
    label: String,
    dateString: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financeColors.cardBorder),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateUtils.formatDisplayDate(dateString),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
