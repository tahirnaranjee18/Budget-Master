package com.example.budgetmaster.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    var id: String = "",
    val username: String = "",
    val email: String = ""
) {
    constructor() : this("", "", "")
}
