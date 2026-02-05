package com.example.budgetmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.budgetmaster.data.Budget
import com.example.budgetmaster.data.FirestoreBudgetRepository
import com.example.budgetmaster.viewmodel.BudgetViewModel
import com.example.budgetmaster.viewmodel.BudgetViewModelFactory

class AddBudgetDetailActivity : AppCompatActivity() {

    private lateinit var budgetNameEditText: EditText
    private lateinit var budgetAmountEditText: EditText
    private lateinit var budgetDescriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var budgetViewModel: BudgetViewModel

    private var selectedCategory: String = "Uncategorized"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_budget_detail)

        budgetNameEditText = findViewById(R.id.budgetNameEditText)
        budgetAmountEditText = findViewById(R.id.budgetAmountEditText)
        budgetDescriptionEditText = findViewById(R.id.budgetDescriptionEditText)
        saveButton = findViewById(R.id.save_button)


        selectedCategory = intent.getStringExtra("BUDGET_CATEGORY") ?: "Uncategorized"

        val appBarTitle = findViewById<TextView>(R.id.toolbar_title)
        appBarTitle.text = "Add Budget: $selectedCategory"


        val application = this.application


        val firestoreBudgetRepository = FirestoreBudgetRepository()


        val viewModelFactory = BudgetViewModelFactory(firestoreBudgetRepository)
        budgetViewModel = ViewModelProvider(this, viewModelFactory)[BudgetViewModel::class.java]


        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        saveButton.setOnClickListener {
            saveBudget()
        }
    }

    private fun saveBudget() {
        val budgetName = budgetNameEditText.text.toString().trim()
        val budgetAmountString = budgetAmountEditText.text.toString().trim()
        val budgetDescription = budgetDescriptionEditText.text.toString().trim()

        if (budgetName.isEmpty()) {
            budgetNameEditText.error = "Budget name is required"
            return
        }

        if (budgetAmountString.isEmpty()) {
            budgetAmountEditText.error = "Budget amount is required"
            return
        }

        val budgetAmount = budgetAmountString.toDoubleOrNull()
        if (budgetAmount == null || budgetAmount <= 0) {
            budgetAmountEditText.error = "Please enter a valid positive amount"
            return
        }


        val newBudget = Budget(
            name = budgetName,
            amount = budgetAmount,
            description = budgetDescription.ifEmpty { null },
            category = selectedCategory
        )


        budgetViewModel.insertBudget(newBudget)
        Toast.makeText(this, "Budget '${newBudget.name}' saved under '$selectedCategory'", Toast.LENGTH_LONG).show()

        val intent = Intent(this, PlansActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}