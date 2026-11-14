package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateString DESC, createdAtMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateString DESC, createdAtMillis DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE uuid = :uuid LIMIT 1")
    suspend fun findByUuid(uuid: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("UPDATE transactions SET status = :newStatus, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: String, updatedAt: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<TransactionEntity>) {
        deleteAll()
        entities.forEach { insertIgnore(it) }
    }

    @Transaction
    suspend fun mergeWithoutDuplicates(entities: List<TransactionEntity>): Int {
        var insertedCount = 0
        for (entity in entities) {
            val existing = findByUuid(entity.uuid)
            if (existing == null) {
                val result = insertIgnore(entity.copy(id = 0L))
                if (result != -1L) insertedCount++
            }
        }
        return insertedCount
    }
}
