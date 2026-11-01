using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Models;

namespace CrvsAfr.Backend.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
    {
    }

    public DbSet<User> Users => Set<User>();
    public DbSet<SystemSetting> Settings => Set<SystemSetting>();
    public DbSet<Survey> Surveys => Set<Survey>();
    public DbSet<Section> Sections => Set<Section>();
    public DbSet<Question> Questions => Set<Question>();
    public DbSet<QuestionOption> QuestionOptions => Set<QuestionOption>();
    public DbSet<Participant> Participants => Set<Participant>();
    public DbSet<Submission> Submissions => Set<Submission>();
    public DbSet<Answer> Answers => Set<Answer>();
    public DbSet<Certificate> Certificates => Set<Certificate>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<User>()
            .HasIndex(u => u.Username)
            .IsUnique();

        modelBuilder.Entity<Survey>()
            .HasIndex(s => s.Code)
            .IsUnique();

        modelBuilder.Entity<Participant>()
            .HasIndex(p => p.ParticipantCode)
            .IsUnique();

        modelBuilder.Entity<Certificate>()
            .HasIndex(c => c.CertificateNumber)
            .IsUnique();

        modelBuilder.Entity<Section>()
            .HasOne<Survey>()
            .WithMany(s => s.Sections)
            .HasForeignKey(sec => sec.SurveyId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Question>()
            .HasOne<Section>()
            .WithMany(sec => sec.Questions)
            .HasForeignKey(q => q.SectionId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<QuestionOption>()
            .HasOne<Question>()
            .WithMany(q => q.Options)
            .HasForeignKey(o => o.QuestionId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Answer>()
            .HasOne<Submission>()
            .WithMany(s => s.Answers)
            .HasForeignKey(a => a.SubmissionId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}
