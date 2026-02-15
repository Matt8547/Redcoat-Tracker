package com.example.redcoattracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.redcoattracker.data.DisciplineDao

class DisciplineViewModelFactory(private val disciplineDao: DisciplineDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisciplineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DisciplineViewModel(disciplineDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
