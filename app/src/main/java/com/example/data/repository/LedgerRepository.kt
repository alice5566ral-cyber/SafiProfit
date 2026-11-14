package com.example.data.repository

import com.example.data.local.SettingsPreferences
import com.example.data.local.TransactionDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.AppSettings
import com.example.data.model.CurrencyCode
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.UUID

class LedgerRepository(
    private val dao: TransactionDao,
    private val settingsPreferences: SettingsPreferences
) {
    val transactionsFlow: Flow<List<TransactionRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    val settingsFlow: Flow<AppSettings> = settingsPreferences.settingsFlow

    suspend fun addIncome(
        amountMad: BigDecimal,
        description: String,
        dateString: String,
        status: TransactionStatus
    ) {
        val now = System.currentTimeMillis()
        val record = TransactionRecord(
            id = 0L,
            uuid = UUID.randomUUID().toString(),
            type = TransactionType.INCOME,
            description = description.trim(),
            amountOriginal = amountMad,
            currency = CurrencyCode.MAD,
            exchangeRate = BigDecimal.ONE,
            amountMad = amountMad,
            dateString = dateString,
            status = status,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.insertIgnore(record.toEntity())
    }

    suspend fun addAdExpense(
        amountOriginal: BigDecimal,
        currency: CurrencyCode,
        exchangeRate: BigDecimal,
        description: String,
        dateString: String
    ) {
        val now = System.currentTimeMillis()
        val effectiveRate = if (currency == CurrencyCode.MAD) BigDecimal.ONE else exchangeRate
        val amountMad = if (currency == CurrencyCode.MAD) {
            amountOriginal
        } else {
            amountOriginal.multiply(effectiveRate)
        }
        val record = TransactionRecord(
            id = 0L,
            uuid = UUID.randomUUID().toString(),
            type = TransactionType.AD_EXPENSE,
            description = description.trim(),
            amountOriginal = amountOriginal,
            currency = currency,
            exchangeRate = effectiveRate,
            amountMad = amountMad,
            dateString = dateString,
            status = TransactionStatus.PAID,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.insertIgnore(record.toEntity())
    }

    suspend fun updateTransaction(record: TransactionRecord) {
        val effectiveRate = if (record.currency == CurrencyCode.MAD) BigDecimal.ONE else record.exchangeRate
        val effectiveMad = if (record.currency == CurrencyCode.MAD) {
            record.amountOriginal
        } else {
            record.amountOriginal.multiply(effectiveRate)
        }
        val updated = record.copy(
            description = record.description.trim(),
            exchangeRate = effectiveRate,
            amountMad = effectiveMad,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.update(updated.toEntity())
    }

    suspend fun markIncomeAsReceived(id: Long) {
        dao.updateStatus(
            id = id,
            newStatus = TransactionStatus.RECEIVED.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAllTransactions() {
        dao.deleteAll()
    }

    suspend fun updateExchangeRates(usdRate: BigDecimal, eurRate: BigDecimal) {
        settingsPreferences.updateExchangeRates(usdRate, eurRate)
    }

    suspend fun setAlwaysShowDecimals(enabled: Boolean) {
        settingsPreferences.setAlwaysShowDecimals(enabled)
    }

    suspend fun setWeekStartsOnMonday(enabled: Boolean) {
        settingsPreferences.setWeekStartsOnMonday(enabled)
    }

    suspend fun exportBackupJson(): String {
        val txs = dao.getAllOnce().map { it.toDomain() }
        val settings = settingsPreferences.settingsFlow.first()
        return BackupSerializer.toJsonString(txs, settings)
    }

    suspend fun restoreBackupJson(json: String, replaceExisting: Boolean): Result<Int> {
        val parsed = BackupSerializer.fromJsonString(json)
        return parsed.map { payload ->
            val entities = payload.transactions.map { it.toEntity() }
            val count = if (replaceExisting) {
                dao.replaceAll(entities)
                entities.size
            } else {
                dao.mergeWithoutDuplicates(entities)
            }
            if (replaceExisting && payload.settings != null) {
                settingsPreferences.updateExchangeRates(
                    payload.settings.defaultUsdRate,
                    payload.settings.defaultEurRate
                )
                settingsPreferences.setAlwaysShowDecimals(payload.settings.alwaysShowDecimals)
                settingsPreferences.setWeekStartsOnMonday(payload.settings.weekStartsOnMonday)
            }
            count
        }
    }
}
