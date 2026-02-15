package com.example.redcoattracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discipline: Discipline)

    @Query("SELECT * FROM disciplines ORDER BY CASE WHEN name = 'Evaluation date' THEN 0 ELSE 1 END, completionDate DESC")
    fun getAllDisciplines(): Flow<List<Discipline>>

    @Query("SELECT * FROM disciplines")
    fun getAllDisciplinesList(): List<Discipline>
}
