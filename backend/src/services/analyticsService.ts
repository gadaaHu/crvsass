import { db } from '../database/db.js';
import { AnalyticsSummary } from '../models/types.js';

export class AnalyticsService {
  public static getSurveyAnalytics(surveyId: number): AnalyticsSummary {
    const subs = db.prepare(`
      SELECT s.*, p.participant_code
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      WHERE s.survey_id = ?
    `).all(surveyId) as any[];

    const preSubs = subs.filter(s => s.test_type === 'PRE');
    const postSubs = subs.filter(s => s.test_type === 'POST');

    const totalSubmissions = subs.length;
    const preCount = preSubs.length;
    const postCount = postSubs.length;

    const avgPre = preCount > 0 
      ? Number((preSubs.reduce((acc, s) => acc + (s.score_percentage != null ? s.score_percentage : ((s.total_score / (s.max_score || 24)) * 100)), 0) / preCount).toFixed(1))
      : 0;

    const avgPost = postCount > 0
      ? Number((postSubs.reduce((acc, s) => acc + (s.score_percentage != null ? s.score_percentage : ((s.total_score / (s.max_score || 24)) * 100)), 0) / postCount).toFixed(1))
      : 0;

    const delta = Number((avgPost - avgPre).toFixed(1));

    // Passing threshold (score_percentage >= 50%)
    const passPre = preCount > 0 ? Math.round((preSubs.filter(s => (s.score_percentage ?? 0) >= 50).length / preCount) * 100) : 0;
    const passPost = postCount > 0 ? Math.round((postSubs.filter(s => (s.score_percentage ?? 0) >= 50).length / postCount) * 100) : 0;

    // Bands count
    const bands = {
      strong: subs.filter(s => s.performance_band === 'STRONG').length,
      satisfactory: subs.filter(s => s.performance_band === 'SATISFACTORY').length,
      needs_reinforcement: subs.filter(s => s.performance_band === 'NEEDS_REINFORCEMENT').length,
      needs_intensive_support: subs.filter(s => s.performance_band === 'NEEDS_INTENSIVE_SUPPORT').length
    };

    // Question accuracy
    const questions = db.prepare(`
      SELECT 
        q.id,
        q.question_number,
        q.prompt_am,
        q.prompt_en,
        sec.code as section_code
      FROM questions q
      JOIN sections sec ON q.section_id = sec.id
      WHERE sec.survey_id = ? AND q.points > 0
      ORDER BY q.question_number ASC
    `).all(surveyId) as any[];

    const questionAccuracy = questions.map(q => {
      const preAnswers = db.prepare(`
        SELECT a.is_correct FROM answers a
        JOIN submissions s ON a.submission_id = s.id
        WHERE s.survey_id = ? AND s.test_type = 'PRE' AND a.question_id = ?
      `).all(surveyId, q.id) as { is_correct: number }[];

      const postAnswers = db.prepare(`
        SELECT a.is_correct FROM answers a
        JOIN submissions s ON a.submission_id = s.id
        WHERE s.survey_id = ? AND s.test_type = 'POST' AND a.question_id = ?
      `).all(surveyId, q.id) as { is_correct: number }[];

      const preAcc = preAnswers.length > 0
        ? Math.round((preAnswers.filter(a => a.is_correct === 1).length / preAnswers.length) * 100)
        : 0;

      const postAcc = postAnswers.length > 0
        ? Math.round((postAnswers.filter(a => a.is_correct === 1).length / postAnswers.length) * 100)
        : 0;

      // As per assessment guideline: flag questions with < 60% accuracy on post-test
      const needsRevision = postAnswers.length > 0 && postAcc < 60;

      return {
        question_number: q.question_number,
        prompt_am: q.prompt_am,
        prompt_en: q.prompt_en,
        section_code: q.section_code,
        pre_accuracy_pct: preAcc,
        post_accuracy_pct: postAcc,
        needs_revision: needsRevision
      };
    });

    const sectionBreakdown = this.getSectionBreakdown(surveyId);

    const facilityBreakdown = db.prepare(`
      SELECT 
        COALESCE(NULLIF(TRIM(p.wereda_kebele), ''), 'Dire Dawa Center') as facility,
        COUNT(s.id) as total_submissions,
        COUNT(DISTINCT p.id) as participant_count,
        ROUND(AVG(s.score_percentage), 1) as avg_score_pct,
        ROUND(SUM(CASE WHEN s.score_percentage >= 50 THEN 1 ELSE 0 END) * 100.0 / COUNT(s.id), 1) as pass_rate_pct,
        SUM(CASE WHEN s.approval_status = 'APPROVED' THEN 1 ELSE 0 END) as certified_count
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      WHERE s.survey_id = ?
      GROUP BY facility
      ORDER BY avg_score_pct DESC
    `).all(surveyId) as any[];

    const roleBreakdown = db.prepare(`
      SELECT 
        COALESCE(NULLIF(TRIM(p.role), ''), 'Registrar') as role,
        COUNT(s.id) as total_count,
        ROUND(AVG(s.score_percentage), 1) as avg_score_pct,
        ROUND(SUM(CASE WHEN s.score_percentage >= 50 THEN 1 ELSE 0 END) * 100.0 / COUNT(s.id), 1) as pass_rate_pct
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      WHERE s.survey_id = ?
      GROUP BY role
      ORDER BY total_count DESC
    `).all(surveyId) as any[];

    const certPipeline = {
      total_eligible: subs.filter(s => s.score_percentage >= 50).length,
      total_approved: subs.filter(s => s.approval_status === 'APPROVED').length,
      pending_approval: subs.filter(s => s.approval_status === 'PENDING' && s.score_percentage >= 50).length,
      needs_reinforcement: subs.filter(s => s.score_percentage < 50).length
    };

    return {
      survey_id: surveyId,
      total_submissions: totalSubmissions,
      pre_test_count: preCount,
      post_test_count: postCount,
      avg_pre_score: avgPre,
      avg_post_score: avgPost,
      avg_improvement_delta: delta,
      pass_rate_pre: passPre,
      pass_rate_post: passPost,
      performance_bands: bands,
      question_accuracy: questionAccuracy,
      section_breakdown: sectionBreakdown,
      facility_breakdown: facilityBreakdown,
      role_breakdown: roleBreakdown,
      certification_pipeline: certPipeline
    };
  }

  /**
   * Section-level Pre vs Post score breakdown.
   * Returns avg score per section for pre-test and post-test submissions.
   */
  public static getSectionBreakdown(surveyId: number): any[] {
    const sections = db.prepare(`
      SELECT id, code, title_am, title_en, order_index
      FROM sections WHERE survey_id = ? ORDER BY order_index ASC
    `).all(surveyId) as any[];

    return sections.map(sec => {
      // Get total possible points for this section
      const totalPts = db.prepare(`
        SELECT COALESCE(SUM(points), 0) as total FROM questions WHERE section_id = ? AND points > 0
      `).get(sec.id) as { total: number };

      // Pre-test avg score for this section
      const preAvg = db.prepare(`
        SELECT COALESCE(AVG(section_score), 0) as avg_score, COUNT(*) as cnt FROM (
          SELECT s.id, SUM(a.score_awarded) as section_score
          FROM submissions s
          JOIN answers a ON a.submission_id = s.id
          JOIN questions q ON a.question_id = q.id
          WHERE s.survey_id = ? AND s.test_type = 'PRE' AND q.section_id = ? AND q.points > 0
          GROUP BY s.id
        )
      `).get(surveyId, sec.id) as { avg_score: number; cnt: number };

      // Post-test avg score for this section
      const postAvg = db.prepare(`
        SELECT COALESCE(AVG(section_score), 0) as avg_score, COUNT(*) as cnt FROM (
          SELECT s.id, SUM(a.score_awarded) as section_score
          FROM submissions s
          JOIN answers a ON a.submission_id = s.id
          JOIN questions q ON a.question_id = q.id
          WHERE s.survey_id = ? AND s.test_type = 'POST' AND q.section_id = ? AND q.points > 0
          GROUP BY s.id
        )
      `).get(surveyId, sec.id) as { avg_score: number; cnt: number };

      const maxPts = totalPts.total || 1;
      const preAvgPct = Math.round((preAvg.avg_score / maxPts) * 100);
      const postAvgPct = Math.round((postAvg.avg_score / maxPts) * 100);
      const gain = postAvgPct - preAvgPct;

      return {
        section_code: sec.code,
        code: sec.code,
        title_am: sec.title_am,
        title_en: sec.title_en,
        max_points: totalPts.total,
        pre_avg_score: Number(preAvg.avg_score.toFixed(1)),
        pre_avg_pct: preAvgPct,
        avg_pre_pct: preAvgPct,
        pre_count: preAvg.cnt,
        post_avg_score: Number(postAvg.avg_score.toFixed(1)),
        post_avg_pct: postAvgPct,
        avg_post_pct: postAvgPct,
        post_count: postAvg.cnt,
        improvement_pct: gain,
        delta: gain
      };
    });
  }

  /**
   * Live sync monitor — returns how many submissions have been received,
   * broken down by test type, with recent submission timestamps.
   */
  public static getLiveSync(surveyId: number): any {
    const total = db.prepare(`
      SELECT COUNT(*) as cnt FROM submissions WHERE survey_id = ?
    `).get(surveyId) as { cnt: number };

    const preCnt = db.prepare(`
      SELECT COUNT(*) as cnt FROM submissions WHERE survey_id = ? AND test_type = 'PRE'
    `).get(surveyId) as { cnt: number };

    const postCnt = db.prepare(`
      SELECT COUNT(*) as cnt FROM submissions WHERE survey_id = ? AND test_type = 'POST'
    `).get(surveyId) as { cnt: number };

    const pendingApproval = db.prepare(`
      SELECT COUNT(*) as cnt FROM submissions WHERE survey_id = ? AND approval_status = 'PENDING' AND score_percentage >= 50
    `).get(surveyId) as { cnt: number };

    const approved = db.prepare(`
      SELECT COUNT(*) as cnt FROM submissions WHERE survey_id = ? AND approval_status = 'APPROVED'
    `).get(surveyId) as { cnt: number };

    // Recent 10 submissions with timestamps
    const recent = db.prepare(`
      SELECT s.id, s.test_type, s.total_score, s.score_percentage, s.performance_band,
             s.submitted_at, s.approval_status, p.participant_code, p.wereda_kebele
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      WHERE s.survey_id = ?
      ORDER BY s.submitted_at DESC
      LIMIT 10
    `).all(surveyId) as any[];

    // Unique participants count
    const uniqueParticipants = db.prepare(`
      SELECT COUNT(DISTINCT participant_id) as cnt FROM submissions WHERE survey_id = ?
    `).get(surveyId) as { cnt: number };

    return {
      total_submissions: total.cnt,
      pre_count: preCnt.cnt,
      post_count: postCnt.cnt,
      pending_approval: pendingApproval.cnt,
      approved_count: approved.cnt,
      unique_participants: uniqueParticipants.cnt,
      recent_submissions: recent,
      last_updated: new Date().toISOString()
    };
  }

  /**
   * Returns all open-ended (TEXT_OPEN) answers that need manual grading,
   * grouped by question number.
   */
  public static getUngradedResponses(surveyId: number): any {
    // Get all TEXT_OPEN questions for this survey
    const openQuestions = db.prepare(`
      SELECT q.id, q.question_number, q.prompt_am, q.prompt_en, q.points, q.rubric_am, q.rubric_en,
             sec.code as section_code
      FROM questions q
      JOIN sections sec ON q.section_id = sec.id
      WHERE sec.survey_id = ? AND q.type = 'TEXT_OPEN' AND q.points > 0
      ORDER BY q.question_number ASC
    `).all(surveyId) as any[];

    const result = openQuestions.map(q => {
      // Get all answers for this question, including participant info
      const answers = db.prepare(`
        SELECT a.id as answer_id, a.submission_id, a.answer_text, a.score_awarded, a.is_correct,
               a.reviewer_notes, s.test_type, s.submitted_at,
               p.participant_code, p.wereda_kebele
        FROM answers a
        JOIN submissions s ON a.submission_id = s.id
        JOIN participants p ON s.participant_id = p.id
        WHERE a.question_id = ? AND s.survey_id = ?
        ORDER BY s.submitted_at DESC
      `).all(q.id, surveyId) as any[];

      const totalResponses = answers.filter((a: any) => a.answer_text && a.answer_text.trim().length > 0).length;
      // Consider "ungraded" as responses that have provisional full credit but no reviewer_notes
      const ungradedCount = answers.filter((a: any) => 
        a.answer_text && a.answer_text.trim().length > 0 && !a.reviewer_notes
      ).length;

      return {
        question_id: q.id,
        question_number: q.question_number,
        prompt_am: q.prompt_am,
        prompt_en: q.prompt_en,
        max_points: q.points,
        rubric_am: q.rubric_am,
        rubric_en: q.rubric_en,
        section_code: q.section_code,
        total_responses: totalResponses,
        ungraded_count: ungradedCount,
        answers: answers.filter((a: any) => a.answer_text && a.answer_text.trim().length > 0)
      };
    });

    return {
      survey_id: surveyId,
      total_open_questions: openQuestions.length,
      questions: result,
      total_ungraded: result.reduce((acc: number, q: any) => acc + q.ungraded_count, 0)
    };
  }

  public static exportSubmissionsCsv(surveyId: number): string {
    const rows = db.prepare(`
      SELECT 
        s.id as submission_id,
        s.test_type,
        s.total_score,
        s.max_score,
        s.score_percentage,
        s.performance_band,
        s.confidence_digital,
        s.confidence_law,
        s.submitted_at,
        p.participant_code,
        p.wereda_kebele,
        p.role,
        p.gender,
        p.experience_years
      FROM submissions s
      JOIN participants p ON s.participant_id = p.id
      WHERE s.survey_id = ?
      ORDER BY s.submitted_at DESC
    `).all(surveyId) as any[];

    if (rows.length === 0) {
      return 'Submission ID,Participant Code,Test Type,Total Score,Max Score,Percentage,Performance Band,Wereda/Kebele,Role,Gender,Experience,Date\n';
    }

    const headers = [
      'Submission ID',
      'Participant Code',
      'Test Type',
      'Total Score',
      'Max Score',
      'Percentage (%)',
      'Performance Band',
      'Wereda/Kebele',
      'Role',
      'Gender',
      'Experience (Years)',
      'Confidence Digital (1-5)',
      'Confidence Law (1-5)',
      'Submitted At'
    ];

    const lines = [headers.join(',')];

    for (const r of rows) {
      lines.push([
        r.submission_id,
        `"${r.participant_code}"`,
        r.test_type,
        r.total_score,
        r.max_score,
        r.score_percentage,
        r.performance_band,
        `"${r.wereda_kebele || ''}"`,
        `"${r.role || ''}"`,
        `"${r.gender || ''}"`,
        r.experience_years || '',
        r.confidence_digital || '',
        r.confidence_law || '',
        `"${r.submitted_at}"`
      ].join(','));
    }

    return lines.join('\n');
  }
}
