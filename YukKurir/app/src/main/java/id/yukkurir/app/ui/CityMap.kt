package id.yukkurir.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import id.yukkurir.app.data.Point
import id.yukkurir.app.ui.theme.CourierColor
import id.yukkurir.app.ui.theme.DropoffColor
import id.yukkurir.app.ui.theme.HighlightColor
import id.yukkurir.app.ui.theme.MapBackground
import id.yukkurir.app.ui.theme.MapLandmark
import id.yukkurir.app.ui.theme.MapPark
import id.yukkurir.app.ui.theme.MapRoad
import id.yukkurir.app.ui.theme.MapWater
import id.yukkurir.app.ui.theme.PickupColor
import id.yukkurir.app.ui.theme.RouteColor

/** Lebar & tinggi peta demo dalam km. */
const val MAP_KM = 10f

/**
 * Peta kota sederhana yang digambar dengan Canvas (tanpa API key).
 * Untuk produksi, ganti dengan Google Maps Compose / MapLibre dan pakai koordinat GPS asli.
 */
@Composable
fun CityMap(
    modifier: Modifier = Modifier,
    pickup: Point? = null,
    dropoff: Point? = null,
    courier: Point? = null,
    highlight: Point? = null,
    landmarks: List<Point> = emptyList(),
    route: List<Point> = emptyList(),
    onTap: ((Point) -> Unit)? = null,
) {
    val tapModifier = if (onTap != null) {
        Modifier.pointerInput(onTap) {
            detectTapGestures { o ->
                onTap(Point(o.x / size.width * MAP_KM, o.y / size.height * MAP_KM))
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MapBackground)
            .then(tapModifier),
    ) {
        val sx = size.width / MAP_KM
        val sy = size.height / MAP_KM
        fun px(p: Point) = Offset(p.x * sx, p.y * sy)

        // Taman & sungai
        drawRect(MapPark, topLeft = Offset(6.8f * sx, 7.2f * sy), size = Size(2.4f * sx, 1.8f * sy))
        val river = Path().apply {
            moveTo(0f, 6.4f * sy)
            cubicTo(2.5f * sx, 5.6f * sy, 4.5f * sx, 7.4f * sy, 6.8f * sx, 6.6f * sy)
            cubicTo(8.2f * sx, 6.1f * sy, 9.2f * sx, 6.6f * sy, 10f * sx, 6.2f * sy)
        }
        drawPath(river, MapWater, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))

        // Jalan: grid tiap 1 km, jalan utama tiap 5 km
        for (i in 0..MAP_KM.toInt()) {
            val width = if (i % 5 == 0) 6.dp.toPx() else 2.5.dp.toPx()
            drawLine(MapRoad, Offset(i * sx, 0f), Offset(i * sx, size.height), strokeWidth = width)
            drawLine(MapRoad, Offset(0f, i * sy), Offset(size.width, i * sy), strokeWidth = width)
        }

        // Landmark
        val half = 4.dp.toPx()
        landmarks.forEach { p ->
            drawRect(MapLandmark, topLeft = px(p) - Offset(half, half), size = Size(half * 2, half * 2))
        }

        // Rute (garis putus-putus)
        if (route.size >= 2) {
            val path = Path().apply {
                val start = px(route.first())
                moveTo(start.x, start.y)
                route.drop(1).forEach { p ->
                    val o = px(p)
                    lineTo(o.x, o.y)
                }
            }
            drawPath(
                path,
                RouteColor,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 14f)),
                ),
            )
        }

        // Sorotan pilihan
        highlight?.let { p ->
            drawCircle(HighlightColor.copy(alpha = 0.25f), 14.dp.toPx(), px(p))
            drawCircle(HighlightColor, 14.dp.toPx(), px(p), style = Stroke(width = 3.dp.toPx()))
        }

        fun marker(p: Point, color: Color, radiusDp: Float) {
            val c = px(p)
            val r = radiusDp.dp.toPx()
            drawCircle(Color.White, r + 2.dp.toPx(), c)
            drawCircle(color, r, c)
            drawCircle(Color.White, r * 0.35f, c)
        }

        pickup?.let { marker(it, PickupColor, 8f) }
        dropoff?.let { marker(it, DropoffColor, 8f) }
        courier?.let { marker(it, CourierColor, 9f) }
    }
}
