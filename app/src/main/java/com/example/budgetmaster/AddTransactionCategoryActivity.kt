package com.example.budgetmaster

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import java.util.Locale

import com.example.budgetmaster.viewmodel.TransactionViewModel
import com.example.budgetmaster.viewmodel.TransactionViewModelFactory
import com.example.budgetmaster.data.FirestoreTransactionRepository

class AddTransactionCategoryActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var foodButton: ImageButton
    private lateinit var transportButton: ImageButton
    private lateinit var medicineButton: ImageButton
    private lateinit var groceriesButton: ImageButton
    private lateinit var rentButton: ImageButton
    private lateinit var giftsButton: ImageButton
    private lateinit var hobbiesButton: ImageButton
    private lateinit var entertainmentButton: ImageButton
    private lateinit var customButton: ImageButton

    private lateinit var totalExpenseTextView: TextView

    private lateinit var transactionViewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction_category)

        backButton = findViewById(R.id.back_button)
        foodButton = findViewById(R.id.food_button)
        transportButton = findViewById(R.id.transport_button)
        medicineButton = findViewById(R.id.medicine_button)
        groceriesButton = findViewById(R.id.groceries_button)
        rentButton = findViewById(R.id.rent_button)
        giftsButton = findViewById(R.id.gifts_button)
        hobbiesButton = findViewById(R.id.savings_button)
        entertainmentButton = findViewById(R.id.entertainment_button)
        customButton = findViewById(R.id.more_button)
        totalExpenseTextView = findViewById(R.id.total_expense)

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        foodButton.setOnClickListener { sendCategoryToDetailActivity("Food") }
        transportButton.setOnClickListener { sendCategoryToDetailActivity("Transport") }
        medicineButton.setOnClickListener { sendCategoryToDetailActivity("Medicine") }
        groceriesButton.setOnClickListener { sendCategoryToDetailActivity("Groceries") }
        rentButton.setOnClickListener { sendCategoryToDetailActivity("Rent") }
        giftsButton.setOnClickListener { sendCategoryToDetailActivity("Gifts") }
        hobbiesButton.setOnClickListener { sendCategoryToDetailActivity("Hobbies") }
        entertainmentButton.setOnClickListener { sendCategoryToDetailActivity("Entertainment") }

        customButton.setOnClickListener {
            showCustomCategoryDialog()
        }

        val firestoreTransactionRepository = FirestoreTransactionRepository()
        val viewModelFactory = TransactionViewModelFactory(firestoreTransactionRepository)
        transactionViewModel = ViewModelProvider(this, viewModelFactory)[TransactionViewModel::class.java]

        transactionViewModel.getTotalExpenseSum().observe(this) { totalAmount ->
            totalExpenseTextView.text = String.format(Locale.getDefault(), "R%.2f", totalAmount ?: 0.0)
        }
    }

    private fun sendCategoryToDetailActivity(category: String) {
        val intent = Intent(this, AddTransactionDetailActivity::class.java)
        intent.putExtra("SELECTED_CATEGORY", category) // Passing data between activities: [https://developer.android.com/guide/components/activities/activity-lifecycle#passing-data-between-activities]
        startActivity(intent)
        finish()
    }

    private fun showCustomCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Custom Category")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_custom_category, null)
        val customCategoryEditText = view.findViewById<EditText>(R.id.custom_category_edit_text)
        builder.setView(view)

        builder.setPositiveButton("Add") { dialog, _ ->
            val newCategory = customCategoryEditText.text.toString().trim()
            if (newCategory.isNotEmpty()) {
                saveCustomCategory(newCategory)
                sendCategoryToDetailActivity(newCategory)
            } else {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun saveCustomCategory(category: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val customCategories = sharedPreferences.getStringSet("custom_categories", mutableSetOf())?.toMutableSet()
        customCategories?.add(category)
        editor.putStringSet("custom_categories", customCategories)
        editor.apply()
        Toast.makeText(this, "$category added as custom category", Toast.LENGTH_SHORT).show()
    }
}