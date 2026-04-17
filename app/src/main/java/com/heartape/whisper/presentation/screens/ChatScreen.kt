package com.heartape.whisper.presentation.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.heartape.whisper.data.local.MemberEntity
import com.heartape.whisper.presentation.viewmodel.ChatViewModel
import com.heartape.whisper.utils.AudioRecorder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val membersMap by viewModel.membersMap.collectAsState()
    val currentUserId = viewModel.currentUserId
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorder(context) }

    var previousMessageCount by remember { mutableStateOf(0) }

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (previousMessageCount == 0 && messages.isNotEmpty()) {
            // 初次进入页面：瞬间滚动到底部
            listState.scrollToItem(messages.size - 1)
        } else if (messages.size > previousMessageCount) {
            // 有新消息到来，判断如果用户处于底部区域附近，平滑下滚
            // 如果用户正在翻看很早的聊天记录，则不要强制把他拉到底部
            if (listState.firstVisibleItemIndex >= previousMessageCount - 5) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
        previousMessageCount = messages.size
    }

    val firstVisibleItem by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItem) {
        // 如果滑到了最顶端（第 0 条），并且当前列表条数至少达到了限制（说明还有历史记录没拉完）
        if (firstVisibleItem == 0 && messages.size >= viewModel.currentLimit) {
            viewModel.loadMoreMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.sessionName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.MoreHoriz, "设置") }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                onSendText = { text -> viewModel.sendMessage("TEXT", text) },
                onSendAudio = { file ->
                    // 实际项目中应先调 upload API，再发 WebSocket
                    viewModel.sendMessage("AUDIO", "[语音消息]")
                },
                audioRecorder = audioRecorder
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.userId == currentUserId
                val senderMember = membersMap[msg.userId]
                senderMember?.let { MessageItem(it, isMine, msg.messageInfo, msg.createTime) }
            }
        }
    }
}

@Composable
fun MessageItem(member: MemberEntity, isMine: Boolean, text: String, timestamp: Long) {
    val align = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMine) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
            if (!isMine) {
                AsyncImage(
                    model = member.avatar,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                if (!isMine) {
                    Text(
                        text = member.aliasName ?: member.username,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                Box(modifier = Modifier.background(color, shape).padding(12.dp)) {
                    Text(text = text, color = textColor)
                }
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (isMine) {
                Spacer(Modifier.width(8.dp))
                AsyncImage(
                    model = member.avatar,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInputBar(
    onSendText: (String) -> Unit,
    onSendAudio: (java.io.File) -> Unit,
    audioRecorder: AudioRecorder
) {
    var text by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isVoiceMode = !isVoiceMode }) {
                Icon(if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic, "切换输入")
            }

            if (isVoiceMode) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    isRecording = true
                                    audioRecorder.startRecording()
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    isRecording = false
                                    audioRecorder.stopRecording()
                                    // 模拟获取文件发送
                                    onSendAudio(java.io.File(""))
                                    true
                                }
                                else -> false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isRecording) "松开 发送" else "按住 说话", color = if (isRecording) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(25.dp),
                    placeholder = { Text("发消息...") }
                )
            }

            IconButton(onClick = { /* 打开表情包面板 */ }) {
                Icon(Icons.Default.EmojiEmotions, "表情")
            }

            if (text.isNotBlank() && !isVoiceMode) {
                IconButton(onClick = {
                    onSendText(text)
                    text = ""
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(onClick = { /* 打开更多面板 */ }) {
                    Icon(Icons.Default.AddCircle, "更多")
                }
            }
        }
    }
}