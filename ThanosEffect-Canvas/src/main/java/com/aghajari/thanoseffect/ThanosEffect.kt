package com.aghajari.thanoseffect

import android.content.Context
import android.view.View
import android.view.ViewGroup

object ThanosEffect {

    private var textureView: ThanosEffectTextureView? = null

    fun start(
        view: View,
        pending: Int = 0,
        autoDelete: Boolean = true,
        drawRectParticles: Boolean = false,
        perPx: Int = EffectUtils.calculateSize(view),
        particleLifeTime: ParticleLifeTime = ParticleLifeTimeImpl,
    ) {
        initialize(view.context)
        textureView?.attach(view, pending, drawRectParticles, perPx, particleLifeTime)

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
        (textureView?.parent as? ViewGroup)?.removeView(textureView)
        textureView = null
    }

    private fun initialize(context: Context) {
        if (textureView == null) {
            val activity = EffectUtils.findActivity(context)
            val rootView =
                activity?.findViewById<View>(android.R.id.content)?.rootView as? ViewGroup
            if (rootView != null) {
                EffectUtils.init(context)
                textureView = ThanosEffectTextureView(context)
                rootView.addView(textureView)
            }
        }
    }
}