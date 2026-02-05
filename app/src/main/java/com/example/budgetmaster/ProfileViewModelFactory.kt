package com.example.budgetmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budgetmaster.data.FirestoreAchievementRepository

class ProfileViewModelFactory(

    private val achievementRepository: FirestoreAchievementRepository,
    private val transactionViewModel: TransactionViewModel,
    private val budgetViewModel: BudgetViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(achievementRepository, transactionViewModel, budgetViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}