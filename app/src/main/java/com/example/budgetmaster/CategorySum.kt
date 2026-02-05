
package com.example.budgetmaster.data

import androidx.room.ColumnInfo


data class CategorySum(
    @ColumnInfo(name = "category") val categoryName: String,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double
)
