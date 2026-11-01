import { db, initDatabase } from './db.js';
import { crvsAssessmentSurvey } from './seedData.js';
import { AuthService } from '../services/authService.js';
import { SettingsService } from '../services/settingsService.js';

export function seedDefaultUsers(): void {
  try {
    const userCount = db.prepare('SELECT COUNT(*) as cnt FROM users').get() as { cnt: number };
    if (userCount.cnt === 0) {
      console.log('Seeding default admin, manager, and examinee accounts...');
      AuthService.register({
        username: 'admin',
        password: 'admin123',
        fullName: 'Dire Dawa System Admin (አስተዳዳሪ)',
        role: 'ADMIN',
        isAdminContext: true
      });

      AuthService.register({
        username: 'manager',
        password: 'manager123',
        fullName: 'CRVS Assessment Manager (ኃላፊ/አስተባባሪ)',
        role: 'USER_MANAGER',
        isAdminContext: true
      });

      AuthService.register({
        username: 'examinee1',
        password: 'user123',
        fullName: 'Abebe Bekele (ተፈታኝ)',
        role: 'EXAMINEE',
        participantCode: 'DD-REG-001',
        weredaKebele: 'Kebele 01',
        gender: 'Male',
        experienceYears: 3.0
      });

      AuthService.register({
        username: 'examinee2',
        password: 'user123',
        fullName: 'Almaz Tadesse (ተፈታኝ)',
        role: 'EXAMINEE',
        participantCode: 'DD-REG-002',
        weredaKebele: 'Kebele 04',
        gender: 'Female',
        experienceYears: 1.5
      });
      console.log('Default accounts created: admin (admin/admin123), manager (manager/manager123), examinee1 (examinee1/user123)');
    }
  } catch (e) {
    console.warn('Error while checking/seeding users:', e);
  }
}

