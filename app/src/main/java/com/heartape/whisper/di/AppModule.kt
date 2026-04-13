package com.heartape.whisper.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.heartape.whisper.BuildConfig
import com.heartape.whisper.data.local.PrefsManager
import com.heartape.whisper.data.remote.ApiService
import com.heartape.whisper.data.remote.VersionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

data class AppConfig(
    val apiBaseUrl: String,
    val fallbackWebUrl: String
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig {
        return AppConfig(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            fallbackWebUrl = BuildConfig.FALLBACK_WEB_URL
        )
    }

    @Provides @Singleton
    fun provideSharedPrefs(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("whisper_prefs", Context.MODE_PRIVATE)
    }

    @Provides @Singleton
    fun provideOkHttpClient(
        prefsManager: PrefsManager,
        versionInterceptor: VersionInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(versionInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                prefsManager.token?.let { request.addHeader("Authorization", it) }
                chain.proceed(request.build())
            }
            .build()
    }

    @Provides @Singleton
    fun provideApiService(client: OkHttpClient, config: AppConfig): ApiService {
        return Retrofit.Builder()
            .baseUrl(config.apiBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides @Singleton
    fun provideGson() = Gson()
}