package com.aghajari.thanoseffect.opengl

import android.content.Context
import java.io.InputStream

internal object GLUtils {

    const val PARTICLE_BYTES: Int = 48
    val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)

    inline fun safeRun(block: () -> Unit) {
        try {
            block.invoke()
        } catch (ignore: Exception) {
        }
    }

    fun readRes(context: Context, rawRes: Int): String? {
        var totalRead = 0
        var readBuffer = ByteArray(64 * 1024)
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.openRawResource(rawRes)
            var readLen: Int
            val buffer = ByteArray(4096)
            while ((inputStream.read(buffer, 0, buffer.size).also { readLen = it }) >= 0) {
                if (readBuffer.size < totalRead + readLen) {
                    val newBuffer = ByteArray(readBuffer.size * 2)
                    System.arraycopy(readBuffer, 0, newBuffer, 0, totalRead)
                    readBuffer = newBuffer
                }
                if (readLen > 0) {
                    System.arraycopy(buffer, 0, readBuffer, totalRead, readLen)
                    totalRead += readLen
                }
            }
        } catch (e: Throwable) {
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (ignore: Throwable) {
            }
        }

        return String(readBuffer, 0, totalRead)
    }
}