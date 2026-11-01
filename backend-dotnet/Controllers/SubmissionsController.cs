using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Data;
using CrvsAfr.Backend.Models;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
[Route("api")]
public class SubmissionsController : ControllerBase
{
    private readonly AppDbContext _db;

    public SubmissionsController(AppDbContext db)
    {
        _db = db;
    }

    private static string GenerateCertificateNumber()
    {
        var year = DateTime.UtcNow.Year;
        var randBytes = RandomNumberGenerator.GetBytes(4);
        var randHex = Convert.ToHexString(randBytes);
        return $"CRVS-{year}-{randHex}";
    }

    private static string DeterminePerformanceBand(double score, double percentage)
    {
        if (score >= 17 || percentage >= 75) return "STRONG";
        if (score >= 13 || percentage >= 55) return "SATISFACTORY";
        if (score >= 9 || percentage >= 40) return "NEEDS_REINFORCEMENT";
        return "NEEDS_INTENSIVE_SUPPORT";
    }

    [HttpPost("submissions")]
    public async Task<IActionResult> CreateSubmission([FromBody] SubmissionInputDto input)
    {
        if (input.SurveyId == 0 || input.Participant == null || string.IsNullOrWhiteSpace(input.Participant.ParticipantCode) || string.IsNullOrWhiteSpace(input.TestType))
        {
            return BadRequest(new
            {
                success = false,
                error = "survey_id, participant.participant_code, and test_type (PRE or POST) are required."
            });
        }

        var answers = input.Answers ?? new List<AnswerInputDto>();

        using var transaction = await _db.Database.BeginTransactionAsync();
        try
        {
            // 1. Upsert participant
            var participant = await _db.Participants.FirstOrDefaultAsync(p => p.ParticipantCode == input.Participant.ParticipantCode);
            if (participant == null)
            {
                participant = new Participant
                {
                    ParticipantCode = input.Participant.ParticipantCode.Trim(),
                    WeredaKebele = input.Participant.WeredaKebele,
                    Role = input.Participant.Role,
                    Gender = input.Participant.Gender,
                    ExperienceYears = input.Participant.ExperienceYears,
                    CreatedAt = DateTime.UtcNow
                };
                _db.Participants.Add(participant);
                await _db.SaveChangesAsync();
            }
            else
            {
                if (!string.IsNullOrEmpty(input.Participant.WeredaKebele)) participant.WeredaKebele = input.Participant.WeredaKebele;
                if (!string.IsNullOrEmpty(input.Participant.Role)) participant.Role = input.Participant.Role;
                if (!string.IsNullOrEmpty(input.Participant.Gender)) participant.Gender = input.Participant.Gender;
                if (input.Participant.ExperienceYears.HasValue) participant.ExperienceYears = input.Participant.ExperienceYears;
                await _db.SaveChangesAsync();
            }

            // 2. Fetch survey & questions for scoring
            var survey = await _db.Surveys.FindAsync(input.SurveyId);
            var questions = await _db.Questions
                .Include(q => q.Options)
                .Where(q => _db.Sections.Any(s => s.Id == q.SectionId && s.SurveyId == input.SurveyId))
                .ToListAsync();

            var qMap = questions.ToDictionary(q => q.Id);
            var numMap = questions.ToDictionary(q => q.QuestionNumber);

            double totalScore = 0;
            double maxScore = 0;
            var answersToInsert = new List<Answer>();

            foreach (var ans in answers)
            {
                Question? q = null;
                if (ans.QuestionId > 0 && qMap.TryGetValue(ans.QuestionId, out var qFound))
                {
                    q = qFound;
                }
                else if (numMap.TryGetValue(ans.QuestionNumber, out var qFoundNum))
                {
                    q = qFoundNum;
                }

                if (q == null) continue;

                double scoreAwarded = 0;
                int isCorrect = 0;

                if (q.Type == "SINGLE_CHOICE")
                {
                    maxScore += q.Points;
                    var correctOpt = q.Options.FirstOrDefault(o => o.IsCorrect == 1);
                    if (!string.IsNullOrWhiteSpace(ans.SelectedOptionKey) && correctOpt != null)
                    {
                        if (string.Equals(ans.SelectedOptionKey.Trim(), correctOpt.OptionKey.Trim(), StringComparison.OrdinalIgnoreCase))
                        {
                            scoreAwarded = q.Points;
                            isCorrect = 1;
                        }
                    }
                }
                else if (q.Type == "TEXT_OPEN")
                {
                    maxScore += q.Points;
                    if (!string.IsNullOrWhiteSpace(ans.AnswerText) && ans.AnswerText.Trim().Length >= 10)
                    {
                        scoreAwarded = q.Points;
                        isCorrect = 1;
                    }
                }

                totalScore += scoreAwarded;

                answersToInsert.Add(new Answer
                {
                    QuestionId = q.Id,
                    QuestionNumber = q.QuestionNumber,
                    SelectedOptionKey = ans.SelectedOptionKey,
                    AnswerText = ans.AnswerText,
                    RatingValue = ans.RatingValue,
                    ScoreAwarded = scoreAwarded,
                    IsCorrect = isCorrect,
                    RubricApplied = q.RubricAm
                });
            }

            if (maxScore == 0) maxScore = 24;
            double percentage = Math.Round((totalScore / maxScore) * 100);
            string band = DeterminePerformanceBand(totalScore, percentage);

            var feedbackRatingsJson = input.FeedbackRatings != null
                ? JsonSerializer.Serialize(input.FeedbackRatings)
                : null;

            var submission = new Submission
            {
                SurveyId = input.SurveyId,
                ParticipantId = participant.Id,
                TestType = input.TestType.ToUpper(),
                TotalScore = totalScore,
                MaxScore = maxScore,
                ScorePercentage = percentage,
                PerformanceBand = band,
                ConfidenceDigital = input.ConfidenceDigital,
                ConfidenceLaw = input.ConfidenceLaw,
                FeedbackRatingsJson = feedbackRatingsJson,
                FeedbackText = input.FeedbackText,
                Notes = input.Notes,
                SubmittedAt = DateTime.UtcNow,
                ApprovalStatus = "PENDING"
            };

            _db.Submissions.Add(submission);
            await _db.SaveChangesAsync();

            foreach (var a in answersToInsert)
            {
                a.SubmissionId = submission.Id;
                _db.Answers.Add(a);
            }
            await _db.SaveChangesAsync();

            // Auto-approve POST tests passing threshold (>= 50%)
            if (submission.TestType == "POST" && submission.ScorePercentage >= 50)
            {
                var certNum = GenerateCertificateNumber();
                submission.ApprovalStatus = "APPROVED";
                submission.ApprovedBy = "System Evaluator";
                submission.ApprovedAt = DateTime.UtcNow;
                submission.CertificateNumber = certNum;

                var cert = new Certificate
                {
                    CertificateNumber = certNum,
                    SubmissionId = submission.Id,
                    ParticipantCode = participant.ParticipantCode,
                    FullName = participant.ParticipantCode,
                    WeredaKebele = participant.WeredaKebele,
                    Role = participant.Role ?? "Certified Registrar",
                    Gender = participant.Gender,
                    SurveyTitleAm = survey?.TitleAm ?? "የድሬዳዋ ወሳኝ ሁነቶች ምዝገባ ብቃት ምዘና",
                    SurveyTitleEn = survey?.TitleEn ?? "Dire Dawa CRVS Registrar Competency Assessment",
                    TestType = submission.TestType,
                    TotalScore = totalScore,
                    MaxScore = maxScore,
                    ScorePercentage = percentage,
                    PerformanceBand = band,
                    IssuedBy = "Dire Dawa CRVS Agency Director",
                    IssuedAt = DateTime.UtcNow
                };
                _db.Certificates.Add(cert);
                await _db.SaveChangesAsync();
            }

            await transaction.CommitAsync();

            return await GetSubmission(submission.Id);
        }
        catch (Exception ex)
        {
            await transaction.RollbackAsync();
            return StatusCode(500, new { success = false, error = ex.Message });
        }
    }

