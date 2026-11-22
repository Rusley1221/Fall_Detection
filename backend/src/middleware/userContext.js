export function userContext(req, res, next) {
  const userId = req.get('X-User-Id');
  if (!userId) {
    return res.status(401).json({
      success: false,
      message: 'Header X-User-Id wajib diisi',
      errors: { user_id: 'Identitas pengguna tidak ditemukan' },
    });
  }
  const apiKey = process.env.API_KEY;
  if (apiKey && req.get('X-API-Key') !== apiKey) {
    return res.status(401).json({ success: false, message: 'Kunci API tidak valid' });
  }
  req.userId = userId;
  return next();
}
