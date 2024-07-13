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

@Composable
fun rememberThanosEffect(): ThanosEffectCompose {
    return remember { ThanosEffectCompose() }
}