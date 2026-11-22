# Step by Step: dari ZIP sampai APK dan GitHub

## Langkah 0 — Persiapan

1. Install **Android Studio Ladybug (2024.2.1) atau lebih baru** (disarankan versi 2025.x).
2. Saat instalasi/first run, pastikan terpasang:
   - **Android SDK Platform 35** (SDK Manager → SDK Platforms → Android 15/API 35)
   - **Android SDK Build-Tools 35.x**
   - **JDK 17** (Android Studio menyediakannya; `Settings → Build Tools → Gradle → Gradle JDK = 17`)
3. Install **Node.js 18 LTS atau lebih baru** (untuk menjalankan REST API).
4. Ekstrak `FallDetection_TA_Syauqi.zip` ke folder tanpa spasi, misalnya `D:\TA\FallDetection_TA`.

> Jika Android Studio menampilkan peringatan *gradle-wrapper.jar missing*, hapus folder
> `android/gradle/wrapper` lalu Syncagain, atau jalankan `gradle wrapper` dari Terminal Android Studio.
> Android Studio akan memakai Gradle bundlenya sendiri (proyek memakai AGP 8.7.3 / Gradle 8.9).

---

## Langkah 1 — Menjalankan REST API (backend)

API harus hidup karena seluruh list/detail di aplikasi berasal dari server.

### Opsi A. Mode memori (paling cepat untuk demo)

```powershell
cd D:\TA\FallDetection_TA\backend
npm install
npm run dev
```

Tunggu sampai tampil:

```
[db] DATABASE_URL tidak ada -> mode memori (data seed demo)
[api] http://localhost:3000/api/v1
```

> ⚠️ **Mode memori hilang setiap server di-restart.** Kalau backend dihentikan lalu dijalankan
> lagi, semua user/kontak/riwayat yang dibuat lewat aplikasi akan hilang, sementara HP masih
> menyimpan `X-User-Id` lama. Gejalanya: `GET /users/me` → **404 "Profil tidak ditemukan"**,
> kontak & riwayat kosong, tapi `/settings` tetap 200 (server membuat pengaturan default untuk
> user ID yang sudah tidak ada). **Solusi cepat: Clear data aplikasi → onboarding ulang.**
> Solusi permanen: pakai mode Supabase (Opsi B) supaya data persist.

Cek dari browser: `http://localhost:3000/api/v1/health`

Data seed otomatis aktif: user **Syauqi Akifur Rahman / 21120124130097**, kontak **Seki
082176119857** di urutan pertama, 3 kontak decoy, dan 8 kejadian.

### Opsi B. Mode Supabase/PostgreSQL (sesuai Bab 5–6 plan)

1. Buat project di https://supabase.com → SQL Editor → jalankan isi `database/schema.sql`,
   lalu `database/seed.sql`.
2. Ambil connection string PostgreSQL **dari dashboard Supabase**:
   1. Buka https://supabase.com/dashboard → pilih project Anda.
   2. Klik tombol **Connect** di kanan atas halaman project.
   3. Pilih **Session pooler** (paling aman untuk PC/Windows yang hanya punya IPv4).
      Alternatif: **Transaction pooler** atau **Direct connection** bila jaringan mendukung.
   4. Salin URI yang muncul, lalu tempel ke `backend/.env`. **Ganti hanya passwordnya**;
      host, port, dan username sudah benar di dalam URI itu.

   Contoh bentuknya (host asli **tidak boleh ditebak**):
   ```env
   DATABASE_URL=postgresql://postgres.[PROJECT-REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:5432/postgres
   API_KEY=fall-detection-ta-2026
   PORT=3000
   ```
   Catatan penting:
   - Pooler memakai username `postgres.[PROJECT-REF]` dan host `aws-...pooler.supabase.com`,
     **bukan** `postgres` + `db.[REF].supabase.co`. Menebak host menyebabkan `ENOTFOUND`.
   - `API_KEY` di sini adalah kunci **REST API tugas ini**, harus sama persis dengan
     `API_KEY` di `android/app/build.gradle.kts` (`fall-detection-ta-2026`).
   - File `.env` dibaca otomatis oleh backend (`src/config/env.js`), tidak perlu `node --env-file`.

   Kesalahan umum yang sekarang ditolak backend dengan pesan jelas:
   - ❌ `DATABASE_URL=https://<ref>.supabase.co/rest/v1/` → itu URL **Data API REST**, bukan PostgreSQL.
   - ❌ `DATABASE_URL` diisi `sb_publishable_...` → itu publishable key Data API, bukan connection string.
   - ❌ `API_KEY=sb_publishable_...` → semua request akan **401 Kunci API tidak valid**.
   - ❌ `ENOTFOUND db.<ref>.supabase.co` → host salah; salin ulang dari tombol **Connect**.
