package com.example.budgetmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budgetmaster.data.FirestoreTransactionRepository


class TransactionViewModelFactory(private val transactionRepository: FirestoreTransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}