package com.towmasterscorp.app.data.api

import com.towmasterscorp.app.data.preferences.AuthPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://app.towmasterscorp.com/api/"

    private var authPreferences: AuthPreferences? = null
    private var retrofit: Retrofit? = null
    private var api: TowMastersApi? = null

    fun initialize(authPreferences: AuthPreferences) {
        this.authPreferences = authPreferences
        this.retrofit = null
        this.api = null
    }

    fun getApi(): TowMastersApi {
        if (api == null) {
            api = getRetrofit().create(TowMastersApi::class.java)
        }
        return api!!
    }

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val token = runBlocking {
                    authPreferences?.tokenFlow?.first()
                }

                val request = chain.request().newBuilder().apply {
                    addHeader("Content-Type", "application/json")
                    addHeader("Accept", "application/json")
                    if (!token.isNullOrEmpty()) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }.build()

                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun resetClient() {
        retrofit = null
        api = null
    }
}
