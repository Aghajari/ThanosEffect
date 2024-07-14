package com.aghajari.thanoseffect

import android.view.ViewGroup
import com.aghajari.thanoseffect.core.ThanosEffectTextureView

/**
 * Provides methods to create and remove texture views for animations.
 */
interface ThanosEffectRenderer {

    /**
     * Creates a [ThanosEffectTextureView] and adds it to the specified root view.
     *
     * @param rootView The root view to which the texture view will be added.
     * @return The created [ThanosEffectTextureView].
     */
    fun createTextureView(rootView: ViewGroup): ThanosEffectTextureView<*>

    /**
     * Removes the specified [ThanosEffectTextureView] from its parent view.
     *
     * @param textureView The texture view to be removed.
     */
    fun removeTextureView(textureView: ThanosEffectTextureView<*>)
}