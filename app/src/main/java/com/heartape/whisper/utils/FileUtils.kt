package com.heartape.whisper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLConnection

object FileUtils {

    fun detectMime(file: File): String {
        val byName = URLConnection.guessContentTypeFromName(file.name)

        val byStream = try {
            file.inputStream().use {
                URLConnection.guessContentTypeFromStream(it)
            }
        } catch (e: Exception) {
            Log.e("detectMime", "类型验证失败", e)
            null
        }

        return byStream ?: byName ?: "application/octet-stream"
    }

    // 从相册 Uri 获取原始临时文件
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ★ 核心压缩算法：尺寸等比缩放 + 质量循环压缩
     * @param originalFile 原始图片文件
     * @param maxSizeKB 目标最大体积 (KB)
     */
    fun compressImage(context: Context, originalFile: File, maxSizeKB: Int = 100): File {
        // 如果原图本身就已经小于目标大小，直接返回原图
        if (originalFile.length() <= maxSizeKB * 1024) {
            return originalFile
        }

        // 1. 第一步：尺寸等比压缩 (防止加载超大图直接导致 OOM)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // 仅测量边界，不真正加载到内存
        BitmapFactory.decodeFile(originalFile.absolutePath, options)

        // 假设头像的最大合理边长为 800px
        var inSampleSize = 1
        val maxDim = 800
        if (options.outHeight > maxDim || options.outWidth > maxDim) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxDim && halfWidth / inSampleSize >= maxDim) {
                inSampleSize *= 2
            }
        }

        // 真正加载缩放后的图片到内存
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options) ?: return originalFile

        // 2. 第二步：质量循环压缩 (逼近目标 KB 大小)
        var quality = 100
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)

        // 循环判断：如果压缩后大于 maxSizeKB，且画质还能继续降(>10)，就继续压
        while (baos.size() > maxSizeKB * 1024 && quality > 10) {
            baos.reset() // 重置字节流
            quality -= 10 // 每次降低 10 的画质
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }

        // 3. 写入最终的压缩文件
        val compressedFile = File(context.cacheDir, "compressed_avatar_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(compressedFile)
        fos.write(baos.toByteArray())
        fos.flush()
        fos.close()
        baos.close()

        // 4. 清理上一步获取的原始临时文件，释放设备存储空间
        if (originalFile.exists() && originalFile.name.startsWith("temp_upload_")) {
            originalFile.delete()
        }

        return compressedFile
    }
}