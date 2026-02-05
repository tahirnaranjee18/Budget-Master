package com.example.budgetmaster.data

import com.google.firebase.firestore.DocumentId

data class Transaction(
    @DocumentId
    var id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val date: Long = System.currentTimeMillis(),
    val message: String? = null,
    val receiptImageUri: String? = null
) {
    constructor() : this("", "", 0.0, "", "", 0L, null, null)
}