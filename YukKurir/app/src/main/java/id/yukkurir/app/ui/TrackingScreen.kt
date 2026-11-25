package id.yukkurir.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.yukkurir.app.data.Courier
import id.yukkurir.app.data.Order
import id.yukkurir.app.data.OrderStatus
import id.yukkurir.app.data.PaymentMethod
import id.yukkurir.app.data.YukRepository
import id.yukkurir.app.data.rupiah
import id.yukkurir.app.data.routePoints
import id.yukkurir.app.ui.theme.Orange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(order: Order) {
    val context = LocalContext.current
    val courierPos = order.courierPos ?: order.pickup.point
    val route = when (order.status) {
        OrderStatus.TO_PICKUP ->
            routePoints(courierPos, order.pickup.point) +
                routePoints(order.pickup.point, order.dropoff.point).drop(1)
        OrderStatus.TO_DROPOFF -> routePoints(courierPos, order.dropoff.point)
        else -> routePoints(order.pickup.point, order.dropoff.point)
    }
    val canCancel = order.status == OrderStatus.SEARCHING ||
        order.status == OrderStatus.NO_COURIER ||
        order.status == OrderStatus.TO_PICKUP

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pesanan ${order.id}") }) },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CityMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                pickup = order.pickup.point,
                dropoff = order.dropoff.point,
                courier = order.courierPos,
                route = route,
            )
            MapLegend()
            StatusCard(order)

            order.courier?.let { courier ->
                CourierCard(
                    courier = courier,
                    onCall = {
                        Toast.makeText(context, "Demo: panggilan telepon belum terhubung", Toast.LENGTH_SHORT).show()
                    },
                    onChat = {
                        Toast.makeText(context, "Demo: chat belum terhubung", Toast.LENGTH_SHORT).show()
                    },
                )
            }

            DetailCard(order)

            if (canCancel) {
                OutlinedButton(
                    onClick = { YukRepository.cancelOrder() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Batalkan pesanan")
                }
            }
        }
    }

    if (order.status == OrderStatus.DELIVERED) {
        RatingDialog()
    }
}

@Composable
private fun StatusCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(order.status.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when (order.status) {
                OrderStatus.SEARCHING -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Kami sedang mencarikan kurir terdekat untukmu.")
                }
                OrderStatus.NO_COURIER -> {
                    Text("Belum ada kurir di sekitar titik jemput. Coba cari lagi sebentar lagi.")
                    Button(onClick = { YukRepository.retrySearch() }) { Text("Cari lagi") }
                }
                OrderStatus.TO_PICKUP, OrderStatus.TO_DROPOFF ->
                    Text("Estimasi tiba: ± ${order.etaMinutes} menit")
                OrderStatus.DELIVERED ->
                    Text("Barangmu sudah sampai. Terima kasih sudah memakai YUK KURIR!")
                OrderStatus.CANCELLED ->
                    Text("Pesanan dibatalkan.")
            }
        }
    }
}

@Composable
private fun CourierCard(courier: Courier, onCall: () -> Unit, onChat: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        courier.name.first().toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(courier.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${courier.vehicle} • ${courier.plate}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("⭐ %.1f".format(courier.rating), fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) { Text("📞 Telepon") }
                OutlinedButton(onClick = onChat, modifier = Modifier.weight(1f)) { Text("💬 Chat") }
            }
        }
    }
}

@Composable
private fun DetailCard(order: Order) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle("Detail pesanan")
            InfoRow("Layanan", "${order.service.emoji} ${order.service.label}")
            InfoRow("Jemput", order.pickup.name)
            InfoRow("Tujuan", order.dropoff.name)
            InfoRow("Barang", order.itemNote)
            InfoRow("Jarak", "%.1f km".format(order.distanceKm))
            InfoRow(
                "Pembayaran",
                if (order.payment == PaymentMethod.CASH) "Tunai" else "YukPay",
            )
            InfoRow("Total", order.fare.rupiah())
        }
    }
}

@Composable
private fun RatingDialog() {
    var stars by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = { YukRepository.closeOrder() },
        title = { Text("Pesanan selesai 🎉") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bagaimana layanan kurirnya?")
                Spacer(Modifier.height(8.dp))
                Row {
                    for (i in 1..5) {
                        Text(
                            text = if (i <= stars) "★" else "☆",
                            fontSize = 36.sp,
                            color = Orange,
                            modifier = Modifier
                                .clickable { stars = i }
                                .padding(4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { YukRepository.rateAndClose(stars) }) { Text("Kirim") }
        },
        dismissButton = {
            TextButton(onClick = { YukRepository.closeOrder() }) { Text("Lewati") }
        },
    )
}
