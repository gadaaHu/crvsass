import { db } from '../database/db.js';
import { AnswerInput, PerformanceBand } from '../models/types.js';

export interface EvaluatedAnswer {
  question_id: number;
  question_number: number;
  selected_option_key?: string;
  answer_text?: string;
  rating_value?: number;
  score_awarded: number;
  is_correct: boolean;
  rubric_applied?: string;
}

export interface EvaluationResult {
  total_score: number;
  max_score: number;
  score_percentage: number;
  performance_band: PerformanceBand;
  answers: EvaluatedAnswer[];
}

export class ScoringService {
  /**
   * Automatically scores submitted answers against survey questions and answer keys.
   */
  public static evaluateSubmission(surveyId: number, answersInput: AnswerInput[]): EvaluationResult {
    // Fetch all questions and their correct options
    const questions = db.prepare(`
      SELECT 
        q.id as question_id,
        q.question_number,
        q.type,
        q.points,
        q.rubric_am,
        q.rubric_en,
        (SELECT option_key FROM question_options WHERE question_id = q.id AND is_correct = 1 LIMIT 1) as correct_option_key
      FROM questions q
      JOIN sections s ON q.section_id = s.id
      WHERE s.survey_id = ?
      ORDER BY q.order_index ASC
    `).all(surveyId) as {
      question_id: number;
      question_number: number;
      type: string;
      points: number;
      rubric_am: string | null;
      rubric_en: string | null;
      correct_option_key: string | null;
    }[];

    const questionMap = new Map(questions.map(q => [q.question_id, q]));
    const numberMap = new Map(questions.map(q => [q.question_number, q]));

    let totalScore = 0;
    let maxScore = 0;
    const evaluatedAnswers: EvaluatedAnswer[] = [];

    // Map input answers
    for (const ans of answersInput) {
      const q = questionMap.get(ans.question_id) || numberMap.get(ans.question_number);
      if (!q) continue;

      let scoreAwarded = 0;
      let isCorrect = false;

      if (q.type === 'SINGLE_CHOICE') {
        maxScore += q.points;
        if (ans.selected_option_key && q.correct_option_key && 
            ans.selected_option_key.trim().toLowerCase() === q.correct_option_key.trim().toLowerCase()) {
          scoreAwarded = q.points;
          isCorrect = true;
        }
      } else if (q.type === 'TEXT_OPEN') {
        maxScore += q.points;
        // Text scenario responses: if provided non-empty, award provisional credit or full points
        if (ans.answer_text && ans.answer_text.trim().length >= 10) {
          scoreAwarded = q.points; // default provisional credit until manual grading
          isCorrect = true;
        }
      } else if (q.type === 'RATING_SCALE') {
        // Ratings do not count towards assessment score
      }

      totalScore += scoreAwarded;

      evaluatedAnswers.push({
        question_id: q.question_id,
        question_number: q.question_number,
        selected_option_key: ans.selected_option_key,
        answer_text: ans.answer_text,
        rating_value: ans.rating_value,
        score_awarded: scoreAwarded,
        is_correct: isCorrect,
        rubric_applied: q.rubric_am || undefined
      });
    }

    // Default maxScore if empty
    if (maxScore === 0) maxScore = 24;

    const percentage = Math.round((totalScore / maxScore) * 100);
    const band = this.determinePerformanceBand(totalScore, percentage);

    return {
      total_score: totalScore,
      max_score: maxScore,
      score_percentage: percentage,
      performance_band: band,
      answers: evaluatedAnswers
    };
  }

  /**
   * Determine official performance classification band:
   * 17–20: Strong (ጠንካራ)
   * 13–16: Satisfactory (አጥጋቢ)
   * 9–12: Needs reinforcement (የማጠናከሪያ ድጋፍ ያስፈልገዋል)
   * 0–8: Needs intensive support (ጠንከር ያለ ክትትልና የአማካሪነት ድጋፍ ያስፈልገዋል)
   */
  public static determinePerformanceBand(score: number, percentage: number): PerformanceBand {
    if (score >= 17 || percentage >= 75) {
      return 'STRONG';
    } else if (score >= 13 || percentage >= 55) {
      return 'SATISFACTORY';
    } else if (score >= 9 || percentage >= 40) {
      return 'NEEDS_REINFORCEMENT';
    } else {
      return 'NEEDS_INTENSIVE_SUPPORT';
    }
  }
}
