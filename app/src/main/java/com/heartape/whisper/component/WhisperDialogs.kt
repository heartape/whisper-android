package com.heartape.whisper.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * 1. 基础弹窗 (Base Dialog)
 * 允许传 null 以隐藏取消按钮
 * 支持外部传入弹窗属性 (如锁死屏幕)
 */
@Composable
fun WhisperBaseDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "确定",
    cancelText: String? = "取消",
    isLoading: Boolean = false,
    confirmEnabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = properties,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(16.dp),

        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = content,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(confirmText)
                }
            }
        },
        dismissButton = cancelText?.let { text ->
            {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

/**
 * 2. 确认弹窗 (Confirm Dialog)
 */
@Composable
fun WhisperConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "确定",
    isLoading: Boolean = false
) {
    WhisperBaseDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = confirmText,
        isLoading = isLoading
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 3. 弹窗内的统一输入框 (Dialog TextField)
 */
@Composable
fun WhisperDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        )
    )
}

/**
 * 4. 强制更新专属弹窗 (无状态的纯 UI 组件)
 */
@Composable
fun WhisperForceUpdateDialog(
    versionName: String,
    releaseNotes: String,
    onGoToStore: () -> Unit
) {
    WhisperBaseDialog(
        title = "安全更新提醒",
        onDismiss = { }, // 强更不可取消
        onConfirm = onGoToStore,
        confirmText = "前往应用商店",
        cancelText = null,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Column {
            Text(
                "当前应用版本已过期，存在兼容性或安全风险。必须升级到最新版本 ($versionName) 才能继续使用。",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Text("更新内容：\n$releaseNotes", style = MaterialTheme.typography.bodySmall)
        }
    }
}
