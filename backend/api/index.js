import { createApp } from '../src/app.js';
import { initDb } from '../src/config/db.js';

let appPromise = null;

export default async function handler(req, res) {
  if (!appPromise) appPromise = initDb().then(() => createApp());
  const app = await appPromise;
  return app(req, res);
}
