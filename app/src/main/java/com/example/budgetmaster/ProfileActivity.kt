package com.example.budgetmaster

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.budgetmaster.data.Achievement
import com.example.budgetmaster.data.FirestoreAchievementRepository
import com.example.budgetmaster.data.FirestoreBudgetRepository
import com.example.budgetmaster.data.FirestoreTransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.budgetmaster.viewmodel.BudgetViewModel
import com.example.budgetmaster.viewmodel.BudgetViewModelFactory
import com.example.budgetmaster.viewmodel.ProfileViewModel
import com.example.budgetmaster.viewmodel.ProfileViewModelFactory
import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


import kotlin.collections.sortedWith

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvUserLevel: TextView
    private lateinit var progressBarLevel: ProgressBar
    private lateinit var tvProgressDetails: TextView
    private lateinit var llAchievementsContainer: LinearLayout
    private lateinit var tvNoAchievements: TextView

    private lateinit var logoutButton: Button
    private lateinit var changePasswordButton: Button

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var budgetViewModel: BudgetViewModel

    private lateinit var homeButton: ImageButton
    private lateinit var analyticsButton: ImageButton
    private lateinit var transactionsButton: ImageButton
    private lateinit var plansButton: ImageButton
    private lateinit var profileButton: ImageButton
    private lateinit var allNavButtons: List<ImageButton>

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseAuth = FirebaseAuth.getInstance()

        tvUsername = findViewById(R.id.tv_username)
        tvUserLevel = findViewById(R.id.tv_user_level)
        progressBarLevel = findViewById(R.id.progress_bar_level)
        tvProgressDetails = findViewById(R.id.tv_progress_details)
        llAchievementsContainer = findViewById(R.id.ll_achievements_container)
        tvNoAchievements = findViewById(R.id.tv_no_achievements)

        logoutButton = findViewById(R.id.btn_logout)
        changePasswordButton = findViewById(R.id.btn_change_password)

        val currentUser = firebaseAuth.currentUser
        val usernameToDisplay = currentUser?.displayName ?: currentUser?.email ?: "Guest"
        tvUsername.text = "Welcome, ${usernameToDisplay}!"

        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val firestoreBudgetRepository = FirestoreBudgetRepository()
        val firestoreAchievementRepository = FirestoreAchievementRepository()

        val transactionViewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, transactionViewModelFactory)[TransactionViewModel::class.java]

        val budgetViewModelFactory = BudgetViewModelFactory(firestoreBudgetRepository)
        budgetViewModel = ViewModelProvider(this, budgetViewModelFactory)[BudgetViewModel::class.java]

        val profileViewModelFactory = ProfileViewModelFactory(firestoreAchievementRepository, transactionViewModel, budgetViewModel)
        profileViewModel = ViewModelProvider(this, profileViewModelFactory)[ProfileViewModel::class.java]

        profileViewModel.userLevel.observe(this) { level ->
            tvUserLevel.text = "Level: $level - ${getLevelName(level)}"
        }

        profileViewModel.progressToNextLevel.observe(this) { progress ->
            progressBarLevel.progress = progress
        }

        profileViewModel.totalTransactionCount.observe(this) { count ->
            val nextLevelRequirement = profileViewModel.nextLevelRequirement.value ?: 0
            if (profileViewModel.userLevel.value == 5) {
                tvProgressDetails.text = "Max Level Reached!"
            } else {
                tvProgressDetails.text = "$count/${nextLevelRequirement} transactions to next level"
            }
        }

        profileViewModel.allAchievements.observe(this) { achievements ->
            updateAchievementsDisplay(achievements)
        }

        homeButton = findViewById(R.id.homeButton)
        analyticsButton = findViewById(R.id.analyticsButton)
        transactionsButton = findViewById(R.id.transactionsButton)
        plansButton = findViewById(R.id.plansButton)
        profileButton = findViewById(R.id.profileButton)
        allNavButtons = listOf(homeButton, analyticsButton, transactionsButton, plansButton, profileButton)

        logoutButton.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
        }

        changePasswordButton.setOnClickListener {
            Toast.makeText(this, "Functionality coming soon.", Toast.LENGTH_SHORT).show()
        }

        setBottomNavigation()
    }

    private fun getLevelName(level: Int): String {
        return when (level) {
            1 -> "Novice Budgeter"
            2 -> "Apprentice Spender"
            3 -> "Savvy Saver"
            4 -> "Financial Strategist"
            5 -> "Budget Master"
            else -> "Unknown"
        }
    }

    private fun updateAchievementsDisplay(achievements: List<Achievement>) {
        llAchievementsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (achievements.isEmpty()) {
            tvNoAchievements.visibility = View.VISIBLE
        } else {
            tvNoAchievements.visibility = View.GONE
            val sortedAchievements = achievements.sortedWith(
                compareByDescending<Achievement> { it.unlocked }
                    .thenByDescending { it.unlockedDate ?: 0 }
            )

            for (achievement in sortedAchievements) {
                val badgeView = inflater.inflate(R.layout.item_achievement_badge, llAchievementsContainer, false)
                val icon: ImageView = badgeView.findViewById(R.id.iv_achievement_icon)
                val name: TextView = badgeView.findViewById(R.id.tv_achievement_name)
                val description: TextView = badgeView.findViewById(R.id.tv_achievement_description)
                val unlockedDateTv: TextView = badgeView.findViewById(R.id.tv_unlocked_date)

                name.text = achievement.name
                description.text = achievement.description

                if (achievement.unlocked) {
                    icon.setImageResource(R.drawable.ic_badge_unlocked)
                    unlockedDateTv.visibility = View.VISIBLE
                    val dateFormat = SimpleDateFormat("MMM dd,yyyy", Locale.getDefault())
                    unlockedDateTv.text = "Unlocked: ${achievement.unlockedDate?.let { dateFormat.format(Date(it)) } ?: "N/A"}"
                    name.setTextColor(Color.BLACK)
                    description.setTextColor(Color.DKGRAY)
                } else {
                    icon.setImageResource(R.drawable.ic_badge_locked)
                    unlockedDateTv.visibility = View.GONE
                    name.setTextColor(Color.parseColor("#9E9E9E"))
                    description.setTextColor(Color.parseColor("#BDBDBD"))
                }
                llAchievementsContainer.addView(badgeView)
            }
        }
    }

    private fun setBottomNavigation() {
        homeButton.setOnClickListener { navigateTo(MainActivity::class.java, "home") }
        analyticsButton.setOnClickListener { navigateTo(HistoryActivity::class.java, "analytics") }
        transactionsButton.setOnClickListener { navigateTo(TransactionsActivity::class.java, "transactions") }
        plansButton.setOnClickListener { navigateTo(PlansActivity::class.java, "plans") }
        profileButton.setOnClickListener { navigateTo(ProfileActivity::class.java, "profile") }

        updateActiveButton("profile")
    }

    private fun navigateTo(activityClass: Class<*>, activeTag: String) {
        Log.d("ProfileActivity", "Navigating from ${this::class.java.simpleName} to ${activityClass.simpleName}")

        if (this::class.java != activityClass) {
            val intent = Intent(this, activityClass)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else {
            Toast.makeText(this, "You are already on the ${activeTag.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} page!", Toast.LENGTH_SHORT).show()
        }
        updateActiveButton(activeTag)
    }

    private fun updateActiveButton(activeTag: String) {
        for (button in allNavButtons) {
            when (button.tag) {
                "home" -> button.setImageResource(if (activeTag == "home") R.drawable.ic_custom_home else R.drawable.ic_custom_home)
                "analytics" -> button.setImageResource(if (activeTag == "analytics") R.drawable.ic_custom_graph else R.drawable.ic_custom_graph)
                "transactions" -> button.setImageResource(if (activeTag == "transactions") R.drawable.ic_custom_arrows else R.drawable.ic_custom_arrows)
                "plans" -> button.setImageResource(if (activeTag == "plans") R.drawable.ic_custom_plans else R.drawable.ic_custom_plans)
                "profile" -> button.setImageResource(if (activeTag == "profile") R.drawable.ic_custom_profile else R.drawable.ic_custom_profile)
            }
        }
    }
}