package com.example.budgetmaster.data

import com.google.firebase.firestore.DocumentId

data class Achievement(
    @DocumentId
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val unlocked: Boolean = false,
    val unlockedDate: Long? = null
) {

    constructor() : this("", "", "", false, null)
}