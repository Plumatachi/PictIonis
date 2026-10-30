package com.etna.pictionis.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// Transition pixel avec effet frame-by-frame (étapes discrètes)
@Composable
fun PixelSlideTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val offset = remember { Animatable(0f) }
    
    // Nombre de frames pour l'animation (plus bas = plus pixelisé)
    val frames = 8f
    
    LaunchedEffect(visible) {
        val target = if (visible) 0f else 100f
        val spec: AnimationSpec<Float> = tween(
            durationMillis = 300,
            easing = { it } // Linear pour effet plus brut
        )
        offset.animateTo(
            targetValue = target,
            animationSpec = spec
        )
    }
    
    // Convertir l'offset en steps discrètes pour effet pixel
    val steppedOffset = (offset.value / (100f / frames)).roundToInt() * (100f / frames)
    
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = with(density) { steppedOffset.dp.toPx().roundToInt() }
                )
            }
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

// Transition pixel avec effet de "glitch" (sauts aléatoires)
@Composable
fun PixelGlitchTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val offset = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        val target = if (visible) 0f else 50f
        val spec: AnimationSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMedium
        )
        offset.animateTo(
            targetValue = target,
            animationSpec = spec
        )
    }
    
    // Effet glitch avec offsets aléatoires
    val glitchOffset = if (!visible && offset.value > 0) {
        ((offset.value % 10f) * 2f).roundToInt()
    } else {
        0
    }
    
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
            .offset {
                IntOffset(
                    x = with(density) { glitchOffset.dp.toPx().roundToInt() },
                    y = with(density) { offset.value.dp.toPx().roundToInt() }
                )
            }
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

// Transition pixel avec effet de "flash" (apparition brutale)
@Composable
fun PixelFlashTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 100) // Flash rapide
            )
        } else {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 100)
            )
        }
    }
    
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
