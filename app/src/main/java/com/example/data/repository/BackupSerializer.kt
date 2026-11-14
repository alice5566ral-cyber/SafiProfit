package com.example.data.repository

import com.example.data.model.AppSettings
import com.example.data.model.CurrencyCode
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.domain.DateUtils
import com.example.domain.MoneyFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.UUID

data class BackupPayload(
    val transactions: List<TransactionRecord>,
    val settings: AppSettings?
)

object BackupSerializer {
    private const val SCHEMA_VERSION = 1

    fun toJsonString(transactions: List<TransactionRecord>, settings: AppSettings): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val settingsObj = JSONObject().apply {
            put("defaultUsdRate", settings.defaultUsdRate.toPlainString())
            put("defaultEurRate", settings.defaultEurRate.toPlainString())
            put("alwaysShowDecimals", settings.alwaysShowDecimals)
            put("weekStartsOnMonday", settings.weekStartsOnMonday)
        }
        root.put("settings", settingsObj)

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("uuid", tx.uuid)
                put("type", tx.type.name)
                put("description", tx.description)
                put("amountOriginal", tx.amountOriginal.toPlainString())
                put("currency", tx.currency.name)
                put("exchangeRate", tx.exchangeRate.toPlainString())
                put("amountMad", tx.amountMad.toPlainString())
                put("dateString", tx.dateString)
                put("status", tx.status.name)
                put("createdAtMillis", tx.createdAtMillis)
                put("updatedAtMillis", tx.updatedAtMillis)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)
        return root.toString(2)
    }

    fun fromJsonString(json: String): Result<BackupPayload> {
        return runCatching {
            val root = JSONObject(json)
            val version = root.optInt("schemaVersion", 0)
            require(version in 1..SCHEMA_VERSION) { "Unsupported backup file version." }

            val txArray = root.getJSONArray("transactions")
            val parsedTransactions = mutableListOf<TransactionRecord>()
            val seenUuids = mutableSetOf<String>()

            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val uuid = obj.optString("uuid").ifBlank { UUID.randomUUID().toString() }
                if (!seenUuids.add(uuid)) continue // Deduplicate inside backup file

                val type = TransactionType.valueOf(obj.getString("type"))
                val description = obj.getString("description").trim()
                require(description.isNotEmpty()) { "Transaction description cannot be empty." }

                val currency = CurrencyCode.valueOf(obj.getString("currency"))
                val amountOriginal = MoneyFormatter.parseBigDecimal(obj.getString("amountOriginal"))
                    ?: throw IllegalArgumentException("Invalid original amount in backup.")
                require(amountOriginal > BigDecimal.ZERO) { "Amount must be positive." }

                val exchangeRate = MoneyFormatter.parseBigDecimal(obj.getString("exchangeRate"))
                    ?: throw IllegalArgumentException("Invalid exchange rate in backup.")
                require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive." }

                val amountMad = if (currency == CurrencyCode.MAD) {
                    amountOriginal
                } else {
                    amountOriginal.multiply(exchangeRate)
                }

                val dateString = obj.getString("dateString")
                require(DateUtils.isValidIsoDate(dateString)) { "Invalid date format in backup: $dateString" }

                val status = TransactionStatus.valueOf(obj.getString("status"))
                val createdAt = obj.optLong("createdAtMillis", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAtMillis", createdAt)

                parsedTransactions.add(
                    TransactionRecord(
                        id = 0L,
                        uuid = uuid,
                        type = type,
                        description = description,
                        amountOriginal = amountOriginal,
                        currency = currency,
                        exchangeRate = exchangeRate,
                        amountMad = amountMad,
                        dateString = dateString,
                        status = status,
                        createdAtMillis = createdAt,
                        updatedAtMillis = updatedAt
                    )
                )
            }

            val settingsObj = root.optJSONObject("settings")
            val settings = settingsObj?.let { s ->
                val usd = MoneyFormatter.parseBigDecimal(s.optString("defaultUsdRate"))
                    ?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal("10.00")
                val eur = MoneyFormatter.parseBigDecimal(s.optString("defaultEurRate"))
                    ?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal("10.85")
                AppSettings(
                    defaultUsdRate = usd,
                    defaultEurRate = eur,
                    alwaysShowDecimals = s.optBoolean("alwaysShowDecimals", false),
                    weekStartsOnMonday = s.optBoolean("weekStartsOnMonday", true)
                )
            }

            BackupPayload(parsedTransactions, settings)
        }
    }
}
