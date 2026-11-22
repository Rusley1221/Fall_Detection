-- Skema PostgreSQL untuk Fall Detection (Supabase)
-- Jalankan di SQL Editor Supabase. Ekstensi pgcrypto diperlukan untuk gen_random_uuid().
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name TEXT NOT NULL,
  nim TEXT,
  phone TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_settings (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  impact_threshold REAL NOT NULL DEFAULT 25.0,
  inactivity_seconds INT NOT NULL DEFAULT 3,
  countdown_seconds INT NOT NULL DEFAULT 20,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS emergency_contacts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  relationship TEXT NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS fall_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_id TEXT,
  detected_at TIMESTAMPTZ NOT NULL,
  latitude NUMERIC(9,6),
  longitude NUMERIC(9,6),
  location_accuracy REAL,
  peak_acceleration REAL NOT NULL,
  inactivity_seconds REAL,
  status TEXT NOT NULL DEFAULT 'confirmed'
    CHECK (status IN ('confirmed','cancelled','resolved')),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (latitude BETWEEN -90 AND 90),
  CHECK (longitude BETWEEN -180 AND 180),
  CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_falls_user_time ON fall_events (user_id, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_contacts_user ON emergency_contacts (user_id);
-- Idempotensi antrean offline: client_id berasal dari ID lokal Room di Android.
CREATE UNIQUE INDEX IF NOT EXISTS idx_falls_client_id
  ON fall_events (user_id, client_id) WHERE client_id IS NOT NULL;

-- Migrasi untuk database yang sudah dibuat sebelum kolom client_id ditambahkan:
-- ALTER TABLE fall_events ADD COLUMN IF NOT EXISTS client_id TEXT;

-- Row Level Security opsional. Kunci service hanya dipakai di server, jadi RLS tidak wajib
-- untuk TA ini. Aktifkan jika later ingin memakai kunci anon di klien.
