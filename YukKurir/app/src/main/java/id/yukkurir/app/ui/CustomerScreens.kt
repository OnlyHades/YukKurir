package id.yukkurir.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.yukkurir.app.data.Fare
import id.yukkurir.app.data.Order
import id.yukkurir.app.data.OrderStatus
import id.yukkurir.app.data.ServiceType
import id.yukkurir.app.data.YukRepository
import id.yukkurir.app.data.rupiah

private enum class CustomerTab { HOME, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRoot(onSwitchRole: () -> Unit) {
    val active by YukRepository.activeOrder.collectAsState()
    var tab by rememberSaveable { mutableStateOf(CustomerTab.HOME) }
    var formService by rememberSaveable { mutableStateOf<ServiceType?>(null) }

    val order = active
    val service = formService

    when {
        // Ada pesanan berjalan -> langsung tampilkan pelacakan
        order != null -> TrackingScreen(order)

        // Sedang mengisi formulir pesanan
        service != null -> {
            BackHandler { formService = null }
            OrderFormScreen(
                initialService = service,
                onBack = { formService = null },
                onOrdered = { formService = null },
            )
        }

        else -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("YUK KURIR", fontWeight = FontWeight.ExtraBold) },
                    actions = { TextButton(onClick = onSwitchRole) { Text("Ganti peran") } },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == CustomerTab.HOME,
                        onClick = { tab = CustomerTab.HOME },
                        icon = { Text("🏠") },
                        label = { Text("Beranda") },
                    )
                    NavigationBarItem(
                        selected = tab == CustomerTab.HISTORY,
                        onClick = { tab = CustomerTab.HISTORY },
                        icon = { Text("🧾") },
                        label = { Text("Riwayat") },
                    )
                }
            },
        ) { pad ->
            when (tab) {
                CustomerTab.HOME -> HomeTab(pad) { formService = it }
                CustomerTab.HISTORY -> HistoryTab(pad)
            }
        }
    }
}

@Composable
private fun HomeTab(pad: PaddingValues, onPickService: (ServiceType) -> Unit) {
    Column(
        modifier = Modifier
            .padding(pad)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Halo! 👋", style = MaterialTheme.typography.titleMedium)
            Text(
                "Mau kirim apa hari ini?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ServiceType.entries.forEach { s ->
                ServiceCard(s, Modifier.weight(1f)) { onPickService(s) }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tarif hemat 💸", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Mulai ${Fare.BASE_FARE.rupiah()} untuk ${Fare.BASE_KM.toInt()} km pertama, " +
                        "lalu ${Fare.PER_KM.rupiah()} per km. Bayar tunai atau YukPay.",
                )
            }
        }

        SectionTitle("Cara kerja YUK KURIR")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1. Pilih layanan, titik jemput, dan titik tujuan.")
            Text("2. Kami carikan kurir terdekat untukmu.")
            Text("3. Pantau posisi kurir di peta secara langsung.")
            Text("4. Barang sampai — beri rating untuk kurirnya.")
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceType, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(service.emoji, fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(service.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(service.subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HistoryTab(pad: PaddingValues) {
    val history by YukRepository.history.collectAsState()

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Belum ada pesanan.\nYuk, kirim barang pertamamu!",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(history, key = { it.id }) { order -> HistoryCard(order) }
        }
    }
}

@Composable
private fun HistoryCard(order: Order) {
    val statusColor = if (order.status == OrderStatus.DELIVERED) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${order.service.emoji} ${order.service.label} • ${order.id}",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(order.status.label, color = statusColor, style = MaterialTheme.typography.labelMedium)
            }
            Text("${order.pickup.name} → ${order.dropoff.name}")
            Text(order.itemNote, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(order.fare.rupiah(), fontWeight = FontWeight.Bold)
                if (order.rating > 0) Text("★".repeat(order.rating))
            }
        }
    }
}
