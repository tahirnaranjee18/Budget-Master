package com.example.budgetmaster

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.budgetmaster.data.Budget
import com.example.budgetmaster.data.Transaction
import com.example.budgetmaster.viewmodel.BudgetViewModel
import com.example.budgetmaster.viewmodel.BudgetViewModelFactory
import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import com.example.budgetmaster.data.FirestoreBudgetRepository
import com.example.budgetmaster.data.FirestoreTransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var welcomeTextView: TextView
    private lateinit var totalExpensesMainAmount: TextView
    private lateinit var pieChart: PieChart
    private lateinit var chartModeToggle: Switch
    private lateinit var legendContainer: LinearLayout

    private lateinit var homeButton: ImageButton
    private lateinit var analyticsButton: ImageButton
    private lateinit var transactionsButton: ImageButton
    private lateinit var plansButton: ImageButton
    private lateinit var profileButton: ImageButton
    private lateinit var allNavButtons: List<ImageButton>

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var budgetViewModel: BudgetViewModel

    private lateinit var allTransactions: LiveData<List<Transaction>>
    private lateinit var allBudgets: LiveData<List<Budget>>
    private lateinit var totalSpentLiveData: LiveData<Double?>
    private lateinit var totalBudgetAllocatedLiveData: LiveData<Double?>

    private val pieChartTriggerLiveData: MediatorLiveData<Unit> = MediatorLiveData()

    enum class ChartMode {
        CATEGORY_EXPENSES,
        BUDGET_VS_SPENT
    }
    private var currentChartMode: ChartMode = ChartMode.CATEGORY_EXPENSES

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        welcomeTextView = findViewById(R.id.welcomeText)
        totalExpensesMainAmount = findViewById(R.id.total_expenses_main_amount)
        pieChart = findViewById(R.id.pieChart)
        chartModeToggle = findViewById(R.id.chartModeToggle)
        legendContainer = findViewById(R.id.legendContainer)

        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
        val usernameToDisplay = currentUser?.displayName ?: currentUser?.email ?: "Guest"
        welcomeTextView.text = "Hi, Welcome Back, ${usernameToDisplay}!"

        homeButton = findViewById(R.id.homeButton)
        analyticsButton = findViewById(R.id.analyticsButton)
        transactionsButton = findViewById(R.id.transactionsButton)
        plansButton = findViewById(R.id.plansButton)
        profileButton = findViewById(R.id.profileButton)
        allNavButtons = listOf(homeButton, analyticsButton, transactionsButton, plansButton, profileButton)

        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val transactionViewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, transactionViewModelFactory)[TransactionViewModel::class.java]

        val firestoreBudgetRepository = FirestoreBudgetRepository()
        val budgetViewModelFactory = BudgetViewModelFactory(firestoreBudgetRepository)
        budgetViewModel = ViewModelProvider(this, budgetViewModelFactory)[BudgetViewModel::class.java]

        allTransactions = transactionViewModel.allTransactions
        allBudgets = budgetViewModel.allBudgets
        totalSpentLiveData = transactionViewModel.getTotalExpenseSum()
        totalBudgetAllocatedLiveData = budgetViewModel.getTotalBudgetAllocated()

        pieChartTriggerLiveData.addSource(allTransactions) { pieChartTriggerLiveData.value = Unit }
        pieChartTriggerLiveData.addSource(allBudgets) { pieChartTriggerLiveData.value = Unit }
        pieChartTriggerLiveData.addSource(totalSpentLiveData) { pieChartTriggerLiveData.value = Unit }
        pieChartTriggerLiveData.addSource(totalBudgetAllocatedLiveData) { pieChartTriggerLiveData.value = Unit }

        pieChartTriggerLiveData.observe(this, Observer {
            displayPieChart()
        })

        chartModeToggle.setOnCheckedChangeListener { _, isChecked ->
            currentChartMode = if (isChecked) ChartMode.BUDGET_VS_SPENT else ChartMode.CATEGORY_EXPENSES
            displayPieChart()
        }

        setupPieChart()

        setBottomNavigation()

        displayPieChart()
    }

    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 61f
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(false)
    }

    private fun displayPieChart() {
        val totalExpenses = totalSpentLiveData.value ?: 0.0
        totalExpensesMainAmount.text = String.format(Locale.getDefault(), "R%.2f", totalExpenses)

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val legendItems = mutableListOf<Pair<String, Int>>()

        if (currentChartMode == ChartMode.CATEGORY_EXPENSES) {
            val transactions = allTransactions.value ?: emptyList()
            val expenseSumsByCategory = transactions
                .filter { it.type == "Expense" }
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            if (expenseSumsByCategory.isEmpty()) {
                entries.add(PieEntry(1f, "No Expenses"))
                colors.add(Color.GRAY)
                legendItems.add(Pair("No Expenses", Color.GRAY))
            } else {
                val categoryColors = resources.getIntArray(R.array.pie_chart_colors)
                var colorIndex = 0
                for ((category, sum) in expenseSumsByCategory.entries.sortedByDescending { it.value }) {
                    if (sum > 0) {
                        entries.add(PieEntry(sum.toFloat()))
                        val color = categoryColors[colorIndex % categoryColors.size]
                        colors.add(color)
                        legendItems.add(Pair("$category: R%.2f".format(Locale.getDefault(), sum), color))
                        colorIndex++
                    }
                }
            }
        } else {
            val totalBudget = totalBudgetAllocatedLiveData.value ?: 0.0
            val totalSpent = totalSpentLiveData.value ?: 0.0

            if (totalBudget == 0.0 && totalSpent == 0.0) {
                entries.add(PieEntry(1f, "No Data"))
                colors.add(Color.GRAY)
                legendItems.add(Pair("No Data", Color.GRAY))
            } else {
                if (totalBudget > 0) {
                    entries.add(PieEntry(totalBudget.toFloat()))
                    colors.add(resources.getColor(R.color.total_budget_color, theme))
                    legendItems.add(Pair("Maximum Amount (Available to Spend): R%.2f".format(Locale.getDefault(), totalBudget), resources.getColor(R.color.total_budget_color, theme)))
                }
                if (totalSpent > 0) {
                    entries.add(PieEntry(totalSpent.toFloat()))
                    colors.add(resources.getColor(R.color.total_spent_color, theme))
                    legendItems.add(Pair("Minimum Amount (Money Spent): R%.2f".format(Locale.getDefault(), totalSpent), resources.getColor(R.color.total_spent_color, theme)))
                }
            }

            if (entries.isEmpty() && (totalBudget > 0 || totalSpent > 0)) {
                entries.add(PieEntry(1f, "Data Missing"))
                colors.add(Color.LTGRAY)
                legendItems.add(Pair("Data Missing", Color.LTGRAY))
            }
        }

        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "No Data"))
            colors.add(Color.GRAY)
            legendItems.add(Pair("No Data", Color.GRAY))
        }

        val dataSet = PieDataSet(entries, "Expense Data")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 5f
        dataSet.setDrawValues(true)
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.2f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        pieChart.data = data
        pieChart.invalidate()

        updateLegend(legendItems)
    }

    private fun updateLegend(legendItems: List<Pair<String, Int>>) {
        legendContainer.removeAllViews()

        if (legendItems.isEmpty()) {
            val noDataText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "No data to display."
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.GRAY)
                gravity = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 16, 0, 0)
            }
            legendContainer.addView(noDataText)
            return
        }

        val inflater = LayoutInflater.from(this)
        for (item in legendItems) {
            val legendView = inflater.inflate(R.layout.item_legend, legendContainer, false)
            val colorSquare: View = legendView.findViewById(R.id.legendColorSquare)
            val label: TextView = legendView.findViewById(R.id.legendLabel)

            colorSquare.setBackgroundColor(item.second)
            label.text = item.first

            legendContainer.addView(legendView)
        }
    }

    private fun setBottomNavigation() {
        homeButton.setOnClickListener { navigateTo(MainActivity::class.java, "home") }
        analyticsButton.setOnClickListener { navigateTo(HistoryActivity::class.java, "analytics") }
        transactionsButton.setOnClickListener { navigateTo(TransactionsActivity::class.java, "transactions") }
        plansButton.setOnClickListener { navigateTo(PlansActivity::class.java, "plans") }
        profileButton.setOnClickListener { navigateTo(ProfileActivity::class.java, "profile") }

        updateActiveButton("home")
    }

    private fun navigateTo(activityClass: Class<*>, activeTag: String) {
        if (this::class.java != activityClass) {
            val intent = Intent(this, activityClass)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        } else {
            Toast.makeText(this, "You are already on the ${activeTag.capitalize(Locale.ROOT)} page!", Toast.LENGTH_SHORT).show()
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