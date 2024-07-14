package com.aghajari.thanoseffect.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.aghajari.thanoseffect.widget.EffectedView
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


object EffectUtils {

    private val applicationHandler = Handler(Looper.getMainLooper())
    private var density: Float = 1f

    private val ALPHAS = floatArrayOf(
        0.3f, 0.6f, 1.0f
    )

    private var MIN_DELTA: Double = 0.0
    private var MAX_DELTA: Double = 0.0

    /**
     * Converts dp units to pixels based on the screen density.
     *
     * @param a The value in dp.
     * @return The value in pixels.
     */
    fun dp(a: Float): Int {
        return (density * a).toInt()
    }

    /**
     * Initializes the utility with the provided context.
     */
    internal fun init(context: Context) {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        val screenRefreshRate = max(60f, display?.refreshRate ?: 0f)
        density = context.resources.displayMetrics.density

        MIN_DELTA = 1.0 / screenRefreshRate
        MAX_DELTA = MIN_DELTA * 4
    }

    /**
     * Generates a random initial alpha value for a given color.
     *
     * @param color The color to base the alpha value on.
     * @return The random initial alpha value.
     */
    fun randomInitialAlpha(color: Int): Int {
        return min(
            Color.alpha(color),
            (ALPHAS.random() * 255).toInt()
        )
    }

    /**
     * Generates a random velocity value.
     *
     * @return The random velocity value.
     */
    fun randomVelocity() = dp(4f) + Random.nextInt(dp(4f))

    /**
     * Generates a random translation value based on an initial minimum dp value.
     *
     * @param initialMinDp The initial minimum dp value.
     * @return The random translation value.
     */
    fun randomTranslation(initialMinDp: Int): Int {
        var t = dp(initialMinDp.toFloat()) + Random.nextInt(dp(56f)).toFloat()
        t *= if (Random.nextInt(10) == 0) -1f else 1f
        if (t < 0) {
            t /= 4f
        }
        return t.toInt()
    }

    /**
     * Generates a random radius value within a specified range.
     *
     * @param size The base size for calculating the radius.
     * @param minRadius The minimum radius.
     * @param maxRadius The maximum radius.
     * @return The random radius value.
     */
    fun randomRadius(
        size: Int,
        minRadius: Int = size / 4,
        maxRadius: Int = size,
    ): Float {
        return (minRadius..maxRadius).random().toFloat()
    }

    fun calculateSize(): Int {
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

    /**
     * Calculates the weight of a view for rendering.
     *
     * @param view The view to calculate the weight for.
     * @return The calculated weight.
     */
    fun calculateWeight(view: EffectedView): Int {
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

    /**
     * Optimizes the size based on the original size and the sum of pending weights.
     *
     * @param original The original size.
     * @param sumOfPendingWeights The sum of pending weights.
     * @return The optimized size.
     */
    fun optimizeSize(original: Int, sumOfPendingWeights: Int = 0): Int {
        return original + 2 * sumOfPendingWeights
    }

    /**
     * Checks if a pixel can be drawn based on its alpha value.
     *
     * @param bitmap The bitmap containing the pixel.
     * @param x The x-coordinate of the pixel.
     * @param y The y-coordinate of the pixel.
     * @return `true` if the pixel can be drawn, `false` otherwise.
     */
    fun canDrawPixel(bitmap: Bitmap, x: Int, y: Int): Boolean {
        return Color.alpha(bitmap.getPixel(x, y)) > 10
    }

    /**
     * Tunes the time to ensure consistent rendering intervals.
     *
     * @param now The current time in nanoseconds.
     * @param lastTime The last recorded time in nanoseconds.
     * @return The adjusted delta time in seconds.
     */
    internal fun timeTuning(
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

    /**
     * Runs a runnable on the UI thread.
     *
     * @param runnable The runnable to execute.
     */
    internal fun runOnUIThread(runnable: Runnable) {
        applicationHandler.post(runnable)
    }

    /**
     * Runs a runnable on the UI thread with a delay.
     *
     * @param runnable The runnable to execute.
     * @param delay The delay in milliseconds before execution.
     */
    internal fun runOnUIThread(runnable: Runnable, delay: Long) {
        applicationHandler.removeCallbacks(runnable)
        if (delay == 0L) {
            applicationHandler.post(runnable)
        } else {
            applicationHandler.postDelayed(runnable, delay)
        }
    }

    /**
     * Finds the activity associated with a given context.
     *
     * @param context The context to search within.
     * @return The associated activity, or `null` if not found.
     */
    internal fun findActivity(context: Context?): Activity? {
        if (context is Activity) {
            return context
        }
        if (context is ContextWrapper) {
            return findActivity(context.baseContext)
        }
        return null
    }

    /**
     * Gets the maximum acceptable delay for rendering.
     *
     * @return The maximum acceptable delay.
     */
    internal fun getMaxAcceptableDelay() = MAX_DELTA
}