package com.qcafr.gatherapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class QuestionType {
    @Json(name = "SINGLE_CHOICE") SINGLE_CHOICE,
    @Json(name = "RATING_SCALE") RATING_SCALE,
    @Json(name = "TEXT_OPEN") TEXT_OPEN,
    @Json(name = "DEMOGRAPHIC") DEMOGRAPHIC
}

enum class TestType {
    @Json(name = "PRE") PRE,
    @Json(name = "POST") POST
}

@JsonClass(generateAdapter = true)
data class QuestionOption(
    val id: Long? = null,
    @Json(name = "question_id") val questionId: Long? = null,
    @Json(name = "option_key") val optionKey: String,
    @Json(name = "label_am") val labelAm: String,
    @Json(name = "label_en") val labelEn: String,
    @Json(name = "is_correct") val isCorrect: Boolean = false,
    @Json(name = "order_index") val orderIndex: Int = 0
)

@JsonClass(generateAdapter = true)
data class Question(
    val id: Long,
    @Json(name = "section_id") val sectionId: Long,
    @Json(name = "question_number") val questionNumber: Int,
    val type: QuestionType,
    @Json(name = "prompt_am") val promptAm: String,
    @Json(name = "prompt_en") val promptEn: String,
    val points: Int = 1,
    @Json(name = "rubric_am") val rubricAm: String? = null,
    @Json(name = "rubric_en") val rubricEn: String? = null,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val options: List<QuestionOption>? = null
)

