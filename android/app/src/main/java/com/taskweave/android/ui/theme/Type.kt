@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.taskweave.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.taskweave.android.R

private fun wght(w: Int) = FontVariation.Settings(FontVariation.weight(w))

/** Body copy + headings — the app shell puts this on the root. */
val Serif = FontFamily(
    Font(R.font.playfair_variable, FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.playfair_variable, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.playfair_variable, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.playfair_variable, FontWeight.Bold, variationSettings = wght(700)),
    Font(R.font.playfair_variable, FontWeight.Black, variationSettings = wght(900)),
    Font(R.font.playfair_italic_variable, FontWeight.Normal, FontStyle.Italic, variationSettings = wght(400)),
    Font(R.font.playfair_italic_variable, FontWeight.Bold, FontStyle.Italic, variationSettings = wght(700)),
)

/** UI chrome — labels, buttons, badges, meta. Almost always uppercase + wide tracking. */
val Sans = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.inter_variable, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.inter_variable, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.inter_variable, FontWeight.Bold, variationSettings = wght(700)),
    Font(R.font.inter_variable, FontWeight.Black, variationSettings = wght(900)),
    Font(R.font.inter_italic_variable, FontWeight.Normal, FontStyle.Italic, variationSettings = wght(400)),
    Font(R.font.inter_italic_variable, FontWeight.Bold, FontStyle.Italic, variationSettings = wght(700)),
)

private val serifBase = TextStyle(fontFamily = Serif)

val TaskWeaveTypography = Typography(
    displayLarge = serifBase.copy(fontSize = 44.sp, fontWeight = FontWeight.Black, lineHeight = 48.sp),
    displayMedium = serifBase.copy(fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp),
    displaySmall = serifBase.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    headlineLarge = serifBase.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = serifBase.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    headlineSmall = serifBase.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleLarge = serifBase.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = serifBase.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = serifBase.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = serifBase.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = serifBase.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = serifBase.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
)
