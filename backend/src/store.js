import { randomUUID } from 'node:crypto';
import { defaultContactsFor, defaultFallsFor, seedData } from './seed-data.js';

const now = () => new Date().toISOString();

/** Penyimpanan in-memory untuk pengujian lokal tanpa Supabase. */
export class MemoryDb {
  constructor() {
    const s = seedData();
    this.users = s.users;
    this.settings = s.settings;
    this.contacts = s.contacts;
    this.falls = s.falls;
  }

  async init() {}

  async findUser(id) {
    return this.users.find((u) => u.id === id) ?? null;
  }

  async createUser(data) {
    const user = {
      id: data.id ?? randomUUID(),
      full_name: data.full_name,
      nim: data.nim ?? null,
      phone: data.phone ?? null,
      created_at: now(),
    };
    const existing = this.users.some((u) => u.id === user.id);
    if (!existing) this.users.push(user);
    if (!this.settings.some((s) => s.user_id === user.id)) {
      this.settings.push({
        user_id: user.id,
        impact_threshold: 25,
        inactivity_seconds: 3,
        countdown_seconds: 20,
        updated_at: now(),
      });
    }
    // Setiap user baru (hasil onboarding) mendapat Seki + decoy dan data contoh.
    // Tanpa ini aplikasi fresh melihat kontak kosong dan SMS darurat gagal.
    if (!this.contacts.some((c) => c.user_id === user.id)) {
      this.contacts.push(...defaultContactsFor(user.id));
    }
    if (!this.falls.some((f) => f.user_id === user.id)) {
      this.falls.push(...defaultFallsFor(user.id));
    }
    return user;
  }

  async updateUser(id, data) {
    const user = await this.findUser(id);
    if (!user) return null;
    if (data.full_name !== undefined) user.full_name = data.full_name;
    if (data.nim !== undefined) user.nim = data.nim;
    if (data.phone !== undefined) user.phone = data.phone;
    return user;
  }

  async getSettings(userId) {
    return (
      this.settings.find((s) => s.user_id === userId) ?? {
        user_id: userId,
        impact_threshold: 25,
        inactivity_seconds: 3,
        countdown_seconds: 20,
        updated_at: now(),
      }
    );
  }

  async updateSettings(userId, data) {
    let s = this.settings.find((x) => x.user_id === userId);
    if (!s) {
      s = { user_id: userId, updated_at: now() };
      this.settings.push(s);
    }
    Object.assign(s, data, { user_id: userId, updated_at: now() });
    return s;
  }

  async listContacts(userId) {
    return this.contacts
      .filter((c) => c.user_id === userId)
      .sort(
        (a, b) =>
          Number(b.is_primary) - Number(a.is_primary) ||
          a.created_at.localeCompare(b.created_at),
      );
  }

  async getContact(userId, id) {
    return this.contacts.find((c) => c.id === id && c.user_id === userId) ?? null;
  }

  async createContact(userId, data) {
    if (data.is_primary) {
      for (const c of this.contacts) {
        if (c.user_id === userId) c.is_primary = false;
      }
    }
    const contact = {
      id: randomUUID(),
      user_id: userId,
      name: data.name,
      phone: data.phone,
      relationship: data.relationship,
      is_primary: Boolean(data.is_primary),
      created_at: now(),
    };
    this.contacts.push(contact);
    return contact;
  }

  async updateContact(userId, id, data) {
    const contact = await this.getContact(userId, id);
    if (!contact) return null;
    if (data.is_primary) {
      for (const c of this.contacts) {
        if (c.user_id === userId) c.is_primary = false;
      }
    }
    Object.assign(contact, {
      name: data.name ?? contact.name,
      phone: data.phone ?? contact.phone,
      relationship: data.relationship ?? contact.relationship,
      is_primary: data.is_primary === undefined ? contact.is_primary : Boolean(data.is_primary),
    });
    return contact;
  }

  async deleteContact(userId, id) {
    const index = this.contacts.findIndex((c) => c.id === id && c.user_id === userId);
    if (index < 0) return false;
    this.contacts.splice(index, 1);
    return true;
  }

  async listFalls(userId, { status, page, limit } = {}) {
    let items = this.falls.filter((f) => f.user_id === userId);
    if (status) items = items.filter((f) => f.status === status);
    items.sort((a, b) => b.detected_at.localeCompare(a.detected_at));
    const start = (page - 1) * limit;
    return items.slice(start, start + limit);
  }

  async getFall(userId, id) {
    return this.falls.find((f) => f.id === id && f.user_id === userId) ?? null;
  }

  async createFall(userId, data) {
    // Idempotensi: retry WorkManager dengan client_id yang sama tidak membuat duplikat.
    if (data.client_id) {
      const existing = this.falls.find(
        (f) => f.user_id === userId && f.client_id === data.client_id,
      );
      if (existing) return existing;
    }
    const fall = {
      id: randomUUID(),
      client_id: data.client_id ?? null,
      user_id: userId,
      detected_at: data.detected_at ?? now(),
      latitude: data.latitude ?? null,
      longitude: data.longitude ?? null,
      location_accuracy: data.location_accuracy ?? null,
      peak_acceleration: data.peak_acceleration,
      inactivity_seconds: data.inactivity_seconds ?? null,
      status: data.status ?? 'confirmed',
      notes: data.notes ?? null,
      created_at: now(),
    };
    this.falls.push(fall);
    return fall;
  }

  async updateFall(userId, id, data) {
    const fall = await this.getFall(userId, id);
    if (!fall) return null;
    if (data.status !== undefined) fall.status = data.status;
    if (data.notes !== undefined) fall.notes = data.notes;
    return fall;
  }

  async deleteFall(userId, id) {
    const index = this.falls.findIndex((f) => f.id === id && f.user_id === userId);
    if (index < 0) return false;
    this.falls.splice(index, 1);
    return true;
  }

  async deleteAllFalls(userId) {
    this.falls = this.falls.filter((f) => f.user_id !== userId);
  }
}
