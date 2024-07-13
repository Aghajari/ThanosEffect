package com.aghajari.thanoseffect

import android.view.ViewGroup
import com.aghajari.thanoseffect.core.ThanosEffectTextureView

interface ThanosEffectRenderer {

    fun createTextureView(rootView: ViewGroup): ThanosEffectTextureView<*>

    fun removeTextureView(textureView: ThanosEffectTextureView<*>)
}