package com.example

import com.example.data.model.BreakdownGranularity
import com.example.data.model.CurrencyCode
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.domain.MoneyFormatter
import com.example.domain.ProfitCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ProfitCalculatorTest {

    @Test
    fun `pending vs received workflow keeps total profit unchanged and updates bank money`() {
        val receivedIncome = TransactionRecord(
            id = 1L,
            type = TransactionType.INCOME,
            description = "Affipartner payout",
            amountOriginal = BigDecimal("830"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("830"),
            dateString = "2026-11-14",
            status = TransactionStatus.RECEIVED
        )
        val pendingIncome = TransactionRecord(
            id = 2L,
            type = TransactionType.INCOME,
            description = "Affipartner payout",
            amountOriginal = BigDecimal("158"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("158"),
            dateString = "2026-11-14",
            status = TransactionStatus.PENDING
        )
        val paidAds = TransactionRecord(
            id = 3L,
            type = TransactionType.AD_EXPENSE,
            description = "Meta Ads",
            amountOriginal = BigDecimal("44.3"),
            currency = CurrencyCode.USD,
            exchangeRate = BigDecimal("10"),
            amountMad = BigDecimal("443.0"),
            dateString = "2026-11-14",
            status = TransactionStatus.PAID
        )

        val before = ProfitCalculator.summarize(listOf(receivedIncome, pendingIncome, paidAds))
        assertEquals(0, BigDecimal("545").compareTo(before.netProfit))
        assertEquals(0, BigDecimal("387").compareTo(before.moneyReceivedInBank))
        assertEquals(0, BigDecimal("158").compareTo(before.pendingIncome))

        // Mark pending as received
        val updatedList = listOf(
            receivedIncome,
            pendingIncome.copy(status = TransactionStatus.RECEIVED),
            paidAds
        )
        val after = ProfitCalculator.summarize(updatedList)
        assertEquals(0, BigDecimal("545").compareTo(after.netProfit))
        assertEquals(0, BigDecimal("545").compareTo(after.moneyReceivedInBank))
        assertEquals(0, BigDecimal.ZERO.compareTo(after.pendingIncome))
    }

    @Test
    fun `exact decimal currency conversion preserves 44_27 x 10 = 442_70 DH`() {
        val amount = MoneyFormatter.parseBigDecimal("44.27")!!
        val rate = MoneyFormatter.parseBigDecimal("10")!!
        val converted = amount.multiply(rate)

        assertEquals("442.70 DH", MoneyFormatter.formatMad(converted, alwaysShowDecimals = false))
        assertEquals(0, BigDecimal("442.70").compareTo(converted))
    }

    @Test
    fun `negative net loss is never hidden`() {
        val income = TransactionRecord(
            id = 1L,
            type = TransactionType.INCOME,
            description = "Payout",
            amountOriginal = BigDecimal("830"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("830"),
            dateString = "2026-11-14",
            status = TransactionStatus.RECEIVED
        )
        val ads = TransactionRecord(
            id = 2L,
            type = TransactionType.AD_EXPENSE,
            description = "Meta Ads",
            amountOriginal = BigDecimal("950"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("950"),
            dateString = "2026-11-14",
            status = TransactionStatus.PAID
        )

        val summary = ProfitCalculator.summarize(listOf(income, ads))
        assertEquals(0, BigDecimal("-120").compareTo(summary.netProfit))
        assertEquals("-120 DH", MoneyFormatter.formatMad(summary.netProfit, includePlusSign = true))
    }

    @Test
    fun `period comparison calculates profit delta and percentage`() {
        val lastWeek = TransactionRecord(
            id = 1L,
            type = TransactionType.INCOME,
            description = "Payout",
            amountOriginal = BigDecimal("1000"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("1000"),
            dateString = "2026-11-04",
            status = TransactionStatus.RECEIVED
        )
        val thisWeek = TransactionRecord(
            id = 2L,
            type = TransactionType.INCOME,
            description = "Payout",
            amountOriginal = BigDecimal("1240"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("1240"),
            dateString = "2026-11-11",
            status = TransactionStatus.RECEIVED
        )

        val comparison = ProfitCalculator.comparePeriods(
            transactions = listOf(lastWeek, thisWeek),
            currentRange = "2026-11-09" to "2026-11-15",
            previousRange = "2026-11-02" to "2026-11-08"
        )
        assertEquals(0, BigDecimal("240").compareTo(comparison.profitDelta))
        assertEquals(0, BigDecimal("24.0").compareTo(comparison.percentChange))
    }

    @Test
    fun `breakdown groups transactions by day week month and year`() {
        val tx1 = TransactionRecord(
            id = 1L,
            type = TransactionType.INCOME,
            description = "Payout 1",
            amountOriginal = BigDecimal("500"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("500"),
            dateString = "2026-11-10",
            status = TransactionStatus.RECEIVED
        )
        val tx2 = TransactionRecord(
            id = 2L,
            type = TransactionType.INCOME,
            description = "Payout 2",
            amountOriginal = BigDecimal("300"),
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = BigDecimal("300"),
            dateString = "2026-10-15",
            status = TransactionStatus.RECEIVED
        )
        val months = ProfitCalculator.buildBreakdown(listOf(tx1, tx2), BreakdownGranularity.MONTH, true)
        val years = ProfitCalculator.buildBreakdown(listOf(tx1, tx2), BreakdownGranularity.YEAR, true)
        assertEquals(2, months.size)
        assertEquals(1, years.size)
        assertEquals(0, BigDecimal("800").compareTo(years.first().summary.netProfit))
    }
}
