package com.tuckercr.catsdogs.ui.pets

import androidx.annotation.ArrayRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.ui.LocalSouthernHemisphere
import com.tuckercr.catsdogs.ui.MoonPhase
import com.tuckercr.catsdogs.ui.moonPhaseAt
import com.tuckercr.catsdogs.ui.theme.PetBolt
import com.tuckercr.catsdogs.ui.theme.PetCatBelly
import com.tuckercr.catsdogs.ui.theme.PetCatFur
import com.tuckercr.catsdogs.ui.theme.PetCloud
import com.tuckercr.catsdogs.ui.theme.PetDogEar
import com.tuckercr.catsdogs.ui.theme.PetDogFace
import com.tuckercr.catsdogs.ui.theme.PetDogFur
import com.tuckercr.catsdogs.ui.theme.PetGround
import com.tuckercr.catsdogs.ui.theme.PetInk
import com.tuckercr.catsdogs.ui.theme.PetInkOnNight
import com.tuckercr.catsdogs.ui.theme.PetMoon
import com.tuckercr.catsdogs.ui.theme.PetMoonUnlit
import com.tuckercr.catsdogs.ui.theme.PetOutline
import com.tuckercr.catsdogs.ui.theme.PetRouter
import com.tuckercr.catsdogs.ui.theme.PetRouterLight
import com.tuckercr.catsdogs.ui.theme.PetScarf
import com.tuckercr.catsdogs.ui.theme.PetScarfAlt
import com.tuckercr.catsdogs.ui.theme.PetSkyCloudy
import com.tuckercr.catsdogs.ui.theme.PetSkyCold
import com.tuckercr.catsdogs.ui.theme.PetSkyHot
import com.tuckercr.catsdogs.ui.theme.PetSkyNight
import com.tuckercr.catsdogs.ui.theme.PetSkyRain
import com.tuckercr.catsdogs.ui.theme.PetSkySnow
import com.tuckercr.catsdogs.ui.theme.PetSkyStorm
import com.tuckercr.catsdogs.ui.theme.PetSkySunny
import com.tuckercr.catsdogs.ui.theme.PetStormCloud
import com.tuckercr.catsdogs.ui.theme.PetSun
import com.tuckercr.catsdogs.ui.theme.PetSunHot
import com.tuckercr.catsdogs.ui.theme.PetTongue
import com.tuckercr.catsdogs.ui.theme.PetUmbrella
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.cos

// The scene is authored on a 260 x 150 grid and scaled to fit.
private const val SCENE_W = 260f
private const val SCENE_H = 150f

val PetMood.skyColor: Color
    get() = when (this) {
        PetMood.SUNNY -> PetSkySunny
        PetMood.CLOUDY -> PetSkyCloudy
        PetMood.RAIN -> PetSkyRain
        PetMood.STORM -> PetSkyStorm
        PetMood.SNOW -> PetSkySnow
        PetMood.HOT -> PetSkyHot
        PetMood.COLD -> PetSkyCold
        PetMood.NIGHT -> PetSkyNight
    }

val PetMood.inkColor: Color
    get() = if (this == PetMood.NIGHT) PetInkOnNight else PetInk

/**
 * The part of the night it is, by the city's local hour. The night scene runs from sunset to
 * sunrise, so any night hour before noon is before dawn and any after noon is evening.
 */
enum class NightPart { EVENING, LATE_NIGHT, BEFORE_DAWN }

fun nightPartAt(localHour: Int): NightPart =
    when (localHour) {
        23, in 0 until 5 -> NightPart.LATE_NIGHT
        in 5 until 12 -> NightPart.BEFORE_DAWN
        else -> NightPart.EVENING
    }

@ArrayRes
private fun PetMood.captionsRes(nightPart: NightPart): Int =
    when (this) {
        PetMood.SUNNY -> R.array.pet_caption_sunny
        PetMood.CLOUDY -> R.array.pet_caption_cloudy
        PetMood.RAIN -> R.array.pet_caption_rain
        PetMood.STORM -> R.array.pet_caption_storm
        PetMood.SNOW -> R.array.pet_caption_snow
        PetMood.HOT -> R.array.pet_caption_hot
        PetMood.COLD -> R.array.pet_caption_cold
        PetMood.NIGHT -> when (nightPart) {
            NightPart.EVENING -> R.array.pet_caption_night_evening
            NightPart.LATE_NIGHT -> R.array.pet_caption_night_late
            NightPart.BEFORE_DAWN -> R.array.pet_caption_night_before_dawn
        }
    }

/**
 * One caption per mood per day, so it varies without flickering between recompositions. The day
 * and (at night) the hour are the city's, from its [utcOffsetSeconds], not the device's.
 */
@Composable
fun petCaption(
    mood: PetMood,
    utcOffsetSeconds: Int = 0,
): String {
    val local = OffsetDateTime.now(ZoneOffset.ofTotalSeconds(utcOffsetSeconds))
    val options = stringArrayResource(mood.captionsRes(nightPartAt(local.hour)))
    return options[local.dayOfYear % options.size]
}

