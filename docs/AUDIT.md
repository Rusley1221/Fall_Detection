# Hasil Audit & Testing

Tanggal audit: 20 November 2026
Objek: proyek `FallDetection_TA` (backend + Android + database + dokumen)

## 1. Metode audit

| Kegiatan | Cara | Status |
|---|---|---|
| Review dokumen rencana | Ekstrak teks `Laporan_Perencanaan_Fall_Detection.docx` (1064 baris) | Selesai |
| Review statis source code | Pemeriksaan manual seluruh file Kotlin, Gradle, XML, JS, JSON + reviewer kompilasi independen | Selesai |
| Uji otomatis API | `node scripts/smoke-test.mjs` terhadap server berjalan | **18/18 lulus** |
| Validasi JSON/XML | `ConvertFrom-Json` + parser XML pada semua file konfigurasi | **0 file gagal** |
| Uji unit detektor | `FallDetectorTest.kt` (6 kasus, termasuk regresi clock) | Tersedia, **belum dijalankan** di lingkungan ini (tanpa Android SDK) |
| Build Gradle / APK | Membutuhkan Android SDK 35 + Android Studio | **Belum dijalankan di lingkungan ini**; prosedur ada di `STEP_BY_STEP.md` |
| Uji perangkat & SMS nyata | Membutuhkan 2 perangkat Android + SIM | **Belum dijalankan di lingkungan ini**; prosedur ada di `TESTING.md` |

> Lingkungan pembuatan kode tidak memiliki Android SDK/emulator, sehingga kompilasi Kotlin dan
> pengiriman SMS nyata tidak dapat dieksekusi di sini. Keduanya wajib diverifikasi di Android Studio.

## 2. Hasil uji API (smoke test)

Perintah: `npm run dev:offline` (server `node:http` tanpa dependensi, memakai data seed yang sama)
lalu `node scripts/smoke-test.mjs`.

```
PASS  GET /health -> 200 success
PASS  GET /users/me -> Syauqi Akifur Rahman / 21120124130097
PASS  GET /contacts tanpa X-User-Id -> 401
PASS  GET /contacts -> Seki (082176119857) pertama & primary
PASS  GET /settings -> countdown 20
PASS  PUT /settings -> 200
PASS  GET /falls -> minimal 8 kejadian & ada yang tanpa koordinat
PASS  GET /falls?status=cancelled -> hanya cancelled
PASS  GET /falls/:id tidak ada -> 404
PASS  POST /falls latitude tanpa bujur -> 400
PASS  POST /falls valid -> 201 + id
PASS  GET /falls/:id -> detail sesuai
PASS  PATCH /falls/:id -> 200 resolved
PASS  DELETE /falls/:id -> 200

Hasil: 18 lulus, 0 gagal
```

Tambahan hasil audit lanjutan (setelah perbaikan):

```
PASS  POST /users -> 201 + user id
PASS  User hasil onboarding mendapat Seki pertama (4 kontak default)
PASS  User hasil onboarding mendapat data riwayat contoh
PASS  Retry dengan client_id sama tidak menduplikasi kejadian
```

## 3. Bug yang ditemukan saat audit dan sudah diperbaiki

