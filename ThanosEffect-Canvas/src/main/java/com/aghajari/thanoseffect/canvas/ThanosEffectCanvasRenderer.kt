package com.aghajari.thanoseffect.canvas

import android.view.ViewGroup
import com.aghajari.thanoseffect.ThanosEffectRenderer
import com.aghajari.thanoseffect.core.ThanosEffectTextureView

object ThanosEffectCanvasRenderer : ThanosEffectRenderer {

    override fun createTextureView(rootView: ViewGroup): ThanosEffectTextureView<*> {
        val textureView = CanvasTextureView(rootView.context)
        rootView.addView(textureView)
        return textureView
    }

    override fun removeTextureView(textureView: ThanosEffectTextureView<*>) {
        (textureView.parent as? ViewGroup)?.removeView(textureView)
    }
}