package com.heartape.whisper.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.utils.ErrorUtils.runSafeSync

object AppStoreNavigator {

    /**
     * 智能导航至应用商店或下载页
     * @param url 后端下发的链接 (可选)
     */
    fun goToStore(context: Context, url: String?, fallbackUrl: String) {

        // 尝试 1：优先信任后端下发的具体链接（可以是特定应用商店协议，也可以是特定活动网页）
        if (!url.isNullOrBlank()) {
            if (tryStartIntent(context, url)) return
        }

        // 尝试 2：动态获取当前包名，唤起手机系统默认的应用商店
        val dynamicMarketUrl = "market://details?id=${context.packageName}"
        if (tryStartIntent(context, dynamicMarketUrl)) return

        // 尝试 3：终极兜底，用浏览器打开官方 Web 下载落地页
        tryStartIntent(context, fallbackUrl)
    }

    /**
     * 安全启动 Intent 的执行器
     * @return true: 启动成功; false: 启动失败 (如找不到对应的 App)
     */
    private fun tryStartIntent(context: Context, urlString: String): Boolean {
        val result = runSafeSync {
            val intent = Intent(Intent.ACTION_VIEW, urlString.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        return result is AppResult.Success
    }
}