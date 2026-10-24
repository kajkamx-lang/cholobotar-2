package com.cholobotar.app

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

const val DEFAULT_SCRIPT =
    "Hey everyone, welcome back to the channel! [EMOTE:HAPPY] " +
    "Today we are talking about something absolutely wild. [EMOTE:SHOCKED] " +
    "Nobody saw this coming, and honestly I still cannot believe it. [BG:DARK] [EMOTE:SKULL] " +
    "Anyway, let us get into it. [BG:STUDIO] [EMOTE:COOL] " +
    "Tap this prompter to pause or play, and edit this text in the Script tab."

val EMOTES: Map<String, String> = linkedMapOf(
    "SHOCKED" to "😱", "HAPPY" to "😄", "ANGRY" to "😡",
    "SAD" to "😢", "LOVE" to "😍", "SKULL" to "💀",
    "THINK" to "🤔", "LAUGH" to "🤣", "COOL" to "😎"
)

val BACKGROUNDS: Map<String, Brush> = linkedMapOf(
    "STUDIO" to Brush.verticalGradient(listOf(Color(0xFFEAF0FF), Color(0xFFFDF2F8))),
    "DARK" to Brush.verticalGradient(listOf(Color(0xFF0B0B12), Color(0xFF1B1B2B))),
    "GREEN" to Brush.verticalGradient(listOf(Color(0xFF00B140), Color(0xFF00B140))),
    "BLUE" to Brush.verticalGradient(listOf(Color(0xFF1E3C72), Color(0xFF2A5298))),
    "SUNSET" to Brush.verticalGradient(listOf(Color(0xFFFF9A5A), Color(0xFF8E3A8E)))
)

class PadButton(
    val id: String,
    val label: String,
    val emoji: String,
    val color: Color,
    val kind: String,   // "emote" | "effect" | "bg"
    val value: String
)

class PadPage(val name: String, val buttons: List<PadButton>)

val PAD_PAGES: List<PadPage> = listOf(
    PadPage(
        "Emotes",
        EMOTES.map { (k, v) ->
            PadButton("emote_$k", k.lowercase().replaceFirstChar { it.uppercase() }, v, Color(0xFFFF6B6B), "emote", k)
        }
    ),
    PadPage(
        "Effects",
        listOf(
            PadButton("fx_horror", "Horror", "🩸", Color(0xFFB00020), "effect", "HORROR_RED"),
            PadButton("fx_spot", "Spotlight", "🔦", Color(0xFFFFB020), "effect", "SPOTLIGHT"),
            PadButton("fx_retro", "Retro", "🌸", Color(0xFFFF6EC7), "effect", "RETRO_GLOW"),
            PadButton("fx_clear", "Clear all", "🚫", Color(0xFF888888), "effect", "CLEAR")
        )
    ),
    PadPage(
        "Backgrounds",
        listOf(
            PadButton("bg_studio", "Studio", "🏢", Color(0xFF4C8DFF), "bg", "STUDIO"),
            PadButton("bg_dark", "Dark", "🌑", Color(0xFF8E6CFF), "bg", "DARK"),
            PadButton("bg_green", "Green", "🟩", Color(0xFF00B140), "bg", "GREEN"),
            PadButton("bg_blue", "Blue", "🌊", Color(0xFF2A5298), "bg", "BLUE"),
            PadButton("bg_sunset", "Sunset", "🌅", Color(0xFFFF9A5A), "bg", "SUNSET")
        )
    )
)
