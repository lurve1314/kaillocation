package com.kail.location.network

import android.util.Log
import com.kail.location.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RuoYiClient {

    private const val TAG = "RuoYiClient"
    private const val JSON_TYPE = "application/json"

    var baseUrl: String = BuildConfig.APP_API_URL

    private val trustAllCertificates = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .sslSocketFactory(
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustAllCertificates), java.security.SecureRandom())
            }.socketFactory,
            trustAllCertificates
        )
        .hostnameVerifier { _, _ -> true }
        .build()

    data class AuthResult(
        val token: String,
        val email: String,
        val id: String,
        val verified: Boolean
    )

    private fun Request.Builder.withTenant(): Request.Builder {
        return this.header("tenant-id", "1")
    }

    private fun Request.Builder.withAuth(token: String): Request.Builder {
        return this.header("Authorization", "Bearer $token")
    }

    fun checkSimulation(token: String): Result<Int> {
        return runCatching {
            val url = "$baseUrl/member/simulation/check"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            if (code != 0) {
                throw Exception(root.optString("msg", "获取剩余次数失败"))
            }
            root.getJSONObject("data").optInt("remainingToday", 0)
        }
    }

    fun useSimulation(token: String): Result<Unit> {
        return runCatching {
            val url = "$baseUrl/member/simulation/use"
            val request = Request.Builder()
                .url(url)
                .post("{}".toRequestBody(JSON_TYPE.toMediaType()))
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            Log.d(TAG, "Use simulation response: $body")

            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            if (code != 0) {
                throw Exception(root.optString("msg", "模拟次数已用完"))
            }
        }
    }

    fun sendMailCode(mail: String, scene: Int): Result<Unit> {
        return runCatching {
            val url = "$baseUrl/member/auth/send-mail-code"
            val json = JSONObject().apply {
                put("mail", mail)
                put("scene", scene)
            }

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_TYPE.toMediaType()))
                .header("Content-Type", JSON_TYPE)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            Log.d(TAG, "Send mail code response: $body")

            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            if (code != 0) {
                throw Exception(root.optString("msg", "发送验证码失败"))
            }
        }
    }

    fun loginByMail(mail: String, code: String): Result<AuthResult> {
        return runCatching {
            val url = "$baseUrl/member/auth/mail-login"
            val json = JSONObject().apply {
                put("mail", mail)
                put("code", code)
            }

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_TYPE.toMediaType()))
                .header("Content-Type", JSON_TYPE)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            Log.d(TAG, "Mail login response: $body")

            val root = JSONObject(body)
            val respCode = root.optInt("code", -1)
            if (respCode != 0) {
                throw Exception(root.optString("msg", "登录失败"))
            }

            val data = root.getJSONObject("data")
            AuthResult(
                token = data.getString("accessToken"),
                email = mail,
                id = data.optString("userId", ""),
                verified = true
            )
        }
    }

    data class SubscriptionStatus(
        val active: Boolean,
        val planName: String,
        val expiresAt: String,
        val daysRemaining: Int
    )

    data class SubscriptionPlan(
        val id: Long,
        val name: String,
        val description: String,
        val price: Int,
        val currency: String,
        val billingInterval: String,
        val billingIntervalCount: Int,
        val trialDays: Int
    )

    suspend fun getPlans(token: String): Result<List<SubscriptionPlan>> {
        return runCatching {
            val url = "$baseUrl/member/subscription-plan/list"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            val root = JSONObject(body)
            val respCode = root.optInt("code", -1)
            if (respCode != 0) {
                throw Exception(root.optString("msg", "获取套餐列表失败"))
            }
            val arr = root.getJSONArray("data")
            val plans = mutableListOf<SubscriptionPlan>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                plans.add(SubscriptionPlan(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    description = item.optString("description", ""),
                    price = item.getInt("price"),
                    currency = item.optString("currency", "CNY"),
                    billingInterval = item.optString("billingInterval", "month"),
                    billingIntervalCount = item.optInt("billingIntervalCount", 1),
                    trialDays = item.optInt("trialDays", 0)
                ))
            }
            plans
        }
    }

    suspend fun getSubscriptionStatus(token: String): Result<SubscriptionStatus> {
        return runCatching {
            val url = "$baseUrl/member/subscription/status"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            Log.d(TAG, "Subscription status response: $body")

            val root = JSONObject(body)
            val respCode = root.optInt("code", -1)
            if (respCode != 0) {
                throw Exception(root.optString("msg", "获取订阅状态失败"))
            }

            val data = root.getJSONObject("data")
            SubscriptionStatus(
                active = data.optBoolean("active", false),
                planName = data.optString("planName", ""),
                expiresAt = data.optString("expiresAt", ""),
                daysRemaining = data.optInt("daysRemaining", 0)
            )
        }
    }

}
