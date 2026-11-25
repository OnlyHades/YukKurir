package id.yukkurir.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.yukkurir.app.data.CourierJob
import id.yukkurir.app.data.Fare
import id.yukkurir.app.data.FinishedJob
import id.yukkurir.app.data.JobOffer
import id.yukkurir.app.data.JobStage
import id.yukkurir.app.data.Order
import id.yukkurir.app.data.PaymentMethod
import id.yukkurir.app.data.YukRepository
import id.yukkurir.app.data.rupiah
import id.yukkurir.app.data.routePoints
import id.yukkurir.app.ui.theme.DropoffColor
import id.yukkurir.app.ui.theme.PickupColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourierRoot(onSwitchRole: () -> Unit) {
    val state by YukRepository.courier.collectAsState()
    val job = state.job
    val offer = state.offer
    val finished = state.finished

    val target: Order? = job?.order ?: offer?.order
    val route = when {
        job != null && job.stage == JobStage.TO_PICKUP ->
            routePoints(state.pos, job.order.pickup.point) +
                routePoints(job.order.pickup.point, job.order.dropoff.point).drop(1)
        job != null -> routePoints(state.pos, job.order.dropoff.point)
        offer != null -> routePoints(offer.order.pickup.point, offer.order.dropoff.point)
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mode Kurir", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(
                        enabled = job == null,
                        onClick = {
                            YukRepository.setCourierOnline(false)
                            onSwitchRole()
                        },
                    ) { Text("Ganti peran") }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OnlineCard(online = state.online, canToggle = job == null)
            StatsRow(earnings = state.earningsToday, jobs = state.jobsToday)

            when {
                job != null -> JobCard(job)
                offer != null -> OfferCard(offer)
                state.online -> WaitingCard()
                else -> OfflineCard()
            }

            CityMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                pickup = target?.pickup?.point,
                dropoff = target?.dropoff?.point,
                courier = state.pos,
                route = route,
            )
            MapLegend()
        }
    }

    if (finished != null) {
        FinishedDialog(finished)
    }
}

@Composable
private fun OnlineCard(online: Boolean, canToggle: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (online) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (online) "Kamu sedang ONLINE" else "Kamu sedang OFFLINE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (online) "Siap menerima orderan" else "Aktifkan untuk mulai menerima orderan",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = online,
                onCheckedChange = { YukRepository.setCourierOnline(it) },
                enabled = canToggle,
            )
        }
    }
}

@Composable
private fun StatsRow(earnings: Int, jobs: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Pendapatan hari ini", earnings.rupiah(), Modifier.weight(1f))
        StatCard("Pesanan selesai", jobs.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WaitingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(16.dp))
            Text("Menunggu orderan masuk…")
        }
    }
}

@Composable
private fun OfflineCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Geser tombol di atas ke ONLINE untuk mulai menerima orderan antar barang & makanan.",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun RoutePreview(order: Order) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(PickupColor, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(order.pickup.name, fontWeight = FontWeight.Medium)
                Text(order.pickup.address, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(DropoffColor, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(order.dropoff.name, fontWeight = FontWeight.Medium)
                Text(order.dropoff.address, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OfferCard(offer: JobOffer) {
    val order = offer.order
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Orderan baru! ${order.service.emoji}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("${order.service.label} • ${order.customerName}")
            LinearProgressIndicator(
                progress = { offer.secondsLeft / YukRepository.OFFER_SECONDS.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Respon dalam ${offer.secondsLeft} detik", style = MaterialTheme.typography.labelMedium)
            RoutePreview(order)
            InfoRow("Ke lokasi jemput", "%.1f km".format(offer.distanceToPickupKm))
            InfoRow("Jarak antar", "%.1f km".format(order.distanceKm))
            InfoRow("Pembayaran", order.payment.label)
            InfoRow("Pendapatanmu", Fare.courierShare(order.fare).rupiah())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { YukRepository.rejectOffer() },
                    modifier = Modifier.weight(1f),
                ) { Text("Tolak") }
                Button(
                    onClick = { YukRepository.acceptOffer() },
                    modifier = Modifier.weight(1f),
                ) { Text("Terima") }
            }
        }
    }
}

@Composable
private fun JobCard(job: CourierJob) {
    val order = job.order
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(job.stage.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Pesanan ${order.id} • ${order.customerName}")
            RoutePreview(order)
            InfoRow("Barang", order.itemNote)
            InfoRow(
                "Pembayaran",
                if (order.payment == PaymentMethod.CASH) {
                    "Tunai ${order.fare.rupiah()}"
                } else {
                    "YukPay (sudah dibayar)"
                },
            )
            InfoRow("Pendapatanmu", Fare.courierShare(order.fare).rupiah())
            Button(
                onClick = { YukRepository.advanceJob() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(job.stage.action, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinishedDialog(finished: FinishedJob) {
    AlertDialog(
        onDismissRequest = { YukRepository.dismissFinished() },
        title = { Text("Pesanan selesai ✅") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Pendapatan bersih: ${finished.earning.rupiah()}", fontWeight = FontWeight.Bold)
                if (finished.order.payment == PaymentMethod.CASH) {
                    Text("Pelanggan membayar tunai ${finished.order.fare.rupiah()}. Pastikan uang sudah kamu terima.")
                } else {
                    Text("Pembayaran lewat YukPay, otomatis masuk ke saldo kamu.")
                }
            }
        },
        confirmButton = {
            Button(onClick = { YukRepository.dismissFinished() }) { Text("Oke") }
        },
    )
}
