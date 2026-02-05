package com.example.budgetmaster

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgetmaster.adapter.GroupedTransactionsAdapter
import com.example.budgetmaster.data.Budget
import com.example.budgetmaster.data.Transaction
import com.example.budgetmaster.data.model.GroupedTransactionItem
import com.example.budgetmaster.utils.DateUtils
import com.example.budgetmaster.viewmodel.BudgetViewModel
import com.example.budgetmaster.viewmodel.BudgetViewModelFactory
import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import com.example.budgetmaster.data.FirestoreBudgetRepository
import com.example.budgetmaster.data.FirestoreTransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HistoryActivity : AppCompatActivity() {

    private lateinit var totalExpensesHistoryAmount: TextView
    private lateinit var dailyButton: Button
    private lateinit var weeklyButton: Button
    private lateinit var monthlyButton: Button
    private lateinit var graphHeadingTextView: TextView
    private lateinit var barChartContainer: LinearLayout
    private lateinit var emptyGraphMessage: TextView
    private lateinit var groupedTransactionsRecyclerView: RecyclerView


    private lateinit var homeButton: ImageButton
    private lateinit var analyticsButton: ImageButton
    private lateinit var transactionsButton: ImageButton
    private lateinit var plansButton: ImageButton
    private lateinit var profileButton: ImageButton
    private lateinit var allNavButtons: List<ImageButton>


    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var budgetViewModel: BudgetViewModel


    private lateinit var groupedTransactionsAdapter: GroupedTransactionsAdapter


    private var currentFilterMode: FilterMode = FilterMode.MONTHLY


    private lateinit var expenseSumsForGraph: MediatorLiveData<Map<String, Double>>
    private lateinit var allBudgetsForGraph: LiveData<List<Budget>>
    private lateinit var allDistinctCategoriesForGraph: LiveData<List<String>>
    private val graphTriggerLiveData: MediatorLiveData<Unit> = MediatorLiveData()


    private var currentExpenseSumsSource: LiveData<Map<String, Double>>? = null

    private var currentTransactionsListSource: LiveData<List<Transaction>>? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)


        totalExpensesHistoryAmount = findViewById(R.id.total_expenses_history_amount)
        dailyButton = findViewById(R.id.dailyButton)
        weeklyButton = findViewById(R.id.weeklyButton)
        monthlyButton = findViewById(R.id.monthlyButton)
        graphHeadingTextView = findViewById(R.id.graphHeadingTextView)
        barChartContainer = findViewById(R.id.barChartContainer)
        emptyGraphMessage = findViewById(R.id.emptyGraphMessage)
        groupedTransactionsRecyclerView = findViewById(R.id.groupedTransactionsRecyclerView)


        homeButton = findViewById(R.id.homeButton)
        analyticsButton = findViewById(R.id.analyticsButton)
        transactionsButton = findViewById(R.id.transactionsButton)
        plansButton = findViewById(R.id.plansButton)
        profileButton = findViewById(R.id.profileButton)
        allNavButtons = listOf(homeButton, analyticsButton, transactionsButton, plansButton, profileButton)


        groupedTransactionsRecyclerView.layoutManager = LinearLayoutManager(this)


        groupedTransactionsAdapter = GroupedTransactionsAdapter(
            { transactionToDelete -> deleteTransaction(transactionToDelete) },
            { imageUrl -> showEnlargedImage(imageUrl) }
        )


        groupedTransactionsRecyclerView.adapter = groupedTransactionsAdapter


        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val transactionViewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, transactionViewModelFactory)[TransactionViewModel::class.java]

        val firestoreBudgetRepository = FirestoreBudgetRepository()
        val budgetViewModelFactory = BudgetViewModelFactory(firestoreBudgetRepository)
        budgetViewModel = ViewModelProvider(this, budgetViewModelFactory)[BudgetViewModel::class.java]

        dailyButton.setOnClickListener { setFilterMode(FilterMode.DAILY) }
        weeklyButton.setOnClickListener { setFilterMode(FilterMode.WEEKLY) }
        monthlyButton.setOnClickListener { setFilterMode(FilterMode.MONTHLY) }


        setBottomNavigation()


        expenseSumsForGraph = MediatorLiveData()

        allBudgetsForGraph = budgetViewModel.allBudgets
        allDistinctCategoriesForGraph = transactionViewModel.allDistinctCategories

        graphTriggerLiveData.addSource(allBudgetsForGraph) {
            graphTriggerLiveData.value = Unit
        }
        graphTriggerLiveData.addSource(allDistinctCategoriesForGraph) {
            graphTriggerLiveData.value = Unit
        }
        graphTriggerLiveData.addSource(expenseSumsForGraph) {
            graphTriggerLiveData.value = Unit
        }

        graphTriggerLiveData.observe(this, Observer {
            updateGraph()
        })

        setFilterMode(currentFilterMode)
    }

    private fun setFilterMode(mode: FilterMode) {
        currentFilterMode = mode
        updateGraphHeading()
        updateButtonStates()
        loadAndDisplayData()
    }

    private fun updateGraphHeading() {
        graphHeadingTextView.text = when (currentFilterMode) {
            FilterMode.DAILY -> "Daily Transactions"
            FilterMode.WEEKLY -> "Weekly Transactions"
            FilterMode.MONTHLY -> "Monthly Transactions"
        }
    }

    private fun updateButtonStates() {
        val selectedColor = ContextCompat.getColorStateList(this, R.color.accent_color)
        val unselectedColor = ContextCompat.getColorStateList(this, R.color.teal_200)
        val textColor = ContextCompat.getColorStateList(this, android.R.color.white)

        dailyButton.backgroundTintList = if (currentFilterMode == FilterMode.DAILY) selectedColor else unselectedColor
        weeklyButton.backgroundTintList = if (currentFilterMode == FilterMode.WEEKLY) selectedColor else unselectedColor
        monthlyButton.backgroundTintList = if (currentFilterMode == FilterMode.MONTHLY) selectedColor else unselectedColor

        dailyButton.setTextColor(textColor)
        weeklyButton.setTextColor(textColor)
        monthlyButton.setTextColor(textColor)
    }

    private fun loadAndDisplayData() {
        val now = System.currentTimeMillis()
        val (graphStartDate, graphEndDate) = when (currentFilterMode) {
            FilterMode.DAILY -> Pair(DateUtils.getStartOfDay(now), DateUtils.getEndOfDay(now))
            FilterMode.WEEKLY -> Pair(DateUtils.getStartOfWeek(now), DateUtils.getEndOfWeek(now))
            FilterMode.MONTHLY -> Pair(DateUtils.getStartOfMonth(now), DateUtils.getEndOfMonth(now))
        }


        currentExpenseSumsSource?.let {
            expenseSumsForGraph.removeSource(it)
        }
        val newExpenseSumsSource = transactionViewModel.getExpenseSumByCategoryAndDateRange(graphStartDate, graphEndDate)
        expenseSumsForGraph.addSource(newExpenseSumsSource) { sums ->
            expenseSumsForGraph.value = sums
        }
        currentExpenseSumsSource = newExpenseSumsSource


        currentTransactionsListSource?.removeObservers(this)

        val transactionsListLiveData: LiveData<List<Transaction>>


        when (currentFilterMode) {
            FilterMode.DAILY -> {

                transactionsListLiveData = transactionViewModel.getTransactionsByDateRange(
                    DateUtils.getStartOfDay(now),
                    DateUtils.getEndOfDay(now)
                )
            }
            FilterMode.WEEKLY -> {

                transactionsListLiveData = transactionViewModel.getTransactionsByDateRange(
                    DateUtils.getStartOfWeek(now),
                    DateUtils.getEndOfWeek(now)
                )
            }
            FilterMode.MONTHLY -> {

                transactionsListLiveData = transactionViewModel.allTransactions
            }
        }
        currentTransactionsListSource = transactionsListLiveData

        transactionsListLiveData.observe(this, Observer { transactions ->
            updateGroupedTransactionsList(transactions)

            val totalExpenses = transactions
                .filter { it.type == "Expense" }
                .sumOf { it.amount }
            totalExpensesHistoryAmount.text = String.format(Locale.getDefault(), "R%.2f", totalExpenses)
        })
    }


    private fun updateGraph() {
        val allBudgets = allBudgetsForGraph.value ?: emptyList()
        val expenseSums = expenseSumsForGraph.value ?: emptyMap()
        val distinctCategories = allDistinctCategoriesForGraph.value ?: emptyList()

        val allInvolvedCategories = mutableSetOf<String>()

        allInvolvedCategories.addAll(distinctCategories)

        allInvolvedCategories.addAll(allBudgets.map { it.category })

        val graphData = mutableMapOf<String, Pair<Double, Double>>()
        var maxGraphValue = 0.0

        for (category in allInvolvedCategories.sorted()) {
            val spent = expenseSums[category] ?: 0.0
            val budget = allBudgets.firstOrNull { it.category == category }?.amount ?: 0.0

            graphData[category] = Pair(spent, budget)
            maxGraphValue = maxOf(maxGraphValue, spent, budget)
        }

        displayBarChart(graphData, maxGraphValue)
    }


    private fun displayBarChart(graphData: Map<String, Pair<Double, Double>>, maxValue: Double) {
        barChartContainer.removeAllViews()
        emptyGraphMessage.visibility = View.GONE

        if (graphData.isEmpty() || (graphData.values.all { it.first == 0.0 && it.second == 0.0 })) {
            emptyGraphMessage.visibility = View.VISIBLE
            return
        }

        val layoutInflater = LayoutInflater.from(this)
        val barMaxHeightPx = resources.getDimensionPixelSize(R.dimen.bar_chart_max_height)

        for (category in graphData.keys.sorted()) {
            val (spent, budget) = graphData[category] ?: Pair(0.0, 0.0)

            val barView = layoutInflater.inflate(R.layout.item_bar_chart_category, barChartContainer, false)

            val tvCategoryName: TextView = barView.findViewById(R.id.tv_category_name)
            val tvCategorySpentAmount: TextView = barView.findViewById(R.id.tv_category_spent_amount)
            val tvCategoryBudgetAmount: TextView = barView.findViewById(R.id.tv_category_budget_amount)
            val barSpent: View = barView.findViewById(R.id.bar_spent)
            val barBudget: View = barView.findViewById(R.id.bar_budget)

            tvCategoryName.text = category
            tvCategorySpentAmount.text = String.format(Locale.getDefault(), "R%.2f", spent)
            tvCategoryBudgetAmount.text = String.format(Locale.getDefault(), "R%.2f", budget)


            val spentHeight = if (maxValue > 0) (spent / maxValue * barMaxHeightPx).toInt() else 0
            val budgetHeight = if (maxValue > 0) (budget / maxValue * barMaxHeightPx).toInt() else 0

            val spentLayoutParams = barSpent.layoutParams as ConstraintLayout.LayoutParams
            spentLayoutParams.height = spentHeight
            barSpent.layoutParams = spentLayoutParams

            val budgetLayoutParams = barBudget.layoutParams as ConstraintLayout.LayoutParams
            budgetLayoutParams.height = budgetHeight
            barBudget.layoutParams = budgetLayoutParams

            if (budget > 0 && spent > budget) {
                barSpent.setBackgroundColor(ContextCompat.getColor(this, R.color.red_500))
            } else {
                barSpent.setBackgroundColor(ContextCompat.getColor(this, R.color.green_500))
            }
            barBudget.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_color))

            barChartContainer.addView(barView)
        }

        if (barChartContainer.childCount == 0) {
            emptyGraphMessage.visibility = View.VISIBLE
        }
    }


    private fun updateGroupedTransactionsList(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            groupedTransactionsAdapter.submitList(emptyList())
            return
        }

        val groupedItems = mutableListOf<GroupedTransactionItem>()

        when (currentFilterMode) {
            FilterMode.DAILY -> {
                val dailyHeaderFormatter = SimpleDateFormat("EEEE, MMMM dd,yyyy", Locale.getDefault())
                val today = DateUtils.getStartOfDay(System.currentTimeMillis())
                val headerTitle = dailyHeaderFormatter.format(today)

                groupedItems.add(GroupedTransactionItem.Header(headerTitle))
                transactions.sortedByDescending { it.date }.forEach { transaction ->
                    groupedItems.add(GroupedTransactionItem.TransactionItem(transaction))
                }
            }
            FilterMode.WEEKLY -> {
                val weeklyHeaderFormatter = SimpleDateFormat("'Week of' MMMM dd,yyyy", Locale.getDefault())
                val startOfWeek = DateUtils.getStartOfWeek(System.currentTimeMillis())
                val headerTitle = weeklyHeaderFormatter.format(startOfWeek)

                groupedItems.add(GroupedTransactionItem.Header(headerTitle))
                transactions.sortedByDescending { it.date }.forEach { transaction ->
                    groupedItems.add(GroupedTransactionItem.TransactionItem(transaction))
                }
            }
            FilterMode.MONTHLY -> {
                val sortedAllTransactions = transactions.sortedByDescending { it.date }

                val monthlyHeaderFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // Fix: Added " yyyy" to formatter

                val groupedByMonth = sortedAllTransactions.groupBy { transaction ->
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.date }
                    monthlyHeaderFormatter.format(cal.time)
                }

                val sortedMonthlyGroups = groupedByMonth.entries.sortedByDescending { (header, _) ->
                    monthlyHeaderFormatter.parse(header)?.time ?: 0L
                }

                for ((monthYearHeader, transactionsInMonth) in sortedMonthlyGroups) {
                    groupedItems.add(GroupedTransactionItem.Header(monthYearHeader))
                    transactionsInMonth.sortedByDescending { it.date }.forEach { transaction ->
                        groupedItems.add(GroupedTransactionItem.TransactionItem(transaction))
                    }
                }
            }
        }
        groupedTransactionsAdapter.submitList(groupedItems)
    }


    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionViewModel.deleteTransaction(transaction.id)
                Toast.makeText(this@HistoryActivity, "Transaction deleted!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "Error deleting transaction: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }


    private fun setBottomNavigation() {
        homeButton.setOnClickListener { navigateTo(MainActivity::class.java, "home") }
        analyticsButton.setOnClickListener { navigateTo(HistoryActivity::class.java, "analytics") }
        transactionsButton.setOnClickListener { navigateTo(TransactionsActivity::class.java, "transactions") }
        plansButton.setOnClickListener { navigateTo(PlansActivity::class.java, "plans") }
        profileButton.setOnClickListener { navigateTo(ProfileActivity::class.java, "profile") }

        updateActiveButton("analytics")
    }

    private fun navigateTo(activityClass: Class<*>, activeTag: String) {
        if (this::class.java != activityClass) {
            val intent = Intent(this, activityClass)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        } else if (activeTag != "analytics") {
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


    private fun showEnlargedImage(imageUrl: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enlarged_image, null)
        dialog.setContentView(dialogView)

        val enlargedImageView: ImageView = dialogView.findViewById(R.id.enlargedImageView)

        Glide.with(this)
            .load(imageUrl)
            .into(enlargedImageView)

        dialogView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.show()
    }


    enum class FilterMode {
        DAILY, WEEKLY, MONTHLY
    }
}