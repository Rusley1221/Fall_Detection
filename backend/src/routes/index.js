import { Router } from 'express';
import { userContext } from '../middleware/userContext.js';
import * as users from '../controllers/users.js';
import * as settings from '../controllers/settings.js';
import * as falls from '../controllers/falls.js';
import * as contacts from '../controllers/contacts.js';

export function createRoutes() {
  const api = Router();

  api.get('/health', (req, res) => {
    res.json({ success: true, message: 'API Fall Detection aktif', data: { status: 'ok' } });
  });

  const usersRouter = Router();
  usersRouter.post('/', users.createUser); // tanpa X-User-Id
  usersRouter.get('/me', userContext, users.getUser);
  usersRouter.put('/me', userContext, users.updateUser);
  api.use('/users', usersRouter);

  const settingsRouter = Router();
  settingsRouter.use(userContext);
  settingsRouter.get('/', settings.getSettings);
  settingsRouter.put('/', settings.updateSettings);
  api.use('/settings', settingsRouter);

  const fallsRouter = Router();
  fallsRouter.use(userContext);
  fallsRouter.get('/', falls.listFalls);
  fallsRouter.post('/', falls.createFall);
  fallsRouter.delete('/', falls.deleteAllFalls);
  fallsRouter.get('/:id', falls.getFall);
  fallsRouter.patch('/:id', falls.updateFall);
  fallsRouter.delete('/:id', falls.deleteFall);
  api.use('/falls', fallsRouter);

  const contactsRouter = Router();
  contactsRouter.use(userContext);
  contactsRouter.get('/', contacts.listContacts);
  contactsRouter.post('/', contacts.createContact);
  contactsRouter.get('/:id', contacts.getContact);
  contactsRouter.put('/:id', contacts.updateContact);
  contactsRouter.delete('/:id', contacts.deleteContact);
  api.use('/contacts', contactsRouter);

  return api;
}
