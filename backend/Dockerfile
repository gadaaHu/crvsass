# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

# Install build dependencies
COPY package*.json ./
RUN npm ci

# Copy TypeScript config and sources
COPY tsconfig.json ./
COPY src ./src
COPY public ./public

# Compile TypeScript
RUN npm run build

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

# Install curl for healthcheck and su-exec for permission management
RUN apk add --no-cache curl su-exec

# Install production dependencies only
COPY package*.json ./
RUN npm ci --omit=dev

# Copy built application and static assets from builder
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/public ./public

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Setup data directory
RUN mkdir -p /app/data && chown -R node:node /app

# Expose HTTP port
EXPOSE 4000

# Health check
HEALTHCHECK --interval=20s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:4000/api/health || exit 1

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["node", "dist/server.js"]
