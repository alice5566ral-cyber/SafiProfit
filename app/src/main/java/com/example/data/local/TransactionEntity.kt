package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.CurrencyCode
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import java.math.BigDecimal

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["dateString"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val uuid: String,
    val type: String,
    val description: String,
    val amountOriginal: String,
    val currency: String,
    val exchangeRate: String,
    val amountMad: String,
    val dateString: String,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

fun TransactionEntity.toDomain(): TransactionRecord {
    return TransactionRecord(
        id = id,
        uuid = uuid,
        type = TransactionType.valueOf(type),
        description = description,
        amountOriginal = BigDecimal(amountOriginal),
        currency = CurrencyCode.valueOf(currency),
        exchangeRate = BigDecimal(exchangeRate),
        amountMad = BigDecimal(amountMad),
        dateString = dateString,
        status = TransactionStatus.valueOf(status),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun TransactionRecord.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        uuid = uuid,
        type = type.name,
        description = description,
        amountOriginal = amountOriginal.toPlainString(),
        currency = currency.name,
        exchangeRate = exchangeRate.toPlainString(),
        amountMad = amountMad.toPlainString(),
        dateString = dateString,
        status = status.name,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}
