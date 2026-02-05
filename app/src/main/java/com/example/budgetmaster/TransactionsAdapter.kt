package com.example.budgetmaster.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgetmaster.R
import com.example.budgetmaster.data.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter(
    private var transactions: List<Transaction>,
    private val onDeleteClick: (Transaction) -> Unit,
    private val onReceiptImageClick: (String) -> Unit
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryIcon: ImageView = itemView.findViewById(R.id.category_icon)
        val transactionTitle: TextView = itemView.findViewById(R.id.transaction_title)
        val transactionCategoryDate: TextView = itemView.findViewById(R.id.transaction_category_date)
        val transactionNotes: TextView = itemView.findViewById(R.id.transaction_message)
        val transactionReceiptImage: ImageView = itemView.findViewById(R.id.transaction_receipt_image)
        val transactionAmount: TextView = itemView.findViewById(R.id.transaction_amount)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_transaction_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        holder.transactionTitle.text = transaction.title
        val dateFormat = SimpleDateFormat("MMMM dd,yyyy", Locale.getDefault())
        holder.transactionCategoryDate.text = "${transaction.category} - ${dateFormat.format(transaction.date)}"
        holder.transactionAmount.text = String.format(Locale.getDefault(), "R%.2f", transaction.amount)

        if (transaction.message.isNullOrEmpty()) {
            holder.transactionNotes.visibility = View.GONE
        } else {
            holder.transactionNotes.visibility = View.VISIBLE
            holder.transactionNotes.text = "Notes: ${transaction.message}"
        }

        if (transaction.receiptImageUri != null && transaction.receiptImageUri.isNotEmpty()) {
            holder.transactionReceiptImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(transaction.receiptImageUri)
                .into(holder.transactionReceiptImage)


            holder.transactionReceiptImage.setOnClickListener {
                onReceiptImageClick(transaction.receiptImageUri)
            }

        } else {
            holder.transactionReceiptImage.setImageDrawable(null)
            holder.transactionReceiptImage.visibility = View.GONE
            holder.transactionReceiptImage.setOnClickListener(null)
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
        holder.categoryIcon.setImageResource(iconResId)

        holder.deleteButton.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        this.transactions = newTransactions
        notifyDataSetChanged()
    }
}