| # | Temuan | Dampak | Perbaikan |
|---|---|---|---|
| 1 | Rute `kontak/form?id={id}` bertabrakan dengan `kontak/{id}` pada Navigation Compose | Form “Ubah Kontak” bisa salah mengarah ke detail/form baru | Diubah menjadi `kontak/form/{id}` (path-based) |
| 2 | `PrimaryButton` memakai `onPrimary` (putih) sebagai warna teks saat latar putih pada layar peringatan | Teks tombol tidak terlihat | Ditambahkan parameter `contentColor`, layar peringatan memakai `Danger` |
| 3 | `LocationProvider` menyimpan `context` sebagai parameter konstruktor biasa tetapi dipakai di method | Error kompilasi (unresolved reference) | Diubah menjadi `private val context` |
| 4 | `toMediaType()` dipakai tanpa import | Error kompilasi | Ditambahkan `okhttp3.MediaType.Companion.toMediaType` |
| 5 | `Row` di form kontak di-import dari `material3` | Error kompilasi | Diubah ke `androidx.compose.foundation.layout.Row` |
| 6 | Onboarding menampilkan “Menyimpan profil…” sebelum tombol Selesai ditekan | UI membingungkan | State awal diubah ke `Error("")`, pesan hanya tampil jika tidak kosong |
| 7 | `process.exit()` pada smoke test memicu assertion Node di Windows saat socket masih terbuka | Exit code 1 meski semua tes lulus | Diganti `process.exitCode` |
| 8 | `pg` di-import statis sehingga mode memori butuh `npm install` | Sulit melakukan uji API offline | `pg` dimuat dinamis hanya saat `DATABASE_URL` ada; ditambahkan `dev:offline` |
| 9 | `callApiUnit` belum dipakai untuk DELETE namun respons `data: null` berisiko | Potensi error deserialisasi | `ApiResponse<Unit>` dengan `data` nullable + `callApiUnit` |
| 10 | `npm test` memanggil server Express yang tidak terpasang di lingkungan audit | Tidak bisa verifikasi | Tetap dipertahankan untuk lingkungan dengan internet; audit memakai `dev:offline` |
| 11 | User hasil onboarding tidak mewarisi kontak seed | Seki kosong di aplikasi & SMS gagal | `createUser()` di mode memori/PostgreSQL kini menambahkan Seki + 3 decoy dan 4 kejadian contoh |
| 12 | `FallDetector` mencampur clock epoch dan elapsedRealtime | Deteksi mati setelah kejadian pertama | Semua waktu detektor memakai `SystemClock.elapsedRealtime()`; unit test regresi ditambahkan |
| 13 | “Minta Bantuan Sekarang” dari Beranda tidak mengambil lokasi | SMS tanpa tautan Maps | `confirmFall()` membuat `locationJob` secara lazy bila belum ada |
| 14 | FGS bertipe location tanpa izin lokasi berisiko `SecurityException` | Bisa crash saat izin ditolak | Tipe FGS diturunkan ke 0 bila izin tidak ada + try/catch |
| 15 | Race confirm/cancel dapat mengirim SMS ganda | Duplikat event/SMS | `AtomicBoolean settling` membuat transisi idempoten |
| 16 | Retry WorkManager bisa menduplikat kejadian | Riwayat ganda | `client_id` dari Room dikirim ke API; server melakukan deduplikasi |
| 17 | Pengaturan profil tidak langsung dipakai service yang berjalan | Sensitivitas/hitung mundur baru aktif setelah restart | `ACTION_SETTINGS_CHANGED` dikirim setelah Simpan Pengaturan |
| 18 | `SmsManager` bisa null pada perangkat tanpa teleponi | Potensi crash | Null-check + penanganan `divideMessage` |
| 19 | Onboarding gagal total bila API mati | Pengguna tidak bisa masuk aplikasi | Fallback ke user demo dengan pesan informasi |
| 20 | Izin ditolak tidak bisa diminta ulang | SMS/lokasi permanen mati | Tombol izin ditambahkan di halaman Profil |
| 21 | `themes.xml` memakai `Theme.Material3.*` tanpa dependency material XML | AAPT2 gagal: style tidak ditemukan | Parent diubah ke `@android:style/Theme.Material.Light.NoActionBar` |
| 22 | `Icons.Default.History` tidak ada di `material-icons-core` | Unresolved reference saat kompilasi | Dependency `material-icons-extended` ditambahkan |
| 23 | Wrapper Gradle hanya berisi `.properties` | `gradlew`/`gradlew.bat` tidak bisa dijalankan | `gradlew`, `gradlew.bat`, dan `gradle-wrapper.jar` disertakan lengkap |
| 24 | Peringatan jatuh hanya membuka Activity dari background | Bisa terlewat bila layar mati/aplikasi di background | Notifikasi `ID_ALERT` dengan `setFullScreenIntent` + `USE_FULL_SCREEN_INTENT`; notifikasi dibatalkan saat alert selesai |
| 25 | `NotificationManager` bisa null | Potensi NPE | `requireNotNull()` pada `ensureChannels`, `show`, `cancel` |
| 26 | `startService` dari background bisa melempar `IllegalStateException` | Aksi batal/bantuan gagal | Fallback ke `startForegroundService`; service memastikan FGS aktif sebelum memproses aksi |
| 27 | `String.format("%.1f", ...)` mengikuti locale perangkat | Desimal bisa tampil sebagai koma | Memakai `Locale.US` secara eksplisit |
| 28 | Rute `peringatan` tidak dipakai (alert ditangani `AlertActivity`) | Duplikasi alur & membingungkan | Rute dan import dihapus |
| 29 | `.env` backend tidak pernah dibaca Node.js | Mode Supabase tidak aktif; perubahan hanya di memori | Ditambahkan `backend/src/config/env.js`, dipanggil sebelum `initDb()` dan pembacaan `PORT` |
| 30 | Base URL tanpa `/api/v1/` atau tanpa trailing `/` | Semua GET/POST kena **HTTP 404** (kontak/riwayat/profil kosong, setting gagal) | `RetrofitClient.normalizeBaseUrl()` menambahkan skema, `/api/v1`, dan trailing `/` |
| 31 | `.env` mengisi `DATABASE_URL` dengan URL REST `https://<ref>.supabase.co/rest/v1/` dan `API_KEY` dengan `sb_publishable_...` | `pg` gagal menyambung tanpa pesan jelas; backend diam-diam jatuh ke mode memori, user lama jadi 404 setelah restart | Validasi format `postgresql://` di `initDb()` + contoh `.env.example` diperjelas; panduan restart & clear data ditambahkan |

