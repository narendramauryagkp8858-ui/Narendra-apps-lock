package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.SecurityEventType
import com.example.data.model.VaultMediaType

class AppLockTypeConverters {
    @TypeConverter
    fun fromVaultMediaType(value: VaultMediaType): String = value.name

    @TypeConverter
    fun toVaultMediaType(value: String): VaultMediaType =
        runCatching { VaultMediaType.valueOf(value) }.getOrDefault(VaultMediaType.PHOTO)

    @TypeConverter
    fun fromSecurityEventType(value: SecurityEventType): String = value.name

    @TypeConverter
    fun toSecurityEventType(value: String): SecurityEventType =
        runCatching { SecurityEventType.valueOf(value) }.getOrDefault(SecurityEventType.SETTINGS_CHANGED)
}

@Database(
    entities = [LockedAppEntity::class, VaultItemEntity::class, SecurityEventEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppLockTypeConverters::class)
abstract class AppLockDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun securityEventDao(): SecurityEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppLockDatabase? = null

        fun getInstance(context: Context): AppLockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppLockDatabase::class.java,
                    "narendra_applock.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
