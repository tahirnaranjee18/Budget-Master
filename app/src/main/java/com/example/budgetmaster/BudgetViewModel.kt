package com.example.budgetmaster.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.budgetmaster.data.Budget
import com.example.budgetmaster.data.FirestoreBudgetRepository
import kotlinx.coroutines.launch

class BudgetViewModel(private val budgetRepository: FirestoreBudgetRepository) : ViewModel() {

    val allBudgets: LiveData<List<Budget>> = budgetRepository.getAllBudgets()

    fun insertBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.insertBudget(budget)
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
        }
    }

    fun getBudgetsByCategory(categoryName: String): LiveData<List<Budget>> {
        return budgetRepository.getBudgetsByCategory(categoryName)
    }

    val allBudgetCategories: LiveData<List<String>> = allBudgets.map { budgets ->
        budgets.map { it.category }.distinct()
    }

    fun getTotalBudgetAllocated(): LiveData<Double?> {
        return allBudgets.map { budgets ->
            if (budgets.isEmpty()) null else budgets.sumOf { it.amount }
        }
    }

    fun getBudgetCount(): LiveData<Int> {
        return allBudgets.map { it.size }
    }

    fun getBudgetAllocatedForCategory(category: String): LiveData<Double?> {
        return allBudgets.map { budgets ->
            val amount = budgets.filter { it.category == category }.sumOf { it.amount }
            if (amount == 0.0 && budgets.none { it.category == category }) null else amount
        }
    }
}