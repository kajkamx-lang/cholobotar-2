package com.cholobotar.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ───────────────────────── STUDIO ─────────────────────────

@Composable
fun StudioScreen(vm: StudioViewModel, onToggleMic: () -> Unit) {
    var showPrompter by rememberSaveable { mutableStateOf(true) }
    val bgName = if (vm.background == "STUDIO" && vm.dark) "DARK" else vm.background

    CompositionLocalProvider(LocalGlass provides StudioGlass) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BACKGROUNDS[bgName] ?: BACKGROUNDS.getValue("STUDIO"))
        ) {
            AvatarView(
                vm,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp)
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
            )
            EffectsOverlay(vm.effects)

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioTopBar(
                    vm = vm,
                    showPrompter = showPrompter,
                    onTogglePrompter = { showPrompter = !showPrompter },
                    onToggleMic = onToggleMic
                )
                if (showPrompter) {
                    TeleprompterView(
                        vm.tp,
                        onTap = { vm.togglePlay() },
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                    PrompterControls(vm)
                }
                Spacer(Modifier.weight(1f))
                PadDock(vm)
            }
        }
    }
}

@Composable
fun StudioTopBar(
    vm: StudioViewModel,
    showPrompter: Boolean,
    onTogglePrompter: () -> Unit,
    onToggleMic: () -> Unit
) {
    val g = LocalGlass.current
    val on = vm.micOn
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(g.panel)
                .border(1.dp, if (on) Color(0xFFFF4D4D) else g.border, shape)
                .clickable(onClick = onToggleMic)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (on) Color(0xFFFF4D4D) else g.subtext)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (on) "LIVE" else "MIC OFF",
                color = g.text, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        GlassChip("Prompter", selected = showPrompter, onClick = onTogglePrompter)
        GlassIconButton(if (vm.dark) "☀" else "☾", onClick = { vm.toggleDark() })
    }
}

@Composable
fun PrompterControls(vm: StudioViewModel) {
    val g = LocalGlass.current
    val running = vm.tp.playing || vm.tp.countdown > 0
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlayPauseButton(running, onClick = { vm.togglePlay() })
            GlassIconButton("↺", diameter = 38.dp, onClick = { vm.restart() })
            Spacer(Modifier.weight(1f))
            GlassIconButton("−", diameter = 36.dp, onClick = { vm.setWpm(vm.tp.wpm - 10) })
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp)) {
                Text("${vm.tp.wpm}", color = g.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("WPM", color = g.subtext, fontSize = 9.sp)
            }
            GlassIconButton("+", diameter = 36.dp, onClick = { vm.setWpm(vm.tp.wpm + 10) })
            Spacer(Modifier.weight(1f))
            GlassIconButton("A−", diameter = 36.dp, onClick = { vm.setFont(vm.tp.fontSp - 2) })
            GlassIconButton("A+", diameter = 36.dp, onClick = { vm.setFont(vm.tp.fontSp + 2) })
        }
    }
}

@Composable
fun PadDock(vm: StudioViewModel) {
    val g = LocalGlass.current
    val haptics = LocalHapticFeedback.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(0) }

    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ACTION PAD", color = g.subtext, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                )
                Spacer(Modifier.weight(1f))
                Text(if (expanded) "Less ▾" else "More ▴", color = g.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!expanded) {
                val quick = PAD_PAGES[0].buttons.take(5)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { b ->
                        PadKey(b, vm.isActive(b), false, Modifier.weight(1f).height(56.dp)) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.press(b)
                        }
                    }
                }
            } else {
                Segmented(PAD_PAGES.map { it.name }, page, { page = it })
                PAD_PAGES[page].buttons.chunked(3).forEach { rowButtons ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowButtons.forEach { b ->
                            PadKey(b, vm.isActive(b), true, Modifier.weight(1f).height(66.dp)) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.press(b)
                            }
                        }
                        repeat(3 - rowButtons.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun PadKey(b: PadButton, on: Boolean, showLabel: Boolean, modifier: Modifier, onPress: () -> Unit) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        b.color.copy(alpha = if (on) 0.60f else 0.30f),
                        b.color.copy(alpha = if (on) 0.30f else 0.10f)
                    )
                )
            )
            .border(if (on) 2.dp else 1.dp, if (on) b.color else g.border, shape)
            .clickable(onClick = onPress),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(b.emoji, fontSize = if (showLabel) 24.sp else 26.sp)
        if (showLabel) {
            Text(b.label, color = g.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AvatarView(vm: StudioViewModel, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    Box(
        modifier.graphicsLayer {
            vm.tick // read per frame so the layer redraws
            translationY = vm.bounce.offsetY * density
            translationX = vm.bounce.shakeX * density
        },
        contentAlignment = Alignment.Center
    ) {
        val img: ImageBitmap? =
            if (vm.mouthOpen) (vm.openBmp ?: vm.closedBmp) else (vm.closedBmp ?: vm.openBmp)
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DefaultAvatar(vm.mouthOpen)
        }
        val e = vm.emote
        if (e != null) {
            Text(e, fontSize = 64.sp, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun DefaultAvatar(open: Boolean) {
    Canvas(Modifier.size(240.dp)) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(Color(0xFFFFC857), r, c)
        drawCircle(Color(0x33FF6B6B), r * 0.14f, Offset(c.x - r * 0.52f, c.y + r * 0.12f))
        drawCircle(Color(0x33FF6B6B), r * 0.14f, Offset(c.x + r * 0.52f, c.y + r * 0.12f))
        drawCircle(Color(0xFF2B2B2B), r * 0.09f, Offset(c.x - r * 0.35f, c.y - r * 0.2f))
        drawCircle(Color(0xFF2B2B2B), r * 0.09f, Offset(c.x + r * 0.35f, c.y - r * 0.2f))
        if (open) {
            drawOval(
                Color(0xFF7A1F1F),
                topLeft = Offset(c.x - r * 0.22f, c.y + r * 0.2f),
                size = Size(r * 0.44f, r * 0.42f)
            )
        } else {
            drawLine(
                Color(0xFF2B2B2B),
                Offset(c.x - r * 0.28f, c.y + r * 0.35f),
                Offset(c.x + r * 0.28f, c.y + r * 0.35f),
                strokeWidth = r * 0.06f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun EffectsOverlay(effects: Set<String>) {
    if ("RETRO_GLOW" in effects) {
        Box(Modifier.fillMaxSize().background(Color(0x33FF6EC7)))
    }
    if ("HORROR_RED" in effects) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color(0x33B00020), Color(0xCC4A0000)))
            )
        )
    }
    if ("SPOTLIGHT" in effects) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color.Transparent, Color(0xDD000000)))
            )
        )
    }
}

