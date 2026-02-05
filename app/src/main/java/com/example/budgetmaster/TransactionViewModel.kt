package com.example.budgetmaster.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmaster.data.Transaction
import com.example.budgetmaster.data.FirestoreTransactionRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.map

class TransactionViewModel(private val transactionRepository: FirestoreTransactionRepository) : ViewModel() {  // ViewModel initialisation: [https://developer.android.com/topic/libraries/architecture/viewmodel]

    val allTransactions: LiveData<List<Transaction>> = transactionRepository.getAllTransactions()

    val totalIncome: LiveData<Double?> = allTransactions.map { transactions ->
        val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        if (income == 0.0 && transactions.none { it.type == "Income" }) null else income
    }

    val totalExpenses: LiveData<Double?> = allTransactions.map { transactions ->
        val expenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        if (expenses == 0.0 && transactions.none { it.type == "Expense" }) null else expenses
    }

    val expenseSumByCategory: LiveData<Map<String, Double>> = allTransactions.map { transactions ->
        transactions
            .filter { it.type == "Expense" }
            .groupBy { it.category }
            .mapValues { (_, transactionsInGroup) -> transactionsInGroup.sumOf { it.amount } }
    }

    val allDistinctCategories: LiveData<List<String>> = allTransactions.map { transactions ->
        transactions.map { it.category }.distinct()
    }

    fun getCategoryExpenseSum(category: String, startDate: Long, endDate: Long): LiveData<Double?> {
        return transactionRepository.getTransactionsByDateRange(startDate, endDate).map { transactions ->
            val amount = transactions.filter { it.category == category && it.type == "Expense" }.sumOf { it.amount }
            if (amount == 0.0 && transactions.none { it.category == category && it.type == "Expense" }) null else amount
        }
    }

    fun getTotalExpenseSum(): LiveData<Double?> {
        return totalExpenses
    }

    val transactionCount: LiveData<Int> = allTransactions.map { it.size }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transactionId)
        }
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return transactionRepository.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>> {
        return transactionRepository.getTransactionsByCategory(category)
    }

    fun getExpenseSumByCategoryAndDateRange(startDate: Long, endDate: Long): LiveData<Map<String, Double>> =
        transactionRepository.getTransactionsByDateRange(startDate, endDate).map { transactionsInDateRange ->
            transactionsInDateRange
                .filter { it.type == "Expense" }
                .groupBy { it.category }
                .mapValues { (_, transactionsInGroup) -> transactionsInGroup.sumOf { it.amount } }
        }

    fun getTransactionsFiltered(category: String, startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return transactionRepository.getTransactionsFiltered(category, startDate, endDate)
    }
}
