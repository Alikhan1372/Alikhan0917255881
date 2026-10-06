package com.example.data

import android.content.Context
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val context: Context,
    private val db: FirebaseFirestore? = null
) {
    companion object {
        private const val ALIAS_DOMAIN = "accounts.example.invalid"
        private const val LEGACY_SESSION_PREFERENCES = "app_auth_session"
    }

    private val prefs = context.getSharedPreferences(LEGACY_SESSION_PREFERENCES, Context.MODE_PRIVATE)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    init {
        prefs.edit().clear().apply()
        if (auth.currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                _currentUser.value = resolveCurrentFirebaseSession()
            }
        }
    }

    private suspend fun resolveCurrentFirebaseSession(): UserProfile? {
        val firebaseUser = auth.currentUser ?: return null
        val snapshot = db?.collection("users")?.document(firebaseUser.uid)?.get()?.await() ?: return null
        if (!snapshot.exists()) {
            auth.signOut()
            return null
        }

        val validation = TrustedUserProfileValidator.validate(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            aliasDomain = ALIAS_DOMAIN,
            data = snapshot.data
        )

        return when (validation) {
            is TrustedProfileValidation.Valid -> validation.profile.toAppProfile()
            is TrustedProfileValidation.Rejected -> {
                auth.signOut()
                null
            }
        }
    }

    suspend fun login(username: String, password: String): Result<UserProfile> {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isEmpty() || password.isBlank()) {
            return Result.failure(Exception("لطفاً نام کاربری و رمز عبور را وارد فرمایید."))
        }

        val email = UsernameAliasMapper.toFirebaseEmail(normalizedUsername, ALIAS_DOMAIN)
            ?: return Result.failure(Exception("نام کاربری وارد‌شده معتبر نیست."))

        return try {
            val credential = auth.signInWithEmailAndPassword(email, password.trim()).await()
            val uid = credential.user?.uid ?: throw IllegalStateException("Firebase session is missing uid")

            val trustedProfile = loadTrustedProfile(uid, email)
                ?: return Result.failure(Exception("حساب کاربری شما فعال یا معتبر نیست."))

            _currentUser.value = trustedProfile
            Result.success(trustedProfile)
        } catch (e: Exception) {
            auth.signOut()
            _currentUser.value = null
            Result.failure(Exception(mapAuthFailure(e)))
        }
    }

    private suspend fun loadTrustedProfile(uid: String, email: String?): UserProfile? {
        val snapshot = db?.collection("users")?.document(uid)?.get()?.await() ?: return null
        if (!snapshot.exists()) return null

        val validation = TrustedUserProfileValidator.validate(
            uid = uid,
            email = email,
            aliasDomain = ALIAS_DOMAIN,
            data = snapshot.data
        )

        return when (validation) {
            is TrustedProfileValidation.Valid -> validation.profile.toAppProfile()
            is TrustedProfileValidation.Rejected -> {
                auth.signOut()
                null
            }
        }
    }

    suspend fun updateDisplayName(newDisplayName: String): Result<UserProfile> {
        val trimmed = newDisplayName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("نام نمایشی نمی‌تواند خالی باشد."))
        }

        val current = _currentUser.value ?: return Result.failure(Exception("کاربر وارد نشده است."))
        val uid = auth.currentUser?.uid ?: current.userId

        val updated = current.copy(displayName = trimmed)
        _currentUser.value = updated

        db?.let { firestore ->
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "displayName" to trimmed
                    ),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) {
                // App is intentionally strict; the source of truth remains the user profile on Firestore.
            }
        }

        return Result.success(updated)
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (_: Exception) { }
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null && _currentUser.value != null

    private fun mapAuthFailure(error: Exception): String {
        return when (error) {
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_INVALID_CREDENTIAL" -> "نام کاربری یا رمز عبور اشتباه است."
                "ERROR_USER_DISABLED" -> "حساب کاربری شما غیرفعال شده است."
                else -> "ورود ناموفق بود."
            }
            else -> error.localizedMessage ?: "ورود ناموفق بود."
        }
    }
}
