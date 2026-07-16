package com.dotz.launcherpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FastlaneDao {
    @Query("SELECT * FROM fastlane_history WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getStream(): Flow<List<FastlaneEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordEvent(event: FastlaneEvent)

    @Query("UPDATE fastlane_history SET isDismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("SELECT * FROM fastlane_history WHERE title LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<FastlaneEvent>

    @Query("DELETE FROM fastlane_history WHERE timestamp < :threshold")
    suspend fun archive(threshold: Long)
}
