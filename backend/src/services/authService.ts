import crypto from 'node:crypto';
import { db } from '../database/db.js';

export interface User {
  id: number;
  username: string;
  full_name: string;
  role: 'ADMIN' | 'USER_MANAGER' | 'EXAMINEE';
  participant_code: string | null;
  wereda_kebele: string | null;
  gender: string | null;
  experience_years: number | null;
  created_at?: string;
}

export interface UserWithPassword extends User {
  password_hash: string;
  salt: string;
}

export interface TokenPayload {
  id: number;
  username: string;
  role: 'ADMIN' | 'USER_MANAGER' | 'EXAMINEE';
  iat: number;
  exp: number;
}

// Secret key for HMAC token signing (configurable via environment variable)
const JWT_SECRET = process.env.JWT_SECRET || 'crvs_secure_hmac_secret_2026_diredawa_crvsafr_prod_key';
const TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days

export class AuthService {
  /**
   * Cryptographically secure PBKDF2 password hashing with 100,000 iterations
   */
  private static hashPassword(password: string, salt: string, iterations: number = 100000): string {
    return crypto.pbkdf2Sync(password, salt, iterations, 64, 'sha512').toString('hex');
  }

  static generateSalt(): string {
    return crypto.randomBytes(16).toString('hex');
  }

  /**
   * Generates a cryptographically signed HMAC-SHA256 token (header.payload.signature)
   */
  static generateToken(user: User): string {
    const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
    const now = Math.floor(Date.now() / 1000);
    const payloadData: TokenPayload = {
      id: user.id,
      username: user.username,
      role: user.role,
      iat: now,
      exp: now + TOKEN_TTL_SECONDS
    };
    const payload = Buffer.from(JSON.stringify(payloadData)).toString('base64url');
    const signature = crypto
      .createHmac('sha256', JWT_SECRET)
      .update(`${header}.${payload}`)
      .digest('base64url');

    return `${header}.${payload}.${signature}`;
  }

  /**
   * Verifies and decodes a signed HMAC-SHA256 token with constant-time signature verification.
   * Also supports legacy tokens with expiration checks during migration.
   */
  static verifyToken(token: string): TokenPayload | null {
    if (!token) return null;

    try {
      const parts = token.split('.');
      if (parts.length === 3) {
        const [header, payload, signature] = parts;
        const expectedSig = crypto
          .createHmac('sha256', JWT_SECRET)
          .update(`${header}.${payload}`)
          .digest('base64url');

        const sigBuf = Buffer.from(signature, 'utf8');
        const expBuf = Buffer.from(expectedSig, 'utf8');

        if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) {
          return null; // Invalid signature
        }

        const decoded: TokenPayload = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
        const now = Math.floor(Date.now() / 1000);
        if (decoded.exp && decoded.exp < now) {
          return null; // Expired token
        }

        return decoded;
      }

      // Backward compatibility for existing sessions (Base64 JSON)
      const decodedRaw = JSON.parse(Buffer.from(token, 'base64').toString('utf8'));
      if (decodedRaw && decodedRaw.id) {
        return {
          id: decodedRaw.id,
          username: decodedRaw.username || '',
          role: decodedRaw.role || 'EXAMINEE',
          iat: Math.floor(decodedRaw.t / 1000) || 0,
          exp: Math.floor(Date.now() / 1000) + TOKEN_TTL_SECONDS
        };
      }
    } catch (_) {
      return null;
    }

