package com.tuckercr.catsdogs.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuckercr.catsdogs.BuildConfig
import com.tuckercr.catsdogs.domain.SavedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan
import kotlin.time.Duration.Companion.seconds
import android.graphics.Canvas as AndroidCanvas

private const val ZOOM = 10
private const val TILE_PX = 256

@Composable
fun RadarCard(
    location: SavedLocation?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (location == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("No location selected")
            }
        } else if (location.latitude == null || location.longitude == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Radar unavailable\n(coordinates not available)")
            }
        } else {
            var animationFrame by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(2.seconds)
                    animationFrame = (animationFrame + 1) % 4
                }
            }

            val tileInfo = remember(location.latitude, location.longitude) {
                TileInfo.from(location.latitude, location.longitude, ZOOM)
            }

            MapTileGrid(
                tileInfo = tileInfo,
                animationFrame = animationFrame,
            )
        }
    }
}

@Composable
private fun MapTileGrid(
    tileInfo: TileInfo,
    animationFrame: Int,
) {
    val context = LocalContext.current

    var mapBitmap by remember(tileInfo) { mutableStateOf<ImageBitmap?>(null) }
    var radarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(tileInfo) {
        mapBitmap = stitchTiles(context, tileInfo) { x, y ->
            "https://tile.openstreetmap.org/$ZOOM/$x/$y.png"
        }
    }

    val layerType = if (animationFrame % 2 == 0) "clouds" else "precipitation"
    val apiKey = BuildConfig.OWM_API_KEY.trim()
    LaunchedEffect(tileInfo, layerType) {
        val loaded = stitchTiles(context, tileInfo) { x, y ->
            "https://tile.openweathermap.org/map/$layerType/$ZOOM/$x/$y.png?appid=$apiKey"
        }
        if (loaded != null) radarBitmap = loaded
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium),
    ) {
        val bitmapTotalPx = (3 * TILE_PX).toFloat()

        // Scale so that one tile fills half the canvas in the larger dimension.
        val tileDisplayPx = maxOf(size.width, size.height) / 2f
        val scale = tileDisplayPx / TILE_PX

        // Where the location falls in the stitched bitmap (row/col 0 = top-left tile).
        val locBmpX = (1 + tileInfo.subFracX.toFloat()) * TILE_PX
        val locBmpY = (1 + tileInfo.subFracY.toFloat()) * TILE_PX

        val offsetX = size.width / 2f - locBmpX * scale
        val offsetY = size.height / 2f - locBmpY * scale

        val dstW = (bitmapTotalPx * scale).toInt()
        val dstH = (bitmapTotalPx * scale).toInt()
        val dst = IntOffset(offsetX.toInt(), offsetY.toInt())
        val dstSize = IntSize(dstW, dstH)

        mapBitmap?.let { drawImage(it, dstOffset = dst, dstSize = dstSize) }
        radarBitmap?.let { drawImage(it, dstOffset = dst, dstSize = dstSize, alpha = 0.7f) }
    }
}

private suspend fun stitchTiles(
    context: android.content.Context,
    tileInfo: TileInfo,
    urlBuilder: (x: Int, y: Int) -> String,
): ImageBitmap? =
    withContext(Dispatchers.IO) {
        val sizeP = 3 * TILE_PX
        val stitched = createBitmap(sizeP, sizeP)
        val canvas = AndroidCanvas(stitched)
        val paint = Paint()

        val jobs = (-1..1).flatMap { row ->
            (-1..1).map { col ->
                async {
                    val x = tileInfo.tileX + col
                    val y = tileInfo.tileY + row
                    val bmp = loadTileBitmap(context, urlBuilder(x, y))
                    Triple(col + 1, row + 1, bmp)
                }
            }
        }

        val results = jobs.awaitAll()
        if (results.all { it.third == null }) return@withContext null

        results.forEach { (col, row, bmp) ->
            if (bmp != null) {
                canvas.drawBitmap(bmp, (col * TILE_PX).toFloat(), (row * TILE_PX).toFloat(), paint)
            }
        }

        stitched.asImageBitmap()
    }

private suspend fun loadTileBitmap(
    context: android.content.Context,
    url: String,
): Bitmap? {
    val request = ImageRequest
        .Builder(context)
        .data(url)
        .allowHardware(false)
        .build()
    val result = Coil.imageLoader(context).execute(request)
    return (result as? SuccessResult)?.drawable?.let {
        (it as? android.graphics.drawable.BitmapDrawable)?.bitmap
    }
}

/** Pre-computed tile coordinates and sub-tile fractional position for a lat/lon. */
data class TileInfo(
    val tileX: Int,
    val tileY: Int,
    /** Fractional position within the tile (0.0–1.0) in the X direction. */
    val subFracX: Double,
    /** Fractional position within the tile (0.0–1.0) in the Y direction. */
    val subFracY: Double,
) {
    companion object {
        fun from(
            lat: Double,
            lon: Double,
            zoom: Int,
        ): TileInfo {
            val n = 2.0.pow(zoom.toDouble())
            val exactX = (lon + 180.0) / 360.0 * n
            val latRad = Math.toRadians(lat)
            val exactY = (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * n
            val tileX = exactX.toInt()
            val tileY = exactY.toInt()
            return TileInfo(
                tileX = tileX,
                tileY = tileY,
                subFracX = exactX - tileX,
                subFracY = exactY - tileY,
            )
        }
    }
}