/**
 * Placeholder illustration of the cat and dog acting out [mood]. Drawn with simple shapes so the
 * feature works end to end; swap the body of this composable for real artwork later.
 */
@Composable
fun PetScene(
    mood: PetMood,
    modifier: Modifier = Modifier,
    moonPhase: MoonPhase = moonPhaseAt(
        java.time.Instant
            .now()
            .epochSecond,
    ),
    // The offline error: the Wi-Fi router lies on its side next to Misty.
    knockedOverRouter: Boolean = false,
) {
    val description = stringResource(R.string.cd_pet_scene)
    val southernHemisphere = LocalSouthernHemisphere.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(SCENE_W / SCENE_H)
            .semantics { contentDescription = description },
    ) {
        val s = size.width / SCENE_W

        fun p(
            x: Float,
            y: Float,
        ) = Offset(x * s, y * s)

        drawSky(mood, s, ::p, moonPhase, southernHemisphere)
        drawOval(PetGround, topLeft = p(20f, 132f), size = Size(220f * s, 16f * s))
        // The pets are authored cat-left, dog-right; mirrored so Sunny (the dog) stands on the left,
        // matching the name. The sky stays put, and the ground shadow is symmetrical.
        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(size.width / 2, 0f)) {
            drawCat(mood, s, ::p)
            drawDog(mood, s, ::p)
            drawProps(mood, s, ::p)
        }
        if (knockedOverRouter) drawKnockedOverRouter(s, ::p)
    }
}

private fun DrawScope.drawKnockedOverRouter(
    s: Float,
    p: (Float, Float) -> Offset,
) {
    // Tipped back on the ground to Misty's right, antennas splayed.
    rotate(degrees = -18f, pivot = p(216f, 128f)) {
        drawLine(PetOutline, p(202f, 122f), p(196f, 104f), strokeWidth = 2.5f * s)
        drawLine(PetOutline, p(230f, 122f), p(236f, 106f), strokeWidth = 2.5f * s)
        drawRoundRect(PetRouter, p(196f, 120f), Size(40f * s, 14f * s), CornerRadius(3f * s))
        listOf(206f, 214f, 222f).forEach { x -> drawCircle(PetRouterLight, 1.8f * s, p(x, 127f)) }
    }
}

private fun DrawScope.oval(
    color: Color,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    s: Float,
) = drawOval(color, topLeft = Offset((cx - rx) * s, (cy - ry) * s), size = Size(rx * 2 * s, ry * 2 * s))

