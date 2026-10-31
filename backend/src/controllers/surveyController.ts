import { Request, Response } from 'express';
import { SurveyService } from '../services/surveyService.js';
import { AuthService } from '../services/authService.js';

export class SurveyController {
  public static listSurveys(req: Request, res: Response): void {
    try {
      const surveys = SurveyService.getAllSurveys();
      res.json({ success: true, data: surveys });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getSurvey(req: Request, res: Response): void {
    try {
      const { id } = req.params;
      const survey = SurveyService.getSurveyById(id as string);
      if (!survey) {
        res.status(404).json({ success: false, error: `Survey '${id}' not found` });
        return;
      }

      // Determine if requester is an authorized admin/manager
      let isPrivileged = false;
      const authHeader = req.headers['authorization'] as string | undefined;
      if (authHeader && authHeader.startsWith('Bearer ')) {
        const token = authHeader.slice(7);
        const user = AuthService.verifyToken(token);
        if (user && (user.role === 'ADMIN' || user.role === 'USER_MANAGER')) {
          isPrivileged = true;
        }
      }

      // Strip is_correct from options for non-privileged callers
      const safeData = isPrivileged ? survey : {
        ...survey,
        sections: (survey as any).sections?.map((sec: any) => ({
          ...sec,
          questions: sec.questions?.map((q: any) => ({
            ...q,
            options: q.options?.map((opt: any) => {
              const { is_correct, ...safeOpt } = opt;
              return safeOpt;
            })
          }))
        }))
      };

      res.json({ success: true, data: safeData });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static createSurvey(req: Request, res: Response): void {
    try {
      const { code, title_am } = req.body;
      if (!code || !title_am) {
        res.status(400).json({ success: false, error: 'code and title_am are required fields' });
        return;
      }
      const created = SurveyService.createSurvey(req.body);
      res.status(201).json({ success: true, data: created });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static getQuestion(req: Request, res: Response): void {
    try {
      const id = Number(req.params.id);
      const question = SurveyService.getQuestionById(id);
      if (!question) {
        res.status(404).json({ success: false, error: `Question ${id} not found` });
        return;
      }
      res.json({ success: true, data: question });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static updateQuestion(req: Request, res: Response): void {
    try {
      const id = Number(req.params.id);
      const updated = SurveyService.updateQuestion(id, req.body);
      res.json({ success: true, data: updated });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static createQuestion(req: Request, res: Response): void {
    try {
      const sectionId = Number(req.params.sectionId);
      if (!req.body.prompt_am) {
        res.status(400).json({ success: false, error: 'prompt_am is required' });
        return;
      }
      const created = SurveyService.createQuestion(sectionId, req.body);
      res.status(201).json({ success: true, data: created });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }

  public static deleteQuestion(req: Request, res: Response): void {
    try {
      const id = Number(req.params.id);
      const success = SurveyService.deleteQuestion(id);
      res.json({ success, message: success ? 'Question deleted successfully' : 'Question not found' });
    } catch (e: any) {
      res.status(500).json({ success: false, error: e.message });
    }
  }
}
