// Data seed untuk demo. Kontak "Seki" sengaja ditempatkan paling atas
// sebagai kontak pengujian SMS (nomor perangkat Android kedua).
import { randomUUID } from 'node:crypto';

const DEMO_USER_ID = '11111111-1111-1111-1111-111111111111';

/** Kontak default untuk user baru (onboarding), dengan Seki sebagai kontak utama. */
export function defaultContactsFor(userId) {
  const base = Date.now();
  const createdAt = (daysAgo) => new Date(base - daysAgo * 86_400_000).toISOString();
  return [
    {
      id: randomUUID(),
      user_id: userId,
      name: 'Seki',
      phone: '082176119857',
      relationship: 'anak',
      is_primary: true,
      created_at: createdAt(9),
    },
    {
      id: randomUUID(),
      user_id: userId,
      name: 'Bapak (decoy)',
      phone: '081234567801',
      relationship: 'ayah',
      is_primary: false,
      created_at: createdAt(8),
    },
    {
      id: randomUUID(),
      user_id: userId,
      name: 'Ibu (decoy)',
      phone: '081234567802',
      relationship: 'ibu',
      is_primary: false,
      created_at: createdAt(7),
    },
    {
      id: randomUUID(),
      user_id: userId,
      name: 'Tetangga (decoy)',
      phone: '081234567803',
      relationship: 'tetangga',
      is_primary: false,
      created_at: createdAt(6),
    },
  ];
}

/** Contoh kejadian agar halaman Riwayat/Detail langsung bisa diuji setelah onboarding. */
export function defaultFallsFor(userId) {
  const base = Date.now();
  const at = (minutesAgo) => new Date(base - minutesAgo * 60_000).toISOString();
  return [
    {
      id: randomUUID(), user_id: userId, detected_at: at(0),
      latitude: -6.9932, longitude: 110.4203, location_accuracy: 12.5,
      peak_acceleration: 28.5, inactivity_seconds: 4.0, status: 'confirmed',
      notes: 'Data contoh: terdeteksi di depan rumah', created_at: at(0),
    },
    {
      id: randomUUID(), user_id: userId, detected_at: at(180),
      latitude: -6.9951, longitude: 110.4188, location_accuracy: 20.0,
      peak_acceleration: 31.2, inactivity_seconds: 5.1, status: 'resolved',
      notes: 'Data contoh: sudah dibantu', created_at: at(179),
    },
    {
      id: randomUUID(), user_id: userId, detected_at: at(400),
      latitude: null, longitude: null, location_accuracy: null,
      peak_acceleration: 26.4, inactivity_seconds: 3.4, status: 'confirmed',
      notes: 'Data contoh: GPS tidak tersedia', created_at: at(399),
    },
    {
      id: randomUUID(), user_id: userId, detected_at: at(900),
      latitude: -7.0012, longitude: 110.4311, location_accuracy: 9.0,
      peak_acceleration: 27.1, inactivity_seconds: 3.8, status: 'cancelled',
      notes: 'Data contoh: peringatan palsu', created_at: at(899),
    },
  ];
}

function iso(offsetMinutes) {
  const base = Date.parse('2026-11-14T10:32:00+07:00');
  return new Date(base + offsetMinutes * 60_000).toISOString();
}

