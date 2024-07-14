package com.aghajari.thanoseffect.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/**
 * Interface representing a view that can be used for Thanos effect animations.
 * This interface has been designed to apply the Thanos effect on various types of surfaces,
 * including Android Views, Composables, or even textures.
 */
interface EffectedView {

    val width: Int
    val height: Int

    val translationX: Float
    val translationY: Float

    fun getLocationInWindow(location: IntArray)

    /**
     * Creates a bitmap representation of the view.
     *
     * @return A bitmap containing the view's drawing.
     */
    fun createBitmap(): Bitmap
}

/**
 * Extension function to convert a [View] into an [EffectedView].
 *
 * @return An instance of [EffectedView] that wraps the original [View].
 */
fun View.asEffectedView() = object : EffectedView {
    override val width: Int
        get() = getWidth()
    override val height: Int
        get() = getHeight()
    override val translationX: Float
        get() = getTranslationX()
    override val translationY: Float
        get() = getTranslationY()

    override fun getLocationInWindow(location: IntArray) {
        this@asEffectedView.getLocationInWindow(location)
    }

    override fun createBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        this@asEffectedView.draw(Canvas(bitmap))
        return bitmap
    }
}