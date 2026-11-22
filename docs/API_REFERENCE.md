# Referensi REST API (Base URL `/api/v1`)

Semua respons memakai format:

```json
{ "success": true, "message": "…", "data": { … } }
```

Gagal: `{ "success": false, "message": "…", "errors": { "kolom": "pesan" } }`.

Header wajib (kecuali `POST /users` dan `GET /health`):
`X-User-Id: <uuid>` dan opsional `X-API-Key: <API_KEY>`.

| Metode | Endpoint | Fungsi |
|---|---|---|
| GET | `/health` | Cek server aktif |
| POST | `/users` | Buat profil saat onboarding |
| GET | `/users/me` | Ambil profil |
| PUT | `/users/me` | Ubah profil (nama, NIM, telepon) |
| GET | `/settings` | Ambil pengaturan sensor |
| PUT | `/settings` | Ubah `impact_threshold`, `inactivity_seconds`, `countdown_seconds` |
| GET | `/falls?status=&page=&limit=` | Daftar kejadian terbaru |
| GET | `/falls/:id` | Detail kejadian |
| POST | `/falls` | Catat kejadian |
| PATCH | `/falls/:id` | Ubah status/catatan |
| DELETE | `/falls/:id` | Hapus kejadian |
| DELETE | `/falls` | Hapus seluruh riwayat pengguna |
| GET | `/contacts` | Daftar kontak (primary lebih dulu) |
| GET | `/contacts/:id` | Detail kontak |
| POST | `/contacts` | Tambah kontak |
| PUT | `/contacts/:id` | Ubah kontak |
| DELETE | `/contacts/:id` | Hapus kontak |

## Contoh `POST /falls`

```json
{
  "detected_at": "2026-11-20T10:32:00+07:00",
  "latitude": -6.993200,
  "longitude": 110.420300,
  "location_accuracy": 12.5,
  "peak_acceleration": 28.5,
  "inactivity_seconds": 4.0,
  "status": "confirmed",
  "notes": "Jatuh terdeteksi, SMS darurat dikirim"
}
```

Kode status: 200/201 sukses, 400 validasi, 401 header identitas hilang, 404 tidak ditemukan,
500 kesalahan server. Status kejadian： `confirmed`, `cancelled`, `resolved`.

## Mode penyimpanan

- Tanpa `DATABASE_URL`: API memakai penyimpanan **in-memory** berisi data seed (cocok untuk demo/uji).
- Dengan `DATABASE_URL`: API memakai **PostgreSQL/Supabase** melalui `pg` (skema di `database/schema.sql`).

Kunci service Supabase hanya ada di environment server, tidak pernah di aplikasi Android.
