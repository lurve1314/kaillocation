package com.kail.location.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

object AuthManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_USER_ID = "auth_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_SUBSCRIBED = "is_subscribed"
    private const val KEY_SUB_EXPIRES = "sub_expires_at"

    private lateinit var prefs: SharedPreferences

    private val _isLoggedIn = mutableStateOf(false)
    private val _email = mutableStateOf("")
    private val _isSubscribed = mutableStateOf(false)

    val isLoggedIn: Boolean get() = _isLoggedIn.value
    val email: String get() = _email.value
    val isSubscribed: Boolean get() = _isSubscribed.value
    val isLoggedInState get() = _isLoggedIn
    val emailState get() = _email
    val isSubscribedState get() = _isSubscribed

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        private set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isLoggedIn.value = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        _email.value = prefs.getString(KEY_EMAIL, "") ?: ""
        _isSubscribed.value = prefs.getBoolean(KEY_SUBSCRIBED, false)
    }

    fun saveAuth(token: String, email: String, userId: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        _isLoggedIn.value = true
        _email.value = email
    }

    fun updateSubscription(subscribed: Boolean, expiresAt: String = "") {
        prefs.edit()
            .putBoolean(KEY_SUBSCRIBED, subscribed)
            .putString(KEY_SUB_EXPIRES, expiresAt)
            .apply()
        _isSubscribed.value = subscribed
    }

    fun clearAuth() {
        prefs.edit()
            .putString(KEY_TOKEN, null)
            .putString(KEY_EMAIL, null)
            .putString(KEY_USER_ID, null)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putBoolean(KEY_SUBSCRIBED, false)
            .putString(KEY_SUB_EXPIRES, null)
            .apply()
        _isLoggedIn.value = false
        _email.value = ""
        _isSubscribed.value = false
    }
}