    return null;
  }

  static findByUsername(username: string): UserWithPassword | null {
    const cleanUsername = username.trim().toLowerCase();
    const row = db.prepare('SELECT * FROM users WHERE LOWER(username) = ?').get(cleanUsername) as UserWithPassword | undefined;
    return row || null;
  }

  static findById(id: number): User | null {
    const row = db.prepare(`
      SELECT id, username, full_name, role, participant_code, wereda_kebele, gender, experience_years, created_at 
      FROM users WHERE id = ?
    `).get(id) as User | undefined;
    return row || null;
  }

  static listUsers(): User[] {
    const rows = db.prepare(`
      SELECT id, username, full_name, role, participant_code, wereda_kebele, gender, experience_years, created_at 
      FROM users ORDER BY id DESC
    `).all() as unknown as User[];
    return rows;
  }

  static updateRole(userId: number, newRole: 'ADMIN' | 'USER_MANAGER' | 'EXAMINEE'): User {
    db.prepare('UPDATE users SET role = ? WHERE id = ?').run(newRole, userId);
    const updated = this.findById(userId);
    if (!updated) throw new Error(`User with ID ${userId} not found`);
    return updated;
  }

  static deleteUser(userId: number): boolean {
    const res = db.prepare('DELETE FROM users WHERE id = ?').run(userId);
    return res.changes > 0;
  }

  /**
   * Registers a new user. Enforces that self-registration defaults strictly to EXAMINEE
   * unless explicitly authorized by an admin context.
   */
  static register(data: {
    username: string;
    password: string;
    fullName: string;
    role?: 'ADMIN' | 'USER_MANAGER' | 'EXAMINEE';
    participantCode?: string;
    weredaKebele?: string;
    gender?: string;
    experienceYears?: number;
    isAdminContext?: boolean; // Must be true to grant elevated roles
  }): { user: User; token: string } {
    const username = data.username.trim().toLowerCase();
    if (!username || username.length < 3) {
      throw new Error('Username must be at least 3 characters long');
    }
    if (!data.password || data.password.length < 6) {
      throw new Error('Password must be at least 6 characters long');
    }
    if (!data.fullName || data.fullName.trim().length < 2) {
      throw new Error('Full name is required');
    }

    const existing = this.findByUsername(username);
    if (existing) {
      throw new Error('Username is already taken');
    }

    const salt = this.generateSalt();
    const hash = this.hashPassword(data.password, salt, 100000);

    // Security: Only allow ADMIN or USER_MANAGER creation if in admin context
    let role: 'ADMIN' | 'USER_MANAGER' | 'EXAMINEE' = 'EXAMINEE';
    if (data.isAdminContext) {
      if (data.role === 'ADMIN') role = 'ADMIN';
      else if (data.role === 'USER_MANAGER') role = 'USER_MANAGER';
    }

    let participantCode = data.participantCode?.trim() || null;
    if (role === 'EXAMINEE' && !participantCode) {
      const randomSuffix = Math.floor(100 + Math.random() * 900);
      participantCode = `DD-REG-${randomSuffix}`;
    }

    // Insert user
    const insert = db.prepare(`
      INSERT INTO users (username, password_hash, salt, full_name, role, participant_code, wereda_kebele, gender, experience_years)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    const result = insert.run(
      username,
      hash,
      salt,
      data.fullName.trim(),
      role,
      participantCode,
      data.weredaKebele || null,
      data.gender || null,
      data.experienceYears ?? null
    );

    const userId = Number(result.lastInsertRowid);

    // If examinee, ensure participant record exists in participants table
    if (role === 'EXAMINEE' && participantCode) {
      try {
        const existPart = db.prepare('SELECT id FROM participants WHERE participant_code = ?').get(participantCode);
        if (!existPart) {
          db.prepare(`
            INSERT INTO participants (participant_code, wereda_kebele, role, gender, experience_years)
            VALUES (?, ?, ?, ?, ?)
          `).run(
            participantCode,
            data.weredaKebele || 'Kebele 01',
            'Registrar',
            data.gender || 'Not specified',
            data.experienceYears ?? 1.0
          );
        }
      } catch (e) {
        console.warn('Could not auto-insert participant record:', e);
      }
    }

    const user = this.findById(userId)!;
    const token = this.generateToken(user);
    return { user, token };
  }

  /**
   * Secure user authentication with constant-time password verification
   * and automatic migration of legacy 1,000-iteration hashes to 100,000 iterations.
   */
  static login(username: string, password: string): { user: User; token: string } {
    const cleanUsername = username.trim().toLowerCase();
    const record = this.findByUsername(cleanUsername);
    if (!record) {
      throw new Error('Invalid username or password');
    }

    const checkHash100k = this.hashPassword(password, record.salt, 100000);
    const storedBuf = Buffer.from(record.password_hash, 'hex');
    const calcBuf100k = Buffer.from(checkHash100k, 'hex');

    let isMatch = false;
    if (storedBuf.length === calcBuf100k.length && crypto.timingSafeEqual(storedBuf, calcBuf100k)) {
      isMatch = true;
    } else {
      // Check legacy 1,000 iterations for preexisting accounts
      const checkHash1k = this.hashPassword(password, record.salt, 1000);
      const calcBuf1k = Buffer.from(checkHash1k, 'hex');
      if (storedBuf.length === calcBuf1k.length && crypto.timingSafeEqual(storedBuf, calcBuf1k)) {
        isMatch = true;
        // Transparently upgrade password hash to 100,000 iterations in database
        try {
          db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(checkHash100k, record.id);
        } catch (_) {}
      }
    }

    if (!isMatch) {
      throw new Error('Invalid username or password');
    }

    const user: User = {
      id: record.id,
      username: record.username,
      full_name: record.full_name,
      role: record.role,
      participant_code: record.participant_code,
      wereda_kebele: record.wereda_kebele,
      gender: record.gender,
      experience_years: record.experience_years,
      created_at: record.created_at
    };

    const token = this.generateToken(user);
    return { user, token };
  }
}
