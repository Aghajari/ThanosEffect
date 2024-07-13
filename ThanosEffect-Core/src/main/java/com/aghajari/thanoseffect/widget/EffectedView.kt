package com.aghajari.thanoseffect.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

interface EffectedView {

    val width: Int
    val height: Int

    val translationX: Float
    val translationY: Float

    fun getLocationInWindow(location: IntArray)
    fun createBitmap(): Bitmap
}

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