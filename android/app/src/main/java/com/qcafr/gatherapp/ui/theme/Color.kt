package com.qcafr.gatherapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Accent colors (shared across modes) ────────────────────────────
val PrimaryIndigo       = Color(0xFF6366F1)
val PrimaryIndigoHover  = Color(0xFF4F46E5)
val AccentEmerald       = Color(0xFF10B981)
val AccentAmber         = Color(0xFFF59E0B)
val AccentCyan          = Color(0xFF06B6D4)
val AccentDanger        = Color(0xFFEF4444)

// ── Dark theme palette ──────────────────────────────────────────────────────
val DarkBgPrimary   = Color(0xFF0B0F19)
val DarkBgSecondary = Color(0xFF111827)
val DarkBgCard      = Color(0xFF172033)
val DarkBorderSubtle= Color(0x26FFFFFF)
val DarkTextWhite   = Color(0xFFF8FAFC)
val DarkTextMuted   = Color(0xFF94A3B8)
val DarkTextDim     = Color(0xFF64748B)

// ── Light theme palette ─────────────────────────────────────────────────────
val LightBgPrimary   = Color(0xFFF8FAFC)
val LightBgSecondary = Color(0xFFF1F5F9)
val LightBgCard      = Color(0xFFFFFFFF)
val LightBorderSubtle= Color(0x1F0F172A)
val LightTextWhite   = Color(0xFF0F172A)   // high contrast slate 900
val LightTextMuted   = Color(0xFF475569)   // slate 600
val LightTextDim     = Color(0xFF94A3B8)   // slate 400

// ── Runtime tokens (updated by AppTheme wrapper) ────────────────────────────
// These are read all over the UI; we keep them as top-level vars that are
// replaced by the themed values via CompositionLocal – see Theme.kt
var BgPrimary    = DarkBgPrimary
var BgSecondary  = DarkBgSecondary
var BgCard       = DarkBgCard
var BorderSubtle = DarkBorderSubtle
var TextWhite    = DarkTextWhite
var TextMuted    = DarkTextMuted
var TextDim      = DarkTextDim

