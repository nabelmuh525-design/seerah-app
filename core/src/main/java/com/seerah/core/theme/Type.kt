package com.seerah.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AmiriDisplayFamily: FontFamily = FontFamily.Serif
val NotoNaskhFamily: FontFamily = FontFamily.Serif
val NotoSansArabicFamily: FontFamily = FontFamily.SansSerif

fun buildSeerahTypography(scale: Float = 1.0f): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AmiriDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (34 * scale).sp,
        lineHeight = (46 * scale).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = AmiriDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (26 * scale).sp,
        lineHeight = (36 * scale).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AmiriDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (22 * scale).sp,
        lineHeight = (32 * scale).sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (19 * scale).sp,
        lineHeight = (27 * scale).sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16 * scale).sp,
        lineHeight = (24 * scale).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoNaskhFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * scale).sp,
        lineHeight = (28 * scale).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * scale).sp,
        lineHeight = (22 * scale).sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (13 * scale).sp,
        lineHeight = (18 * scale).sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11 * scale).sp,
        lineHeight = (16 * scale).sp
    )
)
