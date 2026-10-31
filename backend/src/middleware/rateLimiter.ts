import { Request, Response, NextFunction } from 'express';

interface RateLimitRecord {
  count: number;
  resetTime: number;
}

const store = new Map<string, RateLimitRecord>();

// Periodic cleanup every 5 minutes to prevent memory leak
setInterval(() => {
  const now = Date.now();
  for (const [key, record] of store.entries()) {
    if (now > record.resetTime) {
      store.delete(key);
    }
  }
}, 5 * 60 * 1000).unref();

export interface RateLimitOptions {
  windowMs: number;
  max: number;
  message?: string;
  prefix?: string;
}

/**
 * Creates an in-memory rate limiting middleware per client IP.
 */
export function rateLimit(options: RateLimitOptions) {
  const { windowMs, max, message = 'Too many requests, please try again later.', prefix = 'rl' } = options;

  return (req: Request, res: Response, next: NextFunction): void => {
    // Determine client IP (respecting forward headers if behind reverse proxy/IIS)
    const forwarded = req.headers['x-forwarded-for'];
    const clientIp = typeof forwarded === 'string'
      ? forwarded.split(',')[0].trim()
      : req.socket.remoteAddress || 'unknown';

    const key = `${prefix}:${clientIp}`;
    const now = Date.now();

    let record = store.get(key);

    if (!record || now > record.resetTime) {
      record = {
        count: 1,
        resetTime: now + windowMs
      };
      store.set(key, record);
    } else {
      record.count += 1;
    }

    const remaining = Math.max(0, max - record.count);
    const retryAfterSec = Math.ceil((record.resetTime - now) / 1000);

    res.setHeader('X-RateLimit-Limit', max);
    res.setHeader('X-RateLimit-Remaining', remaining);
    res.setHeader('X-RateLimit-Reset', Math.ceil(record.resetTime / 1000));

    if (record.count > max) {
      res.setHeader('Retry-After', retryAfterSec);
      res.status(429).json({
        success: false,
        error: message,
        retryAfterSeconds: retryAfterSec
      });
      return;
    }

    next();
  };
}
