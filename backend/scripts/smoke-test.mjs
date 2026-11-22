// Uji otomatis endpoint API. Jalankan API dulu: npm run dev
const base = process.env.API_BASE_URL ?? 'http://localhost:3000/api/v1';
const userId = '11111111-1111-1111-1111-111111111111';
const apiKey = 'fall-detection-ta-2026';

let passed = 0;
let failed = 0;

function check(name, condition, detail = '') {
  if (condition) {
    passed++;
    console.log(`  PASS  ${name}`);
  } else {
    failed++;
    console.error(`  FAIL  ${name} ${detail}`);
  }
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth) {
    headers['X-User-Id'] = userId;
    headers['X-API-Key'] = apiKey;
  }
  const response = await fetch(`${base}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await response.json();
  return { status: response.status, json };
}

console.log(`Smoke test API: ${base}`);

const health = await request('/health', { auth: false });
check('GET /health -> 200 success', health.status === 200 && health.json.success === true);

const me = await request('/users/me');
check(
  'GET /users/me -> Syauqi Akifur Rahman / 21120124130097',
  me.status === 200 &&
    me.json.data?.full_name === 'Syauqi Akifur Rahman' &&
    me.json.data?.nim === '21120124130097',
  JSON.stringify(me.json),
);

const newUser = await request('/users', {
  method: 'POST',
  auth: false,
  body: { full_name: 'Syauqi Akifur Rahman', nim: '21120124130097', phone: '082176119857' },
});
check('POST /users -> 201 + user id', newUser.status === 201 && Boolean(newUser.json.data?.id));

if (newUser.json.data?.id) {
  const freshContacts = await fetch(`${base}/contacts`, {
    headers: { 'X-User-Id': newUser.json.data.id, 'X-API-Key': apiKey },
  }).then((r) => r.json());
  check(
    'User hasil onboarding mendapat Seki pertama (4 kontak default)',
    freshContacts.data?.length === 4 &&
      freshContacts.data[0].name === 'Seki' &&
      freshContacts.data[0].phone === '082176119857',
    JSON.stringify(freshContacts.data?.[0] ?? {}),
  );
  const freshFalls = await fetch(`${base}/falls?limit=100`, {
    headers: { 'X-User-Id': newUser.json.data.id, 'X-API-Key': apiKey },
  }).then((r) => r.json());
  check('User hasil onboarding mendapat data riwayat contoh', freshFalls.data?.length >= 4);
}

const noHeader = await request('/contacts', { auth: false });
check('GET /contacts tanpa X-User-Id -> 401', noHeader.status === 401);

const contacts = await request('/contacts');
check(
  'GET /contacts -> Seki (082176119857) pertama & primary',
  contacts.status === 200 &&
    contacts.json.data?.[0]?.name === 'Seki' &&
    contacts.json.data?.[0]?.phone === '082176119857' &&
    contacts.json.data?.[0]?.is_primary === true,
  JSON.stringify(contacts.json.data?.[0] ?? {}),
);

const settings = await request('/settings');
check('GET /settings -> countdown 20', settings.status === 200 && settings.json.data?.countdown_seconds === 20);

const savedSettings = await request('/settings', {
  method: 'PUT',
  body: { impact_threshold: 25, inactivity_seconds: 3, countdown_seconds: 20 },
});
check('PUT /settings -> 200', savedSettings.status === 200 && savedSettings.json.success === true);

const falls = await request('/falls?limit=100');
check(
  'GET /falls -> minimal 8 kejadian & ada yang tanpa koordinat',
  falls.status === 200 &&
    falls.json.data?.length >= 8 &&
    falls.json.data.some((f) => f.latitude === null && f.longitude === null),
);

const filtered = await request('/falls?status=cancelled');
check(
  'GET /falls?status=cancelled -> hanya cancelled',
  filtered.status === 200 && filtered.json.data.every((f) => f.status === 'cancelled'),
);

const missing = await request('/falls/00000000-0000-4000-8000-000000000000');
check('GET /falls/:id tidak ada -> 404', missing.status === 404);

const invalid = await request('/falls', {
  method: 'POST',
  body: {
    detected_at: '2026-11-20T10:00:00+07:00',
    latitude: -6.9,
    longitude: null,
    peak_acceleration: 25,
  },
});
check('POST /falls latitude tanpa bujur -> 400', invalid.status === 400);

const created = await request('/falls', {
  method: 'POST',
  body: {
    detected_at: '2026-11-20T10:00:00+07:00',
    latitude: -6.9932,
    longitude: 110.4203,
    peak_acceleration: 28.5,
    status: 'confirmed',
    notes: 'smoke test',
  },
});
check('POST /falls valid -> 201 + id', created.status === 201 && Boolean(created.json.data?.id));

if (created.json.data?.id) {
  const id = created.json.data.id;
  const detail = await request(`/falls/${id}`);
  check('GET /falls/:id -> detail sesuai', detail.status === 200 && detail.json.data?.id === id);

  const patched = await request(`/falls/${id}`, {
    method: 'PATCH',
    body: { status: 'resolved', notes: 'smoke test selesai' },
  });
  check('PATCH /falls/:id -> 200 resolved', patched.status === 200 && patched.json.data?.status === 'resolved');

  const deleted = await request(`/falls/${id}`, { method: 'DELETE' });
  check('DELETE /falls/:id -> 200', deleted.status === 200 && deleted.json.success === true);
}

const idempotentBody = {
  detected_at: '2026-11-20T12:00:00+07:00',
  latitude: -6.99,
  longitude: 110.42,
  peak_acceleration: 27.5,
  status: 'confirmed',
  client_id: `smoke-idempotency-${Date.now()}`,
};
const idem1 = await request('/falls', { method: 'POST', body: idempotentBody });
const idem2 = await request('/falls', { method: 'POST', body: idempotentBody });
check(
  'Retry dengan client_id sama tidak menduplikasi kejadian',
  idem1.status === 201 && idem2.status === 201 && idem1.json.data?.id === idem2.json.data?.id,
);

console.log(`\nHasil: ${passed} lulus, ${failed} gagal`);
process.exitCode = failed === 0 ? 0 : 1;
