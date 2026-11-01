using Microsoft.EntityFrameworkCore;
using CrvsAfr.Backend.Models;
using CrvsAfr.Backend.Services;

namespace CrvsAfr.Backend.Data;

public static class DbInitializer
{
    public static void Initialize(AppDbContext context)
    {
        // Ensure database tables are created
        context.Database.EnsureCreated();

        // 1. Seed Default Settings
        var existingSettings = context.Settings.ToDictionary(s => s.Key);
        var defaultSettings = new List<SystemSetting>
        {
            new() { Key = "system_name", Value = "CRVS Question Gathering & Assessment System (የወሳኝ ኩነቶች ምዘና ሥርዓት)", Description = "System title displayed on dashboard and headers", Category = "GENERAL" },
            new() { Key = "agency_name", Value = "Dire Dawa Administration CRVS Agency (ድሬዳዋ አስተዳደር ወሳኝ ኩነቶች ኤጀንሲ)", Description = "Government Agency / Organization Name", Category = "GENERAL" },
            new() { Key = "manager_unlock_code", Value = "CRVS2026", Description = "Manager authorization code required to unlock Post-Test assessment", Category = "SECURITY" },
            new() { Key = "passing_score_percentage", Value = "50", Description = "Minimum percentage score required to pass assessment", Category = "ASSESSMENT" },
            new() { Key = "post_test_requires_manager", Value = "true", Description = "Enforce manager authorization code before examinees take Post-Test", Category = "ASSESSMENT" },
            new() { Key = "allow_self_registration", Value = "true", Description = "Allow new examinees to register directly on portal", Category = "SECURITY" },
            new() { Key = "max_exam_duration_mins", Value = "60", Description = "Maximum recommended duration for full assessment (minutes)", Category = "ASSESSMENT" },
            new() { Key = "certificate_signatory_name", Value = "Ato Solomon Worku (አቶ ሰለሞን ወርቁ)", Description = "Official signatory name appearing on approved competency certificates", Category = "CERTIFICATE" },
            new() { Key = "certificate_signatory_title", Value = "Dire Dawa CRVS Agency Director (የድሬዳዋ ወሳኝ ኩነቶች ኤጀንሲ ዳይሬክተር)", Description = "Official signatory title appearing on approved competency certificates", Category = "CERTIFICATE" }
        };

        foreach (var s in defaultSettings)
        {
            if (!existingSettings.ContainsKey(s.Key))
            {
                context.Settings.Add(s);
            }
        }
        context.SaveChanges();

        // 2. Seed Default Accounts (Admin, Manager, Examinees)
        if (!context.Users.Any())
        {
            var adminSalt = PasswordHasher.GenerateSalt();
            var managerSalt = PasswordHasher.GenerateSalt();
            var examinee1Salt = PasswordHasher.GenerateSalt();

            context.Users.AddRange(
                new User
                {
                    Username = "admin",
                    PasswordHash = PasswordHasher.HashPassword("admin123", adminSalt),
                    Salt = adminSalt,
                    FullName = "Dire Dawa System Admin (አስተዳዳሪ)",
                    Role = "ADMIN",
                    Status = "ACTIVE"
                },
                new User
                {
                    Username = "manager",
                    PasswordHash = PasswordHasher.HashPassword("manager123", managerSalt),
                    Salt = managerSalt,
                    FullName = "CRVS Assessment Manager (ኃላፊ/አስተባባሪ)",
                    Role = "USER_MANAGER",
                    Status = "ACTIVE"
                },
                new User
                {
                    Username = "examinee1",
                    PasswordHash = PasswordHasher.HashPassword("user123", examinee1Salt),
                    Salt = examinee1Salt,
                    FullName = "Abebe Bekele (ተፈታኝ)",
                    Role = "EXAMINEE",
                    ParticipantCode = "DD-REG-001",
                    WeredaKebele = "Kebele 01",
                    Gender = "Male",
                    ExperienceYears = 3.0,
                    Status = "ACTIVE"
                }
            );
            context.SaveChanges();
        }

        // 3. Seed Survey & Questions if missing
        if (!context.Surveys.Any(s => s.Code == "CRVS_DIRE_DAWA_1370"))
        {
            SeedSurveyCurriculum(context);
        }
    }

