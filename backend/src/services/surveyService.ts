import { db } from '../database/db.js';
import { Survey, Section, Question, QuestionOption } from '../models/types.js';

export class SurveyService {
  public static getAllSurveys(): Survey[] {
    const rows = db.prepare(`
      SELECT 
        s.*,
        (SELECT COUNT(*) FROM questions q JOIN sections sec ON q.section_id = sec.id WHERE sec.survey_id = s.id) as question_count,
        (SELECT COUNT(*) FROM submissions sub WHERE sub.survey_id = s.id) as submission_count
      FROM surveys s
      ORDER BY s.id ASC
    `).all() as any[];

    return rows.map(r => ({
      id: r.id,
      code: r.code,
      title_am: r.title_am,
      title_en: r.title_en,
      description_am: r.description_am,
      description_en: r.description_en,
      total_points: r.total_points,
      passing_score: r.passing_score,
      created_at: r.created_at,
      question_count: r.question_count,
      submission_count: r.submission_count
    } as any));
  }

  public static getSurveyById(idOrCode: number | string): Survey | null {
    const isNum = typeof idOrCode === 'number' || !isNaN(Number(idOrCode));
    const surveyRow = (isNum
      ? db.prepare('SELECT * FROM surveys WHERE id = ?').get(Number(idOrCode))
      : db.prepare('SELECT * FROM surveys WHERE code = ?').get(String(idOrCode))) as any;

    if (!surveyRow) return null;

    const sectionsRows = db.prepare(`
      SELECT * FROM sections WHERE survey_id = ? ORDER BY order_index ASC
    `).all(surveyRow.id) as any[];

    const sections: Section[] = [];

    for (const secRow of sectionsRows) {
      const questionRows = db.prepare(`
        SELECT * FROM questions WHERE section_id = ? ORDER BY order_index ASC
      `).all(secRow.id) as any[];

      const questions: Question[] = [];

      for (const qRow of questionRows) {
        const optionRows = db.prepare(`
          SELECT * FROM question_options WHERE question_id = ? ORDER BY order_index ASC
        `).all(qRow.id) as any[];

        questions.push({
          id: qRow.id,
          section_id: qRow.section_id,
          question_number: qRow.question_number,
          type: qRow.type,
          prompt_am: qRow.prompt_am,
          prompt_en: qRow.prompt_en,
          points: qRow.points,
          rubric_am: qRow.rubric_am,
          rubric_en: qRow.rubric_en,
          order_index: qRow.order_index,
          options: optionRows.map(o => ({
            id: o.id,
            question_id: o.question_id,
            option_key: o.option_key,
            label_am: o.label_am,
            label_en: o.label_en,
            is_correct: Boolean(o.is_correct),
            order_index: o.order_index
          }))
        });
      }

      sections.push({
        id: secRow.id,
        survey_id: secRow.survey_id,
        code: secRow.code,
        title_am: secRow.title_am,
        title_en: secRow.title_en,
        description_am: secRow.description_am,
        description_en: secRow.description_en,
        order_index: secRow.order_index,
        questions
      });
    }

    return {
      id: surveyRow.id,
      code: surveyRow.code,
      title_am: surveyRow.title_am,
      title_en: surveyRow.title_en,
      description_am: surveyRow.description_am,
      description_en: surveyRow.description_en,
      total_points: surveyRow.total_points,
      passing_score: surveyRow.passing_score,
      created_at: surveyRow.created_at,
      sections
    };
  }

