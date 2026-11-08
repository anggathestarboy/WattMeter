package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY endTime DESC")
    fun getAllSessions(): Flow<List<ChargingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSession): Long

    @Delete
    suspend fun deleteSession(session: ChargingSession)

    @Query("DELETE FROM charging_sessions")
    suspend fun clearAll()
}
