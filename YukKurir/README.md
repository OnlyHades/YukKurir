# YUK KURIR

Aplikasi Android kurir antar **barang & makanan** (tanpa angkut penumpang) dengan alur seperti Grab/Gojek:
pelanggan memesan → sistem mencari kurir terdekat → kurir menerima → pelanggan memantau kurir di peta → barang sampai → rating.

Dibuat dengan **Kotlin + Jetpack Compose (Material 3)**. Satu aplikasi berisi dua mode: **Pelanggan** dan **Kurir**.

> **Status: prototipe yang bisa dijalankan (mode demo).** Backend, peta, dan GPS disimulasikan di dalam aplikasi
> supaya seluruh alur bisa dicoba tanpa server dan tanpa API key. Lihat bagian "Menuju produksi" di bawah.

## Cara menjalankan

1. Buka folder `YukKurir` dengan **Android Studio** (Koala 2024.1 atau lebih baru).
2. Tunggu *Gradle sync* selesai (butuh internet untuk mengunduh dependensi, JDK 17).
3. Pilih emulator atau HP Android (minimal Android 7.0 / API 24), lalu klik **Run ▶**.
4. Untuk APK: menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Fitur

**Mode Pelanggan**
- Dua layanan: **Kurir Box** (paket/dokumen/barang) dan **Antar Makanan**.
- Pilih titik jemput & tujuan dari daftar landmark atau ketuk langsung di peta.
- Detail barang: isi paket, ukuran (kecil/sedang/besar), perkiraan berat.
- Estimasi tarif langsung berdasarkan jarak, ukuran, dan berat.
- Pembayaran: Tunai atau YukPay (dompet — tiruan).
- Pelacakan: mencari kurir → kurir menuju jemput → barang diantar → selesai, lengkap dengan ETA dan posisi kurir bergerak di peta.
- Batalkan pesanan, rating kurir, dan riwayat pesanan.

**Mode Kurir**
- Saklar Online/Offline.
- Orderan masuk dengan hitungan mundur 15 detik: Terima / Tolak.
- Langkah pengantaran: menuju jemput → barang diambil → antar → selesai.
- Pendapatan hari ini dan jumlah pesanan selesai (potongan aplikasi 20%).

## Aturan tarif (bisa diubah di `data/Fare.kt`)

| Komponen | Nilai |
|---|---|
| Tarif dasar (2 km pertama) | Rp 5.000 |
| Tambahan per km | Rp 2.500 |
| Pengali ukuran | Kecil ×1,0 · Sedang ×1,25 · Besar ×1,6 |
| Biaya penanganan makanan | Rp 1.000 |
| Berat di atas 5 kg | Rp 1.000 per kg |
| Potongan aplikasi | 20% |

Hasil dibulatkan ke atas per Rp 500.

## Struktur kode

```
app/src/main/java/id/yukkurir/app/
├── MainActivity.kt
├── data/
│   ├── Models.kt          # Order, Courier, status, state kurir
│   ├── Fare.kt            # perhitungan tarif + format rupiah
│   └── YukRepository.kt   # "server" tiruan: matching kurir, simulasi gerak, orderan masuk
└── ui/
    ├── theme/Theme.kt
    ├── YukApp.kt          # pilih peran (Pelanggan / Kurir)
    ├── CustomerScreens.kt # beranda & riwayat
    ├── OrderForm.kt       # form pesan + pemilih lokasi
    ├── TrackingScreen.kt  # pelacakan pesanan + rating
    ├── CourierScreens.kt  # dasbor kurir
    ├── CityMap.kt         # peta kota sederhana (Canvas)
    └── CommonUi.kt
```

`YukRepository` adalah satu-satunya tempat "backend". Layar UI hanya membaca `StateFlow` dan memanggil fungsi aksi,
jadi saat backend sungguhan siap, cukup ganti isi repository tanpa menyentuh UI.

## Menuju produksi (belum ada di prototipe ini)

| Kebutuhan | Saran |
|---|---|
| Login & akun | Firebase Auth (OTP nomor HP), profil pelanggan & kurir, verifikasi KTP/SIM kurir |
| Backend & data | Firebase/Firestore atau server sendiri (REST + WebSocket); simpan pesanan, kurir, riwayat |
| Peta & lokasi asli | Google Maps Compose atau MapLibre; pencarian alamat (Places/Geocoding); rute & jarak asli (Directions API) |
| Posisi kurir real-time | Foreground service + Fused Location, kirim koordinat tiap beberapa detik |
| Matching kurir | Query kurir online terdekat (geohash), tawarkan berurutan dengan timeout |
| Notifikasi orderan | Firebase Cloud Messaging (push) |
| Pembayaran | Payment gateway lokal (mis. Midtrans, Xendit), saldo & penarikan kurir |
| Chat & telepon | Chat in-app, telepon dengan nomor tersamar |
| Keamanan & legal | Foto bukti pengantaran, laporan masalah, kebijakan privasi, izin usaha |

## Catatan

- Nama kurir, plat nomor, dan lokasi pada mode demo adalah data fiktif.
- Tema saat ini hanya mode terang; ikon aplikasi berupa kotak paket sederhana (ganti di `res/drawable/ic_launcher_foreground.xml`).
