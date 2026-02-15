package com.example.redcoattracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.redcoattracker.data.Discipline
import com.example.redcoattracker.data.DisciplineDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class JtacStatus { GREEN, YELLOW, RED }

class DisciplineViewModel(private val disciplineDao: DisciplineDao) : ViewModel() {

    val allDisciplines = disciplineDao.getAllDisciplines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val jtacStatus: StateFlow<JtacStatus> = allDisciplines.map {
        val today = LocalDate.now()
        val evaluation = it.find { d -> d.name == "Evaluation date" }
        val otherDisciplines = it.filter { d -> d.name != "Evaluation date" }

        val requiredDisciplines = listOf(
            "Type 1", "Type 2", "Type 3", "BOC", "BOT", "FW Cas", "RW Cas", "Hot", "VDL", "Laser", "Rem Obs", "LLTTP", "Night", "IR"
        )

        if (evaluation == null || ChronoUnit.MONTHS.between(evaluation.completionDate, today) >= 18) {
            JtacStatus.RED
        } else {
            val missingDisciplines = requiredDisciplines.any { required -> otherDisciplines.none { d -> d.name == required } }
            val expiredDisciplines = otherDisciplines.any { d -> ChronoUnit.MONTHS.between(d.completionDate, today) >= 6 }

            if (missingDisciplines || expiredDisciplines) {
                JtacStatus.YELLOW
            } else {
                JtacStatus.GREEN
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, JtacStatus.RED)

    fun insert(name: String, completionDate: LocalDate) {
        viewModelScope.launch {
            disciplineDao.insert(Discipline(name = name, completionDate = completionDate))
        }
    }
}
