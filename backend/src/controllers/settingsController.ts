import { Request, Response } from 'express';
import { SettingsService } from '../services/settingsService.js';
import { AuthenticatedRequest } from '../middleware/authMiddleware.js';

export class SettingsController {
  /**
   * GET /api/settings
   * Returns all system settings for ADMIN / USER_MANAGER, or public configuration for unauthenticated
   */
  static async getSettings(req: Request, res: Response): Promise<void> {
    try {
      const authReq = req as AuthenticatedRequest;
      const isAdminOrManager = authReq.user?.role === 'ADMIN' || authReq.user?.role === 'USER_MANAGER';

      if (isAdminOrManager) {
        const allSettings = SettingsService.getAllSettings();
        const settingsMap = SettingsService.getSettingsMap();
        res.json({
          success: true,
          data: {
            settings: allSettings,
            map: settingsMap
          }
        });
      } else {
        const publicSettings = SettingsService.getPublicSettings();
        res.json({
          success: true,
          data: publicSettings
        });
      }
    } catch (err: any) {
      res.status(500).json({
        success: false,
        error: err.message || 'Failed to retrieve settings'
      });
    }
  }

  /**
   * PUT /api/settings
   * Bulk or single update of system settings (ADMIN only)
   */
  static async updateSettings(req: Request, res: Response): Promise<void> {
    try {
      const body = req.body;
      if (!body || typeof body !== 'object') {
        res.status(400).json({ success: false, error: 'Invalid settings payload' });
        return;
      }

      const updatedMap = SettingsService.updateMultipleSettings(body);
      const allSettings = SettingsService.getAllSettings();

      res.json({
        success: true,
        message: 'Settings updated successfully',
        data: {
          settings: allSettings,
          map: updatedMap
        }
      });
    } catch (err: any) {
      res.status(500).json({
        success: false,
        error: err.message || 'Failed to update settings'
      });
    }
  }

  /**
   * POST /api/settings/verify-manager-code
   * Validates Post-Test unlock code dynamically against stored database configuration
   */
  static async verifyManagerCode(req: Request, res: Response): Promise<void> {
    try {
      const { code } = req.body;
      if (!code) {
        res.status(400).json({ success: false, valid: false, error: 'Authorization code is required' });
        return;
      }

      const isValid = SettingsService.verifyManagerUnlockCode(code);
      res.json({
        success: true,
        valid: isValid,
        message: isValid ? 'Authorization code verified' : 'Invalid authorization code'
      });
    } catch (err: any) {
      res.status(500).json({
        success: false,
        valid: false,
        error: err.message || 'Failed to verify manager code'
      });
    }
  }
}
