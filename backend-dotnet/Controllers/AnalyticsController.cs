using System.Text;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Data;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
[Route("api")]
public class AnalyticsController : ControllerBase
{
    private readonly AppDbContext _db;

    public AnalyticsController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet("analytics/{surveyId}")]
    public async Task<IActionResult> GetAnalytics(int surveyId)
    {
        var submissions = await _db.Submissions
            .Where(s => s.SurveyId == surveyId)
            .ToListAsync();

        var totalSubmissions = submissions.Count;
        var preSubmissions = submissions.Where(s => s.TestType == "PRE").ToList();
        var postSubmissions = submissions.Where(s => s.TestType == "POST").ToList();

        var avgPreScore = preSubmissions.Count > 0 ? Math.Round(preSubmissions.Average(s => s.ScorePercentage), 1) : 0;
        var avgPostScore = postSubmissions.Count > 0 ? Math.Round(postSubmissions.Average(s => s.ScorePercentage), 1) : 0;
        var avgGain = Math.Round(avgPostScore - avgPreScore, 1);

        var bands = submissions
            .GroupBy(s => s.PerformanceBand)
            .ToDictionary(g => g.Key, g => g.Count());

        // Top missed questions calculation
        var answersWithPrompt = await (
            from a in _db.Answers
            join sub in _db.Submissions on a.SubmissionId equals sub.Id
            join q in _db.Questions on a.QuestionId equals q.Id
            where sub.SurveyId == surveyId
            select new { a.QuestionNumber, q.PromptAm, a.IsCorrect }
        ).ToListAsync();

        var missedQuestions = answersWithPrompt
            .GroupBy(x => new { x.QuestionNumber, x.PromptAm })
            .Select(g => new
            {
                question_number = g.Key.QuestionNumber,
                prompt_am = g.Key.PromptAm,
                total_attempts = g.Count(),
                incorrect_count = g.Count(a => a.IsCorrect == 0),
                error_rate = g.Count() > 0 ? Math.Round((double)g.Count(a => a.IsCorrect == 0) / g.Count() * 100, 1) : 0
            })
            .OrderByDescending(x => x.incorrect_count)
            .Take(5)
            .ToList();

        var analytics = new
        {
            survey_id = surveyId,
            total_submissions = totalSubmissions,
            pre_test_count = preSubmissions.Count,
            post_test_count = postSubmissions.Count,
            avg_pre_score = avgPreScore,
            avg_post_score = avgPostScore,
            avg_gain = avgGain,
            performance_bands = bands,
            top_missed_questions = missedQuestions
        };

        return Ok(new { success = true, data = analytics });
    }

    [HttpGet("analytics/{surveyId}/sections")]
    public async Task<IActionResult> GetSectionBreakdown(int surveyId)
    {
        var sections = await _db.Sections
            .Where(s => s.SurveyId == surveyId)
            .Select(sec => new
            {
                section_id = sec.Id,
                title_am = sec.TitleAm,
                title_en = sec.TitleEn,
                question_count = _db.Questions.Count(q => q.SectionId == sec.Id),
                avg_score = (
                    from a in _db.Answers
                    join q in _db.Questions on a.QuestionId equals q.Id
                    where q.SectionId == sec.Id
                    select (double?)a.ScoreAwarded
                ).Average() ?? 0
            })
            .ToListAsync();

        return Ok(new { success = true, data = sections });
    }

    [HttpGet("export/{surveyId}/csv")]
    public async Task<IActionResult> ExportCsv(int surveyId)
    {
        var list = await _db.Submissions
            .Include(s => s.Participant)
            .Where(s => s.SurveyId == surveyId)
            .OrderBy(s => s.SubmittedAt)
            .ToListAsync();

        var sb = new StringBuilder();
        sb.AppendLine("ID,Participant Code,Wereda/Kebele,Role,Gender,Test Type,Total Score,Max Score,Percentage,Band,Approval Status,Certificate Number,Submitted At");

        foreach (var s in list)
        {
            var pCode = s.Participant?.ParticipantCode ?? "";
            var pWereda = s.Participant?.WeredaKebele ?? "";
            var pRole = s.Participant?.Role ?? "";
            var pGender = s.Participant?.Gender ?? "";
            sb.AppendLine($"{s.Id},\"{pCode}\",\"{pWereda}\",\"{pRole}\",\"{pGender}\",{s.TestType},{s.TotalScore},{s.MaxScore},{s.ScorePercentage}%,{s.PerformanceBand},{s.ApprovalStatus},{s.CertificateNumber},{s.SubmittedAt:yyyy-MM-dd HH:mm:ss}");
        }

        var bytes = Encoding.UTF8.GetPreamble().Concat(Encoding.UTF8.GetBytes(sb.ToString())).ToArray();
        return File(bytes, "text/csv; charset=utf-8", $"qcafr_survey_{surveyId}_submissions.csv");
    }

    [HttpGet("export/{surveyId}/json")]
    public async Task<IActionResult> ExportJson(int surveyId)
    {
        var list = await _db.Submissions
            .Include(s => s.Participant)
            .Include(s => s.Answers)
            .Where(s => s.SurveyId == surveyId)
            .OrderBy(s => s.SubmittedAt)
            .ToListAsync();

        return Ok(new { success = true, count = list.Count, data = list });
    }
}
