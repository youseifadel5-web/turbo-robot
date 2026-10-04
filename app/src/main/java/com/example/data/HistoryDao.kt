package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem): Long

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    /** Keep a single row per URL — delete old then insert as newest. */
    @Query("DELETE FROM history WHERE url = :url OR title = :title")
    suspend fun deleteByUrlOrTitle(url: String, title: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
