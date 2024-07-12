package com.aghajari.thanoseffect

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


internal object EffectUtils {

    const val PARTICLE_BYTES: Int = 48
    private val applicationHandler = Handler(Looper.getMainLooper())
    private var density: Float = 1f

    val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)
    private val ALPHAS = floatArrayOf(
        0.3f, 0.6f, 1.0f
    )

    private var MIN_DELTA: Double = 0.0
    internal var MAX_DELTA: Double = 0.0

    fun dp(a: Float): Int {
        return (density * a).toInt()
    }

    fun init(context: Context) {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        val screenRefreshRate = max(60f, display?.refreshRate ?: 0f)
        density = context.resources.displayMetrics.density

        MIN_DELTA = 1.0 / screenRefreshRate
        MAX_DELTA = MIN_DELTA * 4
    }

    fun randomInitialAlpha(color: Int): Float {
        return min(
            Color.alpha(color) / 255f,
            ALPHAS.random()
        )
    }

    fun randomVelocity() = dp(4f) + Random.nextInt(dp(4f))

    fun randomTranslation(initialMinDp: Int): Int {
        var t = dp(initialMinDp.toFloat()) + Random.nextInt(dp(56f)).toFloat()
        t *= if (Random.nextInt(10) == 0) -1f else 1f
        if (t < 0) {
            t /= 4f
        }
        return t.toInt()
    }

    fun randomRadius(
        size: Int,
        minRadius: Int = size / 4,
        maxRadius: Int = size,
    ): Float {
        return (minRadius..maxRadius).random().toFloat()
    }

    fun calculateSize(view: View): Int {
        return 6

        /*val size = max(view.width, view.height)

        return if (size <= dp(300f)) {
            X_HIGH_RESOLUTION
        } else if (size <= dp(450f)) {
            HIGH_RESOLUTION
        } else if (size <= dp(500f)) {
            AVERAGE_RESOLUTION
        } else if (size <= dp(620f)) {
            LOW_RESOLUTION
        } else {
            X_LOW_RESOLUTION
        }*/
    }

    fun calculatePendingSize(view: View): Int {
        val size = max(view.width, view.height)

        return if (size <= dp(150f)) {
            1
        } else if (size <= dp(250f)) {
            1
        } else if (size <= dp(300f)) {
            2
        } else if (size <= dp(420f)) {
            3
        } else {
            3
        }
    }

    fun optimizeSize(original: Int, sumOfPendingValues: Int = 0): Int {
        return original + 2 * sumOfPendingValues
    }

    fun canDrawPixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        return Color.alpha(bitmap.getPixel(x, y)) > 10
    }

    fun timeTuning(
        now: Long,
        lastTime: Long,
    ): Double {
        val deltaTime = (now - lastTime) / 1000000000.0
        if (deltaTime < MIN_DELTA) {
            val wait = MIN_DELTA - deltaTime
            try {
                val milli = (wait * 1000L).toLong()
                val nano = ((wait - milli / 1000.0) * 1000000000).toInt()
                Thread.sleep(milli, nano)
            } catch (ignore: Exception) {
            }
            return MIN_DELTA
        } else if (deltaTime > MAX_DELTA) {
            return MAX_DELTA
        } else {
            return deltaTime
        }
    }

    fun runOnUIThread(runnable: Runnable) {
        applicationHandler.post(runnable)
    }

    fun runOnUIThread(runnable: Runnable, delay: Long) {
        applicationHandler.removeCallbacks(runnable)
        if (delay == 0L) {
            applicationHandler.post(runnable)
        } else {
            applicationHandler.postDelayed(runnable, delay)
        }
    }

    fun findActivity(context: Context?): Activity? {
        if (context is Activity) {
            return context
        }
        if (context is ContextWrapper) {
            return findActivity(context.baseContext)
        }
        return null
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

    inline fun safeRun(block: () -> Unit) {
        try {
            block.invoke()
        } catch (ignore: Exception) {
        }
    }
}