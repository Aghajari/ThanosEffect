package com.aghajari.thanoseffect.opengl

import android.graphics.Canvas
import android.view.ViewGroup
import android.widget.FrameLayout
import com.aghajari.thanoseffect.ThanosEffectRenderer
import com.aghajari.thanoseffect.core.ThanosEffectTextureView

/**
 * An implementation of [ThanosEffectRenderer] that uses OpenGL for rendering effects.
 */
object ThanosEffectOpenGLRenderer : ThanosEffectRenderer {

    override fun createTextureView(rootView: ViewGroup): ThanosEffectTextureView<*> {
        val textureView = GLTextureView(rootView.context)
        val container: FrameLayout = object : FrameLayout(rootView.context) {
            override fun dispatchDraw(canvas: Canvas) {
                super.dispatchDraw(canvas)
                textureView.renderFirstFrameByCanvasIfNeeded(canvas)
            }
        }
        container.addView(textureView)
        rootView.addView(container)
        return textureView
    }

    override fun removeTextureView(textureView: ThanosEffectTextureView<*>) {
        val textureViewContainer = textureView.parent as? ViewGroup
        textureViewContainer?.removeView(textureView)
        (textureViewContainer?.parent as? ViewGroup)?.removeView(textureViewContainer)
    }
}