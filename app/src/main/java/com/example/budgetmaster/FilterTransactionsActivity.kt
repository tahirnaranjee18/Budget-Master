package com.example.budgetmaster

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class FilterTransactionsActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var fromDateTextView: TextView
    private lateinit var tillDateTextView: TextView
    private lateinit var btnSelectFromDate: ImageView
    private lateinit var btnSelectTillDate: ImageView
    private lateinit var applyButton: Button
    private lateinit var clearButton: Button
    private lateinit var backButton: ImageView

    private var fromDateMillis: Long? = null
    private var tillDateMillis: Long? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.filter_transaction)

        categorySpinner = findViewById(R.id.category_spinner)
        fromDateTextView = findViewById(R.id.tv_selected_from_date)
        tillDateTextView = findViewById(R.id.tv_selected_till_date)
        btnSelectFromDate = findViewById(R.id.btn_select_from_date)
        btnSelectTillDate = findViewById(R.id.btn_select_till_date)
        applyButton = findViewById(R.id.btn_apply_filter)
        clearButton = findViewById(R.id.btn_clear_filter)
        backButton = findViewById(R.id.back_button)

        val categories = listOf("All Categories", "Food", "Transport", "Medicine", "Groceries", "Rent", "Gifts", "Hobbies", "Entertainment")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        btnSelectFromDate.setOnClickListener {
            showDatePicker { dateInMillis ->
                fromDateMillis = dateInMillis
                fromDateTextView.text = dateFormat.format(Date(dateInMillis))
            }
        }

        fromDateTextView.setOnClickListener {
            btnSelectFromDate.performClick()
        }

        btnSelectTillDate.setOnClickListener {
            showDatePicker { dateInMillis ->
                tillDateMillis = dateInMillis
                tillDateTextView.text = dateFormat.format(Date(dateInMillis))
            }
        }

        tillDateTextView.setOnClickListener {
            btnSelectTillDate.performClick()
        }

        applyButton.setOnClickListener {
            val selectedCategory = categorySpinner.selectedItem as String
            val categoryFilter = if (selectedCategory == "All Categories") null else selectedCategory

            if (fromDateMillis != null && tillDateMillis != null && fromDateMillis!! > tillDateMillis!!) {
                Toast.makeText(this, "From date cannot be after Till date", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val effectiveTillDate = tillDateMillis ?: System.currentTimeMillis()
            val effectiveFromDate = fromDateMillis ?: 0L

            val intent = Intent(this, TransactionsActivity::class.java).apply {
                putExtra("filter_category", categoryFilter)
                putExtra("filter_start_date", effectiveFromDate)
                putExtra("filter_end_date", effectiveTillDate)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        clearButton.setOnClickListener {
            categorySpinner.setSelection(0)
            fromDateMillis = null
            tillDateMillis = null
            fromDateTextView.text = "Select Date"
            tillDateTextView.text = "Select Date"
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}