using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace CrvsAfr.Backend.Models;

[Table("users")]
public class User
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Required]
    [Column("username")]
    public string Username { get; set; } = string.Empty;

    [Required]
    [Column("password_hash")]
    public string PasswordHash { get; set; } = string.Empty;

    [Required]
    [Column("salt")]
    public string Salt { get; set; } = string.Empty;

    [Required]
    [Column("full_name")]
    public string FullName { get; set; } = string.Empty;

    [Required]
    [Column("role")]
    public string Role { get; set; } = "EXAMINEE"; // ADMIN, USER_MANAGER, EXAMINEE

    [Column("participant_code")]
    public string? ParticipantCode { get; set; }

    [Column("wereda_kebele")]
    public string? WeredaKebele { get; set; }

    [Column("gender")]
    public string? Gender { get; set; }

    [Column("experience_years")]
    public double? ExperienceYears { get; set; }

    [Column("status")]
    public string Status { get; set; } = "ACTIVE"; // ACTIVE, INACTIVE

    [Column("created_at")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

[Table("settings")]
public class SystemSetting
{
    [Key]
    [Column("key")]
    public string Key { get; set; } = string.Empty;

    [Required]
    [Column("value")]
    public string Value { get; set; } = string.Empty;

    [Column("description")]
    public string? Description { get; set; }

    [Column("category")]
    public string Category { get; set; } = "GENERAL"; // GENERAL, ASSESSMENT, SECURITY, CERTIFICATE

    [Column("updated_at")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

[Table("surveys")]
public class Survey
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Required]
    [Column("code")]
    public string Code { get; set; } = string.Empty;

    [Required]
    [Column("title_am")]
    public string TitleAm { get; set; } = string.Empty;

    [Required]
    [Column("title_en")]
    public string TitleEn { get; set; } = string.Empty;

    [Column("description_am")]
    public string? DescriptionAm { get; set; }

    [Column("description_en")]
    public string? DescriptionEn { get; set; }

    [Column("total_points")]
    public int TotalPoints { get; set; } = 24;

    [Column("passing_score")]
    public int PassingScore { get; set; } = 15;

    [Column("created_at")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public List<Section> Sections { get; set; } = new();
}

[Table("sections")]
public class Section
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("survey_id")]
    public int SurveyId { get; set; }

    [Required]
    [Column("code")]
    public string Code { get; set; } = string.Empty;

    [Required]
    [Column("title_am")]
    public string TitleAm { get; set; } = string.Empty;

    [Required]
    [Column("title_en")]
    public string TitleEn { get; set; } = string.Empty;

    [Column("description_am")]
    public string? DescriptionAm { get; set; }

    [Column("description_en")]
    public string? DescriptionEn { get; set; }

    [Column("order_index")]
    public int OrderIndex { get; set; }

    public List<Question> Questions { get; set; } = new();
}

[Table("questions")]
public class Question
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("section_id")]
    public int SectionId { get; set; }

    [Column("question_number")]
    public int QuestionNumber { get; set; }

    [Required]
    [Column("type")]
    public string Type { get; set; } = "SINGLE_CHOICE"; // SINGLE_CHOICE, RATING_SCALE, TEXT_OPEN, DEMOGRAPHIC

    [Required]
    [Column("prompt_am")]
    public string PromptAm { get; set; } = string.Empty;

    [Required]
    [Column("prompt_en")]
    public string PromptEn { get; set; } = string.Empty;

    [Column("points")]
    public int Points { get; set; } = 1;

    [Column("rubric_am")]
    public string? RubricAm { get; set; }

    [Column("rubric_en")]
    public string? RubricEn { get; set; }

    [Column("order_index")]
    public int OrderIndex { get; set; }

    public List<QuestionOption> Options { get; set; } = new();
}

[Table("question_options")]
public class QuestionOption
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("question_id")]
    public int QuestionId { get; set; }

    [Required]
    [Column("option_key")]
    public string OptionKey { get; set; } = string.Empty; // ሀ, ለ, ሐ, መ

    [Required]
    [Column("label_am")]
    public string LabelAm { get; set; } = string.Empty;

    [Required]
    [Column("label_en")]
    public string LabelEn { get; set; } = string.Empty;

    [Column("is_correct")]
    public int IsCorrect { get; set; } // 1 for true, 0 for false

    [Column("order_index")]
    public int OrderIndex { get; set; }
}

