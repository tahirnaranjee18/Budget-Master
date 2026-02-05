package com.example.budgetmaster

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREF_NAME = "budget_master_prefs"


    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

}