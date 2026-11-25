package id.yukkurir.app.data

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/** Aturan tarif demo. Ubah angka di sini untuk menyesuaikan harga. */
object Fare {
    const val BASE_FARE = 5000          // tarif dasar (Rp) untuk jarak awal
    const val BASE_KM = 2f              // jarak yang sudah termasuk tarif dasar
    const val PER_KM = 2500             // tarif per km setelah jarak awal
    const val FOOD_HANDLING = 1000      // biaya penanganan makanan
    const val HEAVY_THRESHOLD_KG = 5f   // di atas ini kena biaya berat
    const val HEAVY_PER_KG = 1000       // biaya per kg kelebihan
    const val PLATFORM_FEE = 0.2f       // potongan aplikasi (20%)

    fun estimate(distanceKm: Float, service: ServiceType, size: ItemSize, weightKg: Float): Int {
        val extraKm = max(0f, distanceKm - BASE_KM)
        var fare = (BASE_FARE + extraKm * PER_KM) * size.multiplier
        if (service == ServiceType.FOOD) fare += FOOD_HANDLING
        if (weightKg > HEAVY_THRESHOLD_KG) {
            fare += ceil(weightKg - HEAVY_THRESHOLD_KG) * HEAVY_PER_KG
        }
        // Dibulatkan ke atas per Rp 500
        return (ceil(fare / 500f) * 500f).toInt()
    }

    /** Pendapatan kurir setelah potongan aplikasi, dibulatkan ke bawah per Rp 100. */
    fun courierShare(fare: Int): Int = ((fare * (1f - PLATFORM_FEE)) / 100f).toInt() * 100
}

private val idNumberFormat: NumberFormat = NumberFormat.getInstance(Locale.forLanguageTag("id-ID"))

fun Int.rupiah(): String = "Rp " + idNumberFormat.format(this)
