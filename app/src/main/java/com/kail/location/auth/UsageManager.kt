package com.kail.location.auth

import android.content.Context
import com.kail.location.network.RuoYiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsageManager {

    fun init(context: Context) {}

    /**
     * Check if user can start simulation (does NOT consume a count)
     */
    suspend fun canStartSimulation(context: Context): Boolean {
        if (!AuthManager.isLoggedIn) {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "请先登录后再使用模拟功能", android.widget.Toast.LENGTH_SHORT).show()
            }
            return false
        }

        if (AuthManager.isSubscribed) {
            return true
        }

        val token = AuthManager.token ?: return false
        val result = withContext(Dispatchers.IO) {
            RuoYiClient.checkSimulation(token)
        }

        return if (result.isSuccess) {
            val remaining = result.getOrThrow()
            if (remaining <= 0) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "今日免费模拟次数已用完，订阅后可无限使用", android.widget.Toast.LENGTH_SHORT).show()
                }
                false
            } else {
                true
            }
        } else {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "今日免费模拟次数已用完，订阅后可无限使用", android.widget.Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    /**
     * Consume one simulation count. Call this when user actually starts simulating.
     */
    suspend fun consumeSimulation(context: Context): Boolean {
        if (!AuthManager.isLoggedIn) return false
        if (AuthManager.isSubscribed) return true

        val token = AuthManager.token ?: return false
        val result = withContext(Dispatchers.IO) {
            RuoYiClient.useSimulation(token)
        }

        return if (result.isSuccess) {
            true
        } else {
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "模拟次数已用完", android.widget.Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}
