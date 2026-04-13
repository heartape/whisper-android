package com.heartape.whisper.data.remote

import android.util.Log
import com.google.gson.Gson
import com.heartape.whisper.data.model.AppResult
import com.heartape.whisper.data.model.MessageDto
import com.heartape.whisper.utils.ErrorUtils.runSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

enum class WsStatus { CONNECTING, CONNECTED, DISCONNECTED }

@Singleton
class StompManager @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null

    private val _wsStatus = MutableStateFlow(WsStatus.DISCONNECTED)
    val wsStatus: StateFlow<WsStatus> = _wsStatus

    private val _incomingMessages = MutableStateFlow<MessageDto?>(null)
    val incomingMessages: StateFlow<MessageDto?> = _incomingMessages

    private val stompScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

     fun connect(token: String) {
        if (_wsStatus.value == WsStatus.CONNECTED) return
        _wsStatus.value = WsStatus.CONNECTING
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/ws")
            .addHeader("Authorization", token)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("CONNECT\naccept-version:1.1,1.0\nAuthorization:$token\n\n\u0000")
                // 防挂起保护：5秒内如果没收到 CONNECTED 帧，强行掐断重连
                stompScope.launch {
                    delay(5000)
                    if (_wsStatus.value != WsStatus.CONNECTED) {
                        Log.e("STOMP", "STOMP握手超时，主动掐断")
                        // 这会触发 onFailure 进行重连
                        webSocket.cancel()
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.startsWith("CONNECTED")) {
                    _wsStatus.value = WsStatus.CONNECTED
                    val frame = "SUBSCRIBE\nid:sub-global-session\ndestination:/user/queue/session\n\n\u0000"
                    webSocket.send(frame)
                }
                else if (text.startsWith("MESSAGE")) {
                    try {
                        val body = text.substringAfter("\n\n").substringBefore("\u0000")
                        val msg = gson.fromJson(body, MessageDto::class.java)
                        _incomingMessages.value = msg
                    } catch (e: Exception) {
                        Log.e("STOMP", "Parse Error", e)
                    }
                } else if (text.startsWith("ERROR")) {
                    Log.e("STOMP", text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _wsStatus.value = WsStatus.DISCONNECTED
                disconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _wsStatus.value = WsStatus.DISCONNECTED
                scheduleReconnect(token)
            }
        })
    }

    private fun scheduleReconnect(token: String) {
        stompScope.launch {
            delay(3000) // 延迟 3 秒重试，完全不阻塞主线程
            connect(token)
        }
    }

    // ★ 核心修改：统一且简化的发送通道逻辑，自动附带 sessionId
    fun sendMessage(sessionId: Long, messageType: String, content: String) {
        val destination = "/app/session"
        val payload = gson.toJson(mapOf(
            "sessionId" to sessionId,
            "messageType" to messageType,
            "messageInfo" to content
        ))
        val frame = "SEND\ndestination:$destination\ncontent-type:application/json\n\n$payload\u0000"
        webSocket?.send(frame)
    }

    fun disconnect() {
        webSocket?.close(1000, "User Logout")
        webSocket = null
        _wsStatus.value = WsStatus.DISCONNECTED
    }
}