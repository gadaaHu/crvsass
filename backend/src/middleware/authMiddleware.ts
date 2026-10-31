import { Request, Response, NextFunction } from 'express';
import { AuthService, User } from '../services/authService.js';
import { UserRole } from '../models/types.js';

// Extend Express Request interface to include authenticated user
export interface AuthenticatedRequest extends Request {
  user?: User;
}

/**
 * Middleware that requires a valid, cryptographically signed Bearer token.
 */
export function authenticate(req: AuthenticatedRequest, res: Response, next: NextFunction): void {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({
        success: false,
        error: 'Authentication required. Please provide a valid Bearer token.'
      });
      return;
    }

    const token = authHeader.substring(7).trim();
    const payload = AuthService.verifyToken(token);

    if (!payload || !payload.id) {
      res.status(401).json({
        success: false,
        error: 'Invalid or expired authentication token.'
      });
      return;
    }

    const user = AuthService.findById(payload.id);
    if (!user) {
      res.status(401).json({
        success: false,
        error: 'Authenticated user no longer exists.'
      });
      return;
    }

    req.user = user;
    next();
  } catch (err: any) {
    res.status(401).json({
      success: false,
      error: 'Authentication failed: ' + (err.message || 'Invalid session')
    });
  }
}

/**
 * Optional authentication middleware: if a Bearer token is provided and valid,
 * req.user is set; otherwise execution continues without error.
 */
export function optionalAuth(req: AuthenticatedRequest, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7).trim();
    const payload = AuthService.verifyToken(token);
    if (payload && payload.id) {
      const user = AuthService.findById(payload.id);
      if (user) {
        req.user = user;
      }
    }
  }
  next();
}

/**
 * Role-based access control middleware: requires authenticated user to possess one of the allowed roles.
 */
export function requireRole(...allowedRoles: UserRole[]) {
  return (req: AuthenticatedRequest, res: Response, next: NextFunction): void => {
    if (!req.user) {
      res.status(401).json({
        success: false,
        error: 'Authentication required.'
      });
      return;
    }

    if (!allowedRoles.includes(req.user.role as UserRole)) {
      res.status(403).json({
        success: false,
        error: `Access denied. Requires one of roles: [${allowedRoles.join(', ')}]. Current role: ${req.user.role}`
      });
      return;
    }

    next();
  };
}
