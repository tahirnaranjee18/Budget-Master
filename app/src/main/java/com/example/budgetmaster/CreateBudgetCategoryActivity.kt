package com.example.budgetmaster

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateBudgetCategoryActivity : AppCompatActivity() {


    private lateinit var entertainmentCategory: LinearLayout
    private lateinit var foodCategory: LinearLayout
    private lateinit var giftsCategory: LinearLayout
    private lateinit var groceriesCategory: LinearLayout
    private lateinit var savingsCategory: LinearLayout
    private lateinit var medicineCategory: LinearLayout
    private lateinit var transportCategory: LinearLayout
    private lateinit var rentCategory: LinearLayout


    private lateinit var addCustomButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_budget_category)

        entertainmentCategory = findViewById(R.id.entertainment_category)
        foodCategory = findViewById(R.id.food_category)
        giftsCategory = findViewById(R.id.gifts_category)
        groceriesCategory = findViewById(R.id.groceries_category)
        savingsCategory = findViewById(R.id.savings_category)
        medicineCategory = findViewById(R.id.medicine_category)
        transportCategory = findViewById(R.id.transport_category)
        rentCategory = findViewById(R.id.rent_category)


        addCustomButton = findViewById(R.id.add_custom_button)


        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        entertainmentCategory.setOnClickListener { onCategorySelected("Entertainment") }
        foodCategory.setOnClickListener { onCategorySelected("Food") }
        giftsCategory.setOnClickListener { onCategorySelected("Gifts") }
        groceriesCategory.setOnClickListener { onCategorySelected("Groceries") }
        savingsCategory.setOnClickListener { onCategorySelected("Hobbies") }
        medicineCategory.setOnClickListener { onCategorySelected("Medicine") }
        transportCategory.setOnClickListener { onCategorySelected("Transport") }
        rentCategory.setOnClickListener { onCategorySelected("Rent") }

        addCustomButton.setOnClickListener {
            showAddCustomCategoryDialog()
        }
    }

    private fun onCategorySelected(category: String) {
        val intent = Intent(this, AddBudgetDetailActivity::class.java)
        intent.putExtra("BUDGET_CATEGORY", category)
        startActivity(intent)
        finish()
    }

    private fun showAddCustomCategoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Custom Category")

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_custom_category, null)
        val customCategoryEditText = view.findViewById<EditText>(R.id.custom_category_edit_text)
        builder.setView(view)

        builder.setPositiveButton("Add") { dialog, _ ->
            val customCategoryName = customCategoryEditText.text.toString().trim()
            if (customCategoryName.isNotEmpty()) {
                onCategorySelected(customCategoryName)
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
}