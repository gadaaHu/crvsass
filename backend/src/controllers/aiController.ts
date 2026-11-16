import { Request, Response } from 'express';
import { SettingsService } from '../services/settingsService.js';
import { AiKnowledgeService } from '../services/aiKnowledgeService.js';

export class AiController {
  /**
   * Chat endpoint for examinees and administrators
   * Synthesizes answer from curriculum questions + uploaded PDF/Word docs + proclamation rules
   */
  static async chat(req: Request, res: Response): Promise<void> {
    try {
      const { message, language = 'am', participantCode } = req.body;

      if (!message || typeof message !== 'string' || message.trim().length === 0) {
        res.status(400).json({ success: false, error: 'Message is required' });
        return;
      }

      // Check admin authorization setting
      const isAiAllowed = SettingsService.getSetting('allow_post_test_ai_tutor', 'true');
      if (isAiAllowed === 'false') {
        res.status(403).json({
          success: false,
          error: language === 'am' 
            ? 'የ AI ረዳት ውይይት በአስተዳዳሪው ለጊዜው አልተፈቀደም።' 
            : 'AI Tutor Chat is currently disabled by administrator.'
        });
        return;
      }

      const lang = (language === 'en' ? 'en' : 'am') as 'am' | 'en';
      const answer = AiKnowledgeService.synthesizeResponse(message, lang);

      res.json({
        success: true,
        data: {
          answer,
          language: lang,
          timestamp: new Date().toISOString()
        },
        answer,
        language: lang,
        timestamp: new Date().toISOString()
      });
    } catch (err: any) {
      console.error('AI chat error:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to process AI response',
        details: err?.message || String(err)
      });
    }
  }

  /**
   * Upload and index a training document (PDF, Word .docx, or Text)
   */
  static async uploadDocument(req: Request, res: Response): Promise<void> {
    try {
      const file = (req as any).file;
      const title = (req.body.title || file?.originalname || 'Untitled Document').trim();

      if (!file) {
        // Also support base64 payload in JSON: { filename, contentBase64, title }
        const { filename, contentBase64 } = req.body;
        if (filename && contentBase64) {
          const buffer = Buffer.from(contentBase64, 'base64');
          const { text, fileType } = await AiKnowledgeService.extractTextFromFile(buffer, filename);
          const doc = AiKnowledgeService.saveDocument(filename, fileType, title, text);
          res.json({ success: true, document: doc });
          return;
        }

        res.status(400).json({ success: false, error: 'No file uploaded or file payload provided' });
        return;
      }

      const { text, fileType } = await AiKnowledgeService.extractTextFromFile(file.buffer, file.originalname);
      const doc = AiKnowledgeService.saveDocument(file.originalname, fileType, title, text);

      res.json({
        success: true,
        message: 'Document uploaded and learned by AI successfully',
        document: doc
      });
    } catch (err: any) {
      console.error('Document upload error:', err);
      res.status(500).json({
        success: false,
        error: 'Failed to process and index document',
        details: err?.message || String(err)
      });
    }
  }

  /**
   * List all learned training documents
   */
  static listDocuments(_req: Request, res: Response): void {
    try {
      const docs = AiKnowledgeService.getAllDocuments();
      res.json({
        success: true,
        documents: docs,
        total: docs.length
      });
    } catch (err: any) {
      res.status(500).json({ success: false, error: 'Failed to list documents' });
    }
  }

  /**
   * Delete a learned document
   */
  static deleteDocument(req: Request, res: Response): void {
    try {
      const id = parseInt(String(req.params.id), 10);
      if (isNaN(id)) {
        res.status(400).json({ success: false, error: 'Invalid document ID' });
        return;
      }
      const ok = AiKnowledgeService.deleteDocument(id);
      res.json({ success: ok, message: ok ? 'Document deleted' : 'Document not found' });
    } catch (err: any) {
      res.status(500).json({ success: false, error: 'Failed to delete document' });
    }
  }

  /**
   * Get knowledge base stats (questions count, docs count, character count)
   */
  static getKnowledgeStats(_req: Request, res: Response): void {
    try {
      const questions = AiKnowledgeService.getTrainingQuestions();
      const docs = AiKnowledgeService.getAllDocuments();
      const totalDocChars = docs.reduce((acc, d) => acc + d.charCount, 0);

      res.json({
        success: true,
        stats: {
          questionsCount: questions.length,
          documentsCount: docs.length,
          totalKnowledgeChars: totalDocChars,
          status: 'ONLINE'
        }
      });
    } catch (err: any) {
      res.status(500).json({ success: false, error: 'Failed to get knowledge stats' });
    }
  }
}
