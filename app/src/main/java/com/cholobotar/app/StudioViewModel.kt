package com.cholobotar.app

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StudioViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("cholobotar", Context.MODE_PRIVATE)

    val gate = VoiceGate()
    val bounce = BounceEngine()
    val tp = TeleprompterState()
    private val mic = MicCapture(gate)

    var dark by mutableStateOf(false)
        private set
    var micOn by mutableStateOf(false)
        private set
    var closedBmp by mutableStateOf<ImageBitmap?>(null)
        private set
    var openBmp by mutableStateOf<ImageBitmap?>(null)
        private set
    var mouthOpen by mutableStateOf(false)
        private set
    var meter by mutableFloatStateOf(0f)
        private set
    var tick by mutableLongStateOf(0L)
        private set
    var emote by mutableStateOf<String?>(null)
        private set
    var effects by mutableStateOf(setOf<String>())
        private set
    var background by mutableStateOf("STUDIO")
        private set
    var scriptText by mutableStateOf(DEFAULT_SCRIPT)
        private set
    var sensitivityDb by mutableFloatStateOf(-38f)
        private set

    private var emoteJob: Job? = null
    private var countdownJob: Job? = null

    init {
        dark = prefs.getBoolean("dark", false)
        scriptText = prefs.getString("script", DEFAULT_SCRIPT) ?: DEFAULT_SCRIPT
        sensitivityDb = prefs.getFloat("openDb", -38f)
        gate.openDb = sensitivityDb
        tp.wpm = prefs.getInt("wpm", 140)
        tp.fontSp = prefs.getInt("font", 30)
        tp.alpha = prefs.getFloat("alpha", 0.85f)
        tp.load(scriptText)
        viewModelScope.launch(Dispatchers.IO) {
            closedBmp = decodeSampled(File(app.filesDir, "closed.png"))
            openBmp = decodeSampled(File(app.filesDir, "open.png"))
        }
    }

    // ---- called once per frame from the UI ----
    fun frame(dt: Float, nowNanos: Long) {
        bounce.update(dt, gate.isOpen, gate.level)
        mouthOpen = gate.isOpen
        meter = gate.level
        tick = nowNanos
        tp.tick(dt) { onMarker(it) }
    }

    private fun onMarker(m: ScriptMarker) {
        when (m.kind) {
            "EMOTE" -> fireEmote(m.value)
            "BG" -> if (BACKGROUNDS.containsKey(m.value)) background = m.value
        }
    }

    // ---- avatar / mic ----
    fun setMic(on: Boolean) {
        micOn = on
        if (on) mic.start(viewModelScope) else mic.stop()
    }

    fun setSensitivity(db: Float) {
        sensitivityDb = db
        gate.openDb = db
        prefs.edit().putFloat("openDb", db).apply()
    }

    fun importImage(uri: Uri, open: Boolean) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(app.filesDir, if (open) "open.png" else "closed.png")
            var bmp: ImageBitmap? = null
            try {
                app.contentResolver.openInputStream(uri)?.use { input ->
                    f.outputStream().use { out -> input.copyTo(out) }
                }
                bmp = decodeSampled(f)
            } catch (e: Exception) {
                bmp = null
            }
            withContext(Dispatchers.Main) {
                if (bmp == null) {
                    f.delete()
                    Toast.makeText(app, "Image load hoyni, arekta PNG try koro", Toast.LENGTH_SHORT).show()
                } else if (open) {
                    openBmp = bmp
                } else {
                    closedBmp = bmp
                }
            }
        }
    }

    fun resetAvatar() {
        val dir = getApplication<Application>().filesDir
        File(dir, "closed.png").delete()
        File(dir, "open.png").delete()
        closedBmp = null
        openBmp = null
    }

    private fun decodeSampled(f: File): ImageBitmap? {
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(f.path, opts)?.asImageBitmap()
    }

    // ---- action pad ----
    fun fireEmote(name: String) {
        val e = EMOTES[name] ?: return
        emote = e
        emoteJob?.cancel()
        emoteJob = viewModelScope.launch {
            delay(2500)
            emote = null
        }
    }

    fun press(b: PadButton) {
        when (b.kind) {
            "emote" -> fireEmote(b.value)
            "bg" -> background = b.value
            "effect" -> effects = when {
                b.value == "CLEAR" -> emptySet()
                b.value in effects -> effects - b.value
                else -> effects + b.value
            }
        }
    }

    fun isActive(b: PadButton): Boolean = when (b.kind) {
        "bg" -> background == b.value
        "effect" -> b.value in effects
        else -> false
    }

    // ---- script / teleprompter ----
    fun updateScript(text: String) {
        scriptText = text
        tp.load(text)
        prefs.edit().putString("script", text).apply()
    }

    fun appendToScript(extra: String) = updateScript(scriptText + extra)

    fun setWpm(v: Int) {
        tp.wpm = v.coerceIn(60, 300)
        prefs.edit().putInt("wpm", tp.wpm).apply()
    }

    fun setFont(v: Int) {
        tp.fontSp = v.coerceIn(18, 72)
        prefs.edit().putInt("font", tp.fontSp).apply()
    }

    fun setPrompterAlpha(v: Float) {
        tp.alpha = v.coerceIn(0.2f, 1f)
        prefs.edit().putFloat("alpha", tp.alpha).apply()
    }

    fun togglePlay() {
        if (tp.playing || tp.countdown > 0) {
            countdownJob?.cancel()
            tp.countdown = 0
            tp.playing = false
            return
        }
        if (tp.script.words.isEmpty()) return
        if (tp.progress >= tp.script.words.size) tp.seekTo(0)
        countdownJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                tp.countdown = n
                delay(1000)
            }
            tp.countdown = 0
            tp.playing = true
        }
    }

    fun restart() {
        countdownJob?.cancel()
        tp.countdown = 0
        tp.playing = false
        tp.seekTo(0)
    }

    // ---- theme ----
    fun toggleDark() {
        dark = !dark
        prefs.edit().putBoolean("dark", dark).apply()
    }

    override fun onCleared() {
        mic.stop()
        super.onCleared()
    }
}
