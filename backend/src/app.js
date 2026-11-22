import express from 'express';
import cors from 'cors';
import { createRoutes } from './routes/index.js';
import { errorHandler, notFound } from './middleware/errorHandler.js';

export function createApp() {
  const app = express();
  app.use(cors());
  app.use(express.json({ limit: '256kb' }));

  app.get('/health', (req, res) => {
    res.json({ success: true, message: 'API Fall Detection aktif', data: { status: 'ok' } });
  });

  app.use('/api/v1', createRoutes());
  app.use(notFound);
  app.use(errorHandler);
  return app;
}

// Diperlukan agar Vercel tidak menganggap file ini sebagai entry point framework.
export default createApp();
