package com.heartape.whisper.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.heartape.whisper.data.model.ApiResponse
import com.heartape.whisper.data.model.VersionDto
import com.heartape.whisper.utils.GlobalEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : Interceptor {

    @Suppress("DEPRECATION")
    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. 获取当前 App 版本号
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        // 2. 将平台和版本信息强行注入到每一个请求的 Header 中
        val request = chain.request().newBuilder()
            .addHeader("X-Client-Platform", "ANDROID")
            .addHeader("X-Client-Version", pkgInfo.versionName!!)
            .addHeader("X-Client-Code", pkgInfo.versionCode.toString())
            .build()

        val response = chain.proceed(request)

        // 3. 全局拦截 426 状态码 (Upgrade Required)
        if (response.code == 426) {
            try {
                // 使用 peekBody 防止消耗掉原始的 response 流，导致后续报错
                val bodyString = response.peekBody(4096).string()

                // 解析服务端下发的新版本信息
                val type = object : TypeToken<ApiResponse<VersionDto>>() {}.type
                val apiResponse: ApiResponse<VersionDto> = gson.fromJson(bodyString, type)

                apiResponse.data.let { versionDto ->
                    // 抛到后台协程，通知全局 UI 锁死并强更
                    CoroutineScope(Dispatchers.IO).launch {
                        GlobalEventBus.emitForceUpdate(versionDto)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (response.code == 401) {
            CoroutineScope(Dispatchers.IO).launch {
                GlobalEventBus.emitAuthError()
            }
        }

        return response
    }
}