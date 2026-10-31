import { Request, Response } from 'express';
import { AnalyticsService } from '../services/analyticsService.js';
import { SubmissionService } from '../services/submissionService.js';

export class AnalyticsController {
  public static getAnalytics(req: Request, res: Response): void {
    try {
      const surveyId = Number(req.params.surveyId);
      const summary = AnalyticsService.getSurveyAnalytics(surveyId);
      res.json({ success: true, data: summary });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static exportCsv(req: Request, res: Response): void {
    try {
      const surveyId = Number(req.params.surveyId);
      const csv = AnalyticsService.exportSubmissionsCsv(surveyId);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="qcafr_survey_${surveyId}_submissions.csv"`);
      res.send(csv);
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static exportJson(req: Request, res: Response): void {
    try {
      const surveyId = Number(req.params.surveyId);
      const submissions = SubmissionService.getSubmissions({ survey_id: surveyId });
      const detailed = submissions.map(s => SubmissionService.getSubmissionById(s.id!));
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', `attachment; filename="qcafr_survey_${surveyId}_export.json"`);
      res.json({ success: true, count: detailed.length, data: detailed });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getSectionBreakdown(req: Request, res: Response): void {
    try {
      const surveyId = Number(req.params.surveyId);
      const breakdown = AnalyticsService.getSectionBreakdown(surveyId);
      res.json({ success: true, data: breakdown });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getLiveSync(req: Request, res: Response): void {
    try {
      const surveyId = Number(req.params.surveyId);
      const sync = AnalyticsService.getLiveSync(surveyId);
      res.json({ success: true, data: sync });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getGradingQueue(req: Request, res: Response): void {
    try {
      const surveyId = Number(req.params.surveyId);
      const queue = AnalyticsService.getUngradedResponses(surveyId);
      res.json({ success: true, data: queue });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }
}

