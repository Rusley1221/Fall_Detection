import { createApp } from './app.js';
import { initDb } from './config/db.js';

const port = Number(process.env.PORT) || 3000;

await initDb();
const app = createApp();

app.listen(port, () => {
  console.info(`[api] http://localhost:${port}/api/v1`);
});
