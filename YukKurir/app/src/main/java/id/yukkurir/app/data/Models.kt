package id.yukkurir.app.data

import kotlin.math.abs
import kotlin.math.sqrt

/** Koordinat sederhana dalam km pada peta demo berukuran 10 x 10 km. */
data class Point(val x: Float, val y: Float) {
    fun distanceTo(o: Point): Float {
        val dx = x - o.x
        val dy = y - o.y
        return sqrt(dx * dx + dy * dy)
    }

    /** Jarak lewat jalan (pola grid) dalam km. Dipakai untuk tarif dan simulasi rute. */
    fun roadDistanceTo(o: Point): Float = abs(x - o.x) + abs(y - o.y)
}

/** Rute berbentuk huruf L (horizontal dulu, lalu vertikal) mengikuti grid jalan peta demo. */
fun routePoints(from: Point, to: Point): List<Point> = listOf(from, Point(to.x, from.y), to)

data class Place(
    val id: String,
    val name: String,
    val address: String,
    val point: Point,
)

enum class ServiceType(val label: String, val emoji: String, val subtitle: String) {
    BOX("Kurir Box", "📦", "Paket, dokumen & barang"),
    FOOD("Antar Makanan", "🍱", "Makanan & minuman"),
}

enum class ItemSize(val label: String, val hint: String, val multiplier: Float) {
    SMALL("Kecil", "Muat di tas / box kecil", 1.0f),
    MEDIUM("Sedang", "Kardus atau box sedang", 1.25f),
    LARGE("Besar", "Box besar di bagasi motor", 1.6f),
}

enum class PaymentMethod(val label: String) {
    CASH("Tunai"),
    WALLET("YukPay"),
}

enum class OrderStatus(val label: String) {
    SEARCHING("Mencari kurir…"),
    NO_COURIER("Kurir belum tersedia"),
    TO_PICKUP("Kurir menuju lokasi jemput"),
    TO_DROPOFF("Barang sedang diantar"),
    DELIVERED("Pesanan selesai"),
    CANCELLED("Dibatalkan"),
}

data class Courier(
    val id: String,
    val name: String,
    val plate: String,
    val vehicle: String,
    val rating: Float,
    val pos: Point,
)

data class Order(
    val id: String,
    val service: ServiceType,
    val pickup: Place,
    val dropoff: Place,
    val itemNote: String,
    val weightKg: Float,
    val size: ItemSize,
    val payment: PaymentMethod,
    val distanceKm: Float,
    val fare: Int,
    val customerName: String = "Kamu",
    val status: OrderStatus = OrderStatus.SEARCHING,
    val courier: Courier? = null,
    val courierPos: Point? = null,
    val etaMinutes: Int = 0,
    val rating: Int = 0,
)

// ---------- Sisi kurir ----------

enum class JobStage(val title: String, val action: String) {
    TO_PICKUP("Menuju lokasi jemput", "Barang sudah diambil"),
    TO_DROPOFF("Antar ke tujuan", "Barang sudah diterima pelanggan"),
}

data class JobOffer(
    val order: Order,
    val secondsLeft: Int,
    val distanceToPickupKm: Float,
)

data class CourierJob(
    val order: Order,
    val stage: JobStage,
)

data class FinishedJob(
    val order: Order,
    val earning: Int,
)

data class CourierState(
    val online: Boolean = false,
    val pos: Point = Point(5f, 5f),
    val offer: JobOffer? = null,
    val job: CourierJob? = null,
    val finished: FinishedJob? = null,
    val earningsToday: Int = 0,
    val jobsToday: Int = 0,
)
