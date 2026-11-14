package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.ChartBarPoint
import java.math.BigDecimal

@Composable
fun SimpleProfitChart(
    points: List<ChartBarPoint>,
    modifier: Modifier = Modifier
) {
    val financeColors = LocalFinanceColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val incomeColor = financeColors.profit
    val adsColor = financeColors.loss

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = "Income vs Advertising",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = incomeColor, label = stringResource(R.string.label_income))
                    LegendDot(color = adsColor, label = stringResource(R.string.label_ads))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hasData = points.any { it.income > BigDecimal.ZERO || it.ads > BigDecimal.ZERO }
            if (points.isEmpty() || !hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add transactions to visualize Income vs Ads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = points.maxOf { maxOf(it.income.toFloat(), it.ads.toFloat()) }
                    .coerceAtLeast(1f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val baselineY = size.height - 4.dp.toPx()
                    val chartHeight = baselineY - 8.dp.toPx()

                    // Subtle horizontal gridlines
                    for (i in 0..2) {
                        val y = 8.dp.toPx() + chartHeight * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val groupCount = points.size
                    val groupWidth = size.width / groupCount
                    val barWidth = (groupWidth * 0.24f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                    val barGap = 4.dp.toPx()

                    points.forEachIndexed { index, point ->
                        val centerX = groupWidth * index + groupWidth / 2f
                        val incomeRatio = (point.income.toFloat() / maxVal).coerceIn(0f, 1f)
                        val adsRatio = (point.ads.toFloat() / maxVal).coerceIn(0f, 1f)

                        val incomeHeight = (chartHeight * incomeRatio).coerceAtLeast(
                            if (point.income > BigDecimal.ZERO) 4.dp.toPx() else 0f
                        )
                        val adsHeight = (chartHeight * adsRatio).coerceAtLeast(
                            if (point.ads > BigDecimal.ZERO) 4.dp.toPx() else 0f
                        )

                        if (incomeHeight > 0f) {
                            drawRoundRect(
                                color = incomeColor,
                                topLeft = Offset(centerX - barWidth - barGap / 2f, baselineY - incomeHeight),
                                size = Size(barWidth, incomeHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        if (adsHeight > 0f) {
                            drawRoundRect(
                                color = adsColor,
                                topLeft = Offset(centerX + barGap / 2f, baselineY - adsHeight),
                                size = Size(barWidth, adsHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    points.forEach { point ->
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
