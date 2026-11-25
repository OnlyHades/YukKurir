package id.yukkurir.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF0B7A75)
val Orange = Color(0xFFFF7A00)

// Warna penanda & peta
val PickupColor = Color(0xFF2E7D32)
val DropoffColor = Color(0xFFD84315)
val CourierColor = Color(0xFF1565C0)
val RouteColor = Color(0xFF0B7A75)
val HighlightColor = Orange
val MapBackground = Color(0xFFE6EFEC)
val MapRoad = Color(0xFFFFFFFF)
val MapPark = Color(0xFFCDE6C8)
val MapWater = Color(0xFFBFDDF2)
val MapLandmark = Color(0xFF9AA9A6)

private val LightScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFEC),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3C9),
    onSecondaryContainer = Color(0xFF3A1D00),
    background = Color(0xFFF6FAF9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3EDEC),
)

@Composable
fun YukKurirTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}
