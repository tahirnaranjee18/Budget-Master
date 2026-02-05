package com.example.budgetmaster.data

import com.google.firebase.firestore.DocumentId

data class Budget(
    @DocumentId
    var id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val description: String? = null,
    val category: String = "",
    val imageUrl: String? = null
) {
    constructor() : this("", "", 0.0, null, "", null)
}