export function notFound(req, res) {
  res.status(404).json({ success: false, message: 'Endpoint tidak ditemukan' });
}

export function errorHandler(error, req, res, next) {
  if (res.headersSent) return next(error);
  console.error('[error]', error?.message ?? error);
  const status = Number(error?.status) || 500;
  res.status(status).json({
    success: false,
    message: status === 500 ? 'Terjadi kesalahan pada server' : error.message,
    errors: error?.errors ?? undefined,
  });
}

export function badRequest(message, errors) {
  const error = new Error(message);
  error.status = 400;
  error.errors = errors;
  return error;
}

export function notFoundError(message = 'Data tidak ditemukan') {
  const error = new Error(message);
  error.status = 404;
  return error;
}
