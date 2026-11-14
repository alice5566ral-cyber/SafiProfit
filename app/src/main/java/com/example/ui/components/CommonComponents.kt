package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CurrencyCode
import com.example.data.model.PeriodSummary
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.domain.DateUtils
import com.example.domain.MoneyFormatter
import com.example.ui.theme.HeroLossEnd
import com.example.ui.theme.HeroLossStart
import com.example.ui.theme.HeroProfitEnd
import com.example.ui.theme.HeroProfitStart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.TabularAmountStyle
import java.math.BigDecimal

@Composable
fun HeroNetProfitCard(
    summary: PeriodSummary,
    alwaysShowDecimals: Boolean,
    modifier: Modifier = Modifier
) {
    val isLoss = summary.netProfit < BigDecimal.ZERO
    val isZero = summary.netProfit.compareTo(BigDecimal.ZERO) == 0

    val gradientStart by animateColorAsState(
        targetValue = if (isLoss) HeroLossStart else HeroProfitStart,
        animationSpec = tween(300),
        label = "heroStart"
    )
    val gradientEnd by animateColorAsState(
        targetValue = if (isLoss) HeroLossEnd else HeroProfitEnd,
        animationSpec = tween(300),
        label = "heroEnd"
    )

    val glowColor = if (isLoss) Color(0x33FB7185) else Color(0x3310B981)
    val badgeBg = if (isLoss) Color(0x33FB7185) else if (isZero) Color(0x26FFFFFF) else Color(0x3310B981)
    val badgeTextColor = if (isLoss) Color(0xFFFDA4AF) else if (isZero) Color(0xFFE2E8F0) else Color(0xFF6EE7B7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .drawBehind {
                drawCircle(
                    color = glowColor,
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.9f, size.height * 0.1f)
                )
            }
            .padding(22.dp)
            .testTag("hero_net_profit_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.current_net_profit),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLoss) Icons.AutoMirrored.Rounded.TrendingDown else Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isLoss -> "${stringResource(R.string.status_loss_prefix)} ${MoneyFormatter.formatMad(summary.netProfit, alwaysShowDecimals, includePlusSign = false)}"
                                isZero -> stringResource(R.string.status_breakeven)
                                else -> stringResource(R.string.status_profitable)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = MoneyFormatter.formatMad(
                    amount = summary.netProfit,
                    alwaysShowDecimals = alwaysShowDecimals,
                    includePlusSign = true
                ),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("hero_net_profit_amount")
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Money Received in Bank
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.money_received_in_bank),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyFormatter.formatMad(
                            amount = summary.moneyReceivedInBank,
                            alwaysShowDecimals = alwaysShowDecimals,
                            includePlusSign = true
                        ),
                        style = MaterialTheme.typography.titleMedium.merge(TabularAmountStyle),
                        color = if (summary.moneyReceivedInBank < BigDecimal.ZERO) Color(0xFFFDA4AF) else Color.White,
                        modifier = Modifier.testTag("money_received_amount")
                    )
                }

                // Pending Payouts
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFFCD34D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.pending_payouts),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyFormatter.formatMad(
                            amount = summary.pendingIncome,
                            alwaysShowDecimals = alwaysShowDecimals,
                            includePlusSign = summary.pendingIncome > BigDecimal.ZERO
                        ),
                        style = MaterialTheme.typography.titleMedium.merge(TabularAmountStyle),
                        color = if (summary.pendingIncome > BigDecimal.ZERO) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.testTag("pending_payouts_amount")
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodSummaryCard(
    title: String,
    summary: PeriodSummary,
    alwaysShowDecimals: Boolean,
    incomeLabel: String = stringResource(R.string.label_income),
    adsLabel: String = stringResource(R.string.label_advertising),
    profitLabel: String = stringResource(R.string.label_net_profit),
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val financeColors = LocalFinanceColors.current
    val isLoss = summary.netProfit < BigDecimal.ZERO
    val profitColor = when {
        isLoss -> financeColors.loss
        summary.netProfit > BigDecimal.ZERO -> financeColors.profit
        else -> MaterialTheme.colorScheme.onSurface
    }
    val pillContainer = when {
        isLoss -> financeColors.lossContainer
        summary.netProfit > BigDecimal.ZERO -> financeColors.profitContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, financeColors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = pillContainer,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (isLoss) {
                            "${stringResource(R.string.net_loss_prefix)} ${MoneyFormatter.formatMad(summary.netProfit, alwaysShowDecimals)}"
                        } else {
                            MoneyFormatter.formatMad(summary.netProfit, alwaysShowDecimals, includePlusSign = true)
                        },
                        style = MaterialTheme.typography.labelLarge.merge(TabularAmountStyle),
                        color = profitColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniColumn(
                    label = incomeLabel,
                    value = MoneyFormatter.formatMad(summary.totalIncome, alwaysShowDecimals),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniColumn(
                    label = adsLabel,
                    value = MoneyFormatter.formatMad(summary.totalAds, alwaysShowDecimals),
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniColumn(
                    label = profitLabel,
                    value = MoneyFormatter.formatMad(summary.netProfit, alwaysShowDecimals, includePlusSign = true),
                    valueColor = profitColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.merge(TabularAmountStyle),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionRecord,
    alwaysShowDecimals: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkAsReceived: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val isIncome = transaction.type == TransactionType.INCOME
    val isPending = transaction.status == TransactionStatus.PENDING

    val iconBg = if (isIncome) financeColors.profitContainer else financeColors.lossContainer
    val iconTint = if (isIncome) financeColors.profit else financeColors.loss
    val amountColor = if (isIncome) financeColors.profit else MaterialTheme.colorScheme.onSurface

    val statusText = when (transaction.status) {
        TransactionStatus.RECEIVED -> stringResource(R.string.status_received)
        TransactionStatus.PENDING -> stringResource(R.string.status_pending)
        TransactionStatus.PAID -> stringResource(R.string.status_paid)
    }
    val statusBg = when (transaction.status) {
        TransactionStatus.RECEIVED -> financeColors.profitContainer
        TransactionStatus.PENDING -> financeColors.pendingContainer
        TransactionStatus.PAID -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusFg = when (transaction.status) {
        TransactionStatus.RECEIVED -> financeColors.profit
        TransactionStatus.PENDING -> financeColors.pending
        TransactionStatus.PAID -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, financeColors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.Campaign,
                        contentDescription = if (isIncome) "Income" else "Advertising Expense",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = DateUtils.formatDisplayDate(transaction.dateString),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!isIncome && transaction.currency != CurrencyCode.MAD) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${MoneyFormatter.formatForeignAmount(transaction.amountOriginal, transaction.currency, alwaysShowDecimals)} × ${MoneyFormatter.formatRateInput(transaction.exchangeRate)} MAD",
                            style = MaterialTheme.typography.labelSmall.merge(TabularAmountStyle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val signedPrefix = if (isIncome) "+" else "-"
                    Text(
                        text = "$signedPrefix${MoneyFormatter.formatMad(transaction.amountMad, alwaysShowDecimals)}",
                        style = MaterialTheme.typography.titleMedium.merge(TabularAmountStyle),
                        color = amountColor
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_tx_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isIncome && isPending && onMarkAsReceived != null) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onMarkAsReceived,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("mark_received_${transaction.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = financeColors.profitContainer,
                        contentColor = financeColors.profit
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_mark_received),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, financeColors.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_confirm_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_confirm_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
