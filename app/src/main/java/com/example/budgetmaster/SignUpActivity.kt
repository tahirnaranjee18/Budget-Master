package com.example.budgetmaster

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.MutableLiveData
import androidx.activity.viewModels
import com.example.budgetmaster.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpActivity : AppCompatActivity() {

    private val signUpViewModel: SignUpViewModel by viewModels {
        SignUpViewModelFactory()
    }

    private lateinit var passwordIcon: ImageView
    private lateinit var confirmPasswordIcon: ImageView

    private var isPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val signInText = findViewById<TextView>(R.id.signInText)

        passwordIcon = findViewById(R.id.passwordIcon)
        confirmPasswordIcon = findViewById(R.id.confirmPasswordIcon)

        signUpViewModel.signUpResult.observe(this) { isSuccess: Boolean? ->
            if (isSuccess == true) {
                Toast.makeText(this@SignUpActivity, "Sign up successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else if (isSuccess == false) {
                val errorMessage = signUpViewModel.message.value ?: "An unknown error occurred during sign up."
                Toast.makeText(
                    this@SignUpActivity,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        passwordIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(passwordInput, passwordIcon, isPasswordVisible)
        }

        confirmPasswordIcon.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(confirmPasswordInput, confirmPasswordIcon, isConfirmPasswordVisible)
        }

        signInText.setOnClickListener {
            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        signUpButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                signUpViewModel.signUp(username, email, password)
            }
        }
    }

    private fun isValidEmail(email: CharSequence): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.ic_visibility)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.ic_visibility_off)
        }
        editText.setSelection(editText.text.length)
    }
}

class SignUpViewModel : ViewModel() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestoreDb = FirebaseFirestore.getInstance()

    val signUpResult = MutableLiveData<Boolean?>(null)
    val message = MutableLiveData<String?>(null)

    suspend fun signUp(username: String, email: String, password: String) {
        signUpResult.postValue(null)
        message.postValue(null)

        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            firebaseUser?.let { user ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
                user.updateProfile(profileUpdates).await()

                val newUserProfile = User(
                    id = user.uid,
                    username = username,
                    email = email
                )
                firestoreDb.collection("users").document(user.uid).set(newUserProfile).await()

                signUpResult.postValue(true)
                message.postValue("User registered successfully!")
            } ?: run {
                signUpResult.postValue(false)
                message.postValue("User registration failed: No Firebase user returned.")
            }
        } catch (e: FirebaseAuthWeakPasswordException) {
            signUpResult.postValue(false)
            message.postValue("Weak Password: ${e.reason}")
        } catch (e: FirebaseAuthUserCollisionException) {
            signUpResult.postValue(false)
            message.postValue("Email already in use: Please use a different email.")
        } catch (e: Exception) {
            signUpResult.postValue(false)
            message.postValue("Sign up failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}

class SignUpViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignUpViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}