[Table("participants")]
public class Participant
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Required]
    [Column("participant_code")]
    public string ParticipantCode { get; set; } = string.Empty;

    [Column("wereda_kebele")]
    public string? WeredaKebele { get; set; }

    [Column("role")]
    public string? Role { get; set; }

    [Column("gender")]
    public string? Gender { get; set; }

    [Column("experience_years")]
    public double? ExperienceYears { get; set; }

    [Column("created_at")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

[Table("submissions")]
public class Submission
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("survey_id")]
    public int SurveyId { get; set; }

    [Column("participant_id")]
    public int ParticipantId { get; set; }

    [Required]
    [Column("test_type")]
    public string TestType { get; set; } = "PRE"; // PRE or POST

    [Column("total_score")]
    public double TotalScore { get; set; }

    [Column("max_score")]
    public double MaxScore { get; set; } = 24;

    [Column("score_percentage")]
    public double ScorePercentage { get; set; }

    [Required]
    [Column("performance_band")]
    public string PerformanceBand { get; set; } = "SATISFACTORY";

    [Column("confidence_digital")]
    public int? ConfidenceDigital { get; set; }

    [Column("confidence_law")]
    public int? ConfidenceLaw { get; set; }

    [Column("feedback_ratings_json")]
    public string? FeedbackRatingsJson { get; set; }

    [Column("feedback_text")]
    public string? FeedbackText { get; set; }

    [Column("notes")]
    public string? Notes { get; set; }

    [Column("submitted_at")]
    public DateTime SubmittedAt { get; set; } = DateTime.UtcNow;

    [Column("approval_status")]
    public string ApprovalStatus { get; set; } = "PENDING"; // PENDING, APPROVED, REJECTED

    [Column("approved_by")]
    public string? ApprovedBy { get; set; }

    [Column("approved_at")]
    public DateTime? ApprovedAt { get; set; }

    [Column("certificate_number")]
    public string? CertificateNumber { get; set; }

    [ForeignKey("ParticipantId")]
    public Participant? Participant { get; set; }

    public List<Answer> Answers { get; set; } = new();
}

[Table("answers")]
public class Answer
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("submission_id")]
    public int SubmissionId { get; set; }

    [Column("question_id")]
    public int QuestionId { get; set; }

    [Column("question_number")]
    public int QuestionNumber { get; set; }

    [Column("selected_option_key")]
    public string? SelectedOptionKey { get; set; }

    [Column("answer_text")]
    public string? AnswerText { get; set; }

    [Column("rating_value")]
    public int? RatingValue { get; set; }

    [Column("score_awarded")]
    public double ScoreAwarded { get; set; }

    [Column("is_correct")]
    public int IsCorrect { get; set; }

    [Column("rubric_applied")]
    public string? RubricApplied { get; set; }

    [Column("reviewer_notes")]
    public string? ReviewerNotes { get; set; }
}

[Table("certificates")]
public class Certificate
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("submission_id")]
    public int SubmissionId { get; set; }

    [Required]
    [Column("certificate_number")]
    public string CertificateNumber { get; set; } = string.Empty;

    [Required]
    [Column("participant_code")]
    public string ParticipantCode { get; set; } = string.Empty;

    [Column("full_name")]
    public string? FullName { get; set; }

    [Column("wereda_kebele")]
    public string? WeredaKebele { get; set; }

    [Column("role")]
    public string? Role { get; set; }

    [Column("gender")]
    public string? Gender { get; set; }

    [Required]
    [Column("survey_title_en")]
    public string SurveyTitleEn { get; set; } = string.Empty;

    [Required]
    [Column("survey_title_am")]
    public string SurveyTitleAm { get; set; } = string.Empty;

    [Required]
    [Column("test_type")]
    public string TestType { get; set; } = "POST";

    [Column("score_percentage")]
    public double ScorePercentage { get; set; }

    [Column("total_score")]
    public double TotalScore { get; set; }

    [Column("max_score")]
    public double MaxScore { get; set; }

    [Required]
    [Column("performance_band")]
    public string PerformanceBand { get; set; } = string.Empty;

    [Required]
    [Column("issued_by")]
    public string IssuedBy { get; set; } = string.Empty;

    [Column("issued_at")]
    public DateTime IssuedAt { get; set; } = DateTime.UtcNow;
}