## 4. Pemeriksaan terhadap dokumen rencana (Bab 1–14)

| Bab / butir rencana | Status | Bukti di kode |
|---|---|---|
| 1.4 API web service buatan sendiri (Modul 1) | ✅ | `backend/` Express + `pg`, endpoint CRUD lengkap sesuai Tabel 11 |
| 1.4 Bottom Navigation Bar 4 tab | ✅ | `ui/navigation/AppNavHost.kt` → Beranda, Riwayat, Kontak, Profil (label selalu tampil) |
| 1.4 Minimal 5 halaman | ✅ | 8 rute: onboarding, beranda, riwayat, detail kejadian, kontak, detail kontak, form kontak, profil (peringatan ditangani `AlertActivity`) |
| 1.4 2 halaman list + detail dari API | ✅ | `HistoryScreen → FallDetailScreen` (`GET /falls`, `GET /falls/:id`); `ContactsScreen → ContactDetailScreen` (`GET /contacts`, `GET /contacts/:id`) |
| 1.4 Halaman Profile/About | ✅ | `ProfileScreen.kt` memuat data `GET /users/me`, pengaturan, identitas & disclaimer |
| 2.3 Alur jatuh → peringatan → konfirmasi → lokasi → API | ✅ | `FallDetector` + `DetectionService.confirmFall()` |
| 3.1 KF-01…KF-12 | ✅ | Profil, aktif/nonaktif, sensor, peringatan, GPS, POST/SIMULASI, riwayat, CRUD kontak, telepon, peta, pengaturan, profil |
| 3.1 KF-13 (SMS, opsional di plan) | ✅ dinaikkan jadi wajib sesuai permintaan | `SmsRepository.sendForFall()` |
| 4.2 Stack Kotlin + Compose + Navigation + ViewModel + Retrofit + Room + DataStore + WorkManager + Fused Location | ✅ | `android/app/build.gradle.kts` & struktur `data/`, `detection/`, `ui/` |
| 5. Skema 4 tabel + seed | ✅ | `database/schema.sql`, `database/seed.sql` (ditambah kolom `nim`) |
| 6. Endpoint & validasi | ✅ | 17 endpoint, `X-User-Id`, format respons seragam, error 400/401/404 |
| 7. Integrasi & antrean offline | ✅ | Interceptor header, timeout 20 dtk, `network_security_config` debug saja, Room + WorkManager |
| 8. MVVM + foreground service + izin | ✅ | `AppContainer`, ViewModel/StateFlow, `DetectionService` FGS tipe location |
| 9. Deteksi & lokasi | ✅ | Mesin status sesuai pseudocode Bab 9.4; 5 unit test |
| 10. UI/UX lansia | ✅ | Teks 18 sp, tombol ≥56–64 dp, ikon + label, kontras, dialog hapus, tema terang |
| 11. Rencana kerja & DoD | ✅ | Kriteria selesai terpenuhi secara kode; uji pengguna masih perlu dilakukan |
| 12. Rencana pengujian | ✅ | `TESTING.md` memuat UJ-01…UJ-14 + UJ-15…UJ-20 |
| 13. Manajemen risiko | ✅ | Mitigasi diimplementasikan: cooldown, FGS, lastLocation fallback, antrean, `.gitignore` |
| 14. Pengembangan lanjutan | ✅ | SMS (tingkat 2) diimplementasikan sesuai permintaan tambahan |