    private static void SeedSurveyCurriculum(AppDbContext context)
    {
        var survey = new Survey
        {
            Code = "CRVS_DIRE_DAWA_1370",
            TitleAm = "የድሬዳዋ ከተማ አስተዳደር የሲቪል ምዝገባ እና ወሳኝ ኩነቶች (አዋጅ 1370/2016) የመዝጋቢዎች የብቃት መመዘኛ ጥያቄዎች",
            TitleEn = "Dire Dawa Administration Civil Registration and Vital Events (Proc. 1370/2024) Competency Assessment",
            DescriptionAm = "የድሬዳዋ አስተዳደር ወሳኝ ኩነቶች መዝጋቢዎች የሕግ፣ የቴክኒክ እና የተግባር ብቃት መመዘኛ",
            DescriptionEn = "Comprehensive competency assessment for civil registrars under Proclamation 1370",
            TotalPoints = 24,
            PassingScore = 15
        };
        context.Surveys.Add(survey);
        context.SaveChanges();

        // Section A
        var secA = new Section
        {
            SurveyId = survey.Id,
            Code = "SECTION_A",
            TitleAm = "ክፍል ፩፡ የሕግ ማዕቀፍ እና የአዋጅ 1370/2016 ድንጋጌዎች",
            TitleEn = "Part A: Legal Framework & Proclamation 1370/2024 Provisions",
            DescriptionAm = "የወሳኝ ኩነቶች ምዝገባ ሕጋዊ መርሆዎች እና የማሳወቂያ የጊዜ ገደቦች",
            DescriptionEn = "Legal principles and statutory notification periods",
            OrderIndex = 1
        };
        context.Sections.Add(secA);
        context.SaveChanges();

        AddQuestionWithChoices(context, secA.Id, 1,
            "በአዋጅ ቁጥር 1370/2016 መሠረት የልደት ኩነት በመደበኛ የጊዜ ገደብ ውስጥ መመዝገብ ያለበት በስንት ቀናት ውስጥ ነው?",
            "According to Proclamation 1370/2024, within how many days must a birth event be registered under normal time?",
            1, new[] {
                ("ሀ", "በ 30 ቀናት ውስጥ", "Within 30 days", 0),
                ("ለ", "በ 90 ቀናት ውስጥ", "Within 90 days", 1),
                ("ሐ", "በ 60 ቀናት ውስጥ", "Within 60 days", 0),
                ("መ", "በ 1 ዓመት ውስጥ", "Within 1 year", 0)
            });

        AddQuestionWithChoices(context, secA.Id, 2,
            "የሞት ኩነት በመደበኛነት መመዝገብ ያለበት ከተከሰተበት ቀን ጀምሮ ባሉት ስንት ቀናት ውስጥ ነው?",
            "Within how many days from occurrence must a death event be normally registered?",
            1, new[] {
                ("ሀ", "በ 15 ቀናት ውስጥ", "Within 15 days", 0),
                ("ለ", "በ 30 ቀናት ውስጥ", "Within 30 days", 1),
                ("ሐ", "በ 60 ቀናት ውስጥ", "Within 60 days", 0),
                ("መ", "በ 90 ቀናት ውስጥ", "Within 90 days", 0)
            });

        AddQuestionWithChoices(context, secA.Id, 3,
            "የጋብቻ ኩነት ከተፈጸመበት ቀን ጀምሮ በስንት ቀናት ውስጥ መመዝገብ ይኖርበታል?",
            "Within how many days from solemnization must marriage be registered?",
            1, new[] {
                ("ሀ", "በ 30 ቀናት ውስጥ", "Within 30 days", 1),
                ("ለ", "በ 45 ቀናት ውስጥ", "Within 45 days", 0),
                ("ሐ", "በ 90 ቀናት ውስጥ", "Within 90 days", 0),
                ("መ", "በ 6 ወራት ውስጥ", "Within 6 months", 0)
            });

        // Section B
        var secB = new Section
        {
            SurveyId = survey.Id,
            Code = "SECTION_B",
            TitleAm = "ክፍል ፪፡ የዲጂታል ሲአርቪኤስ ስርዓት አጠቃቀም እና የመረጃ ደህንነት",
            TitleEn = "Part B: Digital CRVS System Operation & Data Security",
            DescriptionAm = "የኤሌክትሮኒክ ምዝገባ፣ የባዮሜትሪክስ እና የመረጃ ጥራት መርሆዎች",
            DescriptionEn = "Electronic data collection, biometrics, and confidentiality",
            OrderIndex = 2
        };
        context.Sections.Add(secB);
        context.SaveChanges();

        AddQuestionWithChoices(context, secB.Id, 9,
            "በዲጂታል CRVS ስርዓት ውስጥ የተሳሳተ የልደት ቀን ተመዝግቦ ቢገኝ መዝጋቢው ምን ማድረግ አለበት?",
            "What should a registrar do if an incorrect birth date is discovered in the system?",
            1, new[] {
                ("ሀ", "በቀጥታ ስርዓቱ ላይ ማጥፋት", "Directly delete from database", 0),
                ("ለ", "በማስረጃ እና በኃላፊ ፈቃድ የማስተካከያ (Amendment) ፎርም መሙላት", "Complete official amendment form with evidence and approval", 1),
                ("ሐ", "አዲስ ምዝገባ በድጋሚ መክፈት", "Create duplicate fresh record", 0),
                ("መ", "ችላ ብሎ መተው", "Ignore and leave unchanged", 0)
            });

        // Section C
        var secC = new Section
        {
            SurveyId = survey.Id,
            Code = "SECTION_C",
            TitleAm = "ክፍል ፫፡ ተግባራዊ ሁኔታዎች እና የስነ-ምግባር ምዘና",
            TitleEn = "Part C: Practical Scenarios & Ethics",
            DescriptionAm = "በመስክ ስራ የሚያጋጥሙ ችግሮችን የመፍታት ብቃት",
            DescriptionEn = "Field challenge problem solving and regulatory compliance",
            OrderIndex = 3
        };
        context.Sections.Add(secC);
        context.SaveChanges();

        AddQuestionWithChoices(context, secC.Id, 17,
            "አንዲት እናት ልጇን በቤት ውስጥ ወልዳ ከ 4 ወራት በኋላ ለምዝገባ ብትመጣ ሂደቱ እንዴት ይስተናገዳል?",
            "If a mother gives birth at home and applies after 4 months, how is the case processed?",
            1, new[] {
                ("ሀ", "መደበኛ የጊዜ ገደብ ስላለፈ አይመዘገብም", "Reject registration outright", 0),
                ("ለ", "የዘገየ ምዝገባ (Late Registration) ደንብ መሠረት በማስረጃ ተረጋግጦ ይስተናገዳል", "Processed under Late Registration protocol with required evidence", 1),
                ("ሐ", "የፍርድ ቤት ውሳኔ እስኪመጣ ይታገዳል", "Must require court verdict first", 0),
                ("መ", "የልደት ምስክር ወረቀት ሳይሰጥ ይመዘገባል", "Record without issuing certificate", 0)
            });
    }

    private static void AddQuestionWithChoices(AppDbContext context, int sectionId, int qNum, string promptAm, string promptEn, int points, (string key, string am, string en, int correct)[] choices)
    {
        var q = new Question
        {
            SectionId = sectionId,
            QuestionNumber = qNum,
            Type = "SINGLE_CHOICE",
            PromptAm = promptAm,
            PromptEn = promptEn,
            Points = points,
            OrderIndex = qNum
        };
        context.Questions.Add(q);
        context.SaveChanges();

        int order = 1;
        foreach (var c in choices)
        {
            context.QuestionOptions.Add(new QuestionOption
            {
                QuestionId = q.Id,
                OptionKey = c.key,
                LabelAm = c.am,
                LabelEn = c.en,
                IsCorrect = c.correct,
                OrderIndex = order++
            });
        }
        context.SaveChanges();
    }
}
