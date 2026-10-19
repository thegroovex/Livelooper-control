package com.groovex.livelooper.control.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovex.livelooper.control.R
import com.groovex.livelooper.control.model.SurfaceModel
import com.groovex.livelooper.control.update.UpdateViewModel

enum class Tab(val label: Int) { LIVE(R.string.tab_live), MIX(R.string.tab_mix), FX(R.string.tab_fx), SETUP(R.string.tab_setup) }

@Composable
fun App(model: SurfaceModel, updates: UpdateViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.LIVE) }
    BoxWithConstraints(Modifier.fillMaxSize().background(LL.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        // Phone landscape vs. large window (tablet, DeX desktop window, external touch monitor).
        val large = maxWidth >= 1000.dp && maxHeight >= 600.dp
        val pad = if (large) 18.dp else 12.dp
        Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = 8.dp)) {
            Header(model, tab, large) { tab = it }
            Spacer(Modifier.height(8.dp))
            val content = Modifier.weight(1f).fillMaxWidth()
            when (tab) {
                Tab.LIVE -> LiveScreen(model, content)
                Tab.SETUP -> SetupScreen(updates, content)
                else -> Placeholder(content)
            }
        }
    }
}

@Composable
private fun Header(model: SurfaceModel, tab: Tab, large: Boolean, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("livelooper", style = llText(if (large) 22.sp else 19.sp))
                Spacer(Modifier.width(6.dp))
                Text("control", style = llText(if (large) 15.sp else 13.sp).copy(brush = LL.brand))
            }
            Text(stringResource(R.string.designed_by), style = llText(8.sp, FontWeight.SemiBold, LL.Dim))
        }
        Spacer(Modifier.width(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tab.entries.forEach { t -> Pill(stringResource(t.label), t == tab) { onTab(t) } }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.height(40.dp).lcd(9.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("%.1f".format(model.bpm), style = llText(20.sp, color = LL.LcdText))
            Text(" BPM", style = llText(8.sp, color = Color(0xFF3C96BE)))
        }
        Spacer(Modifier.weight(1f))
        val shape = RoundedCornerShape(50)
        Row(
            Modifier.clip(shape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, LL.Yellow.copy(alpha = 0.6f), shape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(shape).background(Brush.radialGradient(listOf(LL.Yellow, LL.Orange))))
            Spacer(Modifier.width(8.dp))
            Text("${stringResource(R.string.demo_mode)} · ${stringResource(R.string.not_connected)}", style = llText(11.sp))
        }
    }
}
