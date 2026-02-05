
package com.example.budgetmaster.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreBudgetRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getUserBudgetsCollection() = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId).collection("budgets")
    }


    suspend fun insertBudget(budget: Budget) {
        val budgetCollection = getUserBudgetsCollection()
        if (budgetCollection == null) {
            Log.e("FirestoreBudgetRepo", "User not logged in, cannot insert budget.")
            throw IllegalStateException("User not logged in.")
        }
        val documentRef = if (budget.id.isEmpty()) {

            budgetCollection.document()
        } else {

            budgetCollection.document(budget.id)
        }
        documentRef.set(budget).await()
        if (budget.id.isEmpty()) {

            budget.id = documentRef.id
        }
        Log.d("FirestoreBudgetRepo", "Budget inserted/updated: ${budget.id}")
    }


    suspend fun deleteBudget(budgetId: String) {
        val budgetCollection = getUserBudgetsCollection()
        if (budgetCollection == null) {
            Log.e("FirestoreBudgetRepo", "User not logged in, cannot delete budget.")
            throw IllegalStateException("User not logged in.")
        }
        budgetCollection.document(budgetId).delete().await()
        Log.d("FirestoreBudgetRepo", "Budget deleted: $budgetId")
    }


    fun getBudgetsByCategory(categoryName: String): LiveData<List<Budget>> {
        val liveData = MutableLiveData<List<Budget>>()
        val budgetCollection = getUserBudgetsCollection()

        if (budgetCollection == null) {
            liveData.value = emptyList()
            return liveData
        }

        budgetCollection
            .whereEqualTo("category", categoryName)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreBudgetRepo", "Listen failed for budgets by category.", e)
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val budgets = snapshot.documents.mapNotNull { document ->
                        document.toObject(Budget::class.java)?.apply { id = document.id }
                    }
                    liveData.value = budgets
                } else {
                    Log.d("FirestoreBudgetRepo", "No budgets found for category: $categoryName")
                    liveData.value = emptyList()
                }
            }
        return liveData
    }

    fun getAllBudgets(): LiveData<List<Budget>> {
        val liveData = MutableLiveData<List<Budget>>()
        val budgetCollection = getUserBudgetsCollection()

        if (budgetCollection == null) {
            liveData.value = emptyList()
            return liveData
        }

        budgetCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreBudgetRepo", "Listen failed for all budgets.", e)
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val budgets = snapshot.documents.mapNotNull { document ->
                        document.toObject(Budget::class.java)?.apply { id = document.id }
                    }
                    liveData.value = budgets
                } else {
                    Log.d("FirestoreBudgetRepo", "No budgets found.")
                    liveData.value = emptyList()
                }
            }
        return liveData
    }
}