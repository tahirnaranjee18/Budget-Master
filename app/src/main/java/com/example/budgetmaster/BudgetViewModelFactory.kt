package com.example.budgetmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budgetmaster.data.FirestoreBudgetRepository


class BudgetViewModelFactory(private val budgetRepository: FirestoreBudgetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(budgetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}