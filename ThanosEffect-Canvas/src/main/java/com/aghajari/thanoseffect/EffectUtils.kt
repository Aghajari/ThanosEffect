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
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


internal object EffectUtils {

    private val applicationHandler = Handler(Looper.getMainLooper())
    private var density: Float = 1f

    const val EACH_LINE_DURATION = 8

    private val ALPHAS = floatArrayOf(
        0.3f, 0.6f, 1.0f
    )

    private var MIN_DELTA: Double = 0.0
    private var MAX_DELTA: Double = 0.0

    private fun dp(a: Float): Int {
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

    fun randomInitialAlpha(color: Int): Int {
        return min(
            Color.alpha(color),
            (ALPHAS.random() * 255).toInt()
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
}