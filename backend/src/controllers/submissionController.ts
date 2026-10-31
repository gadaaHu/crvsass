import { Request, Response } from 'express';
import { SubmissionService } from '../services/submissionService.js';
import { SubmissionInput } from '../models/types.js';
import { generateCertificateHtml } from '../services/certificateTemplateRenderer.js';

export class SubmissionController {
  public static createSubmission(req: Request, res: Response): void {
    try {
      const input: SubmissionInput = req.body;
      if (!input.survey_id || !input.participant?.participant_code || !input.test_type) {
        res.status(400).json({
          success: false,
          error: 'survey_id, participant.participant_code, and test_type (PRE or POST) are required.'
        });
        return;
      }
      if (!Array.isArray(input.answers)) {
        input.answers = [];
      }

      const submission = SubmissionService.createSubmission(input);
      res.status(201).json({ success: true, data: submission });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static listSubmissions(req: Request, res: Response): void {
    try {
      const surveyId = req.query.survey_id ? Number(req.query.survey_id) : undefined;
      const testType = req.query.test_type ? String(req.query.test_type) : undefined;
      const participantCode = req.query.participant_code ? String(req.query.participant_code) : undefined;

      const submissions = SubmissionService.getSubmissions({
        survey_id: surveyId,
        test_type: testType,
        participant_code: participantCode
      });
      res.json({ success: true, count: submissions.length, data: submissions });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getSubmission(req: Request, res: Response): void {
    try {
      const id = Number(req.params.id);
      const submission = SubmissionService.getSubmissionById(id);
      if (!submission) {
        res.status(404).json({ success: false, error: `Submission with ID ${id} not found` });
        return;
      }
      res.json({ success: true, data: submission });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static gradeSubmission(req: Request, res: Response): void {
    try {
      const submissionId = Number(req.params.id);
      const { question_number, score, reviewer_notes } = req.body;

      if (question_number === undefined || score === undefined) {
        res.status(400).json({
          success: false,
          error: 'question_number and score are required for grading.'
        });
        return;
      }

      const updated = SubmissionService.gradeOpenQuestion(submissionId, Number(question_number), Number(score), reviewer_notes);
      res.json({ success: true, data: updated });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static approveSubmission(req: Request, res: Response): void {
    try {
      const submissionId = Number(req.params.id);
      const approvedBy = req.body?.approved_by || 'Manager';
      const certificate = SubmissionService.approveSubmission(submissionId, approvedBy);
      res.json({ success: true, data: certificate });
    } catch (e: any) {
      const statusCode = e.message?.includes('below the 50%') ? 422 : 500;
      res.status(statusCode).json({ success: false, error: e.message });
    }
  }

  public static rejectSubmission(req: Request, res: Response): void {
    try {
      const submissionId = Number(req.params.id);
      const rejectedBy = req.body?.rejected_by || 'Manager';
      const success = SubmissionService.rejectSubmission(submissionId, rejectedBy);
      if (!success) {
        res.status(404).json({ success: false, error: 'Submission not found' });
        return;
      }
      res.json({ success: true, message: 'Submission rejected.' });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getCertificate(req: Request, res: Response): void {
    try {
      const submissionId = Number(req.params.id);
      const cert = SubmissionService.getCertificateBySubmission(submissionId);
      if (!cert) {
        res.status(404).json({ success: false, error: 'No certificate issued for this submission.' });
        return;
      }
      res.json({ success: true, data: cert });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getCertificateHtml(req: Request, res: Response): void {
    try {
      const submissionId = Number(req.params.id);
      let cert = SubmissionService.getCertificateBySubmission(submissionId);
      if (!cert) {
        const sub = SubmissionService.getSubmissionById(submissionId);
        if (!sub) {
          res.status(404).send('<h3>Submission not found</h3>');
          return;
        }
        cert = {
          certificate_number: (sub as any).certificate_number || `CRVS-2026-CERT${sub.id}`,
          participant_code: sub.participant_code,
          full_name: sub.participant_code,
          wereda_kebele: sub.wereda_kebele || 'Dire Dawa Administration',
          role: sub.role || 'Certified Vital Events Registrar',
          gender: sub.gender || 'Female',
          survey_title_en: (sub as any).survey_title_en || 'Dire Dawa CRVS Registrar Competency Assessment',
          survey_title_am: (sub as any).survey_title_am || 'የድሬዳዋ ወሳኝ ሁነቶች ምዝገባ ብቃት ምዘና',
          test_type: sub.test_type,
          score_percentage: sub.score_percentage,
          total_score: sub.total_score,
          max_score: sub.max_score,
          performance_band: sub.performance_band,
          issued_by: (sub as any).approved_by || 'Dire Dawa CRVS Agency Director',
          issued_at: (sub as any).approved_at || sub.submitted_at,
          submission_id: sub.id
        };
      }
      const html = generateCertificateHtml(cert);
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      res.send(html);
    } catch (e: any) {
      res.status(500).send(`<h3>Error generating certificate: ${e.message}</h3>`);
    }
  }

  public static getCertificateByNumberHtml(req: Request, res: Response): void {
    try {
      const certNumber = String(req.params.certNumber);
      const cert = SubmissionService.getCertificateByNumber(certNumber);
      if (!cert) {
        res.status(404).send(`<h3>Certificate ${certNumber} not found</h3>`);
        return;
      }
      const html = generateCertificateHtml(cert);
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      res.send(html);
    } catch (e: any) {
      res.status(500).send(`<h3>Error generating certificate: ${e.message}</h3>`);
    }
  }

  public static getCertificatesForParticipant(req: Request, res: Response): void {
    try {
      const code = String(req.params.code);
      const certs = SubmissionService.getCertificatesForParticipant(code);
      res.json({ success: true, count: certs.length, data: certs });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static deleteSubmission(req: Request, res: Response): void {
    try {
      const id = Number(req.params.id);
      const success = SubmissionService.deleteSubmission(id);
      if (!success) {
        res.status(404).json({ success: false, error: `Submission ${id} not found` });
        return;
      }
      res.json({ success: true, message: `Submission ${id} deleted successfully` });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }
}
