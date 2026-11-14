package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.model.CurrencyCode
import com.example.data.model.TransactionStatus
import com.example.data.repository.LedgerRepository
import com.example.domain.ProfitCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

import com.example.data.model.AppSettings
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.repository.BackupSerializer
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SafiProfit", appName)
    }

    @Test
    fun `backup serialization roundtrip preserves exact decimals and prevents duplicate uuids`() {
        val tx = TransactionRecord(
            id = 1L,
            uuid = "uuid-abc-123",
            type = TransactionType.AD_EXPENSE,
            description = "Meta Ads",
            amountOriginal = BigDecimal("44.27"),
            currency = CurrencyCode.USD,
            exchangeRate = BigDecimal("10.15"),
            amountMad = BigDecimal("449.3405"),
            dateString = "2026-11-14",
            status = TransactionStatus.PAID
        )
        val settings = AppSettings(defaultUsdRate = BigDecimal("10.15"))
        val json = BackupSerializer.toJsonString(listOf(tx), settings)
        val decoded = BackupSerializer.fromJsonString(json)

        assertTrue(decoded.isSuccess)
        val payload = decoded.getOrThrow()
        assertEquals(1, payload.transactions.size)
        assertEquals(0, BigDecimal("44.27").compareTo(payload.transactions.first().amountOriginal))
        assertEquals(0, BigDecimal("449.3405").compareTo(payload.transactions.first().amountMad))
        assertEquals(0, BigDecimal("10.15").compareTo(payload.settings!!.defaultUsdRate))
    }

    @Test
    fun `room repository persists transactions and updates pending to received without duplicates`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LedgerRepository(db.transactionDao(), SettingsPreferences(context))

            repo.addIncome(
                amountMad = BigDecimal("830"),
                description = "Affipartner payout",
                dateString = "2026-11-14",
                status = TransactionStatus.RECEIVED
            )
            repo.addIncome(
                amountMad = BigDecimal("158"),
                description = "Affipartner payout",
                dateString = "2026-11-14",
                status = TransactionStatus.PENDING
            )
            repo.addAdExpense(
                amountOriginal = BigDecimal("44.27"),
                currency = CurrencyCode.USD,
                exchangeRate = BigDecimal("10"),
                description = "Meta Ads",
                dateString = "2026-11-14"
            )

            val initialList = repo.transactionsFlow.first()
            assertEquals(3, initialList.size)

            val beforeSummary = ProfitCalculator.summarize(initialList)
            assertEquals(0, BigDecimal("545.30").compareTo(beforeSummary.netProfit))
            assertEquals(0, BigDecimal("387.30").compareTo(beforeSummary.moneyReceivedInBank))

            val pendingItem = initialList.first { it.status == TransactionStatus.PENDING }
            repo.markIncomeAsReceived(pendingItem.id)

            val afterList = repo.transactionsFlow.first()
            assertEquals(3, afterList.size) // No duplicate created
            val afterSummary = ProfitCalculator.summarize(afterList)
            assertEquals(0, BigDecimal("545.30").compareTo(afterSummary.netProfit))
            assertEquals(0, BigDecimal("545.30").compareTo(afterSummary.moneyReceivedInBank))
        } finally {
            db.close()
        }
    }
}
