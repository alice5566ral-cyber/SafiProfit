package com.example.data.model

import java.math.BigDecimal
import java.util.UUID

enum class TransactionType {
    INCOME,
    AD_EXPENSE
}

enum class TransactionStatus {
    RECEIVED,
    PENDING,
    PAID
}

enum class CurrencyCode(val symbol: String, val displayLabel: String) {
    MAD("DH", "MAD (DH)"),
    USD("$", "USD ($)"),
    EUR("€", "EUR (€)")
}

data class TransactionRecord(
    val id: Long = 0L,
    val uuid: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val description: String,
    val amountOriginal: BigDecimal,
    val currency: CurrencyCode,
    val exchangeRate: BigDecimal,
    val amountMad: BigDecimal,
    val dateString: String, // ISO YYYY-MM-DD
    val status: TransactionStatus,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class PeriodSummary(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val receivedIncome: BigDecimal = BigDecimal.ZERO,
    val pendingIncome: BigDecimal = BigDecimal.ZERO,
    val totalAds: BigDecimal = BigDecimal.ZERO,
    val netProfit: BigDecimal = BigDecimal.ZERO,
    val moneyReceivedInBank: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0
)

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom")
}

enum class BreakdownGranularity(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

data class PeriodComparison(
    val current: PeriodSummary,
    val previous: PeriodSummary,
    val profitDelta: BigDecimal,
    val percentChange: BigDecimal? // Null when previous <= 0 to avoid misleading ratios
)

data class BreakdownBucket(
    val key: String,
    val title: String,
    val subtitle: String,
    val summary: PeriodSummary
)

enum class TransactionFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    ADS("Ads"),
    PENDING("Pending"),
    RECEIVED("Received")
}

data class AppSettings(
    val defaultUsdRate: BigDecimal = BigDecimal("10.00"),
    val defaultEurRate: BigDecimal = BigDecimal("10.85"),
    val alwaysShowDecimals: Boolean = false,
    val weekStartsOnMonday: Boolean = true
)
