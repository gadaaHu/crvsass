import { Request, Response } from 'express';
import { AuthService } from '../services/authService.js';
import { AuthenticatedRequest } from '../middleware/authMiddleware.js';

export class AuthController {
  static async signup(req: Request, res: Response): Promise<void> {
    try {
      const { username, password, fullName, role, participantCode, weredaKebele, gender, experienceYears } = req.body;
      if (!username || !password || !fullName) {
        res.status(400).json({ success: false, error: 'Username, password, and full name are required' });
        return;
      }

      // Check if this request is from an authenticated admin creating another account
      const authReq = req as AuthenticatedRequest;
      const isAdminContext = authReq.user?.role === 'ADMIN';

      const result = AuthService.register({
        username,
        password,
        fullName,
        role: role === 'ADMIN' ? 'ADMIN' : role === 'USER_MANAGER' ? 'USER_MANAGER' : 'EXAMINEE',
        participantCode,
        weredaKebele,
        gender,
        experienceYears: experienceYears ? Number(experienceYears) : undefined,
        isAdminContext
      });

      res.status(201).json({
        success: true,
        data: result
      });
    } catch (err: any) {
      res.status(400).json({
        success: false,
        error: err.message || 'Failed to register'
      });
    }
  }

  static async signin(req: Request, res: Response): Promise<void> {
    try {
      const { username, password } = req.body;
      if (!username || !password) {
        res.status(400).json({ success: false, error: 'Username and password are required' });
        return;
      }

      const result = AuthService.login(username, password);
      res.json({
        success: true,
        data: result
      });
    } catch (err: any) {
      res.status(401).json({
        success: false,
        error: err.message || 'Authentication failed'
      });
    }
  }

  static async me(req: Request, res: Response): Promise<void> {
    try {
      const authHeader = req.headers.authorization;
      if (!authHeader) {
        res.status(401).json({ success: false, error: 'No authorization header' });
        return;
      }

      const token = authHeader.replace(/^Bearer\s+/i, '').trim();
      const payload = AuthService.verifyToken(token);
      if (!payload?.id) {
        res.status(401).json({ success: false, error: 'Invalid or expired token' });
        return;
      }

      const user = AuthService.findById(payload.id);
      if (!user) {
        res.status(404).json({ success: false, error: 'User not found' });
        return;
      }

      res.json({
        success: true,
        data: { user }
      });
    } catch (err: any) {
      res.status(401).json({
        success: false,
        error: 'Invalid or expired session'
      });
    }
  }

  static async listUsers(req: Request, res: Response): Promise<void> {
    try {
      const users = AuthService.listUsers();
      res.json({
        success: true,
        data: users
      });
    } catch (err: any) {
      res.status(500).json({
        success: false,
        error: err.message || 'Failed to list users'
      });
    }
  }

  static async updateRole(req: Request, res: Response): Promise<void> {
    try {
      const id = Number(req.params.id);
      const { role } = req.body;
      if (!role || !['ADMIN', 'USER_MANAGER', 'EXAMINEE'].includes(role)) {
        res.status(400).json({ success: false, error: 'Valid role (ADMIN, USER_MANAGER, EXAMINEE) is required' });
        return;
      }
      const updated = AuthService.updateRole(id, role as any);
      res.json({ success: true, data: updated });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  static async deleteUser(req: Request, res: Response): Promise<void> {
    try {
      const id = Number(req.params.id);
      const success = AuthService.deleteUser(id);
      res.json({ success, message: success ? 'User deleted' : 'User not found' });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }
}
