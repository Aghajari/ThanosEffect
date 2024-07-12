package com.aghajari.thanoseffect

import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

object ThanosEffect {

    private var textureView: ThanosEffectTextureView? = null

    fun start(
        view: View,
        pending: Int = 0,
        autoDelete: Boolean = true,
        perPx: Int = EffectUtils.calculateSize(view),
        particleLifeTime: ParticleLifeTime = ParticleLifeTimeImpl,
    ) {
        initialize(view.context)
        textureView?.attach(view, pending, perPx, particleLifeTime)

        if (autoDelete) {
            view.postDelayed({
                (view.parent as? ViewGroup)?.removeView(view)
            }, 100)
        }
    }

    fun pause(pause: Boolean) {
        textureView?.pause(pause)
    }

    internal fun destroy() {
        val textureViewContainer = textureView?.parent as? ViewGroup
        EffectUtils.runOnUIThread {
            textureViewContainer?.removeView(textureView)
            (textureViewContainer?.parent as? ViewGroup)?.removeView(textureViewContainer)
            textureView = null
        }
    }

    private fun initialize(context: Context) {
        if (textureView == null) {
            val activity = EffectUtils.findActivity(context)
            val rootView =
                activity?.findViewById<View>(android.R.id.content)?.rootView as? ViewGroup
            if (rootView != null) {
                EffectUtils.init(context)
                textureView = ThanosEffectTextureView(context)
                makeTextureViewContainer(rootView).addView(textureView)
            }
        }
    }

    private fun makeTextureViewContainer(rootView: ViewGroup): FrameLayout {
        val container: FrameLayout = object : FrameLayout(rootView.context) {
            override fun dispatchDraw(canvas: Canvas) {
                super.dispatchDraw(canvas)
                textureView?.renderFirstFrameByCanvasIfNeeded(canvas)
            }
        }
        rootView.addView(container)
        return container
    }
}