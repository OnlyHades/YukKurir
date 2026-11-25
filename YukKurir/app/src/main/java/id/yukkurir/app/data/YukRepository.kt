package id.yukkurir.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.random.Random

/**
 * "Server" tiruan yang berjalan di dalam aplikasi supaya seluruh alur bisa dicoba tanpa backend.
 *
 * Untuk produksi, ganti isi class ini dengan pemanggilan ke backend sungguhan
 * (REST/WebSocket/Firebase) — API publiknya (StateFlow + fungsi aksi) bisa dipertahankan
 * sehingga layar UI tidak perlu diubah.
 */
object YukRepository {

    const val OFFER_SECONDS = 15

    private const val TICK_MS = 350L
    private const val STEP_KM = 0.15f
    private const val MAX_PICKUP_KM = 12f
    private const val KM_PER_MINUTE = 0.42f // ± 25 km/jam

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val places: List<Place> = listOf(
        Place("p1", "Pasar Sentral", "Jl. Pasar Raya No. 1", Point(1.5f, 2.0f)),
        Place("p2", "Alun-alun Kota", "Jl. Merdeka No. 10", Point(5.0f, 5.0f)),
        Place("p3", "Terminal Utara", "Jl. Lintas Utara No. 5", Point(6.5f, 1.0f)),
        Place("p4", "Kampus Merdeka", "Jl. Pendidikan No. 22", Point(8.5f, 3.5f)),
        Place("p5", "RS Sehat Sentosa", "Jl. Kesehatan No. 8", Point(3.0f, 7.5f)),
        Place("p6", "Mall Nusantara", "Jl. Niaga No. 100", Point(7.0f, 6.5f)),
        Place("p7", "Perumahan Griya Asri", "Jl. Melati Blok C-12", Point(1.5f, 8.5f)),
        Place("p8", "Stasiun Kota", "Jl. Kereta Api No. 3", Point(4.0f, 3.0f)),
        Place("p9", "Warung Makan Bu Tini", "Jl. Kenanga No. 17", Point(2.5f, 5.5f)),
        Place("p10", "Kantor Pos Pusat", "Jl. Pos No. 2", Point(6.0f, 8.5f)),
    )

    /** Armada kurir tiruan yang dipakai sisi pelanggan. */
    private val fleet: List<Courier> = listOf(
        Courier("c1", "Budi Santoso", "B 4521 KLM", "Motor + box", 4.9f, Point(2.0f, 3.5f)),
        Courier("c2", "Agus Prasetyo", "D 7788 QRS", "Motor + box", 4.8f, Point(6.0f, 4.0f)),
        Courier("c3", "Dewi Lestari", "L 3310 TUV", "Motor + box", 4.9f, Point(3.5f, 6.5f)),
        Courier("c4", "Rizky Maulana", "N 9042 ABC", "Motor + box", 4.7f, Point(7.5f, 7.5f)),
        Courier("c5", "Siti Aminah", "H 1265 XYZ", "Motor + box", 5.0f, Point(5.0f, 2.0f)),
    )

    private var nextId = 1001

    // ===================== SISI PELANGGAN =====================

    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder.asStateFlow()

    private val _history = MutableStateFlow<List<Order>>(emptyList())
    val history: StateFlow<List<Order>> = _history.asStateFlow()

    private var orderJob: Job? = null

    fun placeOrder(
        service: ServiceType,
        pickup: Place,
        dropoff: Place,
        note: String,
        weightKg: Float,
        size: ItemSize,
        payment: PaymentMethod,
    ) {
        if (_activeOrder.value != null) return
        val distance = pickup.point.roadDistanceTo(dropoff.point)
        val order = Order(
            id = "YK-${nextId++}",
            service = service,
            pickup = pickup,
            dropoff = dropoff,
            itemNote = note.trim(),
            weightKg = weightKg,
            size = size,
            payment = payment,
            distanceKm = distance,
            fare = Fare.estimate(distance, service, size, weightKg),
        )
        _activeOrder.value = order
        orderJob?.cancel()
        orderJob = scope.launch { runCustomerOrder(order) }
    }

    fun cancelOrder() {
        val order = _activeOrder.value ?: return
        if (order.status == OrderStatus.DELIVERED) return
        orderJob?.cancel()
        _history.update { listOf(order.copy(status = OrderStatus.CANCELLED)) + it }
        _activeOrder.value = null
    }

    fun retrySearch() {
        val order = _activeOrder.value ?: return
        if (order.status != OrderStatus.NO_COURIER) return
        orderJob?.cancel()
        orderJob = scope.launch { runCustomerOrder(order) }
    }

    fun rateAndClose(stars: Int) {
        val order = _activeOrder.value ?: return
        _history.update { list ->
            list.map { if (it.id == order.id) it.copy(rating = stars) else it }
        }
        _activeOrder.value = null
    }

    fun closeOrder() {
        _activeOrder.value = null
    }

    private fun updateOrder(block: (Order) -> Order) {
        _activeOrder.update { current -> current?.let(block) }
    }

    private suspend fun runCustomerOrder(order: Order) {
        updateOrder { it.copy(status = OrderStatus.SEARCHING, courier = null, courierPos = null) }
        delay(2500)

        // Matching: kurir terdekat dari titik jemput
        val courier = fleet.minByOrNull { it.pos.roadDistanceTo(order.pickup.point) }
        if (courier == null || courier.pos.roadDistanceTo(order.pickup.point) > MAX_PICKUP_KM) {
            updateOrder { it.copy(status = OrderStatus.NO_COURIER) }
            return
        }

        updateOrder {
            it.copy(
                status = OrderStatus.TO_PICKUP,
                courier = courier,
                courierPos = courier.pos,
                etaMinutes = eta(courier.pos.roadDistanceTo(order.pickup.point)),
            )
        }
        travel(courier.pos, order.pickup.point) { pos, remainingKm ->
            updateOrder { it.copy(courierPos = pos, etaMinutes = eta(remainingKm)) }
        }

        delay(1500) // kurir mengambil barang
        updateOrder {
            it.copy(
                status = OrderStatus.TO_DROPOFF,
                courierPos = order.pickup.point,
                etaMinutes = eta(order.distanceKm),
            )
        }
        travel(order.pickup.point, order.dropoff.point) { pos, remainingKm ->
            updateOrder { it.copy(courierPos = pos, etaMinutes = eta(remainingKm)) }
        }

        updateOrder {
            it.copy(status = OrderStatus.DELIVERED, courierPos = order.dropoff.point, etaMinutes = 0)
        }
        val done = _activeOrder.value ?: return
        _history.update { listOf(done) + it }
    }

    // ===================== SISI KURIR =====================

    private val _courier = MutableStateFlow(CourierState())
    val courier: StateFlow<CourierState> = _courier.asStateFlow()

    private var offerLoopJob: Job? = null
    private var countdownJob: Job? = null

    fun setCourierOnline(online: Boolean) {
        val state = _courier.value
        if (online == state.online) return
        if (!online && state.job != null) return // tidak bisa offline saat mengantar
        countdownJob?.cancel()
        offerLoopJob?.cancel()
        _courier.update { it.copy(online = online, offer = null) }
        if (online) {
            offerLoopJob = scope.launch { generateOffers() }
        }
    }

    fun acceptOffer() {
        val offer = _courier.value.offer ?: return
        countdownJob?.cancel()
        _courier.update {
            it.copy(
                offer = null,
                job = CourierJob(offer.order.copy(status = OrderStatus.TO_PICKUP), JobStage.TO_PICKUP),
            )
        }
    }

    fun rejectOffer() {
        countdownJob?.cancel()
        _courier.update { it.copy(offer = null) }
    }

    fun advanceJob() {
        val job = _courier.value.job ?: return
        when (job.stage) {
            JobStage.TO_PICKUP -> _courier.update {
                it.copy(
                    pos = job.order.pickup.point,
                    job = job.copy(stage = JobStage.TO_DROPOFF),
                )
            }
            JobStage.TO_DROPOFF -> {
                val earning = Fare.courierShare(job.order.fare)
                _courier.update {
                    it.copy(
                        pos = job.order.dropoff.point,
                        job = null,
                        finished = FinishedJob(job.order.copy(status = OrderStatus.DELIVERED), earning),
                        earningsToday = it.earningsToday + earning,
                        jobsToday = it.jobsToday + 1,
                    )
                }
            }
        }
    }

    fun dismissFinished() {
        _courier.update { it.copy(finished = null) }
    }

    private suspend fun generateOffers() {
        while (true) {
            delay(Random.nextLong(4000L, 8000L))
            val state = _courier.value
            if (!state.online || state.offer != null || state.job != null || state.finished != null) continue
            val order = randomOrder()
            _courier.update {
                it.copy(
                    offer = JobOffer(
                        order = order,
                        secondsLeft = OFFER_SECONDS,
                        distanceToPickupKm = it.pos.roadDistanceTo(order.pickup.point),
                    ),
                )
            }
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (true) {
                delay(1000L)
                val offer = _courier.value.offer ?: return@launch
                if (offer.secondsLeft <= 1) {
                    _courier.update { it.copy(offer = null) }
                    return@launch
                }
                _courier.update { it.copy(offer = offer.copy(secondsLeft = offer.secondsLeft - 1)) }
            }
        }
    }

    private fun randomOrder(): Order {
        val a = places.random()
        var b = places.random()
        while (b.id == a.id) b = places.random()

        val service = ServiceType.entries.random()
        val size = if (service == ServiceType.BOX) ItemSize.entries.random() else ItemSize.SMALL
        val weight = if (service == ServiceType.FOOD) 1f else listOf(1f, 2f, 3f, 6f).random()
        val note = if (service == ServiceType.FOOD) {
            listOf("2 porsi nasi goreng", "Es teh & mie ayam", "Kopi 3 gelas", "Martabak manis").random()
        } else {
            listOf("Dokumen penting", "Paket baju online", "Kardus oleh-oleh", "Spare part motor").random()
        }
        val distance = a.point.roadDistanceTo(b.point)
        return Order(
            id = "YK-${nextId++}",
            service = service,
            pickup = a,
            dropoff = b,
            itemNote = note,
            weightKg = weight,
            size = size,
            payment = PaymentMethod.entries.random(),
            distanceKm = distance,
            fare = Fare.estimate(distance, service, size, weight),
            customerName = listOf("Rina", "Dimas", "Fajar", "Maya", "Hendra").random(),
        )
    }

    // ===================== UTILITAS =====================

    private fun eta(km: Float): Int = max(1, ceil(km / KM_PER_MINUTE).toInt())

    /** Menggerakkan titik dari [from] ke [to] mengikuti rute L, memanggil [onStep] tiap langkah. */
    private suspend fun travel(from: Point, to: Point, onStep: (Point, Float) -> Unit) {
        val corner = Point(to.x, from.y)
        val legs = listOf(from to corner, corner to to)
        var remaining = from.roadDistanceTo(to)
        for ((a, b) in legs) {
            val length = a.roadDistanceTo(b)
            if (length <= 0f) continue
            val steps = max(1, ceil(length / STEP_KM).toInt())
            for (i in 1..steps) {
                delay(TICK_MS)
                val t = i / steps.toFloat()
                val pos = Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                onStep(pos, remaining - length * t)
            }
            remaining -= length
        }
    }
}