    [HttpGet("submissions")]
    public async Task<IActionResult> ListSubmissions([FromQuery] int? survey_id, [FromQuery] string? test_type, [FromQuery] string? participant_code)
    {
        var query = _db.Submissions
            .Include(s => s.Participant)
            .AsQueryable();

        if (survey_id.HasValue && survey_id > 0)
            query = query.Where(s => s.SurveyId == survey_id.Value);

        if (!string.IsNullOrWhiteSpace(test_type))
            query = query.Where(s => s.TestType == test_type.ToUpper());

        if (!string.IsNullOrWhiteSpace(participant_code))
            query = query.Where(s => s.Participant != null && s.Participant.ParticipantCode.Contains(participant_code));

        var list = await query
            .OrderByDescending(s => s.SubmittedAt)
            .Select(s => new
            {
                id = s.Id,
                survey_id = s.SurveyId,
                participant_id = s.ParticipantId,
                test_type = s.TestType,
                total_score = s.TotalScore,
                max_score = s.MaxScore,
                score_percentage = s.ScorePercentage,
                performance_band = s.PerformanceBand,
                confidence_digital = s.ConfidenceDigital,
                confidence_law = s.ConfidenceLaw,
                feedback_ratings_json = s.FeedbackRatingsJson,
                feedback_text = s.FeedbackText,
                notes = s.Notes,
                approval_status = s.ApprovalStatus,
                approved_by = s.ApprovedBy,
                approved_at = s.ApprovedAt,
                certificate_number = s.CertificateNumber,
                submitted_at = s.SubmittedAt,
                participant_code = s.Participant != null ? s.Participant.ParticipantCode : null,
                wereda_kebele = s.Participant != null ? s.Participant.WeredaKebele : null,
                role = s.Participant != null ? s.Participant.Role : null,
                gender = s.Participant != null ? s.Participant.Gender : null,
                experience_years = s.Participant != null ? s.Participant.ExperienceYears : null
            })
            .ToListAsync();

        return Ok(new { success = true, count = list.Count, data = list });
    }

    [HttpGet("submissions/{id}")]
    public async Task<IActionResult> GetSubmission(int id)
    {
        var sub = await _db.Submissions
            .Include(s => s.Participant)
            .Include(s => s.Answers)
            .FirstOrDefaultAsync(s => s.Id == id);

        if (sub == null)
        {
            return NotFound(new { success = false, error = $"Submission with ID {id} not found" });
        }

        var survey = await _db.Surveys.FindAsync(sub.SurveyId);

        // Fetch question details for answers
        var qIds = sub.Answers.Select(a => a.QuestionId).Distinct().ToList();
        var questions = await _db.Questions
            .Include(q => q.Options)
            .Where(q => qIds.Contains(q.Id))
            .ToDictionaryAsync(q => q.Id);

        var result = new
        {
            id = sub.Id,
            survey_id = sub.SurveyId,
            participant_id = sub.ParticipantId,
            test_type = sub.TestType,
            total_score = sub.TotalScore,
            max_score = sub.MaxScore,
            score_percentage = sub.ScorePercentage,
            performance_band = sub.PerformanceBand,
            confidence_digital = sub.ConfidenceDigital,
            confidence_law = sub.ConfidenceLaw,
            feedback_ratings_json = sub.FeedbackRatingsJson,
            feedback_text = sub.FeedbackText,
            notes = sub.Notes,
            approval_status = sub.ApprovalStatus,
            approved_by = sub.ApprovedBy,
            approved_at = sub.ApprovedAt,
            certificate_number = sub.CertificateNumber,
            submitted_at = sub.SubmittedAt,
            participant_code = sub.Participant?.ParticipantCode,
            wereda_kebele = sub.Participant?.WeredaKebele,
            role = sub.Participant?.Role,
            gender = sub.Participant?.Gender,
            experience_years = sub.Participant?.ExperienceYears,
            survey_title_am = survey?.TitleAm,
            survey_title_en = survey?.TitleEn,
            answers = sub.Answers.OrderBy(a => a.QuestionNumber).Select(a =>
            {
                questions.TryGetValue(a.QuestionId, out var q);
                var correctOpt = q?.Options.FirstOrDefault(o => o.IsCorrect == 1);
                return new
                {
                    id = a.Id,
                    submission_id = a.SubmissionId,
                    question_id = a.QuestionId,
                    question_number = a.QuestionNumber,
                    selected_option_key = a.SelectedOptionKey,
                    answer_text = a.AnswerText,
                    rating_value = a.RatingValue,
                    score_awarded = a.ScoreAwarded,
                    is_correct = a.IsCorrect == 1,
                    rubric_applied = a.RubricApplied,
                    reviewer_notes = a.ReviewerNotes,
                    prompt_am = q?.PromptAm,
                    prompt_en = q?.PromptEn,
                    max_points = q?.Points ?? 1,
                    question_type = q?.Type,
                    correct_option_key = correctOpt?.OptionKey
                };
            })
        };

        return Ok(new { success = true, data = result });
    }

