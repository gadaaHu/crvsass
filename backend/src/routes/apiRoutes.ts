import { Router } from 'express';
import { SurveyController } from '../controllers/surveyController.js';
import { SubmissionController } from '../controllers/submissionController.js';
import { AnalyticsController } from '../controllers/analyticsController.js';
import { AuthController } from '../controllers/authController.js';
import { SettingsController } from '../controllers/settingsController.js';
import { authenticate, optionalAuth, requireRole } from '../middleware/authMiddleware.js';
import { JurisdictionService } from '../services/jurisdictionService.js';
import { rateLimit } from '../middleware/rateLimiter.js';

export const apiRouter = Router();

// Rate limiters for abuse prevention
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 30,
  message: 'Too many authentication attempts from this IP. Please try again after 15 minutes.',
  prefix: 'rl-auth'
});

const submissionLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  message: 'Submission rate limit reached. Please wait a moment before submitting again.',
  prefix: 'rl-sub'
});

// Jurisdictions — all 14 Ethiopian CRVS regions (Public, no auth required)
apiRouter.get('/jurisdictions', (_req, res) => {
  res.json({ success: true, data: JurisdictionService.getAllJurisdictions() });
});

// Health check endpoint (Public)
apiRouter.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'CRVS Assessment & Question Gathering System',
    version: '1.1.0',
    timestamp: new Date().toISOString()
  });
});

// Authentication & Users
apiRouter.post('/auth/signup', optionalAuth, authLimiter, AuthController.signup);
apiRouter.post('/auth/signin', authLimiter, AuthController.signin);
apiRouter.post('/auth/login', authLimiter, AuthController.signin); // Alias
apiRouter.get('/auth/me', authenticate, AuthController.me);

// User Management & Roles (Admin & Manager)
apiRouter.get(['/users', '/auth/users'], authenticate, requireRole('ADMIN', 'USER_MANAGER'), AuthController.listUsers);
apiRouter.get('/users/stats', authenticate, requireRole('ADMIN', 'USER_MANAGER'), AuthController.getUserStats);
apiRouter.get('/users/:id', authenticate, requireRole('ADMIN', 'USER_MANAGER'), AuthController.getUser);
apiRouter.post('/users', authenticate, requireRole('ADMIN', 'USER_MANAGER'), AuthController.createUser);
apiRouter.put('/users/:id', authenticate, requireRole('ADMIN', 'USER_MANAGER'), AuthController.updateUser);
apiRouter.patch(['/users/:id/role', '/auth/users/:id/role'], authenticate, requireRole('ADMIN'), AuthController.updateRole);
apiRouter.patch('/users/:id/password', authenticate, requireRole('ADMIN'), AuthController.resetPassword);
apiRouter.delete(['/users/:id', '/auth/users/:id'], authenticate, requireRole('ADMIN'), AuthController.deleteUser);

// System Settings & Configuration (Admin)
apiRouter.get('/settings', optionalAuth, SettingsController.getSettings);
apiRouter.put('/settings', authenticate, requireRole('ADMIN'), SettingsController.updateSettings);
apiRouter.post('/settings/verify-manager-code', SettingsController.verifyManagerCode);

// Surveys & Questions (Public reads, Admin-only writes)
apiRouter.get('/surveys', SurveyController.listSurveys);
apiRouter.get('/surveys/:id', SurveyController.getSurvey);
apiRouter.post('/surveys', authenticate, requireRole('ADMIN'), SurveyController.createSurvey);
apiRouter.get('/questions/:id', SurveyController.getQuestion);
apiRouter.put('/questions/:id', authenticate, requireRole('ADMIN'), SurveyController.updateQuestion);
apiRouter.post('/sections/:sectionId/questions', authenticate, requireRole('ADMIN'), SurveyController.createQuestion);
apiRouter.delete('/questions/:id', authenticate, requireRole('ADMIN'), SurveyController.deleteQuestion);

// Submissions & Gathering
apiRouter.post('/submissions', submissionLimiter, optionalAuth, SubmissionController.createSubmission);
apiRouter.get('/submissions', SubmissionController.listSubmissions);
apiRouter.get('/submissions/:id', SubmissionController.getSubmission);
apiRouter.patch('/submissions/:id/grade', authenticate, requireRole('ADMIN', 'USER_MANAGER'), SubmissionController.gradeSubmission);
apiRouter.patch('/submissions/:id/approve', authenticate, requireRole('ADMIN', 'USER_MANAGER'), SubmissionController.approveSubmission);
apiRouter.patch('/submissions/:id/reject', authenticate, requireRole('ADMIN', 'USER_MANAGER'), SubmissionController.rejectSubmission);
apiRouter.get('/submissions/:id/certificate', SubmissionController.getCertificate);
apiRouter.get('/submissions/:id/certificate/html', SubmissionController.getCertificateHtml);
apiRouter.delete('/submissions/:id', authenticate, requireRole('ADMIN'), SubmissionController.deleteSubmission);

// Certificates (Public verification)
apiRouter.get('/certificates/participant/:code', SubmissionController.getCertificatesForParticipant);
apiRouter.get('/certificates/:certNumber/html', SubmissionController.getCertificateByNumberHtml);

// Analytics & Reports
apiRouter.get('/analytics/:surveyId', AnalyticsController.getAnalytics);
apiRouter.get('/analytics/:surveyId/sections', AnalyticsController.getSectionBreakdown);
apiRouter.get('/analytics/:surveyId/live', AnalyticsController.getLiveSync);
apiRouter.get('/analytics/:surveyId/grading-queue', authenticate, requireRole('ADMIN', 'USER_MANAGER'), AnalyticsController.getGradingQueue);
apiRouter.get('/export/:surveyId/csv', AnalyticsController.exportCsv);
apiRouter.get('/export/:surveyId/json', AnalyticsController.exportJson);
