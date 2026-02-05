package com.example.budgetmaster.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreTransactionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getUserTransactionsCollection() = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId).collection("transactions")
    }

    suspend fun insertTransaction(transaction: Transaction) {
        val transactionCollection = getUserTransactionsCollection()
        if (transactionCollection == null) {
            Log.e("FirestoreTransactionRepo", "User not logged in, cannot insert transaction.")
            throw IllegalStateException("User not logged in.")
        }

        val documentRef = if (transaction.id.isEmpty()) {
            transactionCollection.document()
        } else {
            transactionCollection.document(transaction.id)
        }

        documentRef.set(transaction).await()
        if (transaction.id.isEmpty()) {
            transaction.id = documentRef.id
        }
        Log.d("FirestoreTransactionRepo", "Transaction inserted/updated: ${transaction.id}")
    }

    suspend fun updateTransaction(transaction: Transaction) {
        if (transaction.id.isEmpty()) {
            Log.e("FirestoreTransactionRepo", "Transaction ID is empty, cannot update.")
            throw IllegalArgumentException("Transaction ID cannot be empty for update.")
        }
        insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: String) {
        val transactionCollection = getUserTransactionsCollection()
        if (transactionCollection == null) {
            Log.e("FirestoreTransactionRepo", "User not logged in, cannot delete transaction.")
            throw IllegalStateException("User not logged in.")
        }
        transactionCollection.document(transactionId).delete().await()
        Log.d("FirestoreTransactionRepo", "Transaction deleted: $transactionId")
    }

    fun getAllTransactions(): LiveData<List<Transaction>> {
        val liveData = MutableLiveData<List<Transaction>>()
        val transactionCollection = getUserTransactionsCollection()

        if (transactionCollection == null) {
            liveData.value = emptyList()
            return liveData
        }

        transactionCollection
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreTransactionRepo", "Listen failed for all transactions.", e)
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val transactions = snapshot.documents.mapNotNull { document ->
                        document.toObject(Transaction::class.java)?.apply { id = document.id }
                    }
                    liveData.value = transactions
                } else {
                    Log.d("FirestoreTransactionRepo", "No transactions found.")
                    liveData.value = emptyList()
                }
            }
        return liveData
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        val liveData = MutableLiveData<List<Transaction>>()
        val transactionCollection = getUserTransactionsCollection()

        if (transactionCollection == null) {
            liveData.value = emptyList()
            return liveData
        }

        transactionCollection
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreTransactionRepo", "Listen failed for transactions by date range.", e)
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val transactions = snapshot.documents.mapNotNull { document ->
                        document.toObject(Transaction::class.java)?.apply { id = document.id }
                    }
                    liveData.value = transactions
                } else {
                    Log.d("FirestoreTransactionRepo", "No transactions found in date range.")
                    liveData.value = emptyList()
                }
            }
        return liveData
    }

    fun getTransactionsByCategory(category: String): LiveData<List<Transaction>> {
        val liveData = MutableLiveData<List<Transaction>>()
        val transactionCollection = getUserTransactionsCollection()

        if (transactionCollection == null) {
            liveData.value = emptyList()
            return liveData
        }

        transactionCollection
            .whereEqualTo("category", category)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreTransactionRepo", "Listen failed for transactions by category.", e)
                    liveData.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val transactions = snapshot.documents.mapNotNull { document ->
                        document.toObject(Transaction::class.java)?.apply { id = document.id }
                    }
                    liveData.value = transactions
                } else {
                    Log.d("FirestoreTransactionRepo", "No transactions found for category: $category")
                    liveData.value = emptyList()
                }
            }
        return liveData
    }

    fun getTransactionsFiltered(category: String, startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        val liveData = MutableLiveData<List<Transaction>>()
        val transactionCollection = getUserTransactionsCollection()

        if (transactionCollection == null) {
            liveData.value = emptyList()
            return liveData
        }

        var query: Query = transactionCollection.orderBy("date", Query.Direction.DESCENDING)

        if (category.isNotBlank()) {
            query = query.whereEqualTo("category", category)
        }
        query = query
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)


        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("FirestoreTransactionRepo", "Listen failed for filtered transactions.", e)
                liveData.value = emptyList()
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val transactions = snapshot.documents.mapNotNull { document ->
                    document.toObject(Transaction::class.java)?.apply { id = document.id }
                }
                liveData.value = transactions
            } else {
                Log.d("FirestoreTransactionRepo", "No filtered transactions found.")
                liveData.value = emptyList()
            }
        }
        return liveData
    }
}