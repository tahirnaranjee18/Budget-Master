package com.example.budgetmaster

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmaster.adapter.BudgetAdapter
import com.example.budgetmaster.data.Budget
import com.example.budgetmaster.data.FirestoreBudgetRepository
import com.example.budgetmaster.data.FirestoreTransactionRepository
import com.example.budgetmaster.viewmodel.BudgetViewModel
import com.example.budgetmaster.viewmodel.BudgetViewModelFactory
import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import java.util.Calendar
import java.util.Locale

class PlansActivity : AppCompatActivity() {

    private lateinit var homeButton: ImageButton
    private lateinit var analyticsButton: ImageButton
    private lateinit var transactionsButton: ImageButton
    private lateinit var plansButton: ImageButton
    private lateinit var profileButton: ImageButton
    private lateinit var allNavButtons: List<ImageButton>

    private lateinit var totalExpensesBudgetsAmount: TextView
    private lateinit var totalAllocatedBudgetTextView: TextView
    private lateinit var totalSpentBudgetTextView: TextView
    private lateinit var overallBudgetProgressBar: ProgressBar
    private lateinit var overallBudgetMessageTextView: TextView

    private lateinit var budgetsRecyclerView: RecyclerView
    private lateinit var budgetAdapter: BudgetAdapter

    private lateinit var budgetViewModel: BudgetViewModel
    private lateinit var transactionViewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plans)

        homeButton = findViewById(R.id.homeButton)
        analyticsButton = findViewById(R.id.analyticsButton)
        transactionsButton = findViewById(R.id.transactionsButton)
        plansButton = findViewById(R.id.plansButton)
        profileButton = findViewById(R.id.profileButton)
        allNavButtons = listOf(homeButton, analyticsButton, transactionsButton, plansButton, profileButton)

        totalExpensesBudgetsAmount = findViewById(R.id.total_expenses_budgets_amount)

        totalAllocatedBudgetTextView = findViewById(R.id.totalAllocatedBudgetTextView)
        totalSpentBudgetTextView = findViewById(R.id.totalSpentBudgetTextView)
        overallBudgetProgressBar = findViewById(R.id.overallBudgetProgressBar)
        overallBudgetMessageTextView = findViewById(R.id.overallBudgetMessageTextView)

        budgetsRecyclerView = findViewById(R.id.budgetsRecyclerView)
        budgetsRecyclerView.layoutManager = LinearLayoutManager(this)

        val firestoreBudgetRepository = FirestoreBudgetRepository()
        val budgetViewModelFactory = BudgetViewModelFactory(firestoreBudgetRepository)
        budgetViewModel = ViewModelProvider(this, budgetViewModelFactory)[BudgetViewModel::class.java]

        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val transactionViewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, transactionViewModelFactory)[TransactionViewModel::class.java]

        budgetAdapter = BudgetAdapter(this, emptyMap()) { budgetToDelete ->
            deleteBudget(budgetToDelete)
        }
        budgetsRecyclerView.adapter = budgetAdapter


        observeBudgetsAndTransactions()
        observeMonthlyExpenses()


        setBottomNavigation()

        val createBudgetButton = findViewById<Button>(R.id.create_budget_button)
        createBudgetButton.setOnClickListener {
            val intent = Intent(this, CreateBudgetCategoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeMonthlyExpenses() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis

        transactionViewModel.getTransactionsByDateRange(startOfMonth, endOfMonth).observe(this, Observer { transactions ->
            val totalExpenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
            totalExpensesBudgetsAmount.text = String.format(Locale.getDefault(), "R%.2f", totalExpenses)
        })
    }

    private fun deleteBudget(budget: Budget) {
        budgetViewModel.deleteBudget(budget.id)
        Toast.makeText(this, "Budget '${budget.name}' deleted!", Toast.LENGTH_SHORT).show()
    }


    private fun observeBudgetsAndTransactions() {
        budgetViewModel.allBudgets.observe(this) { budgets ->
            transactionViewModel.expenseSumByCategory.observe(this) { categorySums ->
                budgetAdapter.updateSpendingMap(categorySums)
                budgetAdapter.submitList(budgets)

                val noBudgetsMessage = findViewById<TextView>(R.id.noBudgetsMessage)
                if (budgets.isEmpty()) {
                    noBudgetsMessage.visibility = TextView.VISIBLE
                    budgetsRecyclerView.visibility = RecyclerView.GONE
                } else {
                    noBudgetsMessage.visibility = TextView.GONE
                    budgetsRecyclerView.visibility = RecyclerView.VISIBLE
                }

                updateOverallBudgetSummary(budgets, categorySums)
            }
        }
    }

    private fun updateOverallBudgetSummary(budgets: List<Budget>, categorySums: Map<String, Double>) {
        var totalAllocated = 0.0
        var totalSpent = 0.0

        for (budget in budgets) {
            totalAllocated += budget.amount
            totalSpent += categorySums[budget.category] ?: 0.0
        }

        totalAllocatedBudgetTextView.text = String.format(Locale.getDefault(), "Maximum Monthly Goal: R%.2f", totalAllocated)
        totalSpentBudgetTextView.text = String.format(Locale.getDefault(), "Minimum Monthly Goal: R%.2f", totalSpent)

        if (totalAllocated > 0) {
            val progress = ((totalSpent / totalAllocated) * 100).toInt()
            overallBudgetProgressBar.progress = progress

            overallBudgetMessageTextView.text = if (totalSpent >= totalAllocated) {
                "Overall budget exceeded by R%.2f!".format(Locale.getDefault(), totalSpent - totalAllocated)
            } else {
                "Overall budget %d%% spent. Remaining: R%.2f".format(Locale.getDefault(), progress, totalAllocated - totalSpent)
            }

            if (totalSpent >= totalAllocated) {
                overallBudgetProgressBar.progressTintList = ContextCompat.getColorStateList(this, R.color.red_500)
            } else {
                overallBudgetProgressBar.progressTintList = ContextCompat.getColorStateList(this, R.color.accent_color)
            }
        } else {
            overallBudgetProgressBar.progress = 0
            overallBudgetMessageTextView.text = "No budgets set yet."
            overallBudgetProgressBar.progressTintList = ContextCompat.getColorStateList(this, R.color.accent_color)
        }
    }


    private fun setBottomNavigation() {
        homeButton.setOnClickListener { navigateTo(MainActivity::class.java, "home") }
        analyticsButton.setOnClickListener { navigateTo(HistoryActivity::class.java, "analytics") }
        transactionsButton.setOnClickListener { navigateTo(TransactionsActivity::class.java, "transactions") }
        plansButton.setOnClickListener { navigateTo(PlansActivity::class.java, "plans") }
        profileButton.setOnClickListener { navigateTo(ProfileActivity::class.java, "profile") }

        updateActiveButton("plans")
    }

    private fun navigateTo(activityClass: Class<*>, activeTag: String) {
        Log.d("PlansActivity", "Navigating from ${this::class.java.simpleName} to ${activityClass.simpleName}")

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