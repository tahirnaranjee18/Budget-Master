package com.example.budgetmaster

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.example.budgetmaster.BuildConfig

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        connectToFirebaseEmulators()
    }

    private fun connectToFirebaseEmulators() {
        if (BuildConfig.DEBUG) {
            try {
                FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)

                FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)

                Log.d("FirebaseApp", "Connected to Firebase Emulators!")

            } catch (e: Exception) {
                Log.e("FirebaseApp", "Failed to connect to Firebase Emulators: ${e.message}", e)
            }
        } else {
            Log.d("FirebaseApp", "Connecting to production Firebase.")
        }
    }
}