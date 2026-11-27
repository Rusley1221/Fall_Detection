# Panduan Pengujian

Siapkan: API berjalan (`npm run dev`), aplikasi terpasang, izin lokasi/notifikasi/SMS diberikan,
koneksi internet aktif, dan (untuk uji SMS nyata) **perangkat Android kedua dengan SIM aktif**
yang nomornya `082176119857` (Seki).

---

## A. Uji API dengan Postman

Import `docs/Postman_Collection.json`. Koleksi sudah memakai variabel:
`baseUrl = http://localhost:3000/api/v1`, `userId = 11111111-1111-1111-1111-111111111111`,
`apiKey = fall-detection-ta-2026`.

| ID | Skenario | Hasil diharapkan |
|---|---|---|
| API-01 | GET `/health` | 200, `success: true` |
| API-02 | GET `/contacts` tanpa `X-User-Id` | 401 |
| API-03 | GET `/contacts` dengan header | 200, **Seki pertama**, `is_primary: true` |
| API-04 | GET `/falls?status=confirmed` | 200, hanya status confirmed |
| API-05 | GET `/falls/xxx` | 404 |
| API-06 | POST `/falls` dengan hanya latitude | 400, pesan lintang/bujur harus berdua |
| API-07 | POST `/falls` valid | 201, id UUID baru |
| API-08 | PATCH `/falls/{id}` status `resolved` | 200 |
| API-09 | POST `/contacts` valid | 201; kontak lama otomatis bukan primary bila `is_primary: true` |
| API-10 | GET `/users/me` | 200, nama **Syauqi Akifur Rahman**, NIM **21120124130097** |
| API-11 | POST `/users` lalu GET `/contacts` dengan `X-User-Id` yang baru | 4 kontak, **Seki pertama** (user onboarding tidak kehilangan kontak seed) |
| API-12 | POST `/falls` dua kali dengan `client_id` sama | 201 keduanya, `id` sama (retry offline tidak menduplikat) |
| API-13 | GET `/falls` dengan user baru | 4 kejadian contoh tampil di Riwayat |

Smoke test otomatis (dengan API berjalan di port 3000):

```powershell
cd backend
npm test
```

---

## B. Uji unit detektor

```powershell
cd android
# lewat Android Studio: pilih task :app:testDebugUnitTest, atau:
.\gradlew.bat testDebugUnitTest
```

6 kasus di `FallDetectorTest.kt`: jatuh → peringatan, guncangan + bergerak → diabaikan,
guncangan di bawah ambang → diabaikan, cooldown, pembatalan, dan regresi clock
(`elapsedRealtime()` dipakai sebelum dan sesudah konfirmasi agar deteksi kedua tetap bekerja).
Wrapper Gradle (`gradlew.bat`, `gradlew`, `gradle-wrapper.jar`) sudah disertakan.

---

## C. Uji aplikasi (lanjutan UJ-01 … UJ-14 dari Bab 12 plan)

| ID | Cara | Hasil diharapkan |
|---|---|---|
| UJ-01 | Aktifkan pemantauan & beri izin | Notifikasi tetap tampil, status **AKTIF** |
| UJ-02 | Tolak izin lokasi | Pemantauan tetap jalan; saat jatuh, koordinat null & detail shows “Lokasi tidak tersedia” |
| UJ-03 | Simulasi jatuh, biarkan hitung mundur habis | `confirmed` tersimpan di riwayat + koordinat + **SMS terkirim** |
| UJ-04 | Simulasi jatuh, tekan “Saya baik-baik saja” | `cancelled`, **tidak ada SMS** |
| UJ-05 | Guncang HP lalu terus bergerak | Tidak muncul peringatan (butuh pola diam) |
| UJ-06 | Dua simulasi dalam < 60 detik | Cooldown mencegah peringatan beruntun (tombol simulasi memang sengaja mengabaikan cooldown untuk demo) |
| UJ-07 | Aktifkan mode pesawat, simulasi, lalu matikan | Kejadian tampil “Menunggu terkirim”, terkirim otomatis saat online via WorkManager |
| UJ-08 | Matikan GPS, simulasi | Kejadian tanpa koordinat; detail menampilkan “Lokasi tidak tersedia” |
| UJ-09 | Buka tab Riwayat | Data dari API, urut terbaru, filter chip bekerja |
| UJ-10 | Ketuk riwayat → Buka di Google Maps | Maps terbuka tepat di koordinat |
| UJ-11 | Tambah/ubah/hapus kontak | Perubahan tampak di list & server; hapus ada dialog konfirmasi |
| UJ-12 | Form kontak: nomor kosong / < 8 digit | Pesan kesalahan jelas, data tidak dikirim |
| UJ-13 | Matikan API, buka Riwayat | Pesan “Tidak dapat terhubung…” + tombol **Coba Lagi**, tidak crash |
| UJ-14 | Pengaturan → Ukuran huruf sistem terbesar | Teks tidak terpotong, halaman bisa digulir |
| UJ-21 | Simulasi jatuh saat layar mati | Notifikasi peringatan membuka layar penuh (`showWhenLocked`) dan tombol batal tetap dapat ditekan |
| UJ-22 | Matikan API saat onboarding pertama | Aplikasi tetap masuk memakai profil demo, pesan penjelasan tampil, Seki tetap ada di Kontak |

---

## D. Uji khusus fitur tambahan SMS darurat

| ID | Skenario | Hasil diharapkan |
|---|---|---|
| UJ-15 | Perangkat kedua terpasang SIM `082176119857`, izin SMS diberikan, simulasi jatuh dibiarkan | Perangkat Seki menerima SMS berisi **“DARURAT! Syauqi Akifur Rahman (NIM 21120124130097)”**, waktu, dan `https://www.google.com/maps/search/?api=1&query=...` (dapat tertunda bila penerima tidak dapat dijangkau) |
| UJ-16 | Perangkat penguji SMS bukan nomor Seki / tidak ada SIM | Tetap tidak crash; Logcat `SmsRepository` mencatat hasil kirim/gagal; status SMS tampil di Beranda |
| UJ-17 | Izin SMS ditolak saat onboarding, lalu jatuh dikonfirmasi | Kejadian tetap tercatat; pesan “Izin SMS belum diberikan”; tidak crash |
| UJ-18 | Toggle “Kirim SMS darurat otomatis” dimatikan di Profil, lalu jatuh | SMS tidak dikirim, pesan “SMS darurat dinonaktifkan” |
| UJ-19 | Modal manual dari Beranda: “Minta Bantuan Sekarang” | Langsung confirmed + SMS tanpa menunggu hitung mundur |
| UJ-20 | Kontak diubah: Seki dihapus, kontak baru ditambahkan | SMS dikirim ke daftar kontak terbaru yang diambil dari API/cache |
| UJ-23 | Tekan “Minta Bantuan Sekarang” dua kali cepat | Hanya satu kejadian & satu SMS (guard `settling`) |
| UJ-24 | Matikan mode pesawat saat antrean offline, tunggu WorkManager | Kejadian terkirim sekali saja; `client_id` mencegah duplikat di server |
| UJ-25 | Pilih sensitivitas **Demo**, angkat HP ±30 cm, lepaskan ke kasur/bantal, biarkan diam ±4 detik | Notifikasi merah 20 detik muncul; Logcat `DetectionService` mencatat peak ≥ 15 m/s²; setelah habis → `confirmed` + SMS |

### Uji UI halaman Riwayat (pembaruan 22 November 2026)

| Kode | Langkah | Hasil yang diharapkan |
|---|---|---|
| UJ-26 | Buka Riwayat dengan lebih dari 10 kejadian | Daftar dikelompokkan **Hari ini / Kemarin / tanggal**; tile ringkasan Total, Terkonfirmasi, Dibatalkan tampil |
| UJ-27 | Ketuk filter **Terkonfirmasi**, lalu **Dibatalkan** | Daftar berubah **instan** tanpa layar loading dan tanpa kehilangan data |
| UJ-28 | Tarik daftar ke bawah (pull-to-refresh) | Indikator refresh muncul, data dimuat ulang, posisi filter tetap |
| UJ-29 | Matikan API lalu tekan ikon refresh | Data lama tetap tampil, banner merah “Coba lagi” muncul, aplikasi tidak crash |
| UJ-30 | Buat kejadian saat offline (mode pesawat) | Kartu amber **“Menunggu terkirim”** dengan border; tombol Maps tetap berfungsi; tap kartu menampilkan pesan antrean |
| UJ-31 | Buka Riwayat pada layar kecil (≤ 360 dp) | Tiga chip filter membungkus ke baris berikutnya, tidak ada chip yang terpotong |

### Uji UI halaman Beranda (pembaruan 26 November 2026)

| Kode | Langkah | Hasil yang diharapkan |
|---|---|---|
| UJ-32 | Buka Beranda saat pemantauan mati | Kartu merah **“Pemantauan NONAKTIF”**, chip sensitivitas + hitung mundur sesuai pengaturan, tombol hijau **Aktifkan Pemantauan** |
| UJ-33 | Tekan **Aktifkan Pemantauan** | Kartu berubah hijau **“Pemantauan AKTIF”**, tombol menjadi biru **Nonaktifkan Pemantauan** |
| UJ-34 | Tarik Beranda ke bawah (pull-to-refresh) | Indikator refresh muncul; angka ringkasan dan kejadian terakhir diperbarui tanpa berpindah tab |
| UJ-35 | Matikan API lalu tarik/refresh Beranda | Data lama tetap tampil, banner merah **“Coba lagi”** muncul, aplikasi tidak crash |
| UJ-36 | Tekan **Minta Bantuan Sekarang** | Dialog konfirmasi menyebut jumlah kontak; **Batal** tidak mengirim SMS, **Ya, kirim** mengirim tepat satu SMS |
| UJ-37 | Cabut izin SMS/lokasi lewat pengaturan aplikasi, lalu kembali ke Beranda | Kartu kuning **“Izin belum lengkap”** muncul dengan tombol **Buka pengaturan aplikasi** |
| UJ-38 | Setelah SMS terkirim, tutup kartu status SMS | Kartu status hilang dan tidak muncul lagi sampai kejadian berikutnya |
| UJ-39 | Tekan **Lihat semua** pada kejadian terakhir dan **Kelola** pada kartu kontak | Pindah ke tab Riwayat / Kontak tanpa menumpuk layar (tombol Back kembali ke Beranda) |

### Uji UI Kontak dan Profil

| Kode | Langkah | Hasil yang diharapkan |
|---|---|---|
| UJ-40 | Tambah kontak, buka detailnya, lalu edit | Tiap penyimpanan hanya kembali satu layar; daftar dan detail menampilkan perubahan tanpa refresh manual |
| UJ-41 | Coba nomor yang sudah terdaftar dengan format `08…` atau `+62…`; ubah isian lalu tekan Back | Nomor duplikat ditandai di kolom telepon saat daftar berhasil dimuat; perubahan yang belum disimpan memunculkan dialog |
| UJ-42 | Buka dialog hapus kontak utama dan kontak terakhir pada profil uji | Peringatan menjelaskan akibatnya; Batal tidak menghapus, tombol hapus dinonaktifkan saat permintaan diproses |
| UJ-43 | Muat daftar kontak, putuskan jaringan, lalu tarik untuk refresh | Daftar lama tetap tampil bersama pesan dan tombol Coba lagi |
| UJ-44 | Buka Profil dengan API pengaturan tidak tersedia | Bagian deteksi menampilkan kesalahan dan Coba lagi, bukan opsi default yang bisa disimpan |
| UJ-45 | Simpan nama pada profil uji dengan NIM dan nomor terisi; lalu simpan sensitivitas secara terpisah | NIM dan nomor tetap sama; tiap tindakan punya umpan balik sendiri |
| UJ-46 | Ubah izin di pengaturan Android lalu kembali ke Profil; tes juga switch SMS | Status izin diperbarui. Switch SMS tersimpan di perangkat tanpa menekan Simpan Pengaturan Deteksi |

### Uji trigger sensor fisik (tanpa tombol "Simulasi Jatuh")

Ambang batas default di `FallDetector.kt`: benturan **≥ 25 m/s²**, jeda **1 detik**, lalu kondisi
diam **3 detik** (mean 7,8–11,8 m/s² dan standar deviasi < 1,0). Untuk memudahkan uji fisik, buka
**Profil → Sensitivitas → Demo** lalu **Simpan Pengaturan**; ini menurunkan ambang menjadi
**15 m/s²** (masih dalam rentang valid backend 15–40). Setelah itu notifikasi 20 detik muncul.

Cara aman memicunya:

1. Pasang casing. Siapkan permukaan lunak: **kasur, bantal tebal, atau karpet tebal**.
   Jangan jatuhkan ke keramik tanpa casing.
2. Pastikan status pemantauan **AKTIF** dan izin notifikasi diberikan.
3. Setelah mengubah sensitivitas, **matikan lalu nyalakan lagi pemantauan** agar `DetectionService`
   membaca nilai terbaru (pengaturan dibaca saat service diinisialisasi).
4. Angkat HP sekitar **30 cm** dari permukaan, posisikan **mendatar**, lalu **lepaskan**
   (jangan dilempar).
5. **Jangan disentuh atau digerakkan selama ±5 detik** setelah mendarat.
6. Notifikasi merah dengan hitung mundur 20 detik harus muncul.
   - Tekan **"SAYA BAIK-BAIK SAJA"** → `cancelled`, tidak ada SMS.
   - Biarkan habis → `confirmed`, SMS dikirim.

Kalau belum muncul, lihat Logcat filter `DetectionService`:

- `Benturan tanpa pola diam, diabaikan (peak=...)` → benturan sudah terbaca, tetapi HP masih
  bergerak/bounce setelah mendarat. Ulangi di permukaan lebih rata dan jangan disentuh.
- Tidak ada log sama sekali → `peak` masih di bawah ambang (**25** default / **15** saat Demo).
  Naikkan ketinggian ke **40–50 cm** atau pakai permukaan sedikit lebih keras (tetap di atas
  bantal/karpet).
- Setelah satu percobaan, **tunggu 60 detik** karena cooldown. Tombol "Simulasi Jatuh" sengaja
  mengabaikan cooldown, tetapi trigger sensor asli tidak.

### Tentang peringatan "Izin SMS berbahaya"

Android menggolongkan `SEND_SMS` sebagai *dangerous permission* karena aplikasi berpotensi
mengirim SMS premium. Dialog peringatan muncul saat onboarding atau saat menekan tombol
**"Izinkan SMS Darurat"** di Profil. Ini perilaku **normal sistem Android**, bukan tanda
aplikasi jahat. Tekan **Allow / Izinkan**.

Penerima SMS adalah **kontak darurat di tab Kontak** (`emergency_contacts`), **bukan** nomor
telepon yang diisi di Profil/onboarding. Nomor Profil hanya identitas pengguna. Untuk menguji
ke nomor teman, tambah/ubah kontak di tab **Kontak**, lalu cek lagi di Supabase →
Table Editor → `emergency_contacts`. Seki `082176119857` harus tetap menjadi kontak utama.

Gunakan format angka saja `08xxxxxxxxxx`. Nomor seperti `+62 812-xxxx-xxxx` diterima backend,
tetapi sekarang dinormalisasi menjadi `0812xxxxxxxx` oleh `Format.phone()` sebelum dikirim.
Kalau SMS ke kontak baru tidak masuk sementara SMS ke Seki masuk, lihat baris Logcat
`SMS darurat terkirim ke <nama> (<nomor>)` — nomor di dalam kurung itulah tujuan sebenarnya.

Faktor yang memengaruhi SMS nyata: **pulsa/SMS kuota pengirim, sinyal, SIM default untuk SMS**
(pada dual-SIM), dan filter spam di ponsel penerima. Jarak antar lokasi tidak berpengaruh selama
pengirim punya sinyal. Uji pembanding tercepat: kirim SMS manual dari aplikasi *Messages* ke nomor
teman. Kalau itu gagal, masalahnya ada di SIM/operator, bukan di aplikasi.

### Kalau SMS baru masuk beberapa jam kemudian

Ini **bukan bug aplikasi**. SMS memakai mekanisme *store-and-forward*: setelah `SmsManager`
menyerahkan pesan, SMSC operator menyimpannya dan mencoba lagi sampai penerima dapat dijangkau.
Keterlambatan berjam-jam wajar bila penerima sinyalnya lemah, HP mati/*airplane mode*, roaming,
atau berbeda operator. Beberapa simulasi berturut-turut juga menghasilkan antrean pesan yang
masuk bergelombang.

Karena aplikasi tidak memasang `sentIntent`/`deliveryIntent` (lihat `AUDIT.md` temuan 9 dan 11),
status **"SMS darurat dikirimkan ke N kontak"** hanya berarti pesan masuk antrean perangkat,
bukan konfirmasi diterima penerima. Untuk pembuktian di laporan:

- Catat **waktu simulasi** dan **waktu SMS benar-benar masuk** di HP tujuan.
- Uji dengan kedua HP dalam jangkauan sinyal baik; matikan **Power Saving / Battery Saver** dan
  berikan battery unlimited untuk aplikasi *Messages* dan Fall Detection.
- Lakukan **satu simulasi**, tunggu 5–10 menit, baru ulangi. Hindari simulasi berturut-turut.
- Kalau perlu bukti paling cepat, pakai Seki `082176119857` di HP kedua yang sinyalnya stabil.

Bila izin ditolak, `SmsRepository` langsung berhenti dengan status **"Izin SMS belum diberikan"**
dan tidak ada SMS yang dikirim. Cara mengaktifkan lagi:

1. **Settings → Apps → Fall Detection → Permissions → SMS → Allow**, atau
2. Buka **Profil → Izinkan SMS Darurat**, lalu tekan Allow pada dialog.

### Kalau Settings menolak dengan pesan "dapat membahayakan informasi pribadi"

Pada Android 14+ (dan beberapa OEM), aplikasi yang dipasang lewat APK/ADB — bukan Play Store —
mendapat status **restricted settings**. Sistem menolak memberikan izin sensitif seperti SMS
sampai status itu dilepas. Ini bukan bug aplikasi.

Solusi 1 — lepas restricted settings:

1. **Settings → Apps → Fall Detection**
2. Ketuk menu **⋮** di kanan atas → **Allow restricted settings / Izinkan pengaturan terbatas**
3. Kembali ke **Permissions → SMS → Allow**
4. Samsung: cek juga **Settings → Security and privacy → Auto Blocker** → matikan atau
   tambahkan pengecualian untuk aplikasi ini.

Solusi 2 — berikan izin lewat ADB (paling pasti):

```powershell
adb devices
adb shell pm grant com.example.falldetection android.permission.SEND_SMS
adb shell dumpsys package com.example.falldetection | findstr SEND_SMS
```

Baris terakhir harus menunjukkan `SEND_SMS: granted=true`. Kalau tetap `granted=false`,
perangkat/OEM benar-benar memblokir izin SMS untuk aplikasi sideload; catat merek HP dan
versi Android untuk dicarikan pengecualian khusus OEM.

### Kalau HP tidak muncul di `adb devices`

Kalau Windows menampilkan *Unknown USB Device (Device Descriptor Request Failed)* di Device
Manager, penyebabnya biasanya **kabel charge-only/rusak atau port USB**, bukan driver. Coba:

1. Pakai kabel data original (bukan kabel khusus cas).
2. Colok ke port USB lain, sebaiknya USB 2.0 di belakang PC (bukan hub).
3. Pastikan layar HP **terbuka**, lalu colok ulang dan terima prompt *Allow USB debugging*
   (centang **Always allow from this computer**). Prompt bisa muncul sebagai notifikasi.

Kalau `adb devices` menampilkan `unauthorized`:

```powershell
adb kill-server
adb start-server
```

Lalu di HP: **Developer options → Revoke USB debugging authorizations → OK**, colok ulang kabel.
Jika masih gagal, hapus `C:\Users\<user>\.android\adbkey` dan `adbkey.pub`, restart server ADB,
lalu terima prompt baru. Xiaomi/Redmi juga perlu **USB debugging (Security settings)** aktif.

Kalau kabel tetap bermasalah, gunakan **Wireless debugging** (Android 11+) tanpa kabel:

1. HP: **Developer options → Wireless debugging → ON**
2. HP: ketuk **Pair device with pairing code**, catat `IP:port` dan kode 6 digit.
3. PC:
   ```powershell
   adb pair 192.168.1.12:37015
   ```
   lalu masukkan kode 6 digit.
4. HP: lihat `IP:port` yang tampil di halaman Wireless debugging (port berbeda dari pairing).
5. PC:
   ```powershell
   adb connect 192.168.1.12:5555
   adb devices
   ```
6. Lanjutkan `pm grant` dengan `-s 192.168.1.12:5555`.

Catatan: kalau emulator dan HP sama-sama terhubung, `adb devices` akan menampilkan dua baris.
Selalu pakai `-s <serial>` agar perintah ditujukan ke HP.

Setelah izin aktif, ulangi simulasi jatuh. Status di Beranda harus berubah dari
**"Izin SMS belum diberikan"** menjadi **"SMS darurat dikirimkan ke 4 kontak"**.

Bukti izin aktif dan pengiriman berhasil (ketiganya harus konsisten):

- Kartu status di Beranda: **"SMS darurat dikirimkan ke N kontak"**
- Logcat filter `SmsRepository`: `SMS darurat terkirim ke Seki (082176119857)`
- SMS benar-benar tampil di aplikasi *Messages* perangkat penerima

Karena Google Play membatasi izin SMS untuk aplikasi umum, tugas akhir ini memakai APK
*sideload* dengan pemberian izin manual, seperti dijelaskan di `AUDIT.md`.

### Cara membuktikan SMS terkirim tanpa SIM kedua

1. Jalankan emulator, lakukan UJ-03.
2. Logcat → filter `SmsRepository`. Akan tampil:
   `SMS darurat terkirim ke Seki (082176119857)` untuk tiap kontak.
3. Beberapa emulator juga menyimpan SMS di aplikasi *Messages*. Jika tidak, log + status di Beranda
   sudah menjadi bukti bahwa `SmsManager` dipanggil.

### Contoh isi pesan

```
DARURAT! Syauqi Akifur Rahman (NIM 21120124130097) terdeteksi jatuh pada Jumat, 20 Nov 2026, 10.32 WIB. Lokasi: https://www.google.com/maps/search/?api=1&query=-6.9932,110.4203. Mohon segera bantu.
```

Jika GPS gagal, bagian lokasi berubah menjadi `Lokasi tidak tersedia (GPS gagal)` dan aplikasi
tetap tidak crash.

---

## E. Uji pengguna (Bab 12.3 plan)

Ajak 2–3 orang non-teknis. Beri tugas tanpa bantuan:
1. Aktifkan pemantauan.
2. Batalkan peringatan palsu.
3. Tambah satu kontak darurat.
4. Lihat detail kejadian terakhir.

Catat: berhasil/tidak, waktu, salah ketuk, komentar. Perbaiki ukuran tombol/label bila diperlukan.