3. Matikan server lama, lalu jalankan `npm run dev` (atau `npm run dev:offline`) lagi.
   Log **harus** berubah menjadi `[db] Mode PostgreSQL/Supabase aktif`.
   Kalau masih muncul `[db] DATABASE_URL tidak ada -> mode memori`, berarti `.env` belum terbaca:
   cek nama file harus tepat `.env` dan berada di folder `backend/`, bukan `backend/src/`.
4. **Clear data aplikasi Android** (Settings → Apps → Fall Detection → Storage → Clear data),
   lalu onboarding ulang. User lama yang dibuat saat mode memori tidak ada di Supabase, jadi
   `X-User-Id`-nya tidak akan cocok dengan baris baru. Setelah onboarding, kontak & riwayat
   akan tersimpan di `user_id` yang baru di Supabase.
5. Verifikasi: tambah kontak / simulasi jatuh, lalu refresh Supabase → Table Editor →
   `emergency_contacts` dan `fall_events`. Baris terbaru harus muncul.

### Alamat API dari Android

| Perangkat | Nilai di `android/gradle.properties` |
|---|---|
| Emulator Android Studio | `apiBaseUrl=http://10.0.2.2:3000/api/v1/` (sudah bawaan) |
| HP fisik, WiFi sama dengan PC | `apiBaseUrl=http://IP_LAN_PC:3000/api/v1/` |
| Release / Vercel | `apiBaseUrlRelease=https://<nama-app>.vercel.app/api/v1/` |

Cek IP LAN PC: `ipconfig` → IPv4 Address, misal `192.168.1.7`.
Ubah nilai di `android/gradle.properties`, klik **Sync Now**, lalu build ulang APK.
`build.gradle.kts` di paket sumber terbaru otomatis membaca dua properti tersebut; tidak perlu
menyunting kode Gradle. **Catatan:** kalau proyek Anda masih memiliki baris
`buildConfigField("String", "API_BASE_URL", "\"http://...\"")` secara langsung di dalam
`defaultConfig`/`buildTypes.release` (versi lama), edit baris itu sendiri, lalu **Sync Now**.
Cek hasilnya lewat Logcat filter `OkHttp` saat membuka Kontak/Profil.

> `RetrofitClient.normalizeBaseUrl()` otomatis menambahkan `/api/v1/` dan trailing `/`,
> jadi `http://10.0.2.2:3000`, `.../api/v1`, dan `.../api/v1/` semuanya diterima.
> Bila tetap muncul **HTTP 404**, cek Logcat filter `OkHttp` untuk melihat URLrequest yang
> sebenarnya dipanggil, lalu pastikan backend berjalan di `http://<host>:3000/api/v1/health`.

Syarat agar HP fisik bisa menjangkau API:
1. PC dan HP terhubung ke WiFi yang sama (bukan guest network/AP isolation).
2. Backend berjalan di port 3000 dan listen di semua interface (default Node.js sudah demikian;
   `server.listen(port)` tanpa host artinya `0.0.0.0`).
