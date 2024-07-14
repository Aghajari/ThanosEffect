package com.aghajari.thanoseffect.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/**
 * Enables the provided thanos effect controller for this composable.
 *
 * @param controller The controller for managing the Thanos effect.
 */
fun Modifier.thanosEffect(
    controller: ThanosEffectCompose
): Modifier {
    return this
        .composed {
            if (controller.hasDestroyed()) {
                return@composed Modifier
            }

            controller.location = remember { mutableStateOf(Offset.Zero) }
            controller.graphicsLayer = rememberGraphicsLayer()

            Modifier
                .onGloballyPositioned { coordinates ->
                    controller.location?.value = coordinates.positionInWindow()
                }
                .drawWithContent {
                    controller.graphicsLayer?.let { graphicsLayer ->
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
                }
        }
}

/**
 * Create a new [ThanosEffectCompose] instance that will automatically be
 * released when the Composable is disposed.
 *
 * @return a ThanosEffectCompose instance
 */
@Composable
fun rememberThanosEffect(): ThanosEffectCompose {
    return remember { ThanosEffectCompose() }
}