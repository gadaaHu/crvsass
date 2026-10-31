import { db } from '../database/db.js';
import { SubmissionInput, SubmissionRecord, AnswerRecord } from '../models/types.js';
import { ScoringService } from './scoringService.js';
import { randomBytes } from 'node:crypto';

function generateCertificateNumber(): string {
  const year = new Date().getFullYear();
  const rand = randomBytes(4).toString('hex').toUpperCase();
  return `CRVS-${year}-${rand}`;
}

export class SubmissionService {
  public static createSubmission(input: SubmissionInput): SubmissionRecord {
    db.exec('BEGIN TRANSACTION');
    try {
      // 1. Upsert participant
      let participantId: number;
      const existingParticipant = db.prepare('SELECT id FROM participants WHERE participant_code = ?')
        .get(input.participant.participant_code) as { id: number } | undefined;

      if (existingParticipant) {
        participantId = existingParticipant.id;
        db.prepare(`
          UPDATE participants
          SET wereda_kebele = COALESCE(?, wereda_kebele),
              role = COALESCE(?, role),
              gender = COALESCE(?, gender),
              experience_years = COALESCE(?, experience_years)
          WHERE id = ?
        `).run(
          input.participant.wereda_kebele || null,
          input.participant.role || null,
          input.participant.gender || null,
          input.participant.experience_years ?? null,
          participantId
        );
      } else {
        const pRes = db.prepare(`
          INSERT INTO participants (participant_code, wereda_kebele, role, gender, experience_years)
          VALUES (?, ?, ?, ?, ?)
        `).run(
          input.participant.participant_code,
          input.participant.wereda_kebele || null,
          input.participant.role || null,
          input.participant.gender || null,
          input.participant.experience_years ?? null
        );
        participantId = Number(pRes.lastInsertRowid);
      }

      // 2. Evaluate and score submission
      const evaluation = ScoringService.evaluateSubmission(input.survey_id, input.answers);

      // 3. Insert submission record
      const feedbackRatingsJson = input.feedback_ratings ? JSON.stringify(input.feedback_ratings) : null;
      const subRes = db.prepare(`
        INSERT INTO submissions (
          survey_id, participant_id, test_type, total_score, max_score, score_percentage,
          performance_band, confidence_digital, confidence_law, feedback_ratings_json, feedback_text, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).run(
        input.survey_id,
        participantId,
        input.test_type,
        evaluation.total_score,
        evaluation.max_score,
        evaluation.score_percentage,
        evaluation.performance_band,
        input.confidence_digital ?? null,
        input.confidence_law ?? null,
        feedbackRatingsJson,
        input.feedback_text || null,
        input.notes || null
      );
      const submissionId = Number(subRes.lastInsertRowid);

      // 4. Insert individual answers
      const insAnswer = db.prepare(`
        INSERT INTO answers (
          submission_id, question_id, question_number, selected_option_key, answer_text,
          rating_value, score_awarded, is_correct, rubric_applied
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `);

      for (const ans of evaluation.answers) {
        insAnswer.run(
          submissionId,
          ans.question_id,
          ans.question_number,
          ans.selected_option_key || null,
          ans.answer_text || null,
          ans.rating_value ?? null,
          ans.score_awarded,
          ans.is_correct ? 1 : 0,
          ans.rubric_applied || null
        );
      }

      db.exec('COMMIT');

      return this.getSubmissionById(submissionId)!;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }

  public static getSubmissions(filter?: { survey_id?: number; test_type?: string; participant_code?: string }): SubmissionRecord[] {
    let sql = `
      SELECT 
        s.*,
        p.participant_code,
        p.wereda_kebele,
        p.role,
        p.gender,
        p.experience_years,
        sur.title_am as survey_title_am,
        sur.title_en as survey_title_en
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      JOIN surveys sur ON s.survey_id = sur.id
      WHERE 1=1
    `;
    const params: any[] = [];

    if (filter?.survey_id) {
      sql += ' AND s.survey_id = ?';
      params.push(filter.survey_id);
    }
    if (filter?.test_type) {
      sql += ' AND s.test_type = ?';
      params.push(filter.test_type);
    }
    if (filter?.participant_code) {
      sql += ' AND p.participant_code LIKE ?';
      params.push(`%${filter.participant_code}%`);
    }

    sql += ' ORDER BY s.submitted_at DESC';

    const rows = db.prepare(sql).all(...params) as any[];
    return rows.map(r => ({
      ...r,
      total_score: Number(r.total_score),
      max_score: Number(r.max_score),
      score_percentage: Number(r.score_percentage)
    }));
  }

  public static getSubmissionById(id: number): SubmissionRecord | null {
    const row = db.prepare(`
      SELECT 
        s.*,
        p.participant_code,
        p.wereda_kebele,
        p.role,
        p.gender,
        p.experience_years,
        sur.title_am as survey_title_am,
        sur.title_en as survey_title_en
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      JOIN surveys sur ON s.survey_id = sur.id
      WHERE s.id = ?
    `).get(id) as any;

    if (!row) return null;

    const answersRows = db.prepare(`
      SELECT 
        a.*,
        q.prompt_am,
        q.prompt_en,
        q.points as max_points,
        q.type as question_type,
        q.rubric_am,
        q.rubric_en,
        (SELECT option_key FROM question_options WHERE question_id = q.id AND is_correct = 1 LIMIT 1) as correct_option_key
      FROM answers a
      JOIN questions q ON a.question_id = q.id
      WHERE a.submission_id = ?
      ORDER BY a.question_number ASC
    `).all(id) as any[];

    return {
      ...row,
      total_score: Number(row.total_score),
      max_score: Number(row.max_score),
      score_percentage: Number(row.score_percentage),
      answers: answersRows.map(a => ({
        ...a,
        is_correct: Boolean(a.is_correct),
        score_awarded: Number(a.score_awarded),
        max_points: Number(a.max_points)
      }))
    };
  }

  public static gradeOpenQuestion(submissionId: number, questionNumber: number, score: number, reviewerNotes?: string): SubmissionRecord {
    db.exec('BEGIN TRANSACTION');
    try {
      db.prepare(`
        UPDATE answers
        SET score_awarded = ?,
            is_correct = CASE WHEN ? > 0 THEN 1 ELSE 0 END,
            reviewer_notes = ?
        WHERE submission_id = ? AND question_number = ?
      `).run(score, score, reviewerNotes || null, submissionId, questionNumber);

      // Recompute submission total
      const totalRow = db.prepare(`
        SELECT SUM(score_awarded) as new_total, SUM(q.points) as max_total
        FROM answers a
        JOIN questions q ON a.question_id = q.id
        WHERE a.submission_id = ? AND q.points > 0
      `).get(submissionId) as { new_total: number; max_total: number };

      const newScore = totalRow.new_total || 0;
      const maxScore = totalRow.max_total || 24;
      const percentage = Math.round((newScore / maxScore) * 100);
      const band = ScoringService.determinePerformanceBand(newScore, percentage);

      db.prepare(`
        UPDATE submissions
        SET total_score = ?,
            score_percentage = ?,
            performance_band = ?
        WHERE id = ?
      `).run(newScore, percentage, band, submissionId);

      db.exec('COMMIT');
      return this.getSubmissionById(submissionId)!;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }

  public static approveSubmission(submissionId: number, approvedBy: string): any {
    const sub = this.getSubmissionById(submissionId);
    if (!sub) throw new Error(`Submission ${submissionId} not found`);
    if (sub.score_percentage < 50) {
      throw new Error(`Score ${sub.score_percentage}% is below the 50% passing threshold. Cannot issue certificate.`);
    }
    if ((sub as any).approval_status === 'APPROVED') {
      // Return existing certificate
      const cert = db.prepare('SELECT * FROM certificates WHERE submission_id = ?').get(submissionId) as any;
      return cert;
    }

    db.exec('BEGIN TRANSACTION');
    try {
      const certNum = generateCertificateNumber();

      db.prepare(`
        UPDATE submissions
        SET approval_status = 'APPROVED', approved_by = ?, approved_at = CURRENT_TIMESTAMP, certificate_number = ?
        WHERE id = ?
      `).run(approvedBy, certNum, submissionId);

      db.prepare(`
        INSERT INTO certificates (
          submission_id, certificate_number, participant_code, full_name,
          wereda_kebele, role, gender, survey_title_en, survey_title_am,
          test_type, score_percentage, total_score, max_score, performance_band, issued_by
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `).run(
        submissionId,
        certNum,
        sub.participant_code,
        sub.participant_code, // full_name defaults to participant_code
        sub.wereda_kebele || null,
        sub.role || null,
        sub.gender || null,
        (sub as any).survey_title_en || 'CRVS Assessment',
        (sub as any).survey_title_am || 'የሲቪል ምዝገባ ምዘና',
        sub.test_type,
        sub.score_percentage,
        sub.total_score,
        sub.max_score,
        sub.performance_band,
        approvedBy
      );

      db.exec('COMMIT');
      return db.prepare('SELECT * FROM certificates WHERE submission_id = ?').get(submissionId) as any;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }

  public static rejectSubmission(submissionId: number, rejectedBy: string): boolean {
    const res = db.prepare(`
      UPDATE submissions SET approval_status = 'REJECTED', approved_by = ?, approved_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `).run(rejectedBy, submissionId);
    return res.changes > 0;
  }

  public static getCertificateBySubmission(submissionId: number): any | null {
    return db.prepare('SELECT * FROM certificates WHERE submission_id = ?').get(submissionId) as any || null;
  }

  public static getCertificateByNumber(certNumber: string): any | null {
    return db.prepare('SELECT * FROM certificates WHERE certificate_number = ?').get(certNumber) as any || null;
  }

  public static getCertificatesForParticipant(participantCode: string): any[] {
    return db.prepare('SELECT * FROM certificates WHERE participant_code = ? ORDER BY issued_at DESC').all(participantCode) as any[];
  }

  public static deleteSubmission(submissionId: number): boolean {
    db.exec('BEGIN TRANSACTION');
    try {
      db.prepare('DELETE FROM answers WHERE submission_id = ?').run(submissionId);
      const res = db.prepare('DELETE FROM submissions WHERE id = ?').run(submissionId);
      db.exec('COMMIT');
      return res.changes > 0;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }
}
