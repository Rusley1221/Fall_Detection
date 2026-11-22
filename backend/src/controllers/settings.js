import { getDb } from '../config/db.js';
import { badRequest } from '../middleware/errorHandler.js';

export async function getSettings(req, res, next) {
  try {
    const settings = (await getDb().getSettings(req.userId)) ?? {
      user_id: req.userId,
      impact_threshold: 25,
      inactivity_seconds: 3,
      countdown_seconds: 20,
    };
    res.json({ success: true, message: 'Pengaturan berhasil diambil', data: settings });
  } catch (error) {
    next(error);
  }
}

export async function updateSettings(req, res, next) {
  try {
    const { impact_threshold, inactivity_seconds, countdown_seconds } = req.body ?? {};
    const errors = {};
    const impact = Number(impact_threshold);
    const inactivity = Number(inactivity_seconds);
    const countdown = Number(countdown_seconds);
    if (!Number.isFinite(impact) || impact < 15 || impact > 40) {
      errors.impact_threshold = 'Ambang benturan harus 15 sampai 40';
    }
    if (!Number.isInteger(inactivity) || inactivity < 1 || inactivity > 10) {
      errors.inactivity_seconds = 'Lama diam harus 1 sampai 10 detik';
    }
    if (!Number.isInteger(countdown) || countdown < 10 || countdown > 60) {
      errors.countdown_seconds = 'Hitung mundur harus 10 sampai 60 detik';
    }
    if (Object.keys(errors).length) throw badRequest('Pengaturan tidak valid', errors);

    const settings = await getDb().updateSettings(req.userId, {
      impact_threshold: impact,
      inactivity_seconds: inactivity,
      countdown_seconds: countdown,
    });
    res.json({ success: true, message: 'Pengaturan berhasil disimpan', data: settings });
  } catch (error) {
    next(error);
  }
}
