package com.heartape.whisper.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * 核心动画拦截器：将目标主题的所有核心颜色进行平滑过渡动画包装
 */
@Composable
fun animateColorSchemeAsState(
    targetColorScheme: ColorScheme,
    durationMillis: Int = 400 // 统一渐变时长 400 毫秒
): ColorScheme {
    val animationSpec = tween<Color>(durationMillis)

    // 1. 核心品牌色
    val primary by animateColorAsState(targetColorScheme.primary, animationSpec, label = "")
    val onPrimary by animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "")
    val primaryContainer by animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "")
    val onPrimaryContainer by animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "")

    val secondary by animateColorAsState(targetColorScheme.secondary, animationSpec, label = "")
    val onSecondary by animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "")
    val secondaryContainer by animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "")
    val onSecondaryContainer by animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec, label = "")

    // 2. 基础背景与表面色
    val background by animateColorAsState(targetColorScheme.background, animationSpec, label = "")
    val onBackground by animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "")
    val surface by animateColorAsState(targetColorScheme.surface, animationSpec, label = "")
    val onSurface by animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "")
    val surfaceVariant by animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "")
    val onSurfaceVariant by animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "")
    val surfaceTint by animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "")

    // 3. M3 最新的 Surface Container 家族 (完美适配新版 Dialog, BottomBar 等组件)
    val surfaceBright by animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "")
    val surfaceDim by animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "")
    val surfaceContainer by animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "")
    val surfaceContainerHigh by animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "")
    val surfaceContainerHighest by animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "")
    val surfaceContainerLow by animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "")
    val surfaceContainerLowest by animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "")

    // 4. 边框线色
    val outline by animateColorAsState(targetColorScheme.outline, animationSpec, label = "")
    val outlineVariant by animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "")
    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = targetColorScheme.inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = targetColorScheme.tertiary,
        onTertiary = targetColorScheme.onTertiary,
        tertiaryContainer = targetColorScheme.tertiaryContainer,
        onTertiaryContainer = targetColorScheme.onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = targetColorScheme.inverseSurface,
        inverseOnSurface = targetColorScheme.inverseOnSurface,
        error = targetColorScheme.error,
        onError = targetColorScheme.onError,
        errorContainer = targetColorScheme.errorContainer,
        onErrorContainer = targetColorScheme.onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        // 其他不常发生剧烈视觉冲突的辅助颜色直接透传
        scrim = targetColorScheme.scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,)
}