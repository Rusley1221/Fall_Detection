import { getDb } from '../config/db.js';
import { badRequest, notFoundError } from '../middleware/errorHandler.js';

const STATUSES = ['confirmed', 'cancelled', 'resolved'];

function validateFallPayload(body, partial = false) {
  const errors = {};
  const value = body ?? {};
  if (!partial || value.detected_at !== undefined) {
    if (typeof value.detected_at !== 'string' || Number.isNaN(Date.parse(value.detected_at))) {
      errors.detected_at = 'Waktu kejadian tidak valid';
    }
  }
  const hasLat = value.latitude !== null && value.latitude !== undefined;
  const hasLng = value.longitude !== null && value.longitude !== undefined;
  if (hasLat !== hasLng) {
    errors.latitude = 'Lintang dan bujur harus diisi berdua atau kosong berdua';
  }
  if (hasLat) {
    const lat = Number(value.latitude);
    const lng = Number(value.longitude);
    if (!Number.isFinite(lat) || lat < -90 || lat > 90) errors.latitude = 'Lintang di luar rentang';
    if (!Number.isFinite(lng) || lng < -180 || lng > 180) errors.longitude = 'Bujur di luar rentang';
  }
  const peak = Number(value.peak_acceleration);
  if (!partial && (!Number.isFinite(peak) || peak < 0 || peak > 200)) {
    errors.peak_acceleration = 'Nilai percepatan tidak valid';
  }
  if (value.status !== undefined && !STATUSES.includes(value.status)) {
    errors.status = 'Status harus confirmed, cancelled, atau resolved';
  }
  return errors;
}

export async function listFalls(req, res, next) {
  try {
    const status = req.query.status ?? null;
    if (status && !STATUSES.includes(status)) {
      throw badRequest('Filter status tidak valid', { status: 'Status tidak dikenal' });
    }
    const page = Math.max(1, Number.parseInt(req.query.page ?? '1', 10) || 1);
    const limit = Math.min(100, Math.max(1, Number.parseInt(req.query.limit ?? '20', 10) || 20));
    const items = await getDb().listFalls(req.userId, { status, page, limit });
    res.json({
      success: true,
      message: 'Riwayat berhasil diambil',
      data: items,
      meta: { page, limit },
    });
  } catch (error) {
    next(error);
  }
}

export async function getFall(req, res, next) {
  try {
    const fall = await getDb().getFall(req.userId, req.params.id);
    if (!fall) throw notFoundError('Kejadian tidak ditemukan');
    res.json({ success: true, message: 'Detail kejadian berhasil diambil', data: fall });
  } catch (error) {
    next(error);
  }
}

export async function createFall(req, res, next) {
  try {
    const body = { ...(req.body ?? {}), status: req.body?.status ?? 'confirmed' };
    const errors = validateFallPayload(body);
    if (Object.keys(errors).length) throw badRequest('Data kejadian tidak valid', errors);
    const fall = await getDb().createFall(req.userId, {
      detected_at: body.detected_at,
      latitude: body.latitude ?? null,
      longitude: body.longitude ?? null,
      location_accuracy: body.location_accuracy ?? null,
      peak_acceleration: Number(body.peak_acceleration),
      inactivity_seconds: body.inactivity_seconds ?? null,
      status: body.status,
      notes: body.notes ?? null,
      client_id: typeof body.client_id === 'string' ? body.client_id : null,
    });
    res.status(201).json({ success: true, message: 'Kejadian jatuh berhasil dicatat', data: fall });
  } catch (error) {
    next(error);
  }
}

export async function updateFall(req, res, next) {
  try {
    const errors = validateFallPayload(req.body ?? {}, true);
    if (Object.keys(errors).length) throw badRequest('Data kejadian tidak valid', errors);
    const existing = await getDb().getFall(req.userId, req.params.id);
    if (!existing) throw notFoundError('Kejadian tidak ditemukan');
    const fall = await getDb().updateFall(req.userId, req.params.id, {
      status: req.body?.status,
      notes: req.body?.notes,
    });
    res.json({ success: true, message: 'Kejadian berhasil diubah', data: fall });
  } catch (error) {
    next(error);
  }
}

export async function deleteFall(req, res, next) {
  try {
    const deleted = await getDb().deleteFall(req.userId, req.params.id);
    if (!deleted) throw notFoundError('Kejadian tidak ditemukan');
    res.json({ success: true, message: 'Kejadian berhasil dihapus', data: null });
  } catch (error) {
    next(error);
  }
}

export async function deleteAllFalls(req, res, next) {
  try {
    await getDb().deleteAllFalls(req.userId);
    res.json({ success: true, message: 'Seluruh riwayat berhasil dihapus', data: null });
  } catch (error) {
    next(error);
  }
}
