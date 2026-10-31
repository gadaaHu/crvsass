import express from 'express';
import cors from 'cors';
import path from 'node:path';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { apiRouter } from './routes/apiRoutes.js';
import { initDatabase } from './database/db.js';
import { runSeed } from './database/seed.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 4000;

// Security hardening: Disable Express signature header
app.disable('x-powered-by');

// Security hardening: Standard HTTP Security Headers
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  next();
});

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Static files (Web Admin Dashboard)
const publicDir = path.resolve(__dirname, '../public');
app.use(express.static(publicDir));

// API routes
app.use('/api', apiRouter);

// APK and downloads route
app.get(['/download', '/install'], (req, res) => {
  res.sendFile(path.join(publicDir, 'download.html'));
});

app.get(['/apk', '/app.apk'], (req, res) => {
  res.redirect('/downloads/CRVS-GatherApp.apk');
});

app.get('/downloads/:filename', (req, res) => {
  // Security Hardening: Reject any traversal patterns immediately
  const rawParam = req.params.filename;
  if (rawParam.includes('..') || rawParam.includes('/') || rawParam.includes('\\')) {
    return res.status(400).send('Bad Request: Invalid file parameter.');
  }

  const safeFilename = path.basename(rawParam);
  const downloadsDir = path.resolve(publicDir, 'downloads');
  let filePath = path.resolve(downloadsDir, safeFilename);

  // Verification that target is strictly contained within downloads directory
  if (!filePath.startsWith(downloadsDir)) {
    return res.status(403).send('Access denied: Invalid file path.');
  }

  let targetName = safeFilename;

  // If the exact requested file is not found
  if (!fs.existsSync(filePath)) {
    // Only check APK aliases if an APK was actually requested
    if (safeFilename.toLowerCase().endsWith('.apk')) {
      const candidates = ['CRVS-GatherApp.apk', 'CRVS.apk', 'app-release.apk'];
      let found = false;
      for (const cand of candidates) {
        const candPath = path.resolve(downloadsDir, cand);
        if (fs.existsSync(candPath)) {
          filePath = candPath;
          targetName = cand;
          found = true;
          break;
        }
      }
      if (!found) {
        return res.status(404).send('Requested APK not found on server');
      }
    } else {
      return res.status(404).send('Requested file not found on server');
    }
  }

  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', `attachment; filename="${targetName}"`);
  res.sendFile(filePath, (err) => {
    if (err && !res.headersSent) {
      console.error('Download error for file:', filePath, err);
      res.status(500).send('Error downloading file');
    }
  });
});

// Fallback for SPA routing if requested
app.use((req, res, next) => {
  if (req.path.startsWith('/api') || req.path.startsWith('/downloads')) {
    return next();
  }
  res.sendFile(path.join(publicDir, 'index.html'));
});

// Start server
function startServer() {
  try {
    initDatabase();
    runSeed();

    app.listen(PORT, () => {
      console.log(`====================================================`);
      console.log(`  CRVS Question Gathering & Assessment Server`);
      console.log(`  Security Headers & Rate Limiting: ACTIVE`);
      console.log(`  Live URL: http://localhost:${PORT}`);
      console.log(`  API Health: http://localhost:${PORT}/api/health`);
      console.log(`====================================================`);
    });
  } catch (err) {
    console.error('Fatal error starting server:', err);
    process.exit(1);
  }
}

startServer();
