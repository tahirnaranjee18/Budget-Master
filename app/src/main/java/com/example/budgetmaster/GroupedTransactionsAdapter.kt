package com.example.budgetmaster.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgetmaster.R
import com.example.budgetmaster.data.Transaction
import com.example.budgetmaster.data.model.GroupedTransactionItem
import java.text.SimpleDateFormat
import java.util.Locale

class GroupedTransactionsAdapter(
    private val onDeleteClick: (Transaction) -> Unit,
    private val onReceiptImageClick: (String) -> Unit
) : ListAdapter<GroupedTransactionItem, RecyclerView.ViewHolder>(GroupedTransactionDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is GroupedTransactionItem.Header -> VIEW_TYPE_HEADER
            is GroupedTransactionItem.TransactionItem -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_TRANSACTION -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
                TransactionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GroupedTransactionItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is GroupedTransactionItem.TransactionItem -> (holder as TransactionViewHolder).bind(item.transaction, onDeleteClick, onReceiptImageClick)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerTextView: TextView = itemView.findViewById(R.id.headerTextView)
        fun bind(title: String) {
            headerTextView.text = title
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryIcon: ImageView = itemView.findViewById(R.id.category_icon)
        val transactionTitle: TextView = itemView.findViewById(R.id.transaction_title)
        val transactionCategoryDate: TextView = itemView.findViewById(R.id.transaction_category_date)
        val transactionMessage: TextView = itemView.findViewById(R.id.transaction_message)
        val transactionReceiptImage: ImageView = itemView.findViewById(R.id.transaction_receipt_image)
        val transactionAmount: TextView = itemView.findViewById(R.id.transaction_amount)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_transaction_button)

        fun bind(transaction: Transaction, onDeleteClick: (Transaction) -> Unit, onReceiptImageClick: (String) -> Unit) {
            transactionTitle.text = transaction.title
            val dateFormat = SimpleDateFormat("MMMM dd,yyyy", Locale.getDefault())
            transactionCategoryDate.text = "${transaction.category} - ${dateFormat.format(transaction.date)}"
            transactionAmount.text = String.format(Locale.getDefault(), "R%.2f", transaction.amount)

            if (transaction.message.isNullOrEmpty()) {
                transactionMessage.visibility = View.GONE
            } else {
                transactionMessage.visibility = View.VISIBLE
                transactionMessage.text = "Notes: ${transaction.message}"
            }

            if (transaction.receiptImageUri != null && transaction.receiptImageUri.isNotEmpty()) {
                transactionReceiptImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(transaction.receiptImageUri)
                    .into(transactionReceiptImage)

                transactionReceiptImage.setOnClickListener {
                    onReceiptImageClick(transaction.receiptImageUri)
                }
            } else {
                transactionReceiptImage.setImageDrawable(null)
                transactionReceiptImage.visibility = View.GONE
                transactionReceiptImage.setOnClickListener(null)
            }

            val iconResId = when (transaction.category) {
                "Food" -> R.drawable.ic_food
                "Transport" -> R.drawable.ic_transport
                "Medicine" -> R.drawable.ic_medicine
                "Groceries" -> R.drawable.ic_groceries
                "Rent" -> R.drawable.ic_rent
                "Gifts" -> R.drawable.ic_gifts
                "Hobbies" -> R.drawable.ic_savings
                "Entertainment" -> R.drawable.ic_entertainment
                else -> R.drawable.ic_add
            }
            categoryIcon.setImageResource(iconResId)

            deleteButton.setOnClickListener {
                onDeleteClick(transaction)
            }
        }
    }
}

class GroupedTransactionDiffCallback : DiffUtil.ItemCallback<GroupedTransactionItem>() {
    override fun areItemsTheSame(oldItem: GroupedTransactionItem, newItem: GroupedTransactionItem): Boolean {
        return when {
            oldItem is GroupedTransactionItem.Header && newItem is GroupedTransactionItem.Header ->
                oldItem.title == newItem.title
            oldItem is GroupedTransactionItem.TransactionItem && newItem is GroupedTransactionItem.TransactionItem ->
                oldItem.transaction.id == newItem.transaction.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: GroupedTransactionItem, newItem: GroupedTransactionItem): Boolean {
        return oldItem == newItem
    }
}