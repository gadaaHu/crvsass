import Foundation

// MARK: - Enums
enum QuestionType: String, Codable, CaseIterable, Identifiable {
    case singleChoice = "SINGLE_CHOICE"
    case textOpen = "TEXT_OPEN"
    case ratingScale = "RATING_SCALE"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .singleChoice: return "Multiple Choice (MCQ)"
        case .textOpen: return "Open Scenario (ክፍት መልስ)"
        case .ratingScale: return "Rating Scale (1–5 ደረጃ)"
        }
    }
}

enum TestType: String, Codable, CaseIterable {
    case pre = "PRE"
    case post = "POST"

    var title: String {
        switch self {
        case .pre: return "Pre-Test (የስልጠና ቅድመ መመዘኛ)"
        case .post: return "Post-Test (የስልጠና ድህረ መመዘኛ)"
        }
    }
}

enum UserRole: String, Codable, CaseIterable, Identifiable {
    case admin = "ADMIN"
    case userManager = "USER_MANAGER"
    case examinee = "EXAMINEE"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .admin: return "Admin (ሥርዓት አስተዳዳሪ)"
        case .userManager: return "User Manager (የተጠቃሚዎች አስተዳዳሪ)"
        case .examinee: return "Examinee (ተፈታኝ)"
        }
    }

    var badgeText: String {
        switch self {
        case .admin: return "ADMIN"
        case .userManager: return "USER MGR"
        case .examinee: return "EXAMINEE"
        }
    }
}

// MARK: - API Response Wrapper
struct ApiResponse<T: Codable>: Codable {
    let success: BooleanLiteralType
    let data: T?
    let error: String?
}

// MARK: - Auth & User Models
struct User: Codable, Identifiable, Hashable {
    let id: Int
    let username: String
    let fullName: String
    var role: String
    let participantCode: String?
    let weredaKebele: String?
    let gender: String?
    let experienceYears: Double?

    enum CodingKeys: String, CodingKey {
        case id, username, role, gender
        case fullName = "full_name"
        case participantCode = "participant_code"
        case weredaKebele = "wereda_kebele"
        case experienceYears = "experience_years"
    }

    var isAdmin: Bool { role == "ADMIN" }
    var isUserManager: Bool { role == "USER_MANAGER" }
    var canManage: Bool { isAdmin || isUserManager }
}

struct AuthData: Codable {
    let user: User
    let token: String
}

struct SignInRequest: Codable {
    let username: String
    let password: String
}

struct SignUpRequest: Codable {
    let username: String
    let password: String
    let fullName: String
    let role: String
    let participantCode: String?
    let weredaKebele: String?
    let gender: String?
    let experienceYears: Double?
}

// MARK: - Assessment & Questions
struct QuestionOption: Codable, Identifiable, Hashable {
    let id: Int?
    let questionId: Int?
    let optionKey: String
    let labelAm: String
    let labelEn: String?
    var isCorrect: Bool
    let orderIndex: Int?

    enum CodingKeys: String, CodingKey {
        case id
        case questionId = "question_id"
        case optionKey = "option_key"
        case labelAm = "label_am"
        case labelEn = "label_en"
        case isCorrect = "is_correct"
        case orderIndex = "order_index"
    }
}

struct Question: Codable, Identifiable, Hashable {
    let id: Int
    let sectionId: Int
    let questionNumber: Int
    let type: QuestionType
    var promptAm: String
    var promptEn: String?
    var points: Int
    var rubricAm: String?
    var rubricEn: String?
    let orderIndex: Int?
    var options: [QuestionOption]?

    enum CodingKeys: String, CodingKey {
        case id, type, points
        case sectionId = "section_id"
        case questionNumber = "question_number"
        case promptAm = "prompt_am"
        case promptEn = "prompt_en"
        case rubricAm = "rubric_am"
        case rubricEn = "rubric_en"
        case orderIndex = "order_index"
        case options
    }
}

struct Section: Codable, Identifiable, Hashable {
    let id: Int
    let surveyId: Int
    let code: String
    let titleAm: String
    let titleEn: String?
    let descriptionAm: String?
    let descriptionEn: String?
    let orderIndex: Int?
    let questions: [Question]?

    enum CodingKeys: String, CodingKey {
        case id, code, questions
        case surveyId = "survey_id"
        case titleAm = "title_am"
        case titleEn = "title_en"
        case descriptionAm = "description_am"
        case descriptionEn = "description_en"
        case orderIndex = "order_index"
    }
}

struct Survey: Codable, Identifiable {
    let id: Int
    let code: String
    let titleAm: String
    let titleEn: String?
    let descriptionAm: String?
    let descriptionEn: String?
    let totalPoints: Int
    let passingScore: Int
    let sections: [Section]

    enum CodingKeys: String, CodingKey {
        case id, code, sections
        case titleAm = "title_am"
        case titleEn = "title_en"
        case descriptionAm = "description_am"
        case descriptionEn = "description_en"
        case totalPoints = "total_points"
        case passingScore = "passing_score"
    }

    var allQuestions: [(sectionTitle: String, question: Question)] {
        var list: [(String, Question)] = []
        for sec in sections {
            let sTitle = sec.titleAm.isEmpty ? (sec.titleEn ?? sec.code) : sec.titleAm
            for q in sec.questions ?? [] {
                list.append((sTitle, q))
            }
        }
        return list
    }
}

// MARK: - Submission Models
struct Participant: Codable {
    var participantCode: String
    var weredaKebele: String
    var role: String
    var gender: String
    var experienceYears: Double

    enum CodingKeys: String, CodingKey {
        case role, gender
        case participantCode = "participant_code"
        case weredaKebele = "wereda_kebele"
        case experienceYears = "experience_years"
    }
}

struct AnswerInput: Codable {
    let questionId: Int
    var selectedOptionKey: String?
    var answerText: String?
    var ratingValue: Int?

    enum CodingKeys: String, CodingKey {
        case questionId = "question_id"
        case selectedOptionKey = "selected_option_key"
        case answerText = "answer_text"
        case ratingValue = "rating_value"
    }
}

struct SubmissionInput: Codable {
    let surveyId: Int
    let participant: Participant
    let testType: String
    let confidenceDigital: Int?
    let confidenceLaw: Int?
    let answers: [AnswerInput]
    let feedbackText: String?

    enum CodingKeys: String, CodingKey {
        case participant, answers
        case surveyId = "survey_id"
        case testType = "test_type"
        case confidenceDigital = "confidence_digital"
        case confidenceLaw = "confidence_law"
        case feedbackText = "feedback_text"
    }
}

struct AnswerResult: Codable, Identifiable {
    var id: Int { questionNumber }
    let questionNumber: Int
    let scoreAwarded: Double
    let maxPoints: Double
    let isCorrect: Bool
    let selectedOptionKey: String?
    let correctOptionKey: String?

    enum CodingKeys: String, CodingKey {
        case questionNumber = "question_number"
        case scoreAwarded = "score_awarded"
        case maxPoints = "max_points"
        case isCorrect = "is_correct"
        case selectedOptionKey = "selected_option_key"
        case correctOptionKey = "correct_option_key"
    }
}

struct SubmissionResponse: Codable, Identifiable {
    let id: Int
    let participantCode: String
    let testType: String
    let totalScore: Double
    let maxScore: Double
    let scorePercentage: Double
    let performanceBand: String
    let submittedAt: String
    let answers: [AnswerResult]?

    enum CodingKeys: String, CodingKey {
        case id
        case participantCode = "participant_code"
        case testType = "test_type"
        case totalScore = "total_score"
        case maxScore = "max_score"
        case scorePercentage = "score_percentage"
        case performanceBand = "performance_band"
        case submittedAt = "submitted_at"
        case answers
    }
}

struct SubmissionRecordItem: Codable, Identifiable {
    let id: Int
    let surveyId: Int?
    let participantCode: String
    let weredaKebele: String?
    let role: String?
    let gender: String?
    let experienceYears: Double?
    let testType: String
    let totalScore: Double
    let maxScore: Double
    let scorePercentage: Double
    let performanceBand: String
    let submittedAt: String

    enum CodingKeys: String, CodingKey {
        case id, role, gender
        case surveyId = "survey_id"
        case participantCode = "participant_code"
        case weredaKebele = "wereda_kebele"
        case experienceYears = "experience_years"
        case testType = "test_type"
        case totalScore = "total_score"
        case maxScore = "max_score"
        case scorePercentage = "score_percentage"
        case performanceBand = "performance_band"
        case submittedAt = "submitted_at"
    }
}
