import { getDb } from '../config/db.js';
import { badRequest, notFoundError } from '../middleware/errorHandler.js';

export async function createUser(req, res, next) {
  try {
    const { full_name, phone, nim } = req.body ?? {};
    const errors = {};
    if (typeof full_name !== 'string' || !full_name.trim()) {
      errors.full_name = 'Nama wajib diisi';
    }
    if (nim !== undefined && nim !== null && typeof nim !== 'string') {
      errors.nim = 'NIM harus berupa teks';
    }
    if (Object.keys(errors).length) throw badRequest('Data profil tidak valid', errors);

    const user = await getDb().createUser({
      full_name: full_name.trim(),
      phone: phone?.trim() || null,
      nim: nim?.trim() || null,
    });
    res.status(201).json({ success: true, message: 'Profil berhasil dibuat', data: user });
  } catch (error) {
    next(error);
  }
}

export async function getUser(req, res, next) {
  try {
    const user = await getDb().findUser(req.userId);
    if (!user) throw notFoundError('Profil tidak ditemukan');
    res.json({ success: true, message: 'Profil berhasil diambil', data: user });
  } catch (error) {
    next(error);
  }
}

export async function updateUser(req, res, next) {
  try {
    const { full_name, phone, nim } = req.body ?? {};
    if (full_name !== undefined && (typeof full_name !== 'string' || !full_name.trim())) {
      throw badRequest('Data profil tidak valid', { full_name: 'Nama tidak boleh kosong' });
    }
    const existing = await getDb().findUser(req.userId);
    if (!existing) throw notFoundError('Profil tidak ditemukan');
    const user = await getDb().updateUser(req.userId, {
      full_name: full_name?.trim(),
      phone: phone?.trim(),
      nim: nim?.trim(),
    });
    res.json({ success: true, message: 'Profil berhasil diubah', data: user });
  } catch (error) {
    next(error);
  }
}