private fun DrawScope.poly(
    color: Color,
    s: Float,
    vararg pts: Float,
) {
    val path = Path().apply {
        moveTo(pts[0] * s, pts[1] * s)
        for (i in 2 until pts.size step 2) lineTo(pts[i] * s, pts[i + 1] * s)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSky(
    mood: PetMood,
    s: Float,
    p: (Float, Float) -> Offset,
    moonPhase: MoonPhase,
    southernHemisphere: Boolean,
) {
    when (mood) {
        PetMood.SUNNY -> drawCircle(PetSun, 18f * s, p(220f, 32f))
        PetMood.HOT -> drawCircle(PetSunHot, 24f * s, p(220f, 32f))
        PetMood.NIGHT -> drawMoon(moonPhase, southernHemisphere, p(215f, 30f), 16f * s)
        PetMood.CLOUDY -> cloud(PetCloud, 210f, 30f, s)
        PetMood.RAIN -> listOf(30f to 14f, 60f to 36f, 200f to 58f, 232f to 20f, 45f to 70f, 245f to 44f)
            .forEach { (x, y) -> drawRoundRect(PetUmbrella, p(x, y), Size(3f * s, 11f * s)) }
        PetMood.STORM -> {
            cloud(PetStormCloud, 200f, 28f, s)
            poly(PetBolt, s, 205f, 40f, 196f, 58f, 204f, 58f, 198f, 74f, 214f, 52f, 206f, 52f, 212f, 40f)
        }
        PetMood.SNOW, PetMood.COLD -> listOf(30f to 18f, 70f to 44f, 200f to 22f, 240f to 50f, 110f to 12f, 180f to 40f)
            .forEach { (x, y) -> drawCircle(PetCloud, 4f * s, p(x, y)) }
    }
}

/**
 * The moon at [phase]: the unlit disc, the lit half, then the terminator, an ellipse that adds to
 * the lit half (gibbous) or cuts into it (crescent). Waxing is lit on the right in the north and
 * on the left in the south.
 */
private fun DrawScope.drawMoon(
    phase: MoonPhase,
    southernHemisphere: Boolean,
    centre: Offset,
    radius: Float,
) {
    drawCircle(PetMoonUnlit, radius, centre)
    when (phase) {
        MoonPhase.NEW -> return
        MoonPhase.FULL -> {
            drawCircle(PetMoon, radius, centre)
            return
        }
        else -> Unit
    }
    val litOnRight = phase.isWaxing != southernHemisphere
    clipRect(
        left = if (litOnRight) centre.x else centre.x - radius,
        top = centre.y - radius,
        right = if (litOnRight) centre.x + radius else centre.x,
        bottom = centre.y + radius,
    ) { drawCircle(PetMoon, radius, centre) }
    val rx = abs(cos(Math.toRadians(phase.drawAngleDegrees.toDouble()))).toFloat() * radius
    drawOval(
        if (phase.isGibbous) PetMoon else PetMoonUnlit,
        topLeft = Offset(centre.x - rx, centre.y - radius),
        size = Size(rx * 2, radius * 2),
    )
}

private fun DrawScope.cloud(
    color: Color,
    cx: Float,
    cy: Float,
    s: Float,
) {
    oval(color, cx, cy + 6f, 28f, 12f, s)
    drawCircle(color, 13f * s, Offset((cx - 10f) * s, cy * s))
    drawCircle(color, 16f * s, Offset((cx + 8f) * s, (cy - 4f) * s))
}

private fun DrawScope.drawCat(
    mood: PetMood,
    s: Float,
    p: (Float, Float) -> Offset,
) {
    // A storm sends the cat low (hiding); otherwise it sits upright.
    val dy = if (mood == PetMood.STORM) 14f else 0f
    oval(PetCatFur, 95f, 112f + dy / 2, 24f, 26f - dy / 2, s)
    drawCircle(PetCatFur, 20f * s, p(95f, 80f + dy))
    poly(PetCatFur, s, 78f, 68f + dy, 82f, 50f + dy, 92f, 64f + dy)
    poly(PetCatFur, s, 98f, 64f + dy, 108f, 50f + dy, 112f, 68f + dy)
    oval(PetCatBelly, 95f, 88f + dy, 11f, 8f, s)
    oval(PetCatBelly, 95f, 118f + dy / 2, 12f, 14f - dy / 3, s)
    eyes(mood, 88f, 102f, 78f + dy, s)
}

private fun DrawScope.drawDog(
    mood: PetMood,
    s: Float,
    p: (Float, Float) -> Offset,
) {
    oval(PetDogFur, 160f, 110f, 28f, 28f, s)
    // A rim of body fur round the face, so it doesn't vanish into the sunny sky (the same colour).
    drawCircle(PetDogFur, 25.5f * s, p(160f, 76f))
    drawCircle(PetDogFace, 23f * s, p(160f, 76f))
    oval(PetDogEar, 139f, 80f, 8f, 15f, s)
    oval(PetDogEar, 181f, 80f, 8f, 15f, s)
    eyes(mood, 152f, 168f, 72f, s)
    oval(PetOutline, 160f, 82f, 5f, 3.5f, s)
    if (mood == PetMood.HOT) {
        oval(PetTongue, 160f, 94f, 4f, 7f, s)
    } else {
        val smile = Path().apply {
            moveTo(154f * s, 88f * s)
            quadraticTo(160f * s, 94f * s, 166f * s, 88f * s)
        }
        drawPath(smile, PetOutline, style = Stroke(width = 2f * s))
    }
}

/** Open eyes, or closed crescents when the pets are asleep at night. */
private fun DrawScope.eyes(
    mood: PetMood,
    lx: Float,
    rx: Float,
    y: Float,
    s: Float,
) {
    if (mood == PetMood.NIGHT) {
        listOf(lx, rx).forEach { x ->
            drawLine(PetOutline, Offset((x - 3f) * s, y * s), Offset((x + 3f) * s, y * s), strokeWidth = 2f * s)
        }
    } else {
        drawCircle(PetOutline, 2.6f * s, Offset(lx * s, y * s))
        drawCircle(PetOutline, 2.6f * s, Offset(rx * s, y * s))
    }
}

private fun DrawScope.drawProps(
    mood: PetMood,
    s: Float,
    p: (Float, Float) -> Offset,
) {
    when (mood) {
        PetMood.RAIN -> {
            val canopy = Path().apply {
                moveTo(70f * s, 56f * s)
                quadraticTo(128f * s, 0f, 190f * s, 56f * s)
                close()
            }
            drawPath(canopy, PetUmbrella)
            drawRect(PetOutline, p(128f, 54f), Size(3f * s, 42f * s))
        }
        PetMood.SUNNY -> {
            drawRoundRect(PetOutline, p(146f, 68f), Size(12f * s, 7f * s))
            drawRoundRect(PetOutline, p(162f, 68f), Size(12f * s, 7f * s))
            drawRect(PetOutline, p(157f, 70f), Size(6f * s, 2f * s))
        }
        PetMood.SNOW, PetMood.COLD -> {
            drawRoundRect(PetScarf, p(136f, 94f), Size(48f * s, 9f * s))
            drawRoundRect(PetScarfAlt, p(72f, 96f), Size(46f * s, 8f * s))
        }
        else -> Unit
    }
}
