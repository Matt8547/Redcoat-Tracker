package com.example.redcoattracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

@Entity(
    tableName = "disciplines",
    indices = [Index(value = ["name"], unique = true)]
)
data class Discipline(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val completionDate: LocalDate
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}
