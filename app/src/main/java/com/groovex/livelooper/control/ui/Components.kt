package com.groovex.livelooper.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun llText(size: TextUnit = 12.sp, weight: FontWeight = FontWeight.ExtraBold, color: Color = LL.Text) =
    TextStyle(fontFamily = LL.Manrope, fontSize = size, fontWeight = weight, color = color)

/** Glass panel matching the livelooper GUI: translucent fill, coloured wash from the top, hairline edge. */
fun Modifier.glass(radius: Dp = 16.dp, tint: Color? = null): Modifier {
    val shape = RoundedCornerShape(radius)
    val wash = if (tint != null) Brush.verticalGradient(listOf(tint.copy(alpha = 0.28f), Color.White.copy(alpha = 0.05f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.04f)))
    return this.clip(shape).background(wash).border(1.dp, Color.White.copy(alpha = 0.13f), shape)
}

fun Modifier.lcd(radius: Dp = 10.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(Color(0xFF0A0B0E)).padding(3.dp).clip(RoundedCornerShape(radius - 2.dp)).background(LL.Lcd)
}

/**
 * Fires on pointer DOWN (not on release) for the lowest perceived latency. Each control consumes its own
 * pointer, so several fingers on several controls work at the same time (multi-touch, also on DeX touch monitors).
 */
fun Modifier.onPress(action: () -> Unit): Modifier = this.pointerInput(action) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false).consume()
        action()
    }
}

@Composable
fun LLButton(
    label: String,
    modifier: Modifier = Modifier,
    active: Color? = null,
    gradient: Boolean = false,
    size: TextUnit = 11.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val base = when {
        gradient -> Modifier.clip(shape).background(LL.brand)
        active != null -> Modifier.clip(shape).background(active.copy(alpha = 0.3f)).border(1.5.dp, active, shape)
        else -> Modifier.clip(shape).background(LL.Button).border(1.dp, Color.White.copy(alpha = 0.1f), shape)
    }
    Box(modifier.then(base).onPress(onClick), contentAlignment = Alignment.Center) {
        Text(label, style = llText(size, color = if (active != null) Color(0xFFFFE6C8) else LL.Text), maxLines = 1)
    }
}

@Composable
fun Pill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val m = if (selected) Modifier.clip(shape).background(LL.brand)
    else Modifier.clip(shape).background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    Box(modifier.then(m).onPress(onClick).padding(horizontal = 14.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, style = llText(11.sp))
    }
}
