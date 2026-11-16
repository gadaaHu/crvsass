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
   * Fixed max_score represents total possible exam points, excluding feedback sections.
   */
  public static evaluateSubmission(surveyId: number, answersInput: AnswerInput[]): EvaluationResult {
    // Fetch survey definition
    const survey = db.prepare('SELECT total_points FROM surveys WHERE id = ?').get(surveyId) as { total_points: number } | undefined;

    // Fetch all questions with section codes and correct options
    const questions = db.prepare(`
      SELECT 
        q.id as question_id,
        q.question_number,
        q.type,
        q.points,
        q.rubric_am,
        q.rubric_en,
        s.code as section_code,
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
      section_code: string;
      correct_option_key: string | null;
    }[];

    const questionMap = new Map(questions.map(q => [q.question_id, q]));
    const numberMap = new Map(questions.map(q => [q.question_number, q]));

    // Fixed total exam points (excluding feedback questions)
    const testQuestions = questions.filter(q => q.section_code !== 'FEEDBACK' && q.points > 0);
    const calculatedMaxPoints = testQuestions.reduce((sum, q) => sum + q.points, 0);
    const maxScore = calculatedMaxPoints > 0 ? calculatedMaxPoints : (survey?.total_points || 29);

    let totalScore = 0;
    const evaluatedAnswers: EvaluatedAnswer[] = [];

    // Map and score input answers
    for (const ans of answersInput) {
      const q = questionMap.get(ans.question_id) || numberMap.get(ans.question_number);
      if (!q) continue;

      let scoreAwarded = 0;
      let isCorrect = false;

      // Only score questions in academic assessment sections (not feedback)
      if (q.section_code !== 'FEEDBACK' && q.points > 0) {
        if (q.type === 'SINGLE_CHOICE') {
          if (ans.selected_option_key && q.correct_option_key && 
              ans.selected_option_key.trim().toLowerCase() === q.correct_option_key.trim().toLowerCase()) {
            scoreAwarded = q.points;
            isCorrect = true;
          }
        } else if (q.type === 'TEXT_OPEN') {
          // Scenario questions (e.g. Q17–Q20): award provisional points for substantive answers
          if (ans.answer_text && ans.answer_text.trim().length >= 10) {
            scoreAwarded = q.points;
            isCorrect = true;
          }
        }
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

    const percentage = maxScore > 0 ? Math.round((totalScore / maxScore) * 100) : 0;
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
   * Determine official performance classification band based on standard rubric:
   * >= 75%: Strong (ጠንካራ) - High Mastery
   * 60–74%: Satisfactory (አጥጋቢ) - Adequate Competence (Passing >= 60%)
   * 40–59%: Needs reinforcement (የማጠናከሪያ ድጋፍ ያስፈልገዋል) - Mentoring
   * < 40%: Needs intensive support (ጠንከር ያለ ክትትልና የአማካሪነት ድጋፍ ያስፈልገዋል)
   */
  public static determinePerformanceBand(score: number, percentage: number): PerformanceBand {
    if (percentage >= 75) {
      return 'STRONG';
    } else if (percentage >= 60) {
      return 'SATISFACTORY';
    } else if (percentage >= 40) {
      return 'NEEDS_REINFORCEMENT';
    } else {
      return 'NEEDS_INTENSIVE_SUPPORT';
    }
  }
}