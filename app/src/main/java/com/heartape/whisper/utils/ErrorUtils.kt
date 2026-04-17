package com.heartape.whisper.utils

import android.database.sqlite.SQLiteException
import com.heartape.whisper.data.model.ApiDataException
import com.heartape.whisper.data.model.ApiException
import com.heartape.whisper.data.model.AppResult
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

object ErrorUtils {

    /**
     * 异步安全执行器 (适用于 Retrofit, Room, 文件 IO 等挂起函数)
     */
    suspend inline fun <T> runSafe(crossinline block: suspend () -> T): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (e: CancellationException) {
            // 必须抛出协程取消异常，否则会破坏结构化并发！
            throw e
        } catch (e: Exception) {
            AppResult.Error(getUserFriendlyMessage(e), e)
        }
    }

    /**
     * 同步安全执行器 (适用于 Intent 跳转、普通的纯函数计算、JSON 解析等)
     */
    inline fun <T> runSafeSync(block: () -> T): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (e: Exception) {
            AppResult.Error(getUserFriendlyMessage(e), e)
        }
    }

    /**
     * 智能重试安全执行器 (Exponential Backoff Retry)
     * @param times 最大重试次数 (默认 3 次)
     * @param initialDelay 首次重试延迟 (默认 1000 毫秒)
     * @param maxDelay 最大延迟上限 (默认 10000 毫秒)
     * @param factor 指数退避乘数 (默认 2.0 倍)
     */
    suspend inline fun <T> runSafeWithRetry(
        times: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        crossinline block: suspend () -> T
    ): AppResult<T> {
        var currentDelay = initialDelay

        for (attempt in 1..times) {
            val result = try {
                AppResult.Success(block())
            } catch (e: CancellationException) {
                throw e // 保护协程结构化并发
            } catch (e: Exception) {
                AppResult.Error(getUserFriendlyMessage(e), e)
            }

            // 1. 如果成功，直接返回
            if (result is AppResult.Success) {
                return result
            }

            // 2. 如果失败，智能判断是否具有“可重试价值”
            val error = result as AppResult.Error
            val shouldRetry = when (val e = error.exception) {
                is ApiException -> false // 业务异常 (如 Token 失效、无权限)，重试一万次也没用，直接放行报错
                is HttpException -> e.code() >= 500 // 只有 5xx (服务器崩溃/网关超时) 才重试，4xx (客户端错误) 不重试
                is UnknownHostException,
                is SocketTimeoutException,
                is ConnectException,
                is IOException -> true // 纯物理网络异常，大概率是信号不好，强烈建议重试
                else -> false // SQLite 等本地未知异常，不重试防死循环
            }

            // 3. 如果不可重试，或者已经达到最大次数，直接抛出最后一次的 Error
            if (!shouldRetry || attempt == times) {
                return result
            }

            // 4. 触发指数退避休眠，等待下一次循环
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }

        return AppResult.Error("网络重试超时")
    }

    @PublishedApi
    internal fun getUserFriendlyMessage(e: Throwable): String {
        return when (e) {
            // === API 业务异常 ===
            is ApiException -> getBusinessErrorMessage(e.code, e.message)
            is ApiDataException -> "数据异常: ${e.message}"

            // === 网络异常 ===
            is UnknownHostException -> "当前无网络连接，请检查网络设置"
            is SocketTimeoutException -> "网络请求超时，请稍后重试"
            is ConnectException -> "无法连接到服务器，请检查网络"
            is HttpException -> "服务器开小差了 (状态码: ${e.code()})"

            // === 本地/系统异常扩充 ===
            is SQLiteException -> "本地数据库操作失败，请重试"
            is SecurityException -> "缺少必要的系统权限"
            is FileNotFoundException -> "文件不存在或已被清理"
            is IOException -> "读写数据时发生异常"
            is IllegalArgumentException -> "非法的参数输入"

            else -> e.message ?: "发生未知系统错误"
        }
    }

    private fun getBusinessErrorMessage(code: Int, serverMessage: String?): String {
        return when (code) {
            10001 -> "您输入的密码有误，请重新输入"
            10002 -> "该账号由于违反社区规定已被限制登录，如有疑问请联系客服。"
            20001 -> "该群聊已解散或不存在"
            else -> serverMessage ?: "业务处理失败 (Code: $code)"
        }
    }
}