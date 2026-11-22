import { MemoryDb } from '../store.js';
import { defaultContactsFor, defaultFallsFor } from '../seed-data.js';
import { loadEnv } from './env.js';

// Pastikan .env terbaca sebelum mengecek DATABASE_URL.
loadEnv();

const numberOrNull = (v) => (v === null || v === undefined ? null : Number(v));

function mapUser(row) {
  if (!row) return null;
  return {
    id: row.id,
    full_name: row.full_name,
    nim: row.nim ?? null,
    phone: row.phone ?? null,
    created_at: row.created_at,
  };
}

function mapSettings(row) {
  if (!row) return null;
  return {
    user_id: row.user_id,
    impact_threshold: numberOrNull(row.impact_threshold) ?? 25,
    inactivity_seconds: Number(row.inactivity_seconds),
    countdown_seconds: Number(row.countdown_seconds),
    updated_at: row.updated_at,
  };
}

function mapContact(row) {
  if (!row) return null;
  return {
    id: row.id,
    user_id: row.user_id,
    name: row.name,
    phone: row.phone,
    relationship: row.relationship,
    is_primary: Boolean(row.is_primary),
    created_at: row.created_at,
  };
}

function mapFall(row) {
  if (!row) return null;
  return {
    id: row.id,
    user_id: row.user_id,
    detected_at: row.detected_at,
    latitude: numberOrNull(row.latitude),
    longitude: numberOrNull(row.longitude),
    location_accuracy: numberOrNull(row.location_accuracy),
    peak_acceleration: numberOrNull(row.peak_acceleration) ?? 0,
    inactivity_seconds: numberOrNull(row.inactivity_seconds),
    status: row.status,
    notes: row.notes ?? null,
    created_at: row.created_at,
  };
}

class PgDb {
  constructor(pool) {
    this.pool = pool;
  }

  async findUser(id) {
    const { rows } = await this.pool.query('SELECT * FROM users WHERE id = $1', [id]);
    return mapUser(rows[0]);
  }

  async createUser(data) {
    const { rows } = await this.pool.query(
      `INSERT INTO users (full_name, nim, phone) VALUES ($1, $2, $3) RETURNING *`,
      [data.full_name, data.nim ?? null, data.phone ?? null],
    );
    const userId = rows[0].id;
    await this.pool.query(
      `INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT (user_id) DO NOTHING`,
      [userId],
    );
    for (const c of defaultContactsFor(userId)) {
      await this.pool.query(
        `INSERT INTO emergency_contacts (id, user_id, name, phone, relationship, is_primary, created_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7) ON CONFLICT (id) DO NOTHING`,
        [c.id, c.user_id, c.name, c.phone, c.relationship, c.is_primary, c.created_at],
      );
    }
    for (const f of defaultFallsFor(userId)) {
      await this.pool.query(
        `INSERT INTO fall_events (id, user_id, detected_at, latitude, longitude,
           location_accuracy, peak_acceleration, inactivity_seconds, status, notes, created_at)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11) ON CONFLICT (id) DO NOTHING`,
        [f.id, f.user_id, f.detected_at, f.latitude, f.longitude, f.location_accuracy,
          f.peak_acceleration, f.inactivity_seconds, f.status, f.notes, f.created_at],
      );
    }
    return mapUser(rows[0]);
  }

  async updateUser(id, data) {
    const { rows } = await this.pool.query(
      `UPDATE users SET full_name = COALESCE($2, full_name), nim = COALESCE($3, nim),
       phone = COALESCE($4, phone) WHERE id = $1 RETURNING *`,
      [id, data.full_name ?? null, data.nim ?? null, data.phone ?? null],
    );
    return mapUser(rows[0]);
  }

  async getSettings(userId) {
    const { rows } = await this.pool.query('SELECT * FROM user_settings WHERE user_id = $1', [
      userId,
    ]);
    return mapSettings(rows[0]);
  }

  async updateSettings(userId, data) {
    const { rows } = await this.pool.query(
      `INSERT INTO user_settings (user_id, impact_threshold, inactivity_seconds, countdown_seconds)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (user_id) DO UPDATE SET impact_threshold = EXCLUDED.impact_threshold,
         inactivity_seconds = EXCLUDED.inactivity_seconds,
         countdown_seconds = EXCLUDED.countdown_seconds, updated_at = now()
       RETURNING *`,
      [
        userId,
        data.impact_threshold ?? 25,
        data.inactivity_seconds ?? 3,
        data.countdown_seconds ?? 20,
      ],
    );
    return mapSettings(rows[0]);
  }

  async listContacts(userId) {
    const { rows } = await this.pool.query(
      `SELECT * FROM emergency_contacts WHERE user_id = $1
       ORDER BY is_primary DESC, created_at ASC`,
      [userId],
    );
    return rows.map(mapContact);
  }

  async getContact(userId, id) {
    const { rows } = await this.pool.query(
      'SELECT * FROM emergency_contacts WHERE id = $1 AND user_id = $2',
      [id, userId],
    );
    return mapContact(rows[0]);
  }

