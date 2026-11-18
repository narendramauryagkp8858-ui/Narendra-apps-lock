package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {
    @Query("SELECT * FROM locked_apps")
    fun observeAll(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    fun observeLocked(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    suspend fun getLockedNow(): List<LockedAppEntity>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LockedAppEntity>)

    @Query("UPDATE locked_apps SET lastUnlockedAt = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastUnlocked(packageName: String, timestamp: Long)

    @Query("UPDATE locked_apps SET lockCount = lockCount + 1 WHERE packageName = :packageName")
    suspend fun incrementLockCount(packageName: String)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}

@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultItemEntity>>

    @Query("SELECT COUNT(*) FROM vault_items")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItemEntity): Long

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultItemEntity?
}

@Dao
interface SecurityEventDao {
    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT 250")
    fun observeRecent(): Flow<List<SecurityEventEntity>>

    @Query("SELECT * FROM security_events WHERE eventType = 'INTRUDER_ALERT' OR eventType = 'AUTH_FAILED' ORDER BY timestamp DESC")
    fun observeIntruderAlerts(): Flow<List<SecurityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SecurityEventEntity): Long

    @Query("DELETE FROM security_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM security_events")
    suspend fun clearAll()
}
