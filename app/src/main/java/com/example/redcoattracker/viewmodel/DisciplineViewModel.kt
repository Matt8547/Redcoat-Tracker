package com.example.redcoattracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.redcoattracker.data.Discipline
import com.example.redcoattracker.data.DisciplineDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DisciplineViewModel(private val disciplineDao: DisciplineDao) : ViewModel() {

    val allDisciplines = disciplineDao.getAllDisciplines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insert(name: String, completionDate: LocalDate) {
        viewModelScope.launch {
            disciplineDao.insert(Discipline(name = name, completionDate = completionDate))
        }
    }
}