  public static createSurvey(data: any): Survey {
    db.exec('BEGIN TRANSACTION');
    try {
      const insSurvey = db.prepare(`
        INSERT INTO surveys (code, title_am, title_en, description_am, description_en, total_points, passing_score)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `);
      const sRes = insSurvey.run(
        data.code,
        data.title_am,
        data.title_en || data.title_am,
        data.description_am || null,
        data.description_en || null,
        data.total_points || 20,
        data.passing_score || 12
      );
      const surveyId = Number(sRes.lastInsertRowid);

      if (Array.isArray(data.sections)) {
        for (const sec of data.sections) {
          const insSec = db.prepare(`
            INSERT INTO sections (survey_id, code, title_am, title_en, description_am, description_en, order_index)
            VALUES (?, ?, ?, ?, ?, ?, ?)
          `);
          const secRes = insSec.run(
            surveyId,
            sec.code,
            sec.title_am,
            sec.title_en || sec.title_am,
            sec.description_am || null,
            sec.description_en || null,
            sec.order_index || 0
          );
          const secId = Number(secRes.lastInsertRowid);

          if (Array.isArray(sec.questions)) {
            for (const q of sec.questions) {
              const insQ = db.prepare(`
                INSERT INTO questions (section_id, question_number, type, prompt_am, prompt_en, points, rubric_am, rubric_en, order_index)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
              `);
              const qRes = insQ.run(
                secId,
                q.question_number,
                q.type || 'SINGLE_CHOICE',
                q.prompt_am,
                q.prompt_en || q.prompt_am,
                q.points || 1,
                q.rubric_am || null,
                q.rubric_en || null,
                q.order_index || 0
              );
              const qId = Number(qRes.lastInsertRowid);

              if (Array.isArray(q.options)) {
                for (const opt of q.options) {
                  const insOpt = db.prepare(`
                    INSERT INTO question_options (question_id, option_key, label_am, label_en, is_correct, order_index)
                    VALUES (?, ?, ?, ?, ?, ?)
                  `);
                  insOpt.run(
                    qId,
                    opt.option_key,
                    opt.label_am,
                    opt.label_en || opt.label_am,
                    opt.is_correct ? 1 : 0,
                    opt.order_index || 0
                  );
                }
              }
            }
          }
        }
      }

      db.exec('COMMIT');
      return this.getSurveyById(surveyId)!;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }

  public static getQuestionById(id: number): Question | null {
    const qRow = db.prepare('SELECT * FROM questions WHERE id = ?').get(id) as any;
    if (!qRow) return null;

    const optionRows = db.prepare(`
      SELECT * FROM question_options WHERE question_id = ? ORDER BY order_index ASC
    `).all(id) as any[];

    return {
      id: qRow.id,
      section_id: qRow.section_id,
      question_number: qRow.question_number,
      type: qRow.type,
      prompt_am: qRow.prompt_am,
      prompt_en: qRow.prompt_en,
      points: qRow.points,
      rubric_am: qRow.rubric_am,
      rubric_en: qRow.rubric_en,
      order_index: qRow.order_index,
      options: optionRows.map(o => ({
        id: o.id,
        question_id: o.question_id,
        option_key: o.option_key,
        label_am: o.label_am,
        label_en: o.label_en,
        is_correct: Boolean(o.is_correct),
        order_index: o.order_index
      }))
    };
  }

  public static updateQuestion(id: number, data: any): Question {
    db.exec('BEGIN TRANSACTION');
    try {
      const existing = db.prepare('SELECT * FROM questions WHERE id = ?').get(id) as any;
      if (!existing) {
        throw new Error(`Question ${id} not found`);
      }

      db.prepare(`
        UPDATE questions
        SET prompt_am = COALESCE(?, prompt_am),
            prompt_en = COALESCE(?, prompt_en),
            points = COALESCE(?, points),
            type = COALESCE(?, type),
            rubric_am = ?,
            rubric_en = ?
        WHERE id = ?
      `).run(
        data.prompt_am !== undefined ? data.prompt_am : null,
        data.prompt_en !== undefined ? data.prompt_en : null,
        data.points !== undefined ? data.points : null,
        data.type !== undefined ? data.type : null,
        data.rubric_am !== undefined ? data.rubric_am : existing.rubric_am,
        data.rubric_en !== undefined ? data.rubric_en : existing.rubric_en,
        id
      );

      // If options are provided, update them
      if (Array.isArray(data.options)) {
        db.prepare('DELETE FROM question_options WHERE question_id = ?').run(id);
        const insOpt = db.prepare(`
          INSERT INTO question_options (question_id, option_key, label_am, label_en, is_correct, order_index)
          VALUES (?, ?, ?, ?, ?, ?)
        `);
        data.options.forEach((opt: any, idx: number) => {
          insOpt.run(
            id,
            opt.option_key || String.fromCharCode(65 + idx),
            opt.label_am || '',
            opt.label_en || opt.label_am || '',
            opt.is_correct ? 1 : 0,
            opt.order_index ?? idx
          );
        });
      }

      // Recalculate survey total points
      const surveyRow = db.prepare(`
        SELECT s.id as survey_id FROM sections sec JOIN surveys s ON sec.survey_id = s.id WHERE sec.id = ?
      `).get(existing.section_id) as any;
      if (surveyRow) {
        const sumRow = db.prepare(`
          SELECT SUM(q.points) as total FROM questions q JOIN sections sec ON q.section_id = sec.id WHERE sec.survey_id = ?
        `).get(surveyRow.survey_id) as any;
        if (sumRow?.total) {
          db.prepare('UPDATE surveys SET total_points = ? WHERE id = ?').run(sumRow.total, surveyRow.survey_id);
        }
      }

      db.exec('COMMIT');
      return this.getQuestionById(id)!;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }

  public static createQuestion(sectionId: number, data: any): Question {
    db.exec('BEGIN TRANSACTION');
    try {
      const maxQ = db.prepare(`
        SELECT COALESCE(MAX(question_number), 0) as max_num, COALESCE(MAX(order_index), 0) as max_order
        FROM questions WHERE section_id = ?
      `).get(sectionId) as any;

      const qNum = data.question_number || (maxQ.max_num + 1);
      const orderIdx = data.order_index || (maxQ.max_order + 1);

      const insQ = db.prepare(`
        INSERT INTO questions (section_id, question_number, type, prompt_am, prompt_en, points, rubric_am, rubric_en, order_index)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `);
      const qRes = insQ.run(
        sectionId,
        qNum,
        data.type || 'SINGLE_CHOICE',
        data.prompt_am,
        data.prompt_en || data.prompt_am,
        data.points ?? 1,
        data.rubric_am || null,
        data.rubric_en || null,
        orderIdx
      );
      const questionId = Number(qRes.lastInsertRowid);

      if (Array.isArray(data.options)) {
        const insOpt = db.prepare(`
          INSERT INTO question_options (question_id, option_key, label_am, label_en, is_correct, order_index)
          VALUES (?, ?, ?, ?, ?, ?)
        `);
        data.options.forEach((opt: any, idx: number) => {
          insOpt.run(
            questionId,
            opt.option_key || String.fromCharCode(65 + idx),
            opt.label_am || '',
            opt.label_en || opt.label_am || '',
            opt.is_correct ? 1 : 0,
            opt.order_index ?? idx
          );
        });
      }

      db.exec('COMMIT');
      return this.getQuestionById(questionId)!;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }

  public static deleteQuestion(id: number): boolean {
    db.exec('BEGIN TRANSACTION');
    try {
      db.prepare('DELETE FROM question_options WHERE question_id = ?').run(id);
      const res = db.prepare('DELETE FROM questions WHERE id = ?').run(id);
      db.exec('COMMIT');
      return res.changes > 0;
    } catch (e) {
      db.exec('ROLLBACK');
      throw e;
    }
  }
}
