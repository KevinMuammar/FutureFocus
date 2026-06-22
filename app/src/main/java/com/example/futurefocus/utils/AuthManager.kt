package com.example.futurefocus.utils

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val userName: String? get() = currentUser?.displayName
    val userEmail: String? get() = currentUser?.email
    val isSignedIn: Boolean get() = auth.currentUser != null

    private fun createUserDocument(user: FirebaseUser) {
        firestore.collection("users")
            .document(user.uid)
            .set(mapOf(
                "email" to (user.email ?: ""),
                "createdAt" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                Log.d("FutureFocusAuth", "User document created: ${user.uid}")
            }
            .addOnFailureListener { error ->
                Log.e("FutureFocusAuth", "Failed to create user document: ${error.message}", error)
            }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { createUserDocument(it) }
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Registrasi gagal")
                }
            }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.user?.let { createUserDocument(it) }
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Login gagal")
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun reauthenticate(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run { onResult(false, "Tidak ada user"); return }
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Autentikasi ulang gagal")
                }
            }
    }

    fun updateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run { onResult(false, "Tidak ada user"); return }
        user.updateEmail(newEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Gagal mengubah email")
                }
            }
    }

    fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run { onResult(false, "Tidak ada user"); return }
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message ?: "Gagal mengubah kata sandi")
                }
            }
    }
}