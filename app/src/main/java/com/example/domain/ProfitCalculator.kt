package com.example.domain

import com.example.data.model.BreakdownBucket
import com.example.data.model.BreakdownGranularity
import com.example.data.model.PeriodComparison
import com.example.data.model.PeriodSummary
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

object ProfitCalculator {

    fun summarize(transactions: List<TransactionRecord>): PeriodSummary {
        var receivedIncome = BigDecimal.ZERO
        var pendingIncome = BigDecimal.ZERO
        var paidAds = BigDecimal.ZERO

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    when (tx.status) {
                        TransactionStatus.RECEIVED -> receivedIncome = receivedIncome.add(tx.amountMad)
                        TransactionStatus.PENDING -> pendingIncome = pendingIncome.add(tx.amountMad)
                        TransactionStatus.PAID -> receivedIncome = receivedIncome.add(tx.amountMad)
                    }
                }
                TransactionType.AD_EXPENSE -> {
                    if (tx.status != TransactionStatus.PENDING) {
                        paidAds = paidAds.add(tx.amountMad)
                    }
                }
            }
        }

        val totalIncome = receivedIncome.add(pendingIncome)
        val netProfit = totalIncome.subtract(paidAds)
        val moneyReceived = receivedIncome.subtract(paidAds)

        return PeriodSummary(
            totalIncome = totalIncome,
            receivedIncome = receivedIncome,
            pendingIncome = pendingIncome,
            totalAds = paidAds,
            netProfit = netProfit,
            moneyReceivedInBank = moneyReceived,
            transactionCount = transactions.size
        )
    }

    fun summarizeRange(
        transactions: List<TransactionRecord>,
        startDateIso: String,
        endDateIso: String
    ): PeriodSummary {
        val effectiveStart = minOf(startDateIso, endDateIso)
        val effectiveEnd = maxOf(startDateIso, endDateIso)
        val filtered = transactions.filter { it.dateString in effectiveStart..effectiveEnd }
        return summarize(filtered)
    }

    fun comparePeriods(
        transactions: List<TransactionRecord>,
        currentRange: Pair<String, String>,
        previousRange: Pair<String, String>
    ): PeriodComparison {
        val current = summarizeRange(transactions, currentRange.first, currentRange.second)
        val previous = summarizeRange(transactions, previousRange.first, previousRange.second)
        val delta = current.netProfit.subtract(previous.netProfit)
        val percent = if (previous.netProfit.compareTo(BigDecimal.ZERO) > 0) {
            delta.multiply(BigDecimal(100))
                .divide(previous.netProfit, 1, RoundingMode.HALF_UP)
        } else {
            null
        }
        return PeriodComparison(
            current = current,
            previous = previous,
            profitDelta = delta,
            percentChange = percent
        )
    }

    fun buildBreakdown(
        transactions: List<TransactionRecord>,
        granularity: BreakdownGranularity,
        weekStartsOnMonday: Boolean
    ): List<BreakdownBucket> {
        if (transactions.isEmpty()) return emptyList()

        return when (granularity) {
            BreakdownGranularity.DAY -> {
                transactions.groupBy { it.dateString }
                    .toSortedMap(compareByDescending { it })
                    .map { (date, list) ->
                        BreakdownBucket(
                            key = date,
                            title = DateUtils.formatRelativeHeader(date),
                            subtitle = "${list.size} ${if (list.size == 1) "entry" else "entries"}",
                            summary = summarize(list)
                        )
                    }
            }
            BreakdownGranularity.WEEK -> {
                transactions.groupBy { DateUtils.startOfWeek(it.dateString, weekStartsOnMonday) }
                    .toSortedMap(compareByDescending { it })
                    .map { (weekStart, list) ->
                        val weekEnd = DateUtils.endOfWeek(weekStart, weekStartsOnMonday)
                        BreakdownBucket(
                            key = weekStart,
                            title = DateUtils.formatWeekRangeLabel(weekStart, weekEnd),
                            subtitle = "${list.size} ${if (list.size == 1) "entry" else "entries"}",
                            summary = summarize(list)
                        )
                    }
            }
            BreakdownGranularity.MONTH -> {
                transactions.groupBy { it.dateString.take(7) }
                    .toSortedMap(compareByDescending { it })
                    .map { (ym, list) ->
                        BreakdownBucket(
                            key = ym,
                            title = DateUtils.formatMonthYear(ym),
                            subtitle = "${list.size} ${if (list.size == 1) "entry" else "entries"}",
                            summary = summarize(list)
                        )
                    }
            }
            BreakdownGranularity.YEAR -> {
                transactions.groupBy { it.dateString.take(4) }
                    .toSortedMap(compareByDescending { it })
                    .map { (year, list) ->
                        BreakdownBucket(
                            key = year,
                            title = year,
                            subtitle = "${list.size} ${if (list.size == 1) "entry" else "entries"}",
                            summary = summarize(list)
                        )
                    }
            }
        }
    }
}
