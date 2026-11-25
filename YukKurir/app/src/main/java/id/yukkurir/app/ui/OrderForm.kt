package id.yukkurir.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.yukkurir.app.data.Fare
import id.yukkurir.app.data.ItemSize
import id.yukkurir.app.data.PaymentMethod
import id.yukkurir.app.data.Place
import id.yukkurir.app.data.Point
import id.yukkurir.app.data.ServiceType
import id.yukkurir.app.data.YukRepository
import id.yukkurir.app.data.rupiah
import id.yukkurir.app.data.routePoints
import id.yukkurir.app.ui.theme.DropoffColor
import id.yukkurir.app.ui.theme.PickupColor
import kotlin.math.roundToInt

private enum class PickTarget { PICKUP, DROPOFF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    initialService: ServiceType,
    onBack: () -> Unit,
    onOrdered: () -> Unit,
) {
    var service by remember { mutableStateOf(initialService) }
    var pickup by remember { mutableStateOf<Place?>(null) }
    var dropoff by remember { mutableStateOf<Place?>(null) }
    var note by remember { mutableStateOf("") }
    var size by remember { mutableStateOf(ItemSize.SMALL) }
    var weight by remember { mutableFloatStateOf(1f) }
    var payment by remember { mutableStateOf(PaymentMethod.CASH) }
    var picking by remember { mutableStateOf<PickTarget?>(null) }

    val p = pickup
    val d = dropoff
    val isFood = service == ServiceType.FOOD
    val effectiveSize = if (isFood) ItemSize.SMALL else size
    val effectiveWeight = if (isFood) 1f else weight
    val distance = if (p != null && d != null) p.point.roadDistanceTo(d.point) else 0f
    val fare = if (p != null && d != null) Fare.estimate(distance, service, effectiveSize, effectiveWeight) else 0
    val samePlace = p != null && d != null && p.id == d.id
    val canOrder = p != null && d != null && !samePlace && note.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesan Kurir") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Estimasi tarif", style = MaterialTheme.typography.labelMedium)
                            Text(
                                if (fare > 0) fare.rupiah() else "-",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (distance > 0f) {
                            Text("%.1f km".format(distance), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Button(
                        onClick = {
                            if (p != null && d != null) {
                                YukRepository.placeOrder(
                                    service = service,
                                    pickup = p,
                                    dropoff = d,
                                    note = note,
                                    weightKg = effectiveWeight,
                                    size = effectiveSize,
                                    payment = payment,
                                )
                                onOrdered()
                            }
                        },
                        enabled = canOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text("Cari Kurir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceType.entries.forEach { s ->
                    FilterChip(
                        selected = service == s,
                        onClick = { service = s },
                        label = { Text("${s.emoji} ${s.label}") },
                    )
                }
            }

            SectionTitle("Rute")
            PlaceField("Titik jemput", pickup, PickupColor) { picking = PickTarget.PICKUP }
            PlaceField("Titik tujuan", dropoff, DropoffColor) { picking = PickTarget.DROPOFF }
            if (samePlace) {
                Text(
                    "Titik jemput dan tujuan tidak boleh sama.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CityMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                pickup = p?.point,
                dropoff = d?.point,
                landmarks = YukRepository.places.map { it.point },
                route = if (p != null && d != null) routePoints(p.point, d.point) else emptyList(),
            )

            SectionTitle(if (isFood) "Pesanan makanan" else "Detail barang")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isFood) "Pesanan (mis. 2 nasi goreng)" else "Isi paket (mis. dokumen, baju)") },
            )

            if (!isFood) {
                Text("Ukuran", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemSize.entries.forEach { s ->
                        FilterChip(
                            selected = size == s,
                            onClick = { size = s },
                            label = { Text(s.label) },
                        )
                    }
                }
                Text(
                    size.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Perkiraan berat: %.1f kg".format(weight), fontWeight = FontWeight.Medium)
                Slider(
                    value = weight,
                    onValueChange = { weight = (it * 2).roundToInt() / 2f },
                    valueRange = 0.5f..20f,
                    steps = 38,
                )
                if (weight > Fare.HEAVY_THRESHOLD_KG) {
                    Text(
                        "Ada biaya tambahan ${Fare.HEAVY_PER_KG.rupiah()} per kg di atas ${Fare.HEAVY_THRESHOLD_KG.toInt()} kg.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle("Pembayaran")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { m ->
                    FilterChip(
                        selected = payment == m,
                        onClick = { payment = m },
                        label = { Text(m.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    picking?.let { target ->
        PlacePickerDialog(
            title = if (target == PickTarget.PICKUP) "Pilih titik jemput" else "Pilih titik tujuan",
            onDismiss = { picking = null },
            onPicked = { place ->
                if (target == PickTarget.PICKUP) pickup = place else dropoff = place
                picking = null
            },
        )
    }
}

@Composable
private fun PlaceField(label: String, place: Place?, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(place?.name ?: "Pilih lokasi", fontWeight = FontWeight.Medium)
                if (place != null) {
                    Text(
                        place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("›", fontSize = 22.sp)
        }
    }
}

@Composable
private fun PlacePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onPicked: (Place) -> Unit,
) {
    var candidate by remember { mutableStateOf<Place?>(null) }
    val places = YukRepository.places

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }

                CityMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    highlight = candidate?.point,
                    landmarks = places.map { it.point },
                    onTap = { pt -> candidate = snapToPlace(pt, places) },
                )
                Text(
                    "Ketuk peta atau pilih dari daftar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(places, key = { it.id }) { place ->
                        val selected = candidate?.id == place.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                )
                                .clickable { candidate = place }
                                .padding(12.dp),
                        ) {
                            Text(place.name, fontWeight = FontWeight.Medium)
                            Text(place.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                val chosen = candidate
                Button(
                    onClick = { if (chosen != null) onPicked(chosen) },
                    enabled = chosen != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (chosen != null) "Pilih: ${chosen.name}" else "Pilih lokasi")
                }
            }
        }
    }
}

/** Menempel ke landmark terdekat (maks 0,7 km); kalau jauh, buat titik bebas di peta. */
private fun snapToPlace(pt: Point, places: List<Place>): Place {
    val nearest = places.minByOrNull { it.point.distanceTo(pt) }
    if (nearest != null && nearest.point.distanceTo(pt) <= 0.7f) return nearest

    val x = pt.x.coerceIn(0f, MAP_KM)
    val y = pt.y.coerceIn(0f, MAP_KM)
    val label = "%.1f, %.1f".format(x, y)
    return Place(
        id = "pin-$label",
        name = "Titik di peta",
        address = "Koordinat $label km",
        point = Point(x, y),
    )
}