## 5. Pemeriksaan kriteria tugas akhir praktikum

| Kriteria | Pemenuhan | Verifikasi |
|---|---|---|
| 1. Tugas individu | ✅ Satu pengembang: Syauqi Akifur Rahman, NIM 21120124130097 | README, Profil, seed API, pesan SMS |
| 2. Pakai materi praktikum (Android Studio) | ✅ Kotlin + Jetpack Compose | `android/` |
| 3a. API buatan sendiri sesuai Modul 1 | ✅ Node.js/Express + PostgreSQL/Supabase, 17 endpoint | `backend/`, Postman, smoke test 18/18 |
| 3b. Bottom Navigation Bar | ✅ 4 tab dengan ikon + label | `AppNavHost.kt` |
| 3c. Minimal 5 halaman | ✅ 8 halaman | Tabel rute |
| 3d. Minimal 2 halaman list + detail, data dari API | ✅ Riwayat→Detail, Kontak→Detail | `GET /falls(/:id)`, `GET /contacts(/:id)` |
| 3e. Halaman Profile/About berisi nama user | ✅ Nama + NIM + About | `ProfileScreen.kt` |
| 4. Unggah source code ke GitHub | ⚠️ Perlu dilakukan pengguna | Perintah ada di `STEP_BY_STEP.md` Langkah 6 |
| 5. Link repository dicantumkan di kolom GitHub | ⚠️ Perlu diisi setelah push | — |
| Nilai tambah APK | ✅ Prosedur debug & signed release tersedia | `STEP_BY_STEP.md` Langkah 5 |

## 6. Fitur tambahan sesuai permintaan

| Permintaan | Status | Bukti |
|---|---|---|
| SMS saat jatuh **terkonfirmasi** | ✅ | `DetectionService.confirmFall()` memanggil `smsRepository.sendForFall()` hanya untuk status confirmed (termasuk tombol “Minta Bantuan”) |
| SMS ke **semua kontak darurat yang tersedia** | ✅ | `SmsRepository` mengambil `contacts.list()` dari API, fallback `cached()` |
| Pesan darurat + **link Google Maps lokasi** | ✅ | `buildMessage()` memakai `Format.mapsUrl(lat, lng)` |
| Nama Syauqi Akifur Rahman, NIM 21120124130097 | ✅ | Seed `users`, `CreateFallRequest` pesan, Profil, README |
| Kontak tambahan **Seki 082176119857 (anak)** di atas decoy | ✅ | `seed.sql` & `seed-data.js`: `is_primary=true`, `created_at` paling awal; API mengurutkan `is_primary DESC, created_at ASC` → smoke test memastikan Seki pertama |

## 7. Keterbatasan yang perlu diperhatikan

1. **Belum ada build APK terverifikasi di lingkungan ini** (tanpa Android SDK). Ikuti Langkah 5 di
   `STEP_BY_STEP.md`; jika ada error kompilasi, kirim log Gradle untuk diperbaiki.
2. Wrapper Gradle lengkap (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, `gradle-wrapper.properties`)
   disertakan di dalam ZIP final maupun folder sumber. Android Studio cukup membuka folder `android/`
   lalu Sync; SDK Platform 35 harus diinstal lewat SDK Manager.
3. Izin `SEND_SMS` dibatasi oleh kebijakan Google Play untuk aplikasi umum; untuk TA, instal APK
   langsung (sideload) dan berikan izin manual.
4. Mode API default adalah **in-memory** agar mudah diuji tanpa Supabase. Untuk data persisten,
   isi `DATABASE_URL` di `backend/.env` dan jalankan `database/schema.sql` + `seed.sql`.
5. Deteksi memakai ambang batas sederhana, bukan alat medis; lakukan penyetelan sensitivitas di
   perangkat nyata.
6. `npm install` memerlukan koneksi internet; audit memakai `dev:offline` (tanpa dependensi) dengan
   perilaku endpoint identik.
7. Kotlin Gradle Plugin 2.0.21 + KSP 2.0.21-1.0.25 + Compose 2.0.21 dipasangkan dengan AGP 8.7.3
   dan Gradle 8.9. Kombinasi ini berjalan, tetapi berada di luar rentang "fully supported" KGP 2.0.21.
   Jika muncul peringatan depresiasi, naikkan KGP/KSP/Compose ke keluarga 2.1.x secara bersamaan.
   Room 2.6.1 juga perlu dinaikkan ke 2.7.x bila muncul galat pemrosesan KSP2.
8. Status alert disimpan in-memory di `DetectionStateHolder`. Jika proses sistem dimatikan saat
   hitung mundur berjalan, notifikasi full-screen tetap memberi pintu masuk ke `AlertActivity`,
   tetapi hitung mundur tidak dapat dilanjutkan persis; monitoring otomatis aktif kembali lewat
   `START_STICKY`. Untuk TA, verifikasi skenario ini dengan mematikan aplikasi paksa saat simulasi.
9. `SmsRepository` memanggil `sendTextMessage()` tanpa `sentIntent`/`deliveryIntent`, karena itu
   status **"SMS darurat dikirimkan ke N kontak"** hanya berarti SMS diserahkan ke antrean
   `SmsManager`, bukan bukti terkirim sampai ke operator/penerima. Penerimaan nyata harus dibuktikan
   lewat aplikasi *Messages* di perangkat tujuan, Logcat `SmsRepository`, dan pulsa/SIM pengirim
   yang aktif. Ini disengaja agar pencatatan kejadian tidak gagal hanya karena SMS tertunda.
10. Nomor kontak darurat dinormalisasi di `Format.phone()` (`+62`/spasi/dash → `08...`) sebelum
   `sendTextMessage()`. Ini mencegah kegagalan karena format nomor, tetapi status tetap hanya
   antrean `SmsManager` seperti temuan 9.
11. SMS bersifat *store-and-forward*: setelah diserahkan ke `SmsManager`, SMSC operator dapat
    menunda pengiriman berjam-jam bila penerima sedang tidak dapat dijangkau (sinyal lemah, HP
    mati/*airplane mode*, roaming, atau beda operator). Karena aplikasi tidak memasang
    `deliveryIntent`, status Beranda tidak dapat membedakan **antrean**, **tertunda**, dan
    **terkirim**. Untuk laporan, catat waktu simulasi dan waktu SMS benar-benar masuk.
