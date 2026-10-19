package com.groovex.livelooper.control.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TrackState { EMPTY, REC, PLAY, DUB, STOP }

class TrackModel(val index: Int, initial: TrackState, lengthBars: Float) {
    var state by mutableStateOf(initial)
    var level by mutableFloatStateOf(0.5f)
    var muted by mutableStateOf(false)
    var bars by mutableFloatStateOf(lengthBars)
    val peaks: FloatArray = FloatArray(160) { i ->
        val beat = (i % 32) / 32f
        ((1f - beat) * (0.55f + 0.45f * ((i * 7919 + index * 104729) % 97) / 97f)).coerceIn(0.06f, 1f)
    }
}

/**
 * Local surface state. In M1 this mirrors livelooper over the remote protocol (every action = host param ID);
 * until then it runs as a self-contained demo so the touch feel can be tested.
 */
class SurfaceModel {
    val tracks = listOf(
        TrackModel(0, TrackState.PLAY, 1.6f), TrackModel(1, TrackState.DUB, 1.1f), TrackModel(2, TrackState.STOP, 0.8f),
        TrackModel(3, TrackState.EMPTY, 0f), TrackModel(4, TrackState.EMPTY, 0f),
    )
    var bpm by mutableFloatStateOf(120f)
    var playing by mutableStateOf(true)

    fun recPlayDub(t: TrackModel) {
        t.state = when (t.state) {
            TrackState.EMPTY -> TrackState.REC
            TrackState.REC -> { if (t.bars == 0f) t.bars = 2f; TrackState.PLAY }
            TrackState.PLAY -> TrackState.DUB
            TrackState.DUB -> TrackState.PLAY
            TrackState.STOP -> TrackState.PLAY
        }
    }

    fun stop(t: TrackModel) { if (t.state != TrackState.EMPTY) t.state = TrackState.STOP }

    fun allStartStop() {
        playing = !playing
        tracks.forEach { t ->
            if (!playing && t.state in setOf(TrackState.PLAY, TrackState.DUB)) t.state = TrackState.STOP
            else if (playing && t.state == TrackState.STOP) t.state = TrackState.PLAY
        }
    }
}
