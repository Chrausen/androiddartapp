package com.clubdarts.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clubdarts.R

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.Bold)
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)
