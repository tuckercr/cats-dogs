package com.tuckercr.catsdogs.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.domain.RadarTimeline
import com.tuckercr.catsdogs.domain.SavedLocation
import com.tuckercr.catsdogs.model.LoadingState
import com.tuckercr.catsdogs.model.RadarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan
import android.graphics.Canvas as AndroidCanvas

// RainViewer's public radar tiles are only served up to zoom 7; higher zooms return a
// "Zoom Level Not Supported" placeholder tile. Regional zoom is the norm for precipitation radar.
private const val ZOOM = 7
private const val TILE_PX = 256
private const val FRAME_INTERVAL_MS = 550L

@Composable
fun RadarCard(
    location: SavedLocation?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (location?.latitude == null || location.longitude == null) {
            CenteredMessage(
                text = if (location == null) {
                    stringResource(R.string.radar_no_location)
                } else {
                    stringResource(R.string.radar_unavailable_coords)
                },
            )
            return@Card
        }

        // Obtained here (not as a default arg) so the coordinate-less path above never needs Hilt.
        val viewModel: RadarViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.load() }
        val timelineState by viewModel.timeline.collectAsStateWithLifecycle()

        val tileInfo = remember(location.latitude, location.longitude) {
            TileInfo.from(location.latitude, location.longitude, ZOOM)
        }

        RadarView(
            tileInfo = tileInfo,
            timelineState = timelineState,
            onRetry = viewModel::retry,
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun RadarView(
    tileInfo: TileInfo,
    timelineState: LoadingState<RadarTimeline>,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current

    // Base OpenStreetMap layer, stitched once per location.
    var baseMap by remember(tileInfo) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(tileInfo) {
        baseMap = stitchTiles(context, tileInfo) { x, y ->
            "https://tile.openstreetmap.org/$ZOOM/$x/$y.png"
        }
    }

    val timeline = (timelineState as? LoadingState.Success)?.data
    val frames = timeline?.frames.orEmpty()

    // Radar overlays are stitched lazily per frame and cached so replays are smooth.
    val overlays = remember(tileInfo, timeline) { mutableStateMapOf<String, ImageBitmap>() }

    // Start on the most recent observed frame ("now").
    val nowIndex = remember(frames) {
        frames.indexOfLast { !it.isForecast }.coerceAtLeast(0)
    }
    var frameIndex by remember(frames) { mutableIntStateOf(nowIndex) }
    var playing by remember(frames) { mutableStateOf(true) }

    LaunchedEffect(frameIndex, tileInfo, timeline) {
        val frame = frames.getOrNull(frameIndex) ?: return@LaunchedEffect
        if (!overlays.containsKey(frame.path)) {
            stitchTiles(context, tileInfo) { x, y ->
                timeline!!.tileUrl(frame, ZOOM, x, y)
            }?.let { overlays[frame.path] = it }
        }
    }

    LaunchedEffect(playing, frames) {
        if (!playing || frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(FRAME_INTERVAL_MS)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    val currentOverlay = frames.getOrNull(frameIndex)?.let { overlays[it.path] }

    Box(modifier = Modifier.fillMaxSize()) {
        RadarCanvas(tileInfo = tileInfo, baseMap = baseMap, overlay = currentOverlay)

        // Timestamp chip (top-start)
        frames.getOrNull(frameIndex)?.let { frame ->
            OverlayChip(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = frameTimeLabel(
                        epochSeconds = frame.timeEpochSeconds,
                        isNow = frameIndex == nowIndex,
                        isForecast = frame.isForecast,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Legend (top-end)
        RadarLegend(modifier = Modifier.align(Alignment.TopEnd))

        // Status / controls (bottom)
        when (timelineState) {
            is LoadingState.Success ->
                RadarControls(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    playing = playing,
                    frameIndex = frameIndex,
                    frameCount = frames.size,
                    onTogglePlay = { playing = !playing },
                    onScrub = {
                        playing = false
                        frameIndex = it
                    },
                )

            is LoadingState.Error ->
                OverlayChip(modifier = Modifier.align(Alignment.BottomCenter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.radar_unavailable),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }

            else ->
                OverlayChip(modifier = Modifier.align(Alignment.BottomCenter)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.radar_loading),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
        }
    }
}

@Composable
private fun RadarCanvas(
    tileInfo: TileInfo,
    baseMap: ImageBitmap?,
    overlay: ImageBitmap?,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium),
    ) {
        val bitmapTotalPx = (3 * TILE_PX).toFloat()
        val tileDisplayPx = maxOf(size.width, size.height) / 2f
        val scale = tileDisplayPx / TILE_PX

        val locBmpX = (1 + tileInfo.subFracX.toFloat()) * TILE_PX
        val locBmpY = (1 + tileInfo.subFracY.toFloat()) * TILE_PX

        val offsetX = size.width / 2f - locBmpX * scale
        val offsetY = size.height / 2f - locBmpY * scale

        val dst = IntOffset(offsetX.toInt(), offsetY.toInt())
        val dstSize = IntSize((bitmapTotalPx * scale).toInt(), (bitmapTotalPx * scale).toInt())

        baseMap?.let { drawImage(it, dstOffset = dst, dstSize = dstSize) }
        overlay?.let { drawImage(it, dstOffset = dst, dstSize = dstSize, alpha = 0.8f) }
    }
}

@Composable
private fun OverlayChip(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        content()
    }
}

@Composable
private fun RadarControls(
    playing: Boolean,
    frameIndex: Int,
    frameCount: Int,
    onTogglePlay: () -> Unit,
    onScrub: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onTogglePlay) {
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.radar_pause else R.string.radar_play,
                ),
            )
        }
        Slider(
            value = frameIndex.toFloat(),
            onValueChange = { onScrub(it.roundToInt().coerceIn(0, (frameCount - 1).coerceAtLeast(0))) },
            valueRange = 0f..(frameCount - 1).coerceAtLeast(1).toFloat(),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
    }
}

@Composable
private fun RadarLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(RADAR_LEGEND_COLORS)),
        )
        Row(
            modifier = Modifier
                .width(96.dp)
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.radar_legend_light), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.radar_legend_heavy), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val RADAR_LEGEND_COLORS = listOf(
    Color(0xFF8CD9FF), // light
    Color(0xFF2E9BE6),
    Color(0xFF39C24A),
    Color(0xFFF4E04D),
    Color(0xFFF39B2E),
    Color(0xFFE24B4B), // heavy
)

private val radarTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

private fun frameTimeLabel(
    epochSeconds: Long,
    isNow: Boolean,
    isForecast: Boolean,
): String {
    val time = Instant
        .ofEpochSecond(epochSeconds)
        .atZone(ZoneId.systemDefault())
        .format(radarTimeFormatter)
    return when {
        isNow -> "Now · $time"
        isForecast -> "Forecast · $time"
        else -> time
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