  async createContact(userId, data) {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      if (data.is_primary) {
        await client.query(
          'UPDATE emergency_contacts SET is_primary = false WHERE user_id = $1',
          [userId],
        );
      }
      const { rows } = await client.query(
        `INSERT INTO emergency_contacts (user_id, name, phone, relationship, is_primary)
         VALUES ($1, $2, $3, $4, $5) RETURNING *`,
        [userId, data.name, data.phone, data.relationship, Boolean(data.is_primary)],
      );
      await client.query('COMMIT');
      return mapContact(rows[0]);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  async updateContact(userId, id, data) {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      if (data.is_primary) {
        await client.query(
          'UPDATE emergency_contacts SET is_primary = false WHERE user_id = $1 AND id <> $2',
          [userId, id],
        );
      }
      const { rows } = await client.query(
        `UPDATE emergency_contacts SET name = COALESCE($3, name), phone = COALESCE($4, phone),
         relationship = COALESCE($5, relationship),
         is_primary = COALESCE($6, is_primary)
         WHERE id = $1 AND user_id = $2 RETURNING *`,
        [
          id,
          userId,
          data.name ?? null,
          data.phone ?? null,
          data.relationship ?? null,
          data.is_primary === undefined ? null : Boolean(data.is_primary),
        ],
      );
      await client.query('COMMIT');
      return mapContact(rows[0]);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  async deleteContact(userId, id) {
    const { rowCount } = await this.pool.query(
      'DELETE FROM emergency_contacts WHERE id = $1 AND user_id = $2',
      [id, userId],
    );
    return rowCount > 0;
  }

  async listFalls(userId, { status, page, limit } = {}) {
    const { rows } = await this.pool.query(
      `SELECT * FROM fall_events WHERE user_id = $1 AND ($2::text IS NULL OR status = $2)
       ORDER BY detected_at DESC LIMIT $3 OFFSET $4`,
      [userId, status ?? null, limit, (page - 1) * limit],
    );
    return rows.map(mapFall);
  }

  async getFall(userId, id) {
    const { rows } = await this.pool.query(
      'SELECT * FROM fall_events WHERE id = $1 AND user_id = $2',
      [id, userId],
    );
    return mapFall(rows[0]);
  }

  async createFall(userId, data) {
    if (data.client_id) {
      const existing = await this.pool.query(
        'SELECT * FROM fall_events WHERE user_id = $1 AND client_id = $2',
        [userId, data.client_id],
      );
      if (existing.rows[0]) return mapFall(existing.rows[0]);
    }
    const { rows } = await this.pool.query(
      `INSERT INTO fall_events (user_id, client_id, detected_at, latitude, longitude,
         location_accuracy, peak_acceleration, inactivity_seconds, status, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) RETURNING *`,
      [
        userId,
        data.client_id ?? null,
        data.detected_at ?? new Date().toISOString(),
        data.latitude ?? null,
        data.longitude ?? null,
        data.location_accuracy ?? null,
        data.peak_acceleration,
        data.inactivity_seconds ?? null,
        data.status ?? 'confirmed',
        data.notes ?? null,
      ],
    );
    return mapFall(rows[0]);
  }

  async updateFall(userId, id, data) {
    const { rows } = await this.pool.query(
      `UPDATE fall_events SET status = COALESCE($3, status), notes = COALESCE($4, notes)
       WHERE id = $1 AND user_id = $2 RETURNING *`,
      [id, userId, data.status ?? null, data.notes ?? null],
    );
    return mapFall(rows[0]);
  }

  async deleteFall(userId, id) {
    const { rowCount } = await this.pool.query(
      'DELETE FROM fall_events WHERE id = $1 AND user_id = $2',
      [id, userId],
    );
    return rowCount > 0;
  }

  async deleteAllFalls(userId) {
    await this.pool.query('DELETE FROM fall_events WHERE user_id = $1', [userId]);
  }
}

let db = null;

export async function initDb() {
  if (db) return db;
  if (process.env.DATABASE_URL) {
    const connectionString = process.env.DATABASE_URL.trim();
    // Kesalahan umum: memasukkan URL REST Supabase / sb_publishable key sebagai DATABASE_URL.
    // pg hanya menerima URI PostgreSQL, bukan https://<ref>.supabase.co/rest/v1.
    if (!/^postgres(ql)?:\/\//i.test(connectionString)) {
      throw new Error(
        'DATABASE_URL tidak valid. Harus connection string PostgreSQL seperti ' +
        'postgresql://postgres:PASSWORD@db.<ref>.supabase.co:6543/postgres ' +
        '(bukan URL REST https://<ref>.supabase.co/rest/v1, dan bukan sb_publishable key). ' +
        'Ambil di Supabase > Settings > Database > Connection string > URI.',
      );
    }
    // pg dimuat secara dinamis agar mode memori bisa dipakai tanpa npm install.
    const { default: pg } = await import('pg');
    const { Pool } = pg;
    const pool = new Pool({
      connectionString,
      ssl: { rejectUnauthorized: false },
      max: 3,
    });
    try {
      await pool.query('SELECT 1');
    } catch (error) {
      throw new Error(
        `Gagal terhubung ke Supabase: ${error?.message ?? error}. ` +
        'Cek password database, project ref, dan pastikan tabel sudah dibuat dari database/schema.sql.',
      );
    }
    db = new PgDb(pool);
    console.info('[db] Mode PostgreSQL/Supabase aktif');
  } else {
    db = new MemoryDb();
    console.warn('[db] DATABASE_URL tidak ada -> mode memori (data seed demo)');
  }
  return db;
}

export function getDb() {
  if (!db) throw new Error('Database belum diinisialisasi');
  return db;
}
