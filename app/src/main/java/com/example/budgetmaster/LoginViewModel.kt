package com.example.budgetmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    val loginResult = MutableLiveData<Boolean>()
    val message = MutableLiveData<String>()

    suspend fun login(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            loginResult.postValue(true)
            message.postValue("Success")
        } catch (e: Exception) {
            loginResult.postValue(false)
            message.postValue(e.localizedMessage ?: "Login failed. Please check your credentials.")
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
            message.postValue("Password reset email sent to $email. Check your inbox.")
        } catch (e: Exception) {
            message.postValue(e.localizedMessage ?: "Failed to send password reset email.")
        }
    }

    override fun onCleared() {
        super.onCleared()

    }
}