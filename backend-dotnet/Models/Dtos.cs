namespace CrvsAfr.Backend.Models;

public record LoginRequest(string Username, string Password);

public record SignupRequest(
    string FullName,
    string Username,
    string Password,
    string? Role,
    string? ParticipantCode,
    string? WeredaKebele,
    string? Gender,
    double? ExperienceYears
);

public record AuthResponse(UserDto User, string Token);

public record UserDto(
    int Id,
    string Username,
    string FullName,
    string Role,
    string? ParticipantCode,
    string? WeredaKebele,
    string? Gender,
    double? ExperienceYears,
    string Status,
    DateTime CreatedAt
);

public record UserStatsDto(
    int TotalUsers,
    int Admins,
    int Managers,
    int Examinees,
    int ActiveUsers
);

public record CreateUserRequest(
    string FullName,
    string Username,
    string Password,
    string Role,
    string? ParticipantCode,
    string? WeredaKebele,
    string? Gender,
    double? ExperienceYears,
    string? Status
);

public record UpdateUserRequest(
    string? FullName,
    string? Role,
    string? ParticipantCode,
    string? WeredaKebele,
    string? Gender,
    double? ExperienceYears,
    string? Status
);

public record ResetPasswordRequest(string NewPassword);

public record UpdateRoleRequest(string Role);

public record VerifyCodeRequest(string Code);

public record VerifyCodeResponse(bool Success, bool Valid, string Message);

public record ParticipantInputDto(
    string ParticipantCode,
    string? WeredaKebele,
    string? Role,
    string? Gender,
    double? ExperienceYears
);

public record AnswerInputDto(
    int QuestionId,
    int QuestionNumber,
    string? SelectedOptionKey,
    string? AnswerText,
    int? RatingValue
);

public record SubmissionInputDto(
    int SurveyId,
    ParticipantInputDto Participant,
    string TestType,
    int? ConfidenceDigital,
    int? ConfidenceLaw,
    List<AnswerInputDto> Answers,
    Dictionary<string, int>? FeedbackRatings,
    string? FeedbackText,
    string? Notes
);

public record QuestionDto(
    int? Id,
    int? SectionId,
    int QuestionNumber,
    string Type,
    string PromptAm,
    string PromptEn,
    int Points,
    string? RubricAm,
    string? RubricEn,
    int OrderIndex,
    List<QuestionOptionDto>? Options
);

public record QuestionOptionDto(
    int? Id,
    int? QuestionId,
    string OptionKey,
    string LabelAm,
    string LabelEn,
    bool IsCorrect,
    int OrderIndex
);

public record SectionDto(
    int? Id,
    int? SurveyId,
    string Code,
    string TitleAm,
    string TitleEn,
    string? DescriptionAm,
    string? DescriptionEn,
    int OrderIndex,
    List<QuestionDto>? Questions
);

public record SurveyDto(
    int? Id,
    string Code,
    string TitleAm,
    string TitleEn,
    string? DescriptionAm,
    string? DescriptionEn,
    int TotalPoints,
    int PassingScore,
    DateTime? CreatedAt,
    List<SectionDto>? Sections
);
