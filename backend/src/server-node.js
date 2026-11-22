// Server alternatif berbasis node:http (tanpa dependensi npm) untuk pengujian offline.
// Perilaku endpoint identik dengan server Express di src/server.js.
import http from 'node:http';
import { initDb } from './config/db.js';
import { userContext } from './middleware/userContext.js';
import * as users from './controllers/users.js';
import * as settings from './controllers/settings.js';
import * as falls from './controllers/falls.js';
import * as contacts from './controllers/contacts.js';

const routes = [];

function add(method, pattern, handler, protectedRoute = true) {
  const keys = [];
  const regexSource = pattern.replace(/:([A-Za-z_]+)/g, (_, key) => {
    keys.push(key);
    return '([^/]+)';
  });
  const wrapped = protectedRoute
    ? (req, res, next) => userContext(req, res, () => handler(req, res, next))
    : handler;
  routes.push({ method, regex: new RegExp(`^${regexSource}$`), keys, handler: wrapped });
}

add('GET', '/health', (req, res) => {
  res.json({ success: true, message: 'API Fall Detection aktif', data: { status: 'ok' } });
}, false);
add('POST', '/users', users.createUser, false);
add('GET', '/users/me', users.getUser);
add('PUT', '/users/me', users.updateUser);
add('GET', '/settings', settings.getSettings);
add('PUT', '/settings', settings.updateSettings);
add('GET', '/falls', falls.listFalls);
add('POST', '/falls', falls.createFall);
add('DELETE', '/falls', falls.deleteAllFalls);
add('GET', '/falls/:id', falls.getFall);
add('PATCH', '/falls/:id', falls.updateFall);
add('DELETE', '/falls/:id', falls.deleteFall);
add('GET', '/contacts', contacts.listContacts);
add('POST', '/contacts', contacts.createContact);
add('GET', '/contacts/:id', contacts.getContact);
add('PUT', '/contacts/:id', contacts.updateContact);
add('DELETE', '/contacts/:id', contacts.deleteContact);

function sendError(res, error) {
  const status = Number(error?.status) || 500;
  if (status === 500) console.error('[error]', error?.message ?? error);
  res.status(status).json({
    success: false,
    message: status === 500 ? 'Terjadi kesalahan pada server' : error.message,
    errors: error?.errors ?? undefined,
  });
}

await initDb();

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url ?? '/', `http://${req.headers.host ?? 'localhost'}`);
  const path = url.pathname.startsWith('/api/v1')
    ? url.pathname.slice('/api/v1'.length) || '/'
    : url.pathname;

  res.status = (code) => {
    res.statusCode = code;
    return res;
  };
  res.json = (payload) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.end(JSON.stringify(payload));
  };

  let rawBody = '';
  try {
    for await (const chunk of req) rawBody += chunk;
  } catch (_) {
    rawBody = '';
  }
  let body = {};
  try {
    body = rawBody ? JSON.parse(rawBody) : {};
  } catch (_) {
    body = {};
  }

  const request = {
    body,
    headers: req.headers,
    query: Object.fromEntries(url.searchParams),
    params: {},
    get(name) {
      return this.headers[String(name).toLowerCase()];
    },
  };

  const match = routes
    .filter((r) => r.method === req.method)
    .map((r) => ({ r, m: r.regex.exec(path) }))
    .find((x) => x.m);

  if (!match) {
    res.status(404).json({ success: false, message: 'Endpoint tidak ditemukan' });
    return;
  }

  match.r.keys.forEach((key, index) => {
    request.params[key] = decodeURIComponent(match.m[index + 1]);
  });

  try {
    await match.r.handler(request, res, (error) => sendError(res, error));
  } catch (error) {
    sendError(res, error);
  }
});

const port = Number(process.env.PORT) || 3000;
server.listen(port, () => {
  console.info(`[api:node] http://localhost:${port}/api/v1 (tanpa dependensi npm)`);
});
