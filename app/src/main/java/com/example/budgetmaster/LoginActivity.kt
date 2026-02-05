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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.budgetmaster.viewmodel.LoginViewModel
import com.example.budgetmaster.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    private lateinit var editTextPassword: EditText
    private lateinit var passwordToggleIcon: ImageView


    private var isPasswordVisible: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())[LoginViewModel::class.java]

        val emailInput = findViewById<EditText>(R.id.editTextUsername)
        editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val signUpButton = findViewById<Button>(R.id.buttonSignUp)
        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgotPassword)


        passwordToggleIcon = findViewById(R.id.passwordToggleIcon)


        loginViewModel.loginResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {

                val errorMessage = loginViewModel.message.value ?: "An unknown error occurred during login."
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        passwordToggleIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(editTextPassword, passwordToggleIcon, isPasswordVisible)
        }


        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            lifecycleScope.launch {
                loginViewModel.login(email, password)
            }
        }

        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        forgotPasswordTextView.setOnClickListener {

            Toast.makeText(this, "Forgot Password functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
