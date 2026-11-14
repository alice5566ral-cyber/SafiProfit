package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TransactionRecord
import com.example.domain.DateUtils
import com.example.domain.MoneyFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HeroNetProfitCard
import com.example.ui.components.PeriodSummaryCard
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.viewmodel.LedgerUiState

@Composable
fun HomeScreen(
    uiState: LedgerUiState,
    onAddIncomeClick: () -> Unit,
    onAddAdExpenseClick: () -> Unit,
    onMarkAsReceived: (Long) -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val alwaysDecimals = uiState.settings.alwaysShowDecimals

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.dp, financeColors.cardBorder)
                    ) {
                        Text(
                            text = DateUtils.formatDisplayDate(DateUtils.todayDateString()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            item {
                HeroNetProfitCard(
                    summary = uiState.allTimeSummary,
                    alwaysShowDecimals = alwaysDecimals
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddIncomeClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("add_income_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_add_income),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = onAddAdExpenseClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("add_ad_expense_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, financeColors.cardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = financeColors.loss,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_add_ad_expense),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(visible = uiState.pendingTransactions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pending_payouts_section"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = financeColors.pendingContainer.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, financeColors.pending.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = financeColors.pending,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${stringResource(R.string.pending_payouts)} (${uiState.pendingTransactions.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            uiState.pendingTransactions.forEach { pendingTx ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, financeColors.cardBorder),
                                    onClick = { onEditTransaction(pendingTx) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pendingTx.description,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${MoneyFormatter.formatMad(pendingTx.amountMad, alwaysDecimals, includePlusSign = true)} • ${DateUtils.formatDisplayDate(pendingTx.dateString)}",
                                                style = MaterialTheme.typography.bodySmall.merge(TabularAmountStyle),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FilledTonalButton(
                                            onClick = { onMarkAsReceived(pendingTx.id) },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("home_mark_received_${pendingTx.id}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = financeColors.profitContainer,
                                                contentColor = financeColors.profit
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = stringResource(R.string.action_mark_received),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isWide) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PeriodSummaryCard(
                                title = stringResource(R.string.period_today),
                                summary = uiState.todaySummary,
                                alwaysShowDecimals = alwaysDecimals,
                                testTag = "today_summary_card",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToReports
                            )
                            PeriodSummaryCard(
                                title = stringResource(R.string.period_this_week),
                                summary = uiState.thisWeekSummary,
                                alwaysShowDecimals = alwaysDecimals,
                                testTag = "week_summary_card",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToReports
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PeriodSummaryCard(
                                title = stringResource(R.string.period_this_month),
                                summary = uiState.thisMonthSummary,
                                alwaysShowDecimals = alwaysDecimals,
                                testTag = "month_summary_card",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToReports
                            )
                            PeriodSummaryCard(
                                title = stringResource(R.string.period_all_time),
                                summary = uiState.allTimeSummary,
                                alwaysShowDecimals = alwaysDecimals,
                                incomeLabel = stringResource(R.string.label_total_income),
                                adsLabel = stringResource(R.string.label_total_advertising),
                                profitLabel = stringResource(R.string.label_total_net_profit),
                                testTag = "all_time_summary_card",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToReports
                            )
                        }
                    }
                }
            } else {
                item {
                    PeriodSummaryCard(
                        title = stringResource(R.string.period_today),
                        summary = uiState.todaySummary,
                        alwaysShowDecimals = alwaysDecimals,
                        testTag = "today_summary_card",
                        onClick = onNavigateToReports
                    )
                }
                item {
                    PeriodSummaryCard(
                        title = stringResource(R.string.period_this_week),
                        summary = uiState.thisWeekSummary,
                        alwaysShowDecimals = alwaysDecimals,
                        testTag = "week_summary_card",
                        onClick = onNavigateToReports
                    )
                }
                item {
                    PeriodSummaryCard(
                        title = stringResource(R.string.period_this_month),
                        summary = uiState.thisMonthSummary,
                        alwaysShowDecimals = alwaysDecimals,
                        testTag = "month_summary_card",
                        onClick = onNavigateToReports
                    )
                }
                item {
                    PeriodSummaryCard(
                        title = stringResource(R.string.period_all_time),
                        summary = uiState.allTimeSummary,
                        alwaysShowDecimals = alwaysDecimals,
                        incomeLabel = stringResource(R.string.label_total_income),
                        adsLabel = stringResource(R.string.label_total_advertising),
                        profitLabel = stringResource(R.string.label_total_net_profit),
                        testTag = "all_time_summary_card",
                        onClick = onNavigateToReports
                    )
                }
            }

            if (uiState.allTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.empty_transactions_title),
                        body = stringResource(R.string.empty_transactions_body)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}
