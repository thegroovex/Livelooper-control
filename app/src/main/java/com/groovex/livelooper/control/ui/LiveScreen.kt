package com.groovex.livelooper.control.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovex.livelooper.control.R
import com.groovex.livelooper.control.model.SurfaceModel
import com.groovex.livelooper.control.model.TrackModel
import com.groovex.livelooper.control.model.TrackState

fun stateColor(s: TrackState) = when (s) {
    TrackState.PLAY -> LL.Green
    TrackState.DUB -> LL.Orange
    TrackState.STOP -> LL.Cyan
    TrackState.REC -> LL.Red
    TrackState.EMPTY -> LL.Grey
}

@Composable
fun stateLabel(s: TrackState) = stringResource(
    when (s) {
        TrackState.PLAY -> R.string.state_play
        TrackState.DUB -> R.string.state_dub
        TrackState.STOP -> R.string.state_stop
        TrackState.REC -> R.string.state_rec
        TrackState.EMPTY -> R.string.state_empty
    }
)

/** Demo playhead: one shared phase in bars, advanced per frame. */
@Composable
fun rememberPhase(model: SurfaceModel): Float {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(model.playing, model.bpm) {
        var last = 0L
        while (model.playing) {
            withFrameNanos { now ->
                if (last != 0L) phase += (now - last) / 1e9f * model.bpm / 60f / 4f
                last = now
            }
        }
    }
    return phase
}

@Composable
fun LiveScreen(model: SurfaceModel, modifier: Modifier = Modifier) {
    val phase = rememberPhase(model)
    Column(modifier) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            model.tracks.forEach { t -> TrackCard(model, t, phase, Modifier.weight(1f).fillMaxHeight()) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LLButton(stringResource(R.string.all_start_stop), Modifier.weight(2f).fillMaxHeight(), gradient = !model.playing) { model.allStartStop() }
            LLButton(stringResource(R.string.undo), Modifier.weight(1f).fillMaxHeight()) {}
            LLButton(stringResource(R.string.tap), Modifier.weight(1f).fillMaxHeight()) {}
            LLButton(stringResource(R.string.capture), Modifier.weight(1f).fillMaxHeight()) {}
        }
    }
}

@Composable
fun TrackCard(model: SurfaceModel, t: TrackModel, phase: Float, modifier: Modifier) {
    val col = LL.Tracks[t.index]
    val sc = stateColor(t.state)
    Column(modifier.glass(14.dp, col).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(3.dp, 12.dp).background(col, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.track_n, t.index + 1), style = llText(11.sp))
        }
        Spacer(Modifier.height(6.dp))
        val prog = if (t.bars > 0f) (phase % t.bars) / t.bars else 0f
        Column(Modifier.fillMaxWidth().height(86.dp).lcd(9.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(stateLabel(t.state), style = llText(18.sp, color = sc))
            if (t.state != TrackState.EMPTY) Waveform(t.peaks, col, if (t.state == TrackState.STOP) 0f else prog, t.state == TrackState.STOP, Modifier.fillMaxSize())
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val d = minOf(maxWidth, maxHeight) * 0.92f
            BigButton(t.state, sc, Modifier.size(d)) { model.recPlayDub(t) }
        }
        LevelBar(t.level, Modifier.fillMaxWidth().height(26.dp)) { t.level = it }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LLButton(stringResource(R.string.stop), Modifier.weight(1f).fillMaxHeight(), size = 10.sp) { model.stop(t) }
            LLButton(stringResource(R.string.mute), Modifier.weight(1f).fillMaxHeight(), active = if (t.muted) LL.Orange else null, size = 10.sp) { t.muted = !t.muted }
        }
    }
}

@Composable
fun Waveform(peaks: FloatArray, col: Color, prog: Float, quiet: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val n = peaks.size
        val step = size.width / n
        val mid = size.height / 2
        for (i in 0 until n) {
            val a = peaks[i] * mid * (if (quiet) 0.4f else 0.95f)
            val played = i.toFloat() / n < prog
            drawLine(col.copy(alpha = if (played) 0.95f else 0.45f), Offset(i * step, mid - a), Offset(i * step, mid + a), strokeWidth = step * 0.6f)
        }
        val x = size.width * prog
        drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun BigButton(state: TrackState, col: Color, modifier: Modifier, onPress: () -> Unit) {
    val lit = state == TrackState.PLAY || state == TrackState.DUB || state == TrackState.REC
    val base = if (lit) col else col.copy(alpha = 0.45f)
    Box(modifier.aspectRatio(1f).onPress(onPress), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val c = center
            if (lit) drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.45f), Color.Transparent), c, r), r, c)
            drawCircle(Color(0xFF0C0D10), r * 0.96f, c)
            val seg = 24
            for (i in 0 until seg) {
                val on = lit
                drawArc(
                    if (on) col else Color(0xFF283C30), i * 360f / seg - 90f + 2f, 360f / seg - 5f, false,
                    topLeft = Offset(c.x - r * 0.88f, c.y - r * 0.88f), size = Size(r * 1.76f, r * 1.76f),
                    style = Stroke(width = r * 0.06f),
                )
            }
            drawCircle(Brush.radialGradient(listOf(base, base.copy(red = base.red * 0.55f, green = base.green * 0.55f, blue = base.blue * 0.55f)), Offset(c.x, c.y - r * 0.25f), r * 0.95f), r * 0.8f, c)
            drawCircle(Color.White.copy(alpha = 0.13f), r * 0.42f, Offset(c.x, c.y - r * 0.38f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.rec), style = llText(16.sp, color = Color.White))
            Text(stringResource(R.string.play_dub), style = llText(9.sp, color = Color.White.copy(alpha = 0.75f)))
        }
    }
}

@Composable
fun LevelBar(value: Float, modifier: Modifier, onChange: (Float) -> Unit) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.level), style = llText(8.sp, color = LL.Dim), modifier = Modifier.weight(1f))
            Text("${(value * 200).toInt()} %", style = llText(8.sp))
        }
        Canvas(
            Modifier.fillMaxWidth().height(14.dp)
                .pointerInput(Unit) { detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) } }
                .pointerInput(Unit) { detectDragGestures { change, _ -> onChange((change.position.x / size.width).coerceIn(0f, 1f)) } }
        ) {
            val h = 6.dp.toPx(); val y = (size.height - h) / 2
            drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
            drawRoundRect(Brush.horizontalGradient(listOf(LL.Green, Color(0xFF96DC3C))), Offset(0f, y), Size(size.width * value, h), CornerRadius(h / 2))
            val ux = size.width / 2
            drawLine(LL.Text, Offset(ux, 0f), Offset(ux, size.height), strokeWidth = 1.5.dp.toPx())
        }
    }
}

@Composable
fun Placeholder(modifier: Modifier) {
    Box(modifier.glass(14.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.coming_soon), style = llText(14.sp, color = LL.Dim))
    }
}
