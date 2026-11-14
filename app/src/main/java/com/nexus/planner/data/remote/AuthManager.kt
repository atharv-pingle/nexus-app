package com.nexus.planner.data.remote

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("nexus_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(hasValidToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getServerUrl(): String {
        return prefs.getString("server_url", "http://192.168.1.100:3066") ?: "http://192.168.1.100:3066"
    }

    fun setServerUrl(url: String) {
        val cleanUrl = url.trim().trimEnd('/')
        prefs.edit().putString("server_url", cleanUrl).apply()
    }

    fun getToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getUsername(): String {
        return prefs.getString("username", "User") ?: "User"
    }

    fun saveSession(token: String, username: String, serverUrl: String) {
        setServerUrl(serverUrl)
        prefs.edit()
            .putString("access_token", token)
            .putString("username", username)
            .apply()
        _isLoggedIn.value = true
    }

    fun clearSession() {
        prefs.edit()
            .remove("access_token")
            .remove("username")
            .apply()
        _isLoggedIn.value = false
    }

    fun hasValidToken(): Boolean {
        return !prefs.getString("access_token", null).isNullOrBlank()
    }
}
