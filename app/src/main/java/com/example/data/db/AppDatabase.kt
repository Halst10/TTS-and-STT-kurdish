package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "TTS" or "STT"
    val inputText: String,
    val outputText: String,
    val voiceName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_profiles")
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val audioPath: String,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val resonance: Float = 1.0f,
    val gender: String = "Neutral",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VoiceDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()

    @Query("SELECT * FROM voice_profiles ORDER BY timestamp DESC")
    fun getAllVoiceProfiles(): Flow<List<VoiceProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfile)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteVoiceProfileById(id: Int)
}

@Database(entities = [HistoryItem::class, VoiceProfile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceDao(): VoiceDao
}