    [HttpPatch("submissions/{id}/approve")]
    [Authorize(Roles = "ADMIN,USER_MANAGER")]
    public async Task<IActionResult> ApproveSubmission(int id, [FromBody] ApproveRequest? req)
    {
        var sub = await _db.Submissions.Include(s => s.Participant).FirstOrDefaultAsync(s => s.Id == id);
        if (sub == null) return NotFound(new { success = false, error = "Submission not found" });

        if (sub.ScorePercentage < 50)
        {
            return StatusCode(422, new { success = false, error = $"Score {sub.ScorePercentage}% is below the 50% passing threshold. Cannot issue certificate." });
        }

        var approver = req?.ApprovedBy ?? User.Identity?.Name ?? "Manager";
        var certNum = sub.CertificateNumber ?? GenerateCertificateNumber();

        sub.ApprovalStatus = "APPROVED";
        sub.ApprovedBy = approver;
        sub.ApprovedAt = DateTime.UtcNow;
        sub.CertificateNumber = certNum;

        var survey = await _db.Surveys.FindAsync(sub.SurveyId);

        var existingCert = await _db.Certificates.FirstOrDefaultAsync(c => c.SubmissionId == id);
        if (existingCert == null)
        {
            existingCert = new Certificate
            {
                CertificateNumber = certNum,
                SubmissionId = sub.Id,
                ParticipantCode = sub.Participant?.ParticipantCode ?? "PARTICIPANT",
                FullName = sub.Participant?.ParticipantCode,
                WeredaKebele = sub.Participant?.WeredaKebele,
                Role = sub.Participant?.Role ?? "Certified Registrar",
                Gender = sub.Participant?.Gender,
                SurveyTitleAm = survey?.TitleAm ?? "የድሬዳዋ ወሳኝ ሁነቶች ምዝገባ ብቃት ምዘና",
                SurveyTitleEn = survey?.TitleEn ?? "Dire Dawa CRVS Registrar Competency Assessment",
                TestType = sub.TestType,
                TotalScore = sub.TotalScore,
                MaxScore = sub.MaxScore,
                ScorePercentage = sub.ScorePercentage,
                PerformanceBand = sub.PerformanceBand,
                IssuedBy = "Dire Dawa CRVS Agency Director",
                IssuedAt = DateTime.UtcNow
            };
            _db.Certificates.Add(existingCert);
        }

        await _db.SaveChangesAsync();

        return Ok(new { success = true, data = existingCert });
    }

    [HttpPatch("submissions/{id}/reject")]
    [Authorize(Roles = "ADMIN,USER_MANAGER")]
    public async Task<IActionResult> RejectSubmission(int id, [FromBody] RejectRequest? req)
    {
        var sub = await _db.Submissions.FindAsync(id);
        if (sub == null) return NotFound(new { success = false, error = "Submission not found" });

        sub.ApprovalStatus = "REJECTED";
        sub.ApprovedBy = req?.RejectedBy ?? User.Identity?.Name ?? "Manager";
        sub.ApprovedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
        return Ok(new { success = true, message = "Submission rejected." });
    }

    [HttpGet("submissions/{id}/certificate")]
    public async Task<IActionResult> GetCertificate(int id)
    {
        var cert = await _db.Certificates.FirstOrDefaultAsync(c => c.SubmissionId == id);
        if (cert == null)
        {
            return NotFound(new { success = false, error = "No certificate issued for this submission." });
        }

        return Ok(new { success = true, data = cert });
    }

    [HttpGet("certificates/participant/{code}")]
    public async Task<IActionResult> GetCertificatesForParticipant(string code)
    {
        var certs = await _db.Certificates
            .Where(c => c.ParticipantCode == code)
            .OrderByDescending(c => c.IssuedAt)
            .ToListAsync();

        return Ok(new { success = true, count = certs.Count, data = certs });
    }

    [HttpDelete("submissions/{id}")]
    [Authorize(Roles = "ADMIN")]
    public async Task<IActionResult> DeleteSubmission(int id)
    {
        var sub = await _db.Submissions.FindAsync(id);
        if (sub == null) return NotFound(new { success = false, error = $"Submission {id} not found" });

        _db.Submissions.Remove(sub);
        await _db.SaveChangesAsync();
        return Ok(new { success = true, message = $"Submission {id} deleted successfully" });
    }
}

public class ApproveRequest
{
    public string? ApprovedBy { get; set; }
}

public class RejectRequest
{
    public string? RejectedBy { get; set; }
}
