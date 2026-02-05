package com.example.budgetmaster.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetmaster.R
import com.example.budgetmaster.data.Budget
import java.util.Locale

class BudgetDiffCallback : DiffUtil.ItemCallback<Budget>() {
    override fun areItemsTheSame(oldItem: Budget, newItem: Budget): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Budget, newItem: Budget): Boolean {
        return oldItem == newItem
    }
}

class BudgetAdapter(
    private val context: Context,
    private var budgetSpendingMap: Map<String, Double>,
    private val onDeleteClickListener: (Budget) -> Unit
) : ListAdapter<Budget, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.budget_item, parent, false)
        return BudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = getItem(position)
        holder.bind(budget, budgetSpendingMap[budget.category] ?: 0.0)
    }

    fun updateSpendingMap(newMap: Map<String, Double>) {
        this.budgetSpendingMap = newMap
        notifyDataSetChanged()
    }


    inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val budgetCategoryIcon: ImageView = itemView.findViewById(R.id.budgetCategoryIcon)
        private val budgetNameTextView: TextView = itemView.findViewById(R.id.budgetNameTextView)
        private val budgetProgressPercentageTextView: TextView = itemView.findViewById(R.id.budgetProgressPercentageTextView)
        private val budgetProgressBar: ProgressBar = itemView.findViewById(R.id.budgetProgressBar)
        private val budgetSpentVsLimitTextView: TextView = itemView.findViewById(R.id.budgetSpentVsLimitTextView)
        private val budgetDescriptionTextView: TextView? = itemView.findViewById(R.id.budgetDescriptionTextView)
        private val budgetAmountTextView: TextView = itemView.findViewById(R.id.budgetAmountTextView)
        private val deleteBudgetButton: ImageButton = itemView.findViewById(R.id.deleteBudgetButton)

        init {
            deleteBudgetButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClickListener.invoke(getItem(position))
                }
            }
        }

        fun bind(budget: Budget, spentAmount: Double) {
            budgetNameTextView.text = budget.name
            budgetAmountTextView.text = String.format(Locale.getDefault(), "R%.2f", budget.amount)

            val progress = if (budget.amount > 0) ((spentAmount / budget.amount) * 100).toInt() else 0
            budgetProgressBar.progress = progress
            budgetProgressPercentageTextView.text = "$progress%"
            budgetSpentVsLimitTextView.text = String.format(Locale.getDefault(), "R%.2f / R%.2f", spentAmount, budget.amount)

            budgetDescriptionTextView?.text = if (spentAmount >= budget.amount) {
                "Budget exceeded by R%.2f!".format(Locale.getDefault(), spentAmount - budget.amount)
            } else if (progress >= 80) {
                "Approaching budget limit. Remaining: R%.2f".format(Locale.getDefault(), budget.amount - spentAmount)
            } else {
                "Remaining: R%.2f".format(Locale.getDefault(), budget.amount - spentAmount)
            }

            val textColor: Int
            val progressBarTint: ColorStateList?

            if (spentAmount >= budget.amount) {
                textColor = ContextCompat.getColor(context, R.color.red_500)
                progressBarTint = ContextCompat.getColorStateList(context, R.color.red_500)
            } else if (progress >= 80) {
                textColor = ContextCompat.getColor(context, R.color.orange_500)
                progressBarTint = ContextCompat.getColorStateList(context, R.color.orange_500)
            } else {
                textColor = ContextCompat.getColor(context, R.color.gray_700)
                progressBarTint = ContextCompat.getColorStateList(context, R.color.teal_500)
            }

            budgetNameTextView.setTextColor(textColor)
            budgetProgressPercentageTextView.setTextColor(textColor)
            budgetSpentVsLimitTextView.setTextColor(textColor)
            budgetDescriptionTextView?.setTextColor(textColor)
            budgetProgressBar.progressTintList = progressBarTint

            val iconResId = getIconForCategory(budget.category)// Refference: [https://developer.android.com/develop/ui/views/components/recyclerview]

            budgetCategoryIcon.setImageResource(iconResId)
        }

        private fun getIconForCategory(category: String): Int {
            return when (category.lowercase(Locale.ROOT)) {
                "entertainment" -> R.drawable.ic_entertainment
                "food" -> R.drawable.ic_food
                "gifts" -> R.drawable.ic_gifts
                "groceries" -> R.drawable.ic_groceries
                "transport" -> R.drawable.ic_transport
                "medicine" -> R.drawable.ic_medicine
                "rent" -> R.drawable.ic_rent
                "hobbies" -> R.drawable.ic_savings
                "holiday" -> R.drawable.ic_holiday
                else -> R.drawable.ic_default_budget
            }
        }
    }
}