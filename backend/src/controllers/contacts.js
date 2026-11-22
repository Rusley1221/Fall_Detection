import { getDb } from '../config/db.js';
import { badRequest, notFoundError } from '../middleware/errorHandler.js';

function validateContact(body) {
  const errors = {};
  const value = body ?? {};
  if (typeof value.name !== 'string' || !value.name.trim()) {
    errors.name = 'Nama kontak wajib diisi';
  }
  if (typeof value.phone !== 'string' || !/^[0-9+ ()-]{8,20}$/.test(value.phone.trim())) {
    errors.phone = 'Nomor telepon tidak valid';
  }
  if (typeof value.relationship !== 'string' || !value.relationship.trim()) {
    errors.relationship = 'Hubungan wajib diisi';
  }
  return errors;
}

function clean(body) {
  return {
    name: body.name.trim(),
    phone: body.phone.trim(),
    relationship: body.relationship.trim(),
    is_primary: Boolean(body.is_primary),
  };
}

export async function listContacts(req, res, next) {
  try {
    const items = await getDb().listContacts(req.userId);
    res.json({ success: true, message: 'Kontak berhasil diambil', data: items });
  } catch (error) {
    next(error);
  }
}

export async function getContact(req, res, next) {
  try {
    const contact = await getDb().getContact(req.userId, req.params.id);
    if (!contact) throw notFoundError('Kontak tidak ditemukan');
    res.json({ success: true, message: 'Detail kontak berhasil diambil', data: contact });
  } catch (error) {
    next(error);
  }
}

export async function createContact(req, res, next) {
  try {
    const errors = validateContact(req.body);
    if (Object.keys(errors).length) throw badRequest('Data kontak tidak valid', errors);
    const contact = await getDb().createContact(req.userId, clean(req.body));
    res.status(201).json({ success: true, message: 'Kontak berhasil ditambah', data: contact });
  } catch (error) {
    next(error);
  }
}

export async function updateContact(req, res, next) {
  try {
    const errors = validateContact(req.body);
    if (Object.keys(errors).length) throw badRequest('Data kontak tidak valid', errors);
    const existing = await getDb().getContact(req.userId, req.params.id);
    if (!existing) throw notFoundError('Kontak tidak ditemukan');
    const contact = await getDb().updateContact(req.userId, req.params.id, clean(req.body));
    res.json({ success: true, message: 'Kontak berhasil diubah', data: contact });
  } catch (error) {
    next(error);
  }
}

export async function deleteContact(req, res, next) {
  try {
    const deleted = await getDb().deleteContact(req.userId, req.params.id);
    if (!deleted) throw notFoundError('Kontak tidak ditemukan');
    res.json({ success: true, message: 'Kontak berhasil dihapus', data: null });
  } catch (error) {
    next(error);
  }
}
