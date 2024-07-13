package com.aghajari.thanoseffect

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.aghajari.thanoseffect.core.ThanosEffectTextureView
import com.aghajari.thanoseffect.core.EffectUtils
import com.aghajari.thanoseffect.core.RenderConfigs
import com.aghajari.thanoseffect.widget.EffectedView
import com.aghajari.thanoseffect.widget.asEffectedView

object ThanosEffect {

    var renderer: ThanosEffectRenderer? = null
    private var textureView: ThanosEffectTextureView<*>? = null

    fun start(
        view: View,
        pending: Int = 0,
        autoDelete: Boolean = true,
        renderConfigs: RenderConfigs = RenderConfigs.default(),
    ) {
        if (renderer == null) {
            throw NullPointerException("You must set ThanosEffect.renderer before using it.")
        }

        initialize(view.context)
        textureView?.attach(view.asEffectedView(), pending, renderConfigs)

        if (autoDelete) {
            view.postDelayed({
                (view.parent as? ViewGroup)?.removeView(view)
            }, 100)
        }
    }

    fun start(
        context: Context,
        effectedView: EffectedView,
        pending: Int = 0,
        renderConfigs: RenderConfigs = RenderConfigs.default(),
    ) {
        if (renderer == null) {
            throw NullPointerException("You must set ThanosEffect.renderer before using it.")
        }

        initialize(context)
        textureView?.attach(effectedView, pending, renderConfigs)
    }

    fun pause(pause: Boolean) {
        textureView?.pause(pause)
    }

    internal fun destroy() {
        EffectUtils.runOnUIThread {
            textureView?.let {
                renderer?.removeTextureView(it)
            }
            textureView = null
        }
    }

    private fun initialize(context: Context) {
        if (textureView == null) {
            val activity = EffectUtils.findActivity(context)
            val rootView = activity?.findViewById<View>(android.R.id.content)
                ?.rootView as? ViewGroup
            if (rootView != null) {
                EffectUtils.init(context)
                textureView = renderer!!.createTextureView(rootView)
            }
        }
    }
}