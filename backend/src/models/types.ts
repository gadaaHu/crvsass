export type QuestionType = 'SINGLE_CHOICE' | 'RATING_SCALE' | 'TEXT_OPEN' | 'DEMOGRAPHIC';

export type TestType = 'PRE' | 'POST';

export type UserRole = 'ADMIN' | 'USER_MANAGER' | 'EXAMINEE';

export type PerformanceBand = 
  | 'STRONG'                // 17–20 (or >= 80%)
  | 'SATISFACTORY'           // 13–16 (or 65–79%)
  | 'NEEDS_REINFORCEMENT'    // 9–12  (or 45–64%)
  | 'NEEDS_INTENSIVE_SUPPORT'; // 0–8 (or < 45%)

export interface QuestionOption {
  id?: number;
  question_id?: number;
  option_key: string;       // e.g. 'ሀ', 'ለ', 'ሐ', 'መ' (or 'A', 'B', 'C', 'D')
  label_am: string;
  label_en: string;
  is_correct: boolean;
  order_index: number;
}

export interface Question {
  id?: number;
  section_id?: number;
  question_number: number;
  type: QuestionType;
  prompt_am: string;
  prompt_en: string;
  points: number;
  rubric_am?: string;
  rubric_en?: string;
  order_index: number;
  options?: QuestionOption[];
}

export interface Section {
  id?: number;
  survey_id?: number;
  code: string;             // e.g. 'SECTION_A', 'SECTION_B', 'SECTION_C', 'FEEDBACK'
  title_am: string;
  title_en: string;
  description_am?: string;
  description_en?: string;
  order_index: number;
  questions?: Question[];
}

export interface Survey {
  id?: number;
  code: string;             // e.g. 'CRVS_DIRE_DAWA_1370'
  title_am: string;
  title_en: string;
  description_am?: string;
  description_en?: string;
  total_points: number;
  passing_score: number;
  created_at?: string;
  sections?: Section[];
}

export interface Participant {
  id?: number;
  participant_code: string;
  wereda_kebele?: string;
  role?: string;            // 'Registrar', 'Health Registrar', 'Supervisor', 'Other'
  gender?: string;          // 'Male', 'Female'
  experience_years?: number;
  created_at?: string;
}

export interface AnswerInput {
  question_id: number;
  question_number: number;
  selected_option_key?: string;
  answer_text?: string;
  rating_value?: number;
}

export interface SubmissionInput {
  survey_id: number;
  participant: {
    participant_code: string;
    wereda_kebele?: string;
    role?: string;
    gender?: string;
    experience_years?: number;
  };
  test_type: TestType;
  confidence_digital?: number; // 1-5 scale
  confidence_law?: number;     // 1-5 scale
  answers: AnswerInput[];
  feedback_ratings?: Record<string, number>;
  feedback_text?: string;
  notes?: string;
  client_timestamp?: string;
}

export interface AnswerRecord {
  id?: number;
  submission_id?: number;
  question_id: number;
  question_number: number;
  selected_option_key?: string;
  answer_text?: string;
  rating_value?: number;
  score_awarded: number;
  is_correct: boolean;
  rubric_applied?: string;
  reviewer_notes?: string;
}

export interface SubmissionRecord {
  id?: number;
  survey_id: number;
  participant_id: number;
  participant_code: string;
  wereda_kebele?: string;
  role?: string;
  gender?: string;
  experience_years?: number;
  test_type: TestType;
  total_score: number;
  max_score: number;
  score_percentage: number;
  performance_band: PerformanceBand;
  confidence_digital?: number;
  confidence_law?: number;
  feedback_ratings_json?: string;
  feedback_text?: string;
  notes?: string;
  submitted_at: string;
  answers?: AnswerRecord[];
}

export interface AnalyticsSummary {
  survey_id: number;
  total_submissions: number;
  pre_test_count: number;
  post_test_count: number;
  avg_pre_score: number;
  avg_post_score: number;
  avg_improvement_delta: number;
  pass_rate_pre: number;
  pass_rate_post: number;
  performance_bands: {
    strong: number;
    satisfactory: number;
    needs_reinforcement: number;
    needs_intensive_support: number;
  };
  question_accuracy: {
    question_number: number;
    prompt_am: string;
    prompt_en: string;
    section_code: string;
    pre_accuracy_pct: number;
    post_accuracy_pct: number;
    needs_revision: boolean; // Flagged if post_accuracy_pct < 60%
  }[];
  section_breakdown?: any[];
  facility_breakdown?: any[];
  role_breakdown?: any[];
  certification_pipeline?: {
    total_eligible: number;
    total_approved: number;
    pending_approval: number;
    needs_reinforcement: number;
  };
}

export interface SystemSetting {
  key: string;
  value: string;
  description?: string;
  category?: 'GENERAL' | 'ASSESSMENT' | 'SECURITY' | 'CERTIFICATE';
  updated_at?: string;
}

export interface UserStats {
  total_users: number;
  admins: number;
  managers: number;
  examinees: number;
  active_users: number;
}
