package com.example.falldetection.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_events")
data class PendingEvent(
    @PrimaryKey val id: String,
    val detected_at: String,
    val latitude: Double?,
    val longitude: Double?,
    val location_accuracy: Double?,
    val peak_acceleration: Double,
    val inactivity_seconds: Double?,
    val status: String,
    val notes: String?,
    val synced: Boolean = false,
)

@Dao
interface PendingEventDao {
    @Query("SELECT * FROM pending_events ORDER BY detected_at DESC")
    fun observeAll(): Flow<List<PendingEvent>>

    @Upsert
    suspend fun upsert(event: PendingEvent)

    @Query("UPDATE pending_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM pending_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM pending_events WHERE synced = 0")
    suspend fun unsynced(): List<PendingEvent>
}

@Database(entities = [PendingEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingEventDao(): PendingEventDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fall_detection.db",
                ).build().also { instance = it }
            }
    }
}