@JsonClass(generateAdapter = true)
data class Section(
    val id: Long,
    @Json(name = "survey_id") val surveyId: Long,
    val code: String,
    @Json(name = "title_am") val titleAm: String,
    @Json(name = "title_en") val titleEn: String,
    @Json(name = "description_am") val descriptionAm: String? = null,
    @Json(name = "description_en") val descriptionEn: String? = null,
    @Json(name = "order_index") val orderIndex: Int = 0,
    val questions: List<Question> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Survey(
    val id: Long,
    val code: String,
    @Json(name = "title_am") val titleAm: String,
    @Json(name = "title_en") val titleEn: String,
    @Json(name = "description_am") val descriptionAm: String? = null,
    @Json(name = "description_en") val descriptionEn: String? = null,
    @Json(name = "total_points") val totalPoints: Int = 24,
    @Json(name = "passing_score") val passingScore: Int = 15,
    @Json(name = "created_at") val createdAt: String? = null,
    val sections: List<Section> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Participant(
    @Json(name = "participant_code") val participantCode: String,
    @Json(name = "wereda_kebele") val weredaKebele: String? = null,
    val role: String? = null,
    val gender: String? = null,
    @Json(name = "experience_years") val experienceYears: Float? = null
)

@JsonClass(generateAdapter = true)
data class AnswerInput(
    @Json(name = "question_id") val questionId: Long,
    @Json(name = "question_number") val questionNumber: Int,
    @Json(name = "selected_option_key") val selectedOptionKey: String? = null,
    @Json(name = "answer_text") val answerText: String? = null,
    @Json(name = "rating_value") val ratingValue: Int? = null
)

@JsonClass(generateAdapter = true)
data class SubmissionInput(
    @Json(name = "survey_id") val surveyId: Long,
    val participant: Participant,
    @Json(name = "test_type") val testType: String,
    @Json(name = "confidence_digital") val confidenceDigital: Int? = null,
    @Json(name = "confidence_law") val confidenceLaw: Int? = null,
    val answers: List<AnswerInput> = emptyList(),
    @Json(name = "feedback_text") val feedbackText: String? = null
)

@JsonClass(generateAdapter = true)
data class AnswerResult(
    @Json(name = "question_number") val questionNumber: Int,
    @Json(name = "score_awarded") val scoreAwarded: Float,
    @Json(name = "max_points") val maxPoints: Float = 1f,
    @Json(name = "is_correct") val isCorrect: Boolean = false,
    @Json(name = "selected_option_key") val selectedOptionKey: String? = null,
    @Json(name = "correct_option_key") val correctOptionKey: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmissionResponse(
    val id: Long,
    @Json(name = "participant_code") val participantCode: String,
    @Json(name = "test_type") val testType: String,
    @Json(name = "total_score") val totalScore: Float,
    @Json(name = "max_score") val maxScore: Float,
    @Json(name = "score_percentage") val scorePercentage: Float,
    @Json(name = "performance_band") val performanceBand: String,
    @Json(name = "submitted_at") val submittedAt: String,
    val answers: List<AnswerResult>? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class User(
    val id: Long,
    val username: String,
    @Json(name = "full_name") val fullName: String,
    val role: String, // "ADMIN" or "EXAMINEE"
    @Json(name = "participant_code") val participantCode: String? = null,
    @Json(name = "wereda_kebele") val weredaKebele: String? = null,
    val gender: String? = null,
    @Json(name = "experience_years") val experienceYears: Float? = null
)

@JsonClass(generateAdapter = true)
data class AuthData(
    val user: User,
    val token: String
)

@JsonClass(generateAdapter = true)
data class SignInRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val username: String,
    val password: String,
    @Json(name = "fullName") val fullName: String,
    val role: String = "EXAMINEE",
    @Json(name = "participantCode") val participantCode: String? = null,
    @Json(name = "weredaKebele") val weredaKebele: String? = null,
    val gender: String? = null,
    @Json(name = "experienceYears") val experienceYears: Float? = null
)

@JsonClass(generateAdapter = true)
data class SubmissionRecordItem(
    val id: Long,
    @Json(name = "survey_id") val surveyId: Long = 1,
    @Json(name = "participant_code") val participantCode: String,
    @Json(name = "wereda_kebele") val weredaKebele: String? = null,
    val role: String? = null,
    val gender: String? = null,
    @Json(name = "experience_years") val experienceYears: Float? = null,
    @Json(name = "test_type") val testType: String,
    @Json(name = "total_score") val totalScore: Float,
    @Json(name = "max_score") val maxScore: Float,
    @Json(name = "score_percentage") val scorePercentage: Float,
    @Json(name = "performance_band") val performanceBand: String,
    @Json(name = "submitted_at") val submittedAt: String,
    @Json(name = "approval_status") val approvalStatus: String? = null,
    @Json(name = "approved_by") val approvedBy: String? = null,
    @Json(name = "approved_at") val approvedAt: String? = null,
    @Json(name = "certificate_number") val certificateNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class PerformanceBandsSummary(
    val strong: Int = 0,
    val satisfactory: Int = 0,
    @Json(name = "needs_reinforcement") val needsReinforcement: Int = 0,
    @Json(name = "needs_intensive_support") val needsIntensiveSupport: Int = 0
)

@JsonClass(generateAdapter = true)
data class QuestionAccuracyItem(
    @Json(name = "question_number") val questionNumber: Int = 0,
    @Json(name = "prompt_am") val promptAm: String = "",
    @Json(name = "prompt_en") val promptEn: String = "",
    @Json(name = "section_code") val sectionCode: String = "",
    @Json(name = "pre_accuracy_pct") val preAccuracyPct: Int = 0,
    @Json(name = "post_accuracy_pct") val postAccuracyPct: Int = 0,
    @Json(name = "needs_revision") val needsRevision: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SectionBreakdownItem(
    val code: String = "",
    @Json(name = "title_am") val titleAm: String = "",
    @Json(name = "title_en") val titleEn: String = "",
    @Json(name = "avg_pre_pct") val avgPrePct: Float = 0f,
    @Json(name = "avg_post_pct") val avgPostPct: Float = 0f,
    val delta: Float = 0f
)

@JsonClass(generateAdapter = true)
data class FacilityBreakdownItem(
    val facility: String = "",
    @Json(name = "total_submissions") val totalSubmissions: Int = 0,
    @Json(name = "participant_count") val participantCount: Int = 0,
    @Json(name = "avg_score_pct") val avgScorePct: Float = 0f,
    @Json(name = "pass_rate_pct") val passRatePct: Float = 0f,
    @Json(name = "certified_count") val certifiedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class RoleBreakdownItem(
    val role: String = "",
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "avg_score_pct") val avgScorePct: Float = 0f,
    @Json(name = "pass_rate_pct") val passRatePct: Float = 0f
)

@JsonClass(generateAdapter = true)
data class RegionBreakdownItem(
    @Json(name = "region_name") val regionName: String = "",
    @Json(name = "amharic_name") val amharicName: String = "",
    @Json(name = "code_prefix") val codePrefix: String = "",
    @Json(name = "total_submissions") val totalSubmissions: Int = 0,
    @Json(name = "pre_count") val preCount: Int = 0,
    @Json(name = "post_count") val postCount: Int = 0,
    @Json(name = "avg_score_pct") val avgScorePct: Float = 0f,
    @Json(name = "pass_rate_pct") val passRatePct: Float = 0f,
    @Json(name = "certified_count") val certifiedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class CertificationPipelineSummary(
    @Json(name = "total_eligible") val totalEligible: Int = 0,
    @Json(name = "total_approved") val totalApproved: Int = 0,
    @Json(name = "pending_approval") val pendingApproval: Int = 0,
    @Json(name = "needs_reinforcement") val needsReinforcement: Int = 0
)

@JsonClass(generateAdapter = true)
data class AnalyticsSummary(
    @Json(name = "survey_id") val surveyId: Long = 1,
    @Json(name = "total_submissions") val totalSubmissions: Int = 0,
    @Json(name = "pre_test_count") val preTestCount: Int = 0,
    @Json(name = "post_test_count") val postTestCount: Int = 0,
    @Json(name = "avg_pre_score") val avgPreScore: Float = 0f,
    @Json(name = "avg_post_score") val avgPostScore: Float = 0f,
    @Json(name = "avg_improvement_delta") val avgImprovementDelta: Float = 0f,
    @Json(name = "pass_rate_pre") val passRatePre: Int = 0,
    @Json(name = "pass_rate_post") val passRatePost: Int = 0,
    @Json(name = "performance_bands") val performanceBands: PerformanceBandsSummary = PerformanceBandsSummary(),
    @Json(name = "question_accuracy") val questionAccuracy: List<QuestionAccuracyItem> = emptyList(),
    @Json(name = "section_breakdown") val sectionBreakdown: List<SectionBreakdownItem> = emptyList(),
    @Json(name = "facility_breakdown") val facilityBreakdown: List<FacilityBreakdownItem> = emptyList(),
    @Json(name = "role_breakdown") val roleBreakdown: List<RoleBreakdownItem> = emptyList(),
    @Json(name = "region_breakdown") val regionBreakdown: List<RegionBreakdownItem> = emptyList(),
    @Json(name = "certification_pipeline") val certificationPipeline: CertificationPipelineSummary? = null
)



