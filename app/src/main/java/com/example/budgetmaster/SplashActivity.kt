
package com.example.budgetmaster

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        Handler(Looper.getMainLooper()).postDelayed({

            val currentUser = FirebaseAuth.getInstance().currentUser
            val intent: Intent

            if (currentUser != null) {

                intent = Intent(this, MainActivity::class.java)
            } else {

                intent = Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, SPLASH_TIME_OUT)
    }
}