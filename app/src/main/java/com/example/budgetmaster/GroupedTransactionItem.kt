
package com.example.budgetmaster.data.model

import com.example.budgetmaster.data.Transaction

sealed class GroupedTransactionItem {
    data class Header(val title: String) : GroupedTransactionItem()
    data class TransactionItem(val transaction: Transaction) : GroupedTransactionItem()
}