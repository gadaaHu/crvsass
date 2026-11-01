import { db } from '../database/db.js';
import { SystemSetting } from '../models/types.js';

export const DEFAULT_SETTINGS: Array<SystemSetting> = [
  {
    key: 'system_name',
    value: 'CRVS Question Gathering & Assessment System (የወሳኝ ኩነቶች ምዘና ሥርዓት)',
    description: 'System title displayed on dashboard and headers',
    category: 'GENERAL'
  },
  {
    key: 'agency_name',
    value: 'Dire Dawa Administration CRVS Agency (ድሬዳዋ አስተዳደር ወሳኝ ኩነቶች ኤጀንሲ)',
    description: 'Government Agency / Organization Name',
    category: 'GENERAL'
  },
  {
    key: 'manager_unlock_code',
    value: 'CRVS2026',
    description: 'Manager authorization code required to unlock Post-Test assessment for examinees',
    category: 'SECURITY'
  },
  {
    key: 'passing_score_percentage',
    value: '50',
    description: 'Minimum percentage score required to pass assessment and obtain competency certification',
    category: 'ASSESSMENT'
  },
  {
    key: 'post_test_requires_manager',
    value: 'true',
    description: 'Enforce manager authorization code before examinees can take Post-Test',
    category: 'ASSESSMENT'
  },
  {
    key: 'allow_self_registration',
    value: 'true',
    description: 'Allow new examinees to register directly on the portal sign-up form',
    category: 'SECURITY'
  },
  {
    key: 'max_exam_duration_mins',
    value: '60',
    description: 'Maximum recommended duration for full assessment (minutes)',
    category: 'ASSESSMENT'
  },
  {
    key: 'certificate_signatory_name',
    value: 'Ato Solomon Worku (አቶ ሰለሞን ወርቁ)',
    description: 'Official signatory name appearing on approved competency certificates',
    category: 'CERTIFICATE'
  },
  {
    key: 'certificate_signatory_title',
    value: 'Dire Dawa CRVS Agency Director (የድሬዳዋ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር)',
    description: 'Official signatory title appearing on approved competency certificates',
    category: 'CERTIFICATE'
  }
];

export class SettingsService {
  /**
   * Seed default settings if they do not exist
   */
  static initDefaultSettings(): void {
    const insert = db.prepare(`
      INSERT OR IGNORE INTO settings (key, value, description, category)
      VALUES (?, ?, ?, ?)
    `);

    for (const item of DEFAULT_SETTINGS) {
      insert.run(item.key, item.value, item.description || '', item.category || 'GENERAL');
    }
  }

  /**
   * Get all settings records
   */
  static getAllSettings(): SystemSetting[] {
    this.initDefaultSettings();
    const rows = db.prepare('SELECT key, value, description, category, updated_at FROM settings ORDER BY category, key').all() as unknown as SystemSetting[];
    return rows;
  }

  /**
   * Get settings as key-value dictionary
   */
  static getSettingsMap(): Record<string, string> {
    const rows = this.getAllSettings();
    const map: Record<string, string> = {};
    for (const row of rows) {
      map[row.key] = row.value;
    }
    return map;
  }

  /**
   * Get public-safe settings (omitting secret manager unlock code for unauthenticated examinees)
   */
  static getPublicSettings(): Record<string, any> {
    const map = this.getSettingsMap();
    return {
      system_name: map.system_name || 'CRVS Assessment System',
      agency_name: map.agency_name || 'Dire Dawa Administration CRVS Agency',
      passing_score_percentage: Number(map.passing_score_percentage) || 50,
      post_test_requires_manager: map.post_test_requires_manager === 'true',
      allow_self_registration: map.allow_self_registration !== 'false',
      max_exam_duration_mins: Number(map.max_exam_duration_mins) || 60,
      certificate_signatory_name: map.certificate_signatory_name || 'Dire Dawa CRVS Agency Director',
      certificate_signatory_title: map.certificate_signatory_title || 'Agency Director'
    };
  }

  /**
   * Get single setting by key
   */
  static getSetting(key: string, defaultValue: string = ''): string {
    const row = db.prepare('SELECT value FROM settings WHERE key = ?').get(key) as { value: string } | undefined;
    if (row && row.value !== undefined) {
      return row.value;
    }
    return defaultValue;
  }

  /**
   * Update or create a single setting
   */
  static updateSetting(key: string, value: string): void {
    db.prepare(`
      INSERT INTO settings (key, value, updated_at)
      VALUES (?, ?, CURRENT_TIMESTAMP)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = CURRENT_TIMESTAMP
    `).run(key, value);
  }

  /**
   * Update multiple settings in one transaction
   */
  static updateMultipleSettings(settings: Record<string, string>): Record<string, string> {
    const updateStmt = db.prepare(`
      INSERT INTO settings (key, value, updated_at)
      VALUES (?, ?, CURRENT_TIMESTAMP)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated_at = CURRENT_TIMESTAMP
    `);

    db.exec('BEGIN TRANSACTION');
    try {
      for (const [key, val] of Object.entries(settings)) {
        if (typeof val === 'string') {
          updateStmt.run(key, val);
        } else if (val !== null && val !== undefined) {
          updateStmt.run(key, String(val));
        }
      }
      db.exec('COMMIT');
    } catch (err) {
      db.exec('ROLLBACK');
      throw err;
    }

    return this.getSettingsMap();
  }

  /**
   * Validate Manager Unlock Code against stored configuration
   */
  static verifyManagerUnlockCode(code: string): boolean {
    if (!code) return false;
    const storedCode = this.getSetting('manager_unlock_code', 'CRVS2026');
    return code.trim().toUpperCase() === storedCode.trim().toUpperCase();
  }
}
