package com.example.redcoattracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplineDao {
    @Insert
    suspend fun insert(discipline: Discipline)

    @Query("SELECT * FROM disciplines ORDER BY completionDate DESC")
    fun getAllDisciplines(): Flow<List<Discipline>>
}
