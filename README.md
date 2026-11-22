# Fall Detection — Tugas Akhir Praktikum Pemrograman Perangkat Bergerak

Aplikasi Android (Kotlin + Jetpack Compose) untuk mendeteksi kemungkinan jatuh, mencatat lokasi kejadian,
menyimpannya melalui **REST API buatan sendiri**, dan mengirim **SMS darurat berisi tautan Google Maps**
ke seluruh kontak darurat ketika jatuh dikonfirmasi.

**Pemilik / Pengembang:** Syauqi Akifur Rahman — NIM 21120124130097 (individu)
**Topik:** Fall Detection dengan Informasi Lokasi Kejadian

---

## 1. Isi paket

```
FallDetection_TA/
├── android/                  # Proyek Android Studio (Kotlin, Jetpack Compose, Material 3)
│   ├── app/src/main/java/... # MVVM: data, detection, location, ui, worker
│   └── app/src/main/res/     # tema, ikon, network security config (debug saja)
├── backend/                  # REST API Node.js + Express (Modul 1)
│   ├── api/index.js          # handler Vercel
│   ├── src/                  # app, routes, controllers, middleware, config, store
│   └── scripts/smoke-test.mjs
├── database/
│   ├── schema.sql            # 4 tabel PostgreSQL/Supabase
│   └── seed.sql              # data demo: user, Seki + 3 decoy, 8 kejadian
├── docs/
│   ├── STEP_BY_STEP.md       # dari extract ZIP sampai APK & GitHub
│   ├── TESTING.md            # skenario uji UJ-01..UJ-17 + cara uji SMS
│   ├── AUDIT.md              # hasil audit terhadap plan & kriteria tugas
│   ├── Postman_Collection.json
│   └── API_REFERENCE.md
└── README.md
```

## 2. Fitur utama

| Fitur | Keterangan |
|---|---|
| Deteksi jatuh | Akselerometer + mesin status benturan → diam (pure Kotlin, diuji unit) |
| Peringatan | Layar hitung mundur, suara, getar, tombol “Saya baik-baik saja” |
| Lokasi | Fused Location Provider, diambil sejak peringatan mulai |
| Pencatatan | POST `/falls` ke API sendiri; antrean Room + WorkManager saat offline |
| Riwayat & detail | List dari `GET /falls`, detail dari `GET /falls/:id` |
| Kontak darurat | CRUD dari `GET/POST/PUT/DELETE /contacts`, detail per kontak; user baru otomatis mendapat Seki + 3 decoy |
| **SMS darurat (tambahan)** | Saat **jatuh terkonfirmasi**, SMS dikirim ke **semua kontak darurat** berisi nama/NIM, waktu, dan tautan Google Maps lokasi |
| Profil | Nama, **NIM 21120124130097**, sensitivitas, hitung mundur, toggle SMS, Tentang Aplikasi |
| Navigasi | Bottom Navigation Bar 4 tab: Beranda, Riwayat, Kontak, Profil |
| Halaman | 8 halaman: onboarding, beranda, riwayat, detail kejadian, kontak, detail/form kontak, profil, peringatan |

## 3. Kontak darurat demo (urutan di API)

| No | Nama | Nomor | Hubungan | Peran |
|---|---|---|---|---|
| 1 | **Seki** | **082176119857** | anak | **Kontak utama + kontak uji SMS ke perangkat Android lain** |
| 2 | Bapak (decoy) | 081234567801 | ayah | Penguji |
| 3 | Ibu (decoy) | 081234567802 | ibu | Penguji |
| 4 | Tetangga (decoy) | 081234567803 | tetangga | Penguji |

Contoh pesan SMS yang dikirim:

```
DARURAT! Syauqi Akifur Rahman (NIM 21120124130097) terdeteksi jatuh pada Jumat, 20 Nov 2026, 10.32 WIB. Lokasi: https://www.google.com/maps/search/?api=1&query=-6.9932,110.4203. Mohon segera bantu.
```

## 4. Ringkasan teknologi

| Lapisan | Teknologi |
|---|---|
| Android | Kotlin 2.0.21, Jetpack Compose + Material 3, Navigation Compose, ViewModel + StateFlow |
| Jaringan | Retrofit 2.11, OkHttp, kotlinx.serialization |
| Lokal | DataStore (profil/preferensi), Room (antrean kejadian), WorkManager (retry) |
| Sensor/lokasi | SensorManager foreground service, Fused Location Provider |
| Backend | Node.js 18+, Express 4, `pg` (Supabase/PostgreSQL); mode memori untuk uji lokal |
| Hosting | Vercel (HTTPS) + Supabase PostgreSQL |

## 5. Mulai cepat

Lihat **[docs/STEP_BY_STEP.md](docs/STEP_BY_STEP.md)** untuk panduan lengkap, dan
**[docs/TESTING.md](docs/TESTING.md)** untuk pengujian termasuk SMS ke nomor Seki.

Singkatnya:

```bash
# 1. Jalankan API (mode memori, tanpa Supabase)
cd backend
npm install
npm run dev
# API: http://localhost:3000/api/v1
# Alternatif tanpa npm install (server node:http, data seed sama):
# npm run dev:offline

# 2. Buka folder android/ dengan Android Studio Ladybug (2024.2.1) atau lebih baru
# 3. Gradle Sync -> Run app pada emulator/perangkat
# 4. Emulator memakai 10.0.2.2:3000; perangkat fisik ganti IP LAN di app/build.gradle.kts
# 5. Build > Build APK: app/build/outputs/apk/debug/app-debug.apk
```

## 6. Catatan penting

- Izin `SEND_SMS` adalah izin berbahaya; harus diberikan pengguna saat onboarding. Google Play
  membatasi penggunaan izin SMS untuk aplikasi umum, tetapi **untuk Tugas Akhir yang di-sideload
  sebagai APK**, pengiriman SMS dapat diuji langsung.
- Pastikan perangkat penguji SMS memiliki kartu SIM aktif / dapat mengirim SMS. Emulator biasanya
  hanya mencatat SMS di Logcat (`SmsRepository`).
- Kunci Supabase **hanya** di server sebagai environment variable. `.env` tidak ikut di-commit.
- Aplikasi ini bukan alat medis dan bukan pengganti layanan darurat resmi.
