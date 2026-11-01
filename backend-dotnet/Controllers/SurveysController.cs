using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Data;
using CrvsAfr.Backend.Models;

namespace CrvsAfr.Backend.Controllers;

[ApiController]
[Route("api")]
public class SurveysController : ControllerBase
{
    private readonly AppDbContext _db;

    public SurveysController(AppDbContext db)
    {
        _db = db;
    }

    [HttpGet("surveys")]
    public async Task<IActionResult> ListSurveys()
    {
        var surveys = await _db.Surveys
            .OrderBy(s => s.Id)
            .Select(s => new
            {
                s.Id,
                s.Code,
                s.TitleAm,
                s.TitleEn,
                s.DescriptionAm,
                s.DescriptionEn,
                s.PassingScore,
                s.TotalPoints,
                s.CreatedAt,
                SectionCount = _db.Sections.Count(sec => sec.SurveyId == s.Id),
                QuestionCount = _db.Questions.Count(q => _db.Sections.Any(sec => sec.Id == q.SectionId && sec.SurveyId == s.Id))
            })
            .ToListAsync();

        return Ok(new { success = true, data = surveys });
    }

    [HttpGet("surveys/{id}")]
    public async Task<IActionResult> GetSurvey(string id)
    {
        Survey? survey = null;
        if (int.TryParse(id, out var intId))
        {
            survey = await _db.Surveys
                .Include(s => s.Sections)
                    .ThenInclude(sec => sec.Questions)
                        .ThenInclude(q => q.Options)
                .FirstOrDefaultAsync(s => s.Id == intId);
        }

        if (survey == null)
        {
            survey = await _db.Surveys
                .Include(s => s.Sections)
                    .ThenInclude(sec => sec.Questions)
                        .ThenInclude(q => q.Options)
                .FirstOrDefaultAsync(s => s.Code == id);
        }

        if (survey == null)
        {
            return NotFound(new { success = false, error = $"Survey '{id}' not found" });
        }

        var authResult = await HttpContext.AuthenticateAsync(JwtBearerDefaults.AuthenticationScheme);
        var principal = authResult.Succeeded ? authResult.Principal : User;
        var userRole = principal?.FindFirstValue(ClaimTypes.Role);
        var isPrivileged = userRole == "ADMIN" || userRole == "USER_MANAGER";

        // Map and safely format (stripping is_correct for non-admin/manager examinees)
        var formatted = new
        {
            id = survey.Id,
            code = survey.Code,
            title_am = survey.TitleAm,
            title_en = survey.TitleEn,
            description_am = survey.DescriptionAm,
            description_en = survey.DescriptionEn,
            total_points = survey.TotalPoints,
            passing_score = survey.PassingScore,
            sections = survey.Sections.OrderBy(sec => sec.OrderIndex).Select(sec => new
            {
                id = sec.Id,
                survey_id = sec.SurveyId,
                code = sec.Code,
                title_am = sec.TitleAm,
                title_en = sec.TitleEn,
                description_am = sec.DescriptionAm,
                description_en = sec.DescriptionEn,
                order_index = sec.OrderIndex,
                questions = sec.Questions.OrderBy(q => q.OrderIndex).Select(q => new
                {
                    id = q.Id,
                    section_id = q.SectionId,
                    question_number = q.QuestionNumber,
                    type = q.Type,
                    prompt_am = q.PromptAm,
                    prompt_en = q.PromptEn,
                    rubric_am = q.RubricAm,
                    rubric_en = q.RubricEn,
                    points = q.Points,
                    order_index = q.OrderIndex,
                    options = q.Options.OrderBy(opt => opt.OrderIndex).Select(opt => new
                    {
                        id = opt.Id,
                        question_id = opt.QuestionId,
                        option_key = opt.OptionKey,
                        label_am = opt.LabelAm,
                        label_en = opt.LabelEn,
                        text_am = opt.LabelAm,
                        text_en = opt.LabelEn,
                        order_index = opt.OrderIndex,
                        is_correct = isPrivileged ? (opt.IsCorrect == 1) : (bool?)null
                    })
                })
            })
        };

        return Ok(new { success = true, data = formatted });
    }

    [HttpPost("surveys")]
    [Authorize(Roles = "ADMIN")]
    public async Task<IActionResult> CreateSurvey([FromBody] Survey input)
    {
        if (string.IsNullOrWhiteSpace(input.Code) || string.IsNullOrWhiteSpace(input.TitleAm))
        {
            return BadRequest(new { success = false, error = "code and title_am are required fields" });
        }

        input.CreatedAt = DateTime.UtcNow;
        _db.Surveys.Add(input);
        await _db.SaveChangesAsync();

        return CreatedAtAction(nameof(GetSurvey), new { id = input.Id.ToString() }, new { success = true, data = input });
    }

    [HttpGet("questions/{id}")]
    public async Task<IActionResult> GetQuestion(int id)
    {
        var question = await _db.Questions
            .Include(q => q.Options)
            .FirstOrDefaultAsync(q => q.Id == id);

        if (question == null)
        {
            return NotFound(new { success = false, error = $"Question {id} not found" });
        }

        return Ok(new { success = true, data = question });
    }

    [HttpPost("sections/{sectionId}/questions")]
    [Authorize(Roles = "ADMIN")]
    public async Task<IActionResult> CreateQuestion(int sectionId, [FromBody] Question input)
    {
        if (string.IsNullOrWhiteSpace(input.PromptAm))
        {
            return BadRequest(new { success = false, error = "prompt_am is required" });
        }

        input.SectionId = sectionId;
        _db.Questions.Add(input);
        await _db.SaveChangesAsync();

        return CreatedAtAction(nameof(GetQuestion), new { id = input.Id }, new { success = true, data = input });
    }

    [HttpPut("questions/{id}")]
    [Authorize(Roles = "ADMIN")]
    public async Task<IActionResult> UpdateQuestion(int id, [FromBody] Question updates)
    {
        var q = await _db.Questions.Include(x => x.Options).FirstOrDefaultAsync(x => x.Id == id);
        if (q == null)
        {
            return NotFound(new { success = false, error = $"Question {id} not found" });
        }

        q.PromptAm = updates.PromptAm ?? q.PromptAm;
        q.PromptEn = updates.PromptEn ?? q.PromptEn;
        q.RubricAm = updates.RubricAm ?? q.RubricAm;
        q.RubricEn = updates.RubricEn ?? q.RubricEn;
        q.Points = updates.Points != 0 ? updates.Points : q.Points;
        q.Type = updates.Type ?? q.Type;

        await _db.SaveChangesAsync();
        return Ok(new { success = true, data = q });
    }

    [HttpDelete("questions/{id}")]
    [Authorize(Roles = "ADMIN")]
    public async Task<IActionResult> DeleteQuestion(int id)
    {
        var q = await _db.Questions.FindAsync(id);
        if (q == null)
        {
            return NotFound(new { success = false, message = "Question not found" });
        }

        _db.Questions.Remove(q);
        await _db.SaveChangesAsync();
        return Ok(new { success = true, message = "Question deleted successfully" });
    }
}