// ───────────────────────── SCRIPT ─────────────────────────

@Composable
fun ScriptScreen(vm: StudioViewModel) {
    val g = LocalGlass.current
    Backdrop(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Script", color = g.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Marker likhle prompter-e dekha jabe na, kintu oi shobder somoy emote ba background auto change hobe.",
                color = g.subtext, fontSize = 13.sp
            )

            SectionLabel("Insert marker")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMOTES.keys.forEach { k ->
                    GlassChip("${EMOTES[k]} $k", onClick = { vm.appendToScript(" [EMOTE:$k] ") })
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BACKGROUNDS.keys.forEach { k ->
                    GlassChip("BG $k", onClick = { vm.appendToScript(" [BG:$k] ") })
                }
            }

            SectionLabel("Your script")
            GlassPanel(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = vm.scriptText,
                    onValueChange = { vm.updateScript(it) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = g.text,
                        unfocusedTextColor = g.text,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = g.accent
                    )
                )
            }
            val words = vm.tp.script.words.size
            val mins = words / vm.tp.wpm.toFloat()
            Text(
                "$words words • about ${"%.1f".format(mins)} min at ${vm.tp.wpm} WPM",
                color = g.subtext, fontSize = 12.sp
            )

            SectionLabel("Prompter")
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabeledSlider("Speed: ${vm.tp.wpm} WPM", vm.tp.wpm.toFloat(), 60f..300f) { vm.setWpm(it.roundToInt()) }
                    LabeledSlider("Font size: ${vm.tp.fontSp} sp", vm.tp.fontSp.toFloat(), 18f..72f) { vm.setFont(it.roundToInt()) }
                    LabeledSlider("Background opacity", vm.tp.alpha, 0.2f..1f) { vm.setPrompterAlpha(it) }
                    Spacer(Modifier.height(4.dp))
                    Segmented(listOf("Normal", "Mirror"), if (vm.tp.mirror) 1 else 0, { vm.tp.mirror = (it == 1) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val g = LocalGlass.current
    Column {
        Text(label, color = g.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

// ───────────────────────── SETUP ─────────────────────────

@Composable
fun SetupScreen(
    vm: StudioViewModel,
    onToggleMic: () -> Unit,
    onPickClosed: () -> Unit,
    onPickOpen: () -> Unit
) {
    val g = LocalGlass.current
    Backdrop(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Setup", color = g.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)

            SectionLabel("Avatar")
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Mouth-closed ar mouth-open PNG choose koro. Choose na korle default face dekhabe.",
                        color = g.subtext, fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PngSlot("Closed", vm.closedBmp, onPickClosed, Modifier.weight(1f))
                        PngSlot("Open", vm.openBmp, onPickOpen, Modifier.weight(1f))
                    }
                    PrimaryButton("Reset to default face", filled = false, onClick = { vm.resetAvatar() })
                }
            }

            SectionLabel("Microphone")
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        if (vm.micOn) "Stop mic" else "Start mic",
                        filled = !vm.micOn,
                        onClick = onToggleMic
                    )
                    LinearProgressIndicator(
                        progress = { vm.meter },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = g.accent,
                        trackColor = g.border
                    )
                    Text(
                        if (vm.mouthOpen) "Mouth: OPEN" else "Mouth: closed",
                        color = g.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    LabeledSlider(
                        "Sensitivity: ${vm.sensitivityDb.roundToInt()} dB",
                        vm.sensitivityDb, -60f..-20f
                    ) { vm.setSensitivity(it) }
                    Text(
                        "Kotha bolo, meter ar mouth dekho. Mouth na khulle slider bam-e (kom dB) nao.",
                        color = g.subtext, fontSize = 12.sp
                    )
                }
            }

            SectionLabel("Appearance")
            Segmented(listOf("Light Studio", "AMOLED Dark"), if (vm.dark) 1 else 0, {
                if ((it == 1) != vm.dark) vm.toggleDark()
            })

            SectionLabel("About")
            Text(
                "Cholobotar v0.2 • Video record korte phone-er built-in screen recorder use koro. Built-in export porer phase-e asbe.",
                color = g.subtext, fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PngSlot(label: String, bmp: ImageBitmap?, onPick: () -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(g.panel)
                .border(1.dp, g.border, shape)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp, contentDescription = label,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("+", color = g.accent, fontSize = 34.sp, fontWeight = FontWeight.Light)
                    Text("Pick PNG", color = g.subtext, fontSize = 11.sp)
                }
            }
        }
        Text(label, color = g.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
