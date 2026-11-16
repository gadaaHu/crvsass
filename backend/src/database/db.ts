import { DatabaseSync } from 'node:sqlite';
import path from 'node:path';
import fs from 'node:fs';

const DATA_DIR = path.resolve(process.cwd(), 'data');
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

const DB_PATH = path.join(DATA_DIR, 'qcafr.sqlite');

export const db = new DatabaseSync(DB_PATH);

export function initDatabase(): void {
  // Enable foreign keys
  db.exec('PRAGMA foreign_keys = ON;');

  // Surveys table
  db.exec(`
    CREATE TABLE IF NOT EXISTS surveys (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      code TEXT UNIQUE NOT NULL,
      title_am TEXT NOT NULL,
      title_en TEXT NOT NULL,
      description_am TEXT,
      description_en TEXT,
      total_points INTEGER NOT NULL DEFAULT 24,
      passing_score INTEGER NOT NULL DEFAULT 15,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Sections table
  db.exec(`
    CREATE TABLE IF NOT EXISTS sections (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      survey_id INTEGER NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
      code TEXT NOT NULL,
      title_am TEXT NOT NULL,
      title_en TEXT NOT NULL,
      description_am TEXT,
      description_en TEXT,
      order_index INTEGER NOT NULL DEFAULT 0
    );
  `);

  // Questions table
  db.exec(`
    CREATE TABLE IF NOT EXISTS questions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      section_id INTEGER NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
      question_number INTEGER NOT NULL,
      type TEXT NOT NULL, -- 'SINGLE_CHOICE', 'RATING_SCALE', 'TEXT_OPEN', 'DEMOGRAPHIC'
      prompt_am TEXT NOT NULL,
      prompt_en TEXT NOT NULL,
      points INTEGER NOT NULL DEFAULT 1,
      rubric_am TEXT,
      rubric_en TEXT,
      order_index INTEGER NOT NULL DEFAULT 0
    );
  `);

  // Question Options table (for multiple choice questions)
  db.exec(`
    CREATE TABLE IF NOT EXISTS question_options (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      question_id INTEGER NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
      option_key TEXT NOT NULL, -- 'ሀ', 'ለ', 'ሐ', 'መ'
      label_am TEXT NOT NULL,
      label_en TEXT NOT NULL,
      is_correct INTEGER NOT NULL DEFAULT 0,
      order_index INTEGER NOT NULL DEFAULT 0
    );
  `);

  // Participants table
  db.exec(`
    CREATE TABLE IF NOT EXISTS participants (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      participant_code TEXT UNIQUE NOT NULL,
      wereda_kebele TEXT,
      role TEXT,
      gender TEXT,
      experience_years REAL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Submissions table
  db.exec(`
    CREATE TABLE IF NOT EXISTS submissions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      survey_id INTEGER NOT NULL REFERENCES surveys(id),
      participant_id INTEGER NOT NULL REFERENCES participants(id),
      test_type TEXT NOT NULL, -- 'PRE' or 'POST'
      total_score REAL NOT NULL DEFAULT 0,
      max_score REAL NOT NULL DEFAULT 24,
      score_percentage REAL NOT NULL DEFAULT 0,
      performance_band TEXT NOT NULL,
      confidence_digital INTEGER,
      confidence_law INTEGER,
      feedback_ratings_json TEXT,
      feedback_text TEXT,
      notes TEXT,
      submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      approval_status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
      approved_by TEXT,
      approved_at DATETIME,
      certificate_number TEXT UNIQUE
    );
  `);

  // Run migrations to add columns if they don't exist (safe for existing databases)
  try { db.exec('ALTER TABLE submissions ADD COLUMN approval_status TEXT NOT NULL DEFAULT \'PENDING\''); } catch(e) {/* already exists */}
  try { db.exec('ALTER TABLE submissions ADD COLUMN approved_by TEXT'); } catch(e) {/* already exists */}
  try { db.exec('ALTER TABLE submissions ADD COLUMN approved_at DATETIME'); } catch(e) {/* already exists */}
  try { db.exec('ALTER TABLE submissions ADD COLUMN certificate_number TEXT'); } catch(e) {/* already exists */}

  // Certificates table
  db.exec(`
    CREATE TABLE IF NOT EXISTS certificates (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      submission_id INTEGER NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
      certificate_number TEXT UNIQUE NOT NULL,
      participant_code TEXT NOT NULL,
      full_name TEXT,
      wereda_kebele TEXT,
      role TEXT,
      gender TEXT,
      survey_title_en TEXT NOT NULL,
      survey_title_am TEXT NOT NULL,
      test_type TEXT NOT NULL,
      score_percentage REAL NOT NULL,
      total_score REAL NOT NULL,
      max_score REAL NOT NULL,
      performance_band TEXT NOT NULL,
      issued_by TEXT NOT NULL,
      issued_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Answers table
  db.exec(`
    CREATE TABLE IF NOT EXISTS answers (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      submission_id INTEGER NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
      question_id INTEGER NOT NULL REFERENCES questions(id),
      question_number INTEGER NOT NULL,
      selected_option_key TEXT,
      answer_text TEXT,
      rating_value INTEGER,
      score_awarded REAL NOT NULL DEFAULT 0,
      is_correct INTEGER NOT NULL DEFAULT 0,
      rubric_applied TEXT,
      reviewer_notes TEXT
    );
  `);

  // Users table for Authentication and Authorization
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      salt TEXT NOT NULL,
      full_name TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'EXAMINEE', -- 'ADMIN' or 'EXAMINEE'
      participant_code TEXT,
      wereda_kebele TEXT,
      gender TEXT,
      experience_years REAL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Users table migration for status
  try { db.exec("ALTER TABLE users ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'"); } catch(e) {/* already exists */}

  // Settings table for system configurations, policies & manager authorization
  db.exec(`
    CREATE TABLE IF NOT EXISTS settings (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL,
      description TEXT,
      category TEXT DEFAULT 'GENERAL',
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Knowledge Documents table for AI RAG / Knowledge Base
  db.exec(`
    CREATE TABLE IF NOT EXISTS knowledge_documents (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      filename TEXT NOT NULL,
      file_type TEXT NOT NULL,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      summary TEXT,
      char_count INTEGER NOT NULL DEFAULT 0,
      uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );
  `);

  // Indexes for query performance
  db.exec(`
    CREATE INDEX IF NOT EXISTS idx_questions_section ON questions(section_id);
    CREATE INDEX IF NOT EXISTS idx_options_question ON question_options(question_id);
    CREATE INDEX IF NOT EXISTS idx_submissions_survey ON submissions(survey_id);
    CREATE INDEX IF NOT EXISTS idx_submissions_participant ON submissions(participant_id);
    CREATE INDEX IF NOT EXISTS idx_answers_submission ON answers(submission_id);
    CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
  `);

  console.log('Database initialized successfully at:', DB_PATH);
}
