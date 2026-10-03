package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val context: Context,
    private val db: FirebaseFirestore? = null
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_auth_session", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        // Must verify that both a valid local record exists AND Firebase Auth is active if initialized
        val savedUsername = prefs.getString("saved_username", null)
        if (!savedUsername.isNullOrEmpty()) {
            val account = UserAccounts.findAccountByUsername(savedUsername)
            if (account != null) {
                val customDisplayName = prefs.getString("display_name_${account.defaultProfile.userId}", null)
                val profile = if (!customDisplayName.isNullOrEmpty()) {
                    account.defaultProfile.copy(displayName = customDisplayName)
                } else {
                    account.defaultProfile
                }
                _currentUser.value = profile
            }
        }
    }

    suspend fun login(username: String, password: String): Result<UserProfile> {
        val account = UserAccounts.findAccountByUsername(username)
            ?: return Result.failure(Exception("نام کاربری یا رمز عبور اشتباه است."))

        if (account.initialPassword != password.trim()) {
            return Result.failure(Exception("نام کاربری یا رمز عبور اشتباه است."))
        }

        if (!account.defaultProfile.active) {
            return Result.failure(Exception("حساب کاربری شما غیرفعال شده است."))
        }

        val customDisplayName = prefs.getString("display_name_${account.defaultProfile.userId}", null)
        val profile = if (!customDisplayName.isNullOrEmpty()) {
            account.defaultProfile.copy(displayName = customDisplayName)
        } else {
            account.defaultProfile
        }

        // Store active session
        prefs.edit().putString("saved_username", account.username).apply()
        _currentUser.value = profile

        // Sync and ensure user profile document in Firestore matches request.auth.uid and profile.userId
        db?.let { firestore ->
            val userPayload = mapOf(
                "userId" to profile.userId,
                "username" to profile.username,
                "displayName" to profile.displayName,
                "role" to profile.role,
                "assignedEquipmentId" to profile.assignedEquipmentId,
                "active" to profile.active
            )

            try {
                // Save under profile.userId
                firestore.collection("users").document(profile.userId).set(
                    userPayload,
                    SetOptions.merge()
                ).await()

                // Also save under auth.currentUser.uid if available
                auth.currentUser?.uid?.let { authUid ->
                    if (authUid != profile.userId) {
                        firestore.collection("users").document(authUid).set(
                            userPayload,
                            SetOptions.merge()
                        ).await()
                    }
                }
            } catch (_: Exception) {
                // Network buffering
            }
        }

        return Result.success(profile)
    }

    suspend fun updateDisplayName(newDisplayName: String): Result<UserProfile> {
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("نام نمایشی نمی‌تواند خالی باشد."))
        }

        val current = _currentUser.value
            ?: return Result.failure(Exception("کاربر وارد نشده است."))

        // Business Rule: ONLY displayName can be edited by the user!
        // username, password, role, assignedEquipmentId, userId are immutable.
        val updated = current.copy(displayName = trimmed)
        prefs.edit().putString("display_name_${current.userId}", trimmed).apply()
        _currentUser.value = updated

        // Sync with Firestore
        db?.let { firestore ->
            try {
                firestore.collection("users").document(current.userId).update(
                    "displayName", trimmed
                ).await()

                auth.currentUser?.uid?.let { authUid ->
                    if (authUid != current.userId) {
                        firestore.collection("users").document(authUid).update(
                            "displayName", trimmed
                        ).await()
                    }
                }
            } catch (_: Exception) {
                // Offline mode will buffer update
            }
        }

        return Result.success(updated)
    }

    fun logout() {
        prefs.edit().remove("saved_username").apply()
        try {
            auth.signOut()
        } catch (_: Exception) { }
        _currentUser.value = null
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null
}
