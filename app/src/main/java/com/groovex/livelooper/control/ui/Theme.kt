package com.groovex.livelooper.control.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.groovex.livelooper.control.R

object LL {
    val Brand0 = Color(0xFFFF7A18)
    val Brand1 = Color(0xFFFF2D6F)
    val Brand2 = Color(0xFFB14BFF)
    val Text = Color(0xFFE6E8EE)
    val Dim = Color(0xFF8E939C)
    val Red = Color(0xFFFF3B30)
    val Green = Color(0xFF30D158)
    val Orange = Color(0xFFFF9F0A)
    val Yellow = Color(0xFFFFD60A)
    val Cyan = Color(0xFF64D2FF)
    val Lcd = Color(0xFF06121A)
    val LcdText = Color(0xFF7FE3FF)
    val Button = Color(0xFF2C2E34)
    val Grey = Color(0xFF787D84)
    val Tracks = listOf(Brand0, Brand1, Brand2, Cyan, Green)

    val brand = Brush.horizontalGradient(listOf(Brand0, Brand1, Brand2))
    val background = Brush.linearGradient(listOf(Color(0xFF2A1E1E), Color(0xFF16141C), Color(0xFF1C1626)))

    val Manrope = FontFamily(
        Font(R.font.manrope_400, FontWeight.Normal),
        Font(R.font.manrope_600, FontWeight.SemiBold),
        Font(R.font.manrope_800, FontWeight.ExtraBold),
    )
}

@Composable
fun LivelooperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = LL.Brand1, secondary = LL.Brand2, background = Color.Black),
        content = content,
    )
}
