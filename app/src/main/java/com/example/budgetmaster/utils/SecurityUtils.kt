package com.example.budgetmaster.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*

object SecurityUtils {

    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun hashPassword(password: String, salt: String): String {
        val passwordWithSalt = password + salt
        try {

            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(passwordWithSalt.toByteArray())
            return Base64.getEncoder().encodeToString(hashBytes)
        } catch (e: NoSuchAlgorithmException) {

            throw RuntimeException("Error hashing password", e)
        }
    }
}
