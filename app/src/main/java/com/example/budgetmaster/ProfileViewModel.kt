package com.example.budgetmaster.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.budgetmaster.data.Achievement
import com.example.budgetmaster.data.FirestoreAchievementRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val achievementRepository: FirestoreAchievementRepository,
    private val transactionViewModel: TransactionViewModel,
    private val budgetViewModel: BudgetViewModel
) : ViewModel() {

    val allAchievements: LiveData<List<Achievement>> = achievementRepository.allAchievements
    val unlockedAchievements: LiveData<List<Achievement>> = achievementRepository.unlockedAchievements

    val totalTransactionCount: LiveData<Int> = achievementRepository.cumulativeTransactionCount

    val totalBudgetCount: LiveData<Int> = budgetViewModel.getBudgetCount()

    private val _userLevel = MutableLiveData<Int>()
    val userLevel: LiveData<Int> = _userLevel

    private val _progressToNextLevel = MutableLiveData<Int>()
    val progressToNextLevel: LiveData<Int> = _progressToNextLevel

    private val _nextLevelRequirement = MutableLiveData<Int>()
    val nextLevelRequirement: LiveData<Int> = _nextLevelRequirement

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val transactionCountObserver: Observer<Int> = Observer { count ->
        updateLevel(count)
        val currentAchievements = allAchievements.value
        if (currentAchievements != null) {
            checkAchievements(count, totalBudgetCount.value ?: 0, currentAchievements)
        }
    }

    private val budgetCountObserver: Observer<Int> = Observer { count ->
        val currentAchievements = allAchievements.value
        if (currentAchievements != null) {
            checkAchievements(totalTransactionCount.value ?: 0, count, currentAchievements)
        }
    }

    private val allAchievementsObserver: Observer<List<Achievement>> = Observer { achievements ->
        val currentTxCount = totalTransactionCount.value
        val currentBudgetCount = totalBudgetCount.value
        if (currentTxCount != null && currentBudgetCount != null) {
            checkAchievements(currentTxCount, currentBudgetCount, achievements)
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser != null) {
            viewModelScope.launch {
                achievementRepository.populateInitialAchievements()
                achievementRepository.startAllAchievementListeners()
            }
        } else {
            achievementRepository.clearAllAchievementListeners()
            (_userLevel as? MutableLiveData)?.postValue(1)
            (_progressToNextLevel as? MutableLiveData)?.postValue(0)
            (_nextLevelRequirement as? MutableLiveData)?.postValue(5)
        }
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)

        totalTransactionCount.observeForever(transactionCountObserver)
        totalBudgetCount.observeForever(budgetCountObserver)
        allAchievements.observeForever(allAchievementsObserver)
    }

    private fun updateLevel(transactionCount: Int) {
        val level = when {
            transactionCount < 5 -> 1
            transactionCount < 15 -> 2
            transactionCount < 30 -> 3
            transactionCount < 50 -> 4
            else -> 5
        }
        _userLevel.value = level

        val nextLevelTxCount = when (level) {
            1 -> 5
            2 -> 15
            3 -> 30
            4 -> 50
            else -> transactionCount
        }
        _nextLevelRequirement.value = nextLevelTxCount

        if (level < 5) {
            val transactionsForCurrentLevel = when (level) {
                1 -> 0
                2 -> 5
                3 -> 15
                4 -> 30
                else -> 0
            }
            val denominator = nextLevelTxCount - transactionsForCurrentLevel
            _progressToNextLevel.value = if (denominator > 0) {
                ((transactionCount - transactionsForCurrentLevel).toDouble() / denominator * 100).toInt()
            } else {
                100
            }
        } else {
            _progressToNextLevel.value = 100
        }
    }

    private fun checkAchievements(transactionCount: Int, budgetCount: Int, achievements: List<Achievement>) {
        viewModelScope.launch {
            achievements.forEach { achievement ->
                if (!achievement.unlocked) {
                    val shouldUnlock = when (achievement.id) {
                        "FIRST_TRANSACTION" -> transactionCount >= 1
                        "TEN_TRANSACTIONS" -> transactionCount >= 10
                        "BUDGET_MASTER" -> budgetCount >= 3
                        "SAVINGS_GURU" -> transactionCount >= 5
                        else -> false
                    }

                    if (shouldUnlock) {
                        val updatedAchievement = achievement.copy(unlocked = true, unlockedDate = System.currentTimeMillis())
                        achievementRepository.insertOrUpdateAchievement(updatedAchievement)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authStateListener)
        totalTransactionCount.removeObserver(transactionCountObserver)
        totalBudgetCount.removeObserver(budgetCountObserver)
        allAchievements.removeObserver(allAchievementsObserver)

        achievementRepository.clearAllAchievementListeners()
    }
}