3. Windows Firewall mengizinkan **Node.js** untuk jaringan **Private**.
4. Uji dari browser HP: `http://IP_LAN_PC:3000/api/v1/health` → harus muncul JSON `success: true`.

---

## Langkah 2 — Membuka dan menyinkronkan proyek Android

1. Android Studio → **File → Open** → pilih folder `D:\TA\FallDetection_TA\android` (bukan root TA).
2. Tunggu **Gradle Sync** selesai. Jika diminta *Trust Project*, klik Trust.
3. Jika ada prompt *Android SDK missing*, install SDK 35 melalui SDK Manager.
4. Pastikan tidak ada error merah di tab **Build**.

---

## Langkah 3 — Menjalankan aplikasi

1. Pilih perangkat:
   - **Emulator**: Device Manager → Create Device → Pixel 7 / API 35 → Start.
   - **HP fisik**: aktifkan Developer Options → USB Debugging → hubungkan via USB.
2. Klik tombol **Run 'app'** (shift+F10).
3. Proses onboarding:
   - Langkah 1: baca penjelasan → **Lanjut**
   - Langkah 2: nama sudah terisi `Syauqi Akifur Rahman`, NIM `21120124130097` → **Lanjut**
   - Langkah 3: berikan izin **Lokasi**, **Notifikasi** (Android 13+), dan **SMS Darurat** → **Selesai & Mulai**
     - Saat meminta izin SMS, Android menampilkan peringatan *"dangerous permission"*. Itu normal —
       tekan **Allow / Izinkan**. Kalau ditolak, SMS darurat tidak dikirim; aktifkan lagi lewat
       Settings → Apps → Fall Detection → Permissions → SMS → Allow.
4. Beranda muncul dengan status *Pemantauan NONAKTIF*. Tekan **Aktifkan Pemantauan**.
5. Notifikasi tetap tampil: *"Pemantauan jatuh aktif"*.

---

## Langkah 4 — Mencoba alur jatuh + SMS darurat (demo cepat)

1. Di Beranda, tekan **Simulasi Jatuh** (bagian Mode Uji).
2. Layar merah hitung mundur muncul (20 detik default):
   - **SAYA BAIK-BAIK SAJA** → kejadian tersimpan sebagai `cancelled`, **SMS tidak dikirim**.
   - Dibiarkan sampai 0 → kejadian `confirmed`, lokasi diambil, lalu **SMS darurat dikirim**
     ke Seki + seluruh kontak lain.
   - **Minta Bantuan Sekarang** → langsung `confirmed` + SMS tanpa menunggu.
3. Lihat Logcat dengan filter `SmsRepository` untuk memastikan pengiriman:
   ```
   SMS darurat terkirim ke Seki (082176119857)
   ```
4. Buka tab **Riwayat** → ketuk kejadian teratas → **Detail Kejadian** → **Buka di Google Maps**.
5. Buka tab **Kontak** → pastikan **Seki** berada paling atas dengan penanda *Utama*.

> Detail prosedur pengujian ada di [TESTING.md](TESTING.md).

---

## Langkah 5 — Build APK

### APK Debug (cepat, langsung terpasang)

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`
Klik *locate* lalu salin ke HP dan install (izinkan instal dari sumber unknown).

### APK Release (signed, untuk penyerahan)

1. **Build → Generate Signed Bundle / APK → APK**
2. **Create new…** keystore:
   - Key store path: misal `D:\TA\fall-detection.jks`
   - Password: minimal 6 karakter (simpan baik-baik; `.jks` sudah masuk `.gitignore`)
   - Key alias: `fall`, validity 25 tahun
3. Pilih modul `app`, build type **release**.
4. **Sebelum release**, arahkan build release ke HTTPS Vercel:
   - Versi terbaru: isi `apiBaseUrlRelease=https://<nama-app>.vercel.app/api/v1/` di
     `android/gradle.properties`.
   - Versi lama yang hardcoded: ganti `"https://YOUR-APP.vercel.app/api/v1/"` pada
     `buildConfigField` di dalam `buildTypes.release`.
   - Demo LAN memakai HTTP? Tambahkan `android:usesCleartextTraffic="true"` pada tag `<application>`
     di `AndroidManifest.xml`, atau deploy HTTPS. Jangan pakai HTTP untuk produksi.
5. Finish → output: `android/app/build/outputs/apk/release/app-release.apk`.

### Opsi: deploy API ke Vercel (HTTPS, disarankan untuk release)

1. Pastikan `backend/.env` **tidak** ikut ter-upload (sudah masuk `.gitignore`).
2. Vercel → **Add New → Project** → import repository GitHub.
3. Saat import, set **Root Directory = `backend`** — `vercel.json` dan `api/index.js` ada di folder
   itu. Framework preset **Other**; build command dan output directory dikosongkan.
4. **Environment Variables** → tambahkan:
   - `DATABASE_URL` = URI PostgreSQL **Session pooler** dari Supabase → Connect → Session pooler.
     Bukan URL `https://...supabase.co/rest/v1/`, bukan pula `sb_publishable_...`.
   - `API_KEY` = `fall-detection-ta-2026` (harus sama dengan `API_KEY` di Android).
   - `PORT` tidak perlu diisi; Vercel menentukan port sendiri.
5. Deploy → tunggu status **Ready** → salin URL, misal `https://fall-detection-ta.vercel.app`.
6. Uji dari browser/HP:
   ```
   https://<nama-app>.vercel.app/api/v1/health
   ```
   Harus merespons `success: true`.
7. Isi URL itu ke Android seperti di Langkah 5 butir 4, lalu **Sync Now** dan build ulang APK release.

Alternatif CLI (jalankan dari folder `backend`):

```powershell
cd backend
npm install -g vercel
vercel login
vercel --prod
```

### troubleshooting Vercel

- Log runtime menampilkan `[db] DATABASE_URL tidak ada -> mode memori` → environment variable
  belum terbaca. Pastikan `DATABASE_URL` dan `API_KEY` ditambahkan di **Settings → Environment
  Variables** untuk environment **Production**, lalu **Redeploy**. File `backend/.env` lokal
  **tidak** dibaca Vercel.
- Log menampilkan `Invalid export found in module ".../src/app.js"` → Vercel salah mendeteksi
  framework. `backend/vercel.json` sudah berisi `"framework": null` dan `backend/src/app.js`
  memiliki `export default createApp()`. Pastikan perubahan itu ikut ter-push ke GitHub.

---

## Langkah 6 — Unggah ke GitHub (wajib kriteria tugas)

```powershell
cd D:\TA\FallDetection_TA
git init
git checkout -b main
git add .
git commit -m "Initial commit: Fall Detection TA - Syauqi Akifur Rahman 21120124130097"
git branch -M main
git remote add origin https://github.com/<username>/fall-detection-ta.git
git push -u origin main
```

Pastikan:
- `.env`, `node_modules/`, `local.properties`, dan file `.jks` **tidak** ikut terunggah.
- README, Postman collection, schema SQL, dan source code ada di repository.
- Salin **URL repository** ke kolom GitHub di form pengumpulan tugas.

---

## Langkah 7 — Verifikasi akhir sebelum dikumpulkan

- [ ] API `/health` merespons `success: true`
- [ ] Emulator/HP bisa membuka Beranda, Riwayat, Kontak, Profil (bottom navigation)
- [ ] Riwayat & detail menampilkan data dari API (bukan hardcode)
- [ ] Simulasi jatuh → cancelled → muncul di riwayat
- [ ] Simulasi jatuh → confirmed → SMS terkirim ke Seki (082176119857)
- [ ] Google Maps terbuka di koordinat kejadian
- [ ] APK debug/release berhasil dibuat
- [ ] Repository GitHub publik & link dicantumkan
