package com.cholobotar.app

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Mic loudness -> mouth open/closed, with hysteresis so it does not flicker. */
class VoiceGate {
    @Volatile
    var openDb: Float = -38f

    @Volatile
    var holdMs: Long = 90L

    @Volatile
    var level: Float = 0f
        private set

    @Volatile
    var isOpen: Boolean = false
        private set

    private var lastLoudAt = 0L
    private val closeDb: Float get() = openDb - 6f

    fun process(pcm: ShortArray, n: Int, nowMs: Long) {
        if (n <= 0) return
        var sum = 0.0
        for (i in 0 until n) {
            val s = pcm[i] / 32768.0
            sum += s * s
        }
        val db = (20.0 * log10(sqrt(sum / n) + 1e-9)).toFloat()
        level = ((db - closeDb) / 30f).coerceIn(0f, 1f)
        if (db > openDb) {
            isOpen = true
            lastLoudAt = nowMs
        } else if (isOpen && db < closeDb && nowMs - lastLoudAt > holdMs) {
            isOpen = false
        }
    }

    fun reset() {
        isOpen = false
        level = 0f
    }
}

/** Procedural bounce + shake while speaking. Values are in dp. */
class BounceEngine(
    var amplitude: Float = 16f,
    var frequency: Float = 6.5f,
    var decay: Float = 0.82f
) {
    private var phase = 0f
    private var energy = 0f

    var offsetY: Float = 0f
        private set
    var shakeX: Float = 0f
        private set

    fun update(dt: Float, speaking: Boolean, level: Float) {
        energy = if (speaking) max(energy, max(level, 0.35f)) else energy * decay.pow(dt * 60f)
        if (energy < 0.01f) {
            energy = 0f
            offsetY = 0f
            shakeX = 0f
            return
        }
        phase += 2f * PI.toFloat() * frequency * dt
        offsetY = -abs(sin(phase)) * amplitude * energy
        shakeX = sin(phase * 3.1f) * amplitude * 0.15f * energy
    }
}

class MicCapture(private val gate: VoiceGate) {
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            val sr = 16_000
            val minBuf = AudioRecord.getMinBufferSize(
                sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return@launch
            val rec = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, sr,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
                )
            } catch (e: Exception) {
                return@launch
            }
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return@launch
            }
            val buf = ShortArray(512)
            try {
                rec.startRecording()
                while (isActive) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n > 0) gate.process(buf, n, SystemClock.elapsedRealtime()) else if (n < 0) break
                }
            } finally {
                try { rec.stop() } catch (e: Exception) { }
                rec.release()
                gate.reset()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