export function seedData() {
  return {
    users: [
      {
        id: DEMO_USER_ID,
        full_name: 'Syauqi Akifur Rahman',
        nim: '21120124130097',
        phone: '082176119857',
        created_at: iso(-60 * 24 * 10),
      },
    ],
    settings: [
      {
        user_id: DEMO_USER_ID,
        impact_threshold: 25,
        inactivity_seconds: 3,
        countdown_seconds: 20,
        updated_at: iso(-60 * 24 * 10),
      },
    ],
    contacts: [
      {
        id: 'c0000000-0000-4000-8000-000000000001',
        user_id: DEMO_USER_ID,
        name: 'Seki',
        phone: '082176119857',
        relationship: 'anak',
        is_primary: true,
        created_at: iso(-60 * 24 * 9),
      },
      {
        id: 'c0000000-0000-4000-8000-000000000002',
        user_id: DEMO_USER_ID,
        name: 'Bapak (decoy)',
        phone: '081234567801',
        relationship: 'ayah',
        is_primary: false,
        created_at: iso(-60 * 24 * 8),
      },
      {
        id: 'c0000000-0000-4000-8000-000000000003',
        user_id: DEMO_USER_ID,
        name: 'Ibu (decoy)',
        phone: '081234567802',
        relationship: 'ibu',
        is_primary: false,
        created_at: iso(-60 * 24 * 7),
      },
      {
        id: 'c0000000-0000-4000-8000-000000000004',
        user_id: DEMO_USER_ID,
        name: 'Tetangga (decoy)',
        phone: '081234567803',
        relationship: 'tetangga',
        is_primary: false,
        created_at: iso(-60 * 24 * 6),
      },
    ],
    falls: [
      {
        id: 'f0000000-0000-4000-8000-000000000001',
        user_id: DEMO_USER_ID,
        detected_at: iso(0),
        latitude: -6.9932,
        longitude: 110.4203,
        location_accuracy: 12.5,
        peak_acceleration: 28.5,
        inactivity_seconds: 4.0,
        status: 'confirmed',
        notes: 'Terdeteksi di depan rumah',
        created_at: iso(0.2),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000002',
        user_id: DEMO_USER_ID,
        detected_at: iso(-180),
        latitude: -6.9951,
        longitude: 110.4188,
        location_accuracy: 20.0,
        peak_acceleration: 31.2,
        inactivity_seconds: 5.1,
        status: 'resolved',
        notes: 'Sudah dibantu tetangga',
        created_at: iso(-179.8),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000003',
        user_id: DEMO_USER_ID,
        detected_at: iso(-400),
        latitude: null,
        longitude: null,
        location_accuracy: null,
        peak_acceleration: 26.4,
        inactivity_seconds: 3.4,
        status: 'confirmed',
        notes: 'GPS tidak tersedia',
        created_at: iso(-399.8),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000004',
        user_id: DEMO_USER_ID,
        detected_at: iso(-900),
        latitude: -7.0012,
        longitude: 110.4311,
        location_accuracy: 9.0,
        peak_acceleration: 27.1,
        inactivity_seconds: 3.8,
        status: 'cancelled',
        notes: 'Peringatan palsu, pengguna baik-baik saja',
        created_at: iso(-899.8),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000005',
        user_id: DEMO_USER_ID,
        detected_at: iso(-1500),
        latitude: -6.9904,
        longitude: 110.4249,
        location_accuracy: 15.5,
        peak_acceleration: 29.8,
        inactivity_seconds: 4.6,
        status: 'resolved',
        notes: null,
        created_at: iso(-1499.8),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000006',
        user_id: DEMO_USER_ID,
        detected_at: iso(-2800),
        latitude: -6.9887,
        longitude: 110.4165,
        location_accuracy: 30.0,
        peak_acceleration: 25.6,
        inactivity_seconds: 3.1,
        status: 'confirmed',
        notes: null,
        created_at: iso(-2799.8),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000007',
        user_id: DEMO_USER_ID,
        detected_at: iso(-4300),
        latitude: -6.9968,
        longitude: 110.4277,
        location_accuracy: 11.0,
        peak_acceleration: 33.4,
        inactivity_seconds: 6.2,
        status: 'resolved',
        notes: 'Dibawa ke klinik',
        created_at: iso(-4299.8),
      },
      {
        id: 'f0000000-0000-4000-8000-000000000008',
        user_id: DEMO_USER_ID,
        detected_at: iso(-7000),
        latitude: -6.9911,
        longitude: 110.4222,
        location_accuracy: 18.0,
        peak_acceleration: 24.9,
        inactivity_seconds: 3.0,
        status: 'cancelled',
        notes: 'Telepon jatuh dari meja',
        created_at: iso(-6999.8),
      },
    ],
  };
}

export { DEMO_USER_ID };