export function runSeed(): void {
  initDatabase();
  SettingsService.initDefaultSettings();
  seedDefaultUsers();

  const existing = db.prepare('SELECT id FROM surveys WHERE code = ?').get(crvsAssessmentSurvey.code) as { id: number } | undefined;

  if (existing) {
    console.log(`Survey ${crvsAssessmentSurvey.code} already exists (ID: ${existing.id}). Skipping re-seeding questions.`);
    return;
  }

  console.log('Seeding CRVS Assessment Survey...');

  const insertSurvey = db.prepare(`
    INSERT INTO surveys (code, title_am, title_en, description_am, description_en, total_points, passing_score)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);

  const insertSection = db.prepare(`
    INSERT INTO sections (survey_id, code, title_am, title_en, description_am, description_en, order_index)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);

  const insertQuestion = db.prepare(`
    INSERT INTO questions (section_id, question_number, type, prompt_am, prompt_en, points, rubric_am, rubric_en, order_index)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  const insertOption = db.prepare(`
    INSERT INTO question_options (question_id, option_key, label_am, label_en, is_correct, order_index)
    VALUES (?, ?, ?, ?, ?, ?)
  `);

  db.exec('BEGIN TRANSACTION');
  try {
    const surveyResult = insertSurvey.run(
      crvsAssessmentSurvey.code,
      crvsAssessmentSurvey.title_am,
      crvsAssessmentSurvey.title_en,
      crvsAssessmentSurvey.description_am,
      crvsAssessmentSurvey.description_en,
      crvsAssessmentSurvey.total_points,
      crvsAssessmentSurvey.passing_score
    );
    const surveyId = Number(surveyResult.lastInsertRowid);

    for (const section of crvsAssessmentSurvey.sections) {
      const secResult = insertSection.run(
        surveyId,
        section.code,
        section.title_am,
        section.title_en,
        section.description_am || null,
        section.description_en || null,
        section.order_index
      );
      const sectionId = Number(secResult.lastInsertRowid);

      for (const q of section.questions as any[]) {
        const qResult = insertQuestion.run(
          sectionId,
          q.question_number,
          q.type,
          q.prompt_am,
          q.prompt_en,
          q.points,
          q.rubric_am || null,
          q.rubric_en || null,
          q.order_index
        );
        const questionId = Number(qResult.lastInsertRowid);

        if (q.options) {
          for (const opt of q.options) {
            insertOption.run(
              questionId,
              opt.option_key,
              opt.label_am,
              opt.label_en,
              opt.is_correct ? 1 : 0,
              opt.order_index
            );
          }
        }
      }
    }

    // Seed sample participants and real pre/post submissions to demonstrate analytics
    seedSampleSubmissions(surveyId);

    db.exec('COMMIT');
    console.log('Seed completed successfully for survey ID:', surveyId);
  } catch (err) {
    db.exec('ROLLBACK');
    console.error('Failed to seed database:', err);
    throw err;
  }
}

function seedSampleSubmissions(surveyId: number): void {
  console.log('Seeding sample participants and pre/post test submissions...');

  const sampleParticipants = [
    { code: 'DD-REG-001', wereda_kebele: 'Kebele 01', role: 'Registrar', gender: 'Female', exp: 3 },
    { code: 'DD-REG-002', wereda_kebele: 'Kebele 04', role: 'Health Registrar', gender: 'Male', exp: 1.5 },
    { code: 'DD-REG-003', wereda_kebele: 'Kebele 07', role: 'Supervisor', gender: 'Male', exp: 5 },
    { code: 'DD-REG-004', wereda_kebele: 'Kebele 09', role: 'Registrar', gender: 'Female', exp: 0.5 }
  ];

  const insertParticipant = db.prepare(`
    INSERT INTO participants (participant_code, wereda_kebele, role, gender, experience_years)
    VALUES (?, ?, ?, ?, ?)
  `);

  const insertSubmission = db.prepare(`
    INSERT INTO submissions (survey_id, participant_id, test_type, total_score, max_score, score_percentage, performance_band, confidence_digital, confidence_law, feedback_ratings_json, feedback_text, notes)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  const insertAnswer = db.prepare(`
    INSERT INTO answers (submission_id, question_id, question_number, selected_option_key, answer_text, score_awarded, is_correct)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);

  // Get question lookup
  const questions = db.prepare(`
    SELECT q.id, q.question_number, q.points, q.type,
      (SELECT option_key FROM question_options WHERE question_id = q.id AND is_correct = 1) as correct_key
    FROM questions q
    JOIN sections s ON q.section_id = s.id
    WHERE s.survey_id = ?
  `).all(surveyId) as { id: number; question_number: number; points: number; type: string; correct_key: string | null }[];

  for (const p of sampleParticipants) {
    const pRes = insertParticipant.run(p.code, p.wereda_kebele, p.role, p.gender, p.exp);
    const participantId = Number(pRes.lastInsertRowid);

    // 1. Pre-test submission (lower baseline score: 10 to 13 points)
    const preScore = 11 + Math.floor(Math.random() * 3);
    const preBand = preScore >= 17 ? 'STRONG' : preScore >= 13 ? 'SATISFACTORY' : preScore >= 9 ? 'NEEDS_REINFORCEMENT' : 'NEEDS_INTENSIVE_SUPPORT';
    
    const preSubRes = insertSubmission.run(
      surveyId,
      participantId,
      'PRE',
      preScore,
      24,
      Math.round((preScore / 24) * 100),
      preBand,
      2, // baseline confidence
      2,
      null,
      null,
      'Pre-training baseline evaluation'
    );
    const preSubId = Number(preSubRes.lastInsertRowid);

    for (const q of questions.filter(x => x.question_number <= 20)) {
      const isCorrect = Math.random() < 0.55;
      const optKey = isCorrect && q.correct_key ? q.correct_key : (q.type === 'SINGLE_CHOICE' ? 'ሀ' : undefined);
      const score = isCorrect ? q.points : 0;
      insertAnswer.run(preSubId, q.id, q.question_number, optKey || null, q.type === 'TEXT_OPEN' ? 'Initial scenario response' : null, score, isCorrect ? 1 : 0);
    }

    // 2. Post-test submission (noticeable post-training improvement: 18 to 22 points)
    const postScore = 18 + Math.floor(Math.random() * 4);
    const postBand = postScore >= 17 ? 'STRONG' : postScore >= 13 ? 'SATISFACTORY' : 'NEEDS_REINFORCEMENT';

    const feedbackObj = JSON.stringify({
      clarity: 5,
      practical: 5,
      trainers: 4,
      relevance: 5
    });

    const postSubRes = insertSubmission.run(
      surveyId,
      participantId,
      'POST',
      postScore,
      24,
      Math.round((postScore / 24) * 100),
      postBand,
      5, // post confidence high
      4,
      feedbackObj,
      'ሥልጠናው በጣም ግልጽ እና ተግባራዊ ነበር። አዲሱ የሲአርቪኤስ ሲስተም ሥራችንን በጣም ያቀላጥፋል።',
      'Post-training completed with strong mastery'
    );
    const postSubId = Number(postSubRes.lastInsertRowid);

    for (const q of questions.filter(x => x.question_number <= 20)) {
      const isCorrect = Math.random() < 0.90;
      const optKey = isCorrect && q.correct_key ? q.correct_key : (q.type === 'SINGLE_CHOICE' ? q.correct_key || 'ለ' : undefined);
      const score = isCorrect ? q.points : (q.type === 'TEXT_OPEN' ? 1 : 0);
      insertAnswer.run(postSubId, q.id, q.question_number, optKey || null, q.type === 'TEXT_OPEN' ? 'በደንቡ መሠረት የዘገየ ምዝገባ ይከናወናል፣ ማስረጃና ምክንያት ተመዝግቦ የምስክር ወረቀት ይሰጣል' : null, score, score > 0 ? 1 : 0);
    }
  }
}

if (process.argv[1]?.endsWith('seed.ts') || process.argv[1]?.endsWith('seed.js')) {
  runSeed();
}
