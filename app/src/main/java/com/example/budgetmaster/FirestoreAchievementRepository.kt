package com.example.budgetmaster.data

import com.example.budgetmaster.data.Achievement
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class FirestoreAchievementRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _allAchievements = MutableLiveData<List<Achievement>>()
    private val _unlockedAchievements = MutableLiveData<List<Achievement>>()
    private val _achievementById = MutableLiveData<Achievement?>()

    private val _cumulativeTransactionCount = MutableLiveData<Int>()

    val allAchievements: LiveData<List<Achievement>> = _allAchievements
    val unlockedAchievements: LiveData<List<Achievement>> = _unlockedAchievements
    val cumulativeTransactionCount: LiveData<Int> = _cumulativeTransactionCount

    private var allAchievementsListener: ListenerRegistration? = null
    private var unlockedAchievementsListener: ListenerRegistration? = null
    private var achievementByIdListener: ListenerRegistration? = null
    private var cumulativeTransactionCountListener: ListenerRegistration? = null


    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun startAllAchievementListeners() {
        clearAllAchievementListeners()

        val userId = getCurrentUserId()
        if (userId == null) {
            Log.w("FirestoreAchievementRepo", "No authenticated user to listen for achievements. Clearing LiveData.")
            _allAchievements.postValue(emptyList())
            _unlockedAchievements.postValue(emptyList())
            _achievementById.postValue(null)
            _cumulativeTransactionCount.postValue(0)
            return
        }

        allAchievementsListener = db.collection("users").document(userId).collection("achievements")
            .orderBy("name")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreAchievementRepo", "Listen failed for all achievements.", e)
                    _allAchievements.postValue(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val achievements = snapshot.documents.mapNotNull { document ->
                        document.toObject(Achievement::class.java)?.apply { id = document.id }
                    }
                    _allAchievements.postValue(achievements)
                    Log.d("FirestoreAchievementRepo", "All achievements updated. Count: ${achievements.size}")
                }
            }

        unlockedAchievementsListener = db.collection("users").document(userId).collection("achievements")
            .whereEqualTo("unlocked", true)
            .orderBy("unlockedDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreAchievementRepo", "Listen failed for unlocked achievements.", e)
                    _unlockedAchievements.postValue(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val achievements = snapshot.documents.mapNotNull { document ->
                        document.toObject(Achievement::class.java)?.apply { id = document.id }
                    }
                    _unlockedAchievements.postValue(achievements)
                    Log.d("FirestoreAchievementRepo", "Unlocked achievements updated. Count: ${achievements.size}")
                }
            }

        cumulativeTransactionCountListener = db.collection("users").document(userId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("FirestoreAchievementRepo", "Listen failed for cumulative transaction count.", e)
                    _cumulativeTransactionCount.postValue(0)
                    return@addSnapshotListener
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val count = documentSnapshot.getLong("transactionsMadeCount")?.toInt() ?: 0
                    _cumulativeTransactionCount.postValue(count)
                    Log.d("FirestoreAchievementRepo", "Cumulative transaction count updated: $count")
                } else {
                    _cumulativeTransactionCount.postValue(0)
                }
            }
    }

    fun clearAllAchievementListeners() {
        allAchievementsListener?.remove()
        unlockedAchievementsListener?.remove()
        achievementByIdListener?.remove()
        cumulativeTransactionCountListener?.remove()

        allAchievementsListener = null
        unlockedAchievementsListener = null
        achievementByIdListener = null
        cumulativeTransactionCountListener = null
        Log.d("FirestoreAchievementRepo", "All achievement and user data listeners cleared.")
    }

    suspend fun insertOrUpdateAchievement(achievement: Achievement) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e("FirestoreAchievementRepo", "User not logged in, cannot insert/update achievement.")
            throw IllegalStateException("User not logged in.")
        }
        db.collection("users").document(userId).collection("achievements").document(achievement.id)
            .set(achievement)
            .await()
        Log.d("FirestoreAchievementRepo", "Achievement inserted/updated: ${achievement.id}")
    }

    fun getAchievementById(achievementId: String): LiveData<Achievement?> {
        achievementByIdListener?.remove()
        achievementByIdListener = null

        val userId = getCurrentUserId()
        if (userId == null) {
            _achievementById.postValue(null)
            return _achievementById
        }

        achievementByIdListener = db.collection("users").document(userId).collection("achievements").document(achievementId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreAchievementRepo", "Listen failed for achievement by ID ($achievementId).", e)
                    _achievementById.postValue(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _achievementById.postValue(snapshot.toObject(Achievement::class.java)?.apply { id = snapshot.id })
                } else {
                    _achievementById.postValue(null)
                }
            }
        return _achievementById
    }


    suspend fun populateInitialAchievements() {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e("FirestoreAchievementRepo", "User not logged in, cannot populate initial achievements.")
            return
        }

        val initialAchievements = listOf(
            Achievement(id = "FIRST_TRANSACTION", name = "First Step", description = "Record your first transaction."),
            Achievement(id = "TEN_TRANSACTIONS", name = "Budget Maestro", description = "Record 10 transactions."),
            Achievement(id = "BUDGET_MASTER", name = "Getting Started", description = "Create three budgets."),
            Achievement(id = "SAVINGS_GURU", name = "Savings Guru", description = "Record 5 transactions where amount is below maximum monthly goal.")
        )

        val achievementsCollectionRef = db.collection("users").document(userId).collection("achievements")

        for (achievement in initialAchievements) {
            try {
                val existingDoc = achievementsCollectionRef.document(achievement.id).get().await()
                if (!existingDoc.exists()) {
                    achievementsCollectionRef.document(achievement.id).set(achievement).await()
                    Log.d("FirestoreAchievementRepo", "Populated initial achievement: ${achievement.name}")
                } else {
                    Log.d("FirestoreAchievementRepo", "Achievement '${achievement.name}' already exists, skipping population.")
                }
            } catch (e: Exception) {
                Log.e("FirestoreAchievementRepo", "Error populating achievement ${achievement.name}: ${e.message}", e)
            }
        }
        val userDocRef = db.collection("users").document(userId)
        try {
            val userDoc = userDocRef.get().await()
            if (!userDoc.contains("transactionsMadeCount")) {
                userDocRef.update("transactionsMadeCount", 0)
                    .addOnSuccessListener {
                        Log.d("FirestoreAchievementRepo", "Initialized transactionsMadeCount to 0 for new user.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreAchievementRepo", "Error initializing transactionsMadeCount: ${e.message}", e)
                    }
            }
        } catch (e: Exception) {
            Log.e("FirestoreAchievementRepo", "Error checking/initializing transactionsMadeCount for user: ${e.message}", e)
        }
    }
}