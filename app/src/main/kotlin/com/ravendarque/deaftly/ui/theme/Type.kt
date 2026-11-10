package com.ravendarque.deaftly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R

private const val HEADING_OPTICAL_SIZE = 24f

private fun atkinson(weight: FontWeight) =
    Font(
        resId = R.font.atkinson_hyperlegible_next,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

private fun bricolage(weight: FontWeight) =
    Font(
        resId = R.font.bricolage_grotesque,
        weight = weight,
        variationSettings =
            FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("opsz", HEADING_OPTICAL_SIZE),
            ),
    )

val AtkinsonHyperlegibleNext =
    FontFamily(
        atkinson(FontWeight.Normal),
        atkinson(FontWeight.Medium),
        atkinson(FontWeight.Bold),
    )

val BricolageGrotesque =
    FontFamily(
        bricolage(FontWeight.Bold),
        bricolage(FontWeight.ExtraBold),
    )

private fun reading(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = AtkinsonHyperlegibleNext,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
)

private fun heading(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = BricolageGrotesque,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing,
)

val DeaftlyTypography =
    Typography(
        displayLarge = heading(57, 64, FontWeight.ExtraBold, (-0.57).sp),
        displayMedium = heading(46, 53, FontWeight.ExtraBold, (-0.46).sp),
        displaySmall = reading(26, 40, FontWeight.Medium),
        headlineLarge = heading(30, 34, FontWeight.ExtraBold, (-0.9).sp),
        headlineMedium = heading(26, 32),
        headlineSmall = heading(22, 28),
        titleLarge = heading(20, 26),
        titleMedium = reading(18, 24, FontWeight.Bold),
        titleSmall = reading(17, 24, FontWeight.Bold),
        bodyLarge = reading(19, 27),
        bodyMedium = reading(17, 24),
        bodySmall = reading(15, 22),
        labelLarge = reading(17, 22, FontWeight.Bold),
        labelMedium = reading(14, 18, FontWeight.Bold, 0.84.sp),
        labelSmall = reading(13, 16, FontWeight.Bold, 0.52.sp),
    )
