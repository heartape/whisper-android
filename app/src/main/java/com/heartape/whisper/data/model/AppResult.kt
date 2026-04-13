package com.heartape.whisper.data.model

/**
 * 全局统一的结果包装密封接口 (Monad 模式)
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val message: String, val exception: Throwable? = null) : AppResult<Nothing>
}

/**
 * 自定义业务异常：专门用于承载服务端 code != 0 的报错
 */
class ApiException(message: String, val code: Int) : Exception(message)

/**
 * 自动解包 ApiResponse
 * 如果 code == 0，直接返回干净的 data；如果不是，抛出业务异常交给上层统一拦截。
 */
fun <T> ApiResponse<T>.unwrap(): T {
    if (this.code == 0) {
        return this.data
    } else {
        throw ApiException("服务器业务异常", this.code)
    }
}