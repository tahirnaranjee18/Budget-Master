package com.example.budgetmaster

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgetmaster.adapter.TransactionsAdapter
import com.example.budgetmaster.data.Transaction
import com.example.budgetmaster.data.FirestoreTransactionRepository
import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    private lateinit var homeButton: ImageButton
    private lateinit var analyticsButton: ImageButton
    private lateinit var transactionsButton: ImageButton
    private lateinit var plansButton: ImageButton
    private lateinit var profileButton: ImageButton
    private lateinit var allNavButtons: List<ImageButton>
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var totalExpenseTextView: TextView

    private lateinit var transactionsAdapter: TransactionsAdapter
    private lateinit var transactionViewModel: TransactionViewModel

    private var currentFilterCategory: String? = null
    private var currentFilterStartDate: Long? = null
    private var currentFilterEndDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val transactionViewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, transactionViewModelFactory)[TransactionViewModel::class.java]

        homeButton = findViewById(R.id.homeButton)
        analyticsButton = findViewById(R.id.analyticsButton)
        transactionsButton = findViewById(R.id.transactionsButton)
        plansButton = findViewById(R.id.plansButton)
        profileButton = findViewById(R.id.profileButton)
        allNavButtons = listOf(homeButton, analyticsButton, transactionsButton, plansButton, profileButton)

        totalExpenseTextView = findViewById(R.id.total_expense)

        transactionsRecyclerView = findViewById(R.id.TransactionsRecyclerView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        transactionsAdapter = TransactionsAdapter(
            emptyList(),
            { transactionToDelete -> deleteTransaction(transactionToDelete) },
            { imageUrl -> showEnlargedImage(imageUrl) }
        )

        transactionsRecyclerView.adapter = transactionsAdapter

        transactionViewModel.totalExpenses.observe(this, Observer { totalAmount ->
            totalExpenseTextView.text = String.format(Locale.getDefault(), "R%.2f", totalAmount ?: 0.0)
        })

        setBottomNavigation()

        val filterSortButton = findViewById<ImageButton>(R.id.filterSortButton)
        filterSortButton.setOnClickListener {
            val intent = Intent(this, FilterTransactionsActivity::class.java)
            startActivity(intent)
        }

        val addTransactionButton = findViewById<Button>(R.id.addTransactionButton)
        addTransactionButton.setOnClickListener {
            val intent = Intent(this, AddTransactionCategoryActivity::class.java)
            startActivity(intent)
        }

        applyFilters()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyFilters()
    }

    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                transactionViewModel.deleteTransaction(transaction.id)
                Toast.makeText(this@TransactionsActivity, "Transaction deleted!", Toast.LENGTH_SHORT).show()
                applyFilters()
            } catch (e: Exception) {
                Toast.makeText(this@TransactionsActivity, "Error deleting transaction: ${e.message}", Toast.LENGTH_LONG).show()
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

        updateActiveButton("transactions")
    }

    private fun navigateTo(activityClass: Class<*>, activeTag: String) {
        Log.d("TransactionsActivity", "Navigating from ${this::class.java.simpleName} to ${activityClass.simpleName}")

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

    private fun applyFilters() {
        val categoryFilter = intent.getStringExtra("filter_category")
        val startDateFilter = intent.getLongExtra("filter_start_date", -1L)
        val endDateFilter = intent.getLongExtra("filter_end_date", -1L)

        currentFilterCategory = categoryFilter
        currentFilterStartDate = if (startDateFilter != -1L) startDateFilter else null
        currentFilterEndDate = if (endDateFilter != -1L) endDateFilter else null

        transactionViewModel.allTransactions.removeObservers(this)

        if (currentFilterCategory == null && currentFilterStartDate == null && currentFilterEndDate == null) {
            transactionViewModel.allTransactions.observe(this, Observer { transactions ->
                transactionsAdapter.updateTransactions(transactions)
            })
        } else {
            val effectiveCategory = currentFilterCategory ?: ""
            val effectiveStartDate = currentFilterStartDate ?: 0L
            val effectiveEndDate = currentFilterEndDate ?: System.currentTimeMillis()

            val calendar = Calendar.getInstance()
            val adjustedEndDate = if (currentFilterEndDate != null) {
                calendar.timeInMillis = effectiveEndDate
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            } else {
                effectiveEndDate
            }

            val filteredTransactionsLiveData = transactionViewModel.getTransactionsFiltered(
                effectiveCategory,
                effectiveStartDate,
                adjustedEndDate
            )

            filteredTransactionsLiveData.observe(this, Observer { transactions ->
                transactionsAdapter.updateTransactions(transactions)
                if (transactions.isEmpty()) {
                    Toast.makeText(this, "No transactions found for the selected filter.", Toast.LENGTH_SHORT).show()
                }
            })
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
}