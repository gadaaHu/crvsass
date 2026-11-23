package com.qcafr.gatherapp.util

/**
 * Single source of truth for CRVS offline keyword-response rules.
 *
 * Both [com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel.getOfflineAiResponse]
 * and any future offline use should delegate here so that proclamation deadlines
 * and document lists only need to be updated in one place.
 *
 * Rules are based on Ethiopian Proclamation No. 1370/2024 (2016 E.C.)
 */
object CrvsOfflineFallback {

    fun getResponse(question: String, isAmharic: Boolean): String {
        val q = question.trim().lowercase()
        return when {
            q.contains("1370") || q.contains("አዋጅ") || q.contains("proclamation") ||
            q.contains("ሕግ") || q.contains("ህግ") || q.contains("law") ->
                if (isAmharic) PROCLAMATION_AM else PROCLAMATION_EN

            q.contains("ልደት") || q.contains("birth") ||
            q.contains("ሕፃን") || q.contains("ህፃን") || q.contains("child") ->
                if (isAmharic) BIRTH_AM else BIRTH_EN

            q.contains("ሞት") || q.contains("death") ||
            q.contains("ሟች") || q.contains("አስከሬን") ->
                if (isAmharic) DEATH_AM else DEATH_EN

            q.contains("ጋብቻ") || q.contains("marriage") || q.contains("ሰርግ") ->
                if (isAmharic) MARRIAGE_AM else MARRIAGE_EN

            q.contains("ፍቺ") || q.contains("divorce") ->
                if (isAmharic) DIVORCE_AM else DIVORCE_EN

            else ->
                if (isAmharic) GENERIC_AM else GENERIC_EN
        }
    }

    // ── Proclamation ──────────────────────────────────────────────────────────

    private const val PROCLAMATION_AM = """📜 **የወሳኝ ኩነቶች ምዝገባ አዋጅ ቁጥር 1370/2024 (2016 ዓ.ም)**

ይህ አዋጅ በኢትዮጵያ የወሳኝ ኩነቶች ምዝገባ ሥርዓትን በዘመናዊ ዲጂታል ቴክኖሎጂ ለማደራጀት የወጣ አዲስ አዋጅ ነው።

**ዋና ዋና ነጥቦች፡**
1. **የኩነቶች ዓይነቶች፡** ልደት፣ ሞት፣ ጋብቻ፣ ፍቺ፣ ጉዲፈቻ እና አባትነትን ማወቅ።
2. **የምዝገባ ግዴታ፡** እያንዳንዱ ዜጋ ወይም ነዋሪ ኩነቶች ሲከሰቱ በተቀመጠው የጊዜ ገደብ የማስመዝገብ ግዴታ አለበት።
3. **ዲጂታል ምዝገባ፡** የወረቀት ሰነዶችን በማስቀረት በማዕከላዊ ዳታቤዝ የተደገፈ ዲጂታል የምስክር ወረቀት በ QR ኮድ ይሰጣል።"""

    private const val PROCLAMATION_EN = """📜 **CRVS Proclamation No. 1370/2024**

This proclamation modernizes Ethiopian civil registration through digital registers and cryptographic QR verification for birth, death, marriage, and divorce."""

    // ── Birth ─────────────────────────────────────────────────────────────────

    private const val BIRTH_AM = """👶 **የልደት ምዝገባ አሰራር እና መመሪያዎች**

**የጊዜ ገደብ፡**
• መደበኛ ምዝገባ፡ ህፃኑ በተወለደ በ **90 ቀናት** ውስጥ ያለ ምንም ቅጣት በነፃ ይመዘገባል።
• የዘገየ ምዝገባ፡ ከ 90 ቀናት በኋላ እስከ 1 ዓመት የዘገየ ምዝገባ ተብሎ በቅጣት ይመዘገባል።

**አስፈላጊ ሰነዶች፡**
1. ከጤና ተቋም የተሰጠ የልደት ማስታወቂያ ሰነድ (Birth Notification Paper)።
2. የወላጆች የታደሰ የቀበሌ መታወቂያ ወይም ፓስፖርት።
3. የወላጆች የጋብቻ ምስክር ወረቀት (ካለ)።"""

    private const val BIRTH_EN = """👶 **Birth Registration Guidelines**

• **Standard Deadline:** Within **90 days** from birth without penalty.
• **Late Registration:** After 90 days up to 1 year.
• **Required:** Official health institution notification paper and parents' valid IDs."""

    // ── Death ─────────────────────────────────────────────────────────────────

    private const val DEATH_AM = """⚰️ **የሞት ምዝገባ አሰራር እና መመሪያዎች**

**የጊዜ ገደብ፡**
• ሞት ከተከሰተበት ቀን ጀምሮ በ **30 ቀናት** ውስጥ መመዝገብ አለበት።

**አስፈላጊ ሰነዶች፡**
1. ከጤና ተቋም የተሰጠ የሞት መንስኤ ማረጋገጫ ሰነድ (MCCD)።
2. የሟች የቀበሌ መታወቂያ።
3. የአስመዝጋቢው (የቅርብ ቤተሰብ) መታወቂያ።"""

    private const val DEATH_EN = """⚰️ **Death Registration Guidelines**

• **Deadline:** Within **30 days** of death.
• **Required:** Medical Certificate of Cause of Death (MCCD) from health institution, ID of the deceased, and ID of the informant."""

    // ── Marriage ──────────────────────────────────────────────────────────────

    private const val MARRIAGE_AM = """💍 **የጋብቻ ምዝገባ አሰራር**

**የጊዜ ገደብ፡** ጋብቻ ከተፈጸመበት ቀን ጀምሮ በ **30 ቀናት** ውስጥ።

**ቅድመ-ሁኔታዎች፡**
1. ሁለቱም ተጋቢዎች በአካል መቅረብ አለባቸው።
2. ዝቅተኛው የጋብቻ ዕድሜ በሕግ **18 ዓመት** ነው።
3. ቢያንስ 2 ህጋዊ እድሜያቸው ከ 18 ዓመት በላይ የሆኑ ምስክሮች ከመታወቂያቸው ጋር መቅረብ አለባቸው።"""

    private const val MARRIAGE_EN = """💍 **Marriage Registration Guidelines**

• **Deadline:** Within **30 days** of celebration.
• Both spouses must be present before the Registrar.
• Minimum legal age is **18 years** with at least 2 witnesses."""

    // ── Divorce ───────────────────────────────────────────────────────────────

    private const val DIVORCE_AM = """⚖️ **የፍቺ ምዝገባ አሰራር**

**የጊዜ ገደብ፡** የመጨረሻ የፍርድ ቤት ውሳኔ ከተሰጠበት ቀን ጀምሮ በ **30 ቀናት** ውስጥ።

**አስፈላጊ ሰነዶች፡**
1. ስልጣን ካለው ፍርድ ቤት የተሰጠ የመጨረሻ የፍቺ ውሳኔ ሰነድ (Court Decree)።
2. የሁለቱም ወይም የአንደኛው ተፋቺ ህጋዊ መታወቂያ።"""

    private const val DIVORCE_EN = """⚖️ **Divorce Registration Guidelines**

• **Deadline:** Within **30 days** of final court decree.
• **Required:** Certified copy of final court divorce judgment and valid ID."""

    // ── Generic ───────────────────────────────────────────────────────────────

    private const val GENERIC_AM = """🤖 **የ CRVS ስልጠና AI አማካሪ**

ለጥያቄዎ እናመሰግናለን! በኢትዮጵያ የወሳኝ ኩነቶች ምዝገባ አዋጅ ቁጥር 1370/2024 መሰረት፡

• የልደት ምዝገባ በ 90 ቀናት ውስጥ
• የሞት ምዝገባ በ 30 ቀናት ውስጥ
• የጋብቻ ምዝገባ በ 30 ቀናት ውስጥ

እባክዎ የተለየ ርዕስ (ለምሳሌ 'የልደት ምዝገባ ጊዜ ገደብ' ወይም 'የሞት ማረጋገጫ ሰነዶች') ጠይቁኝ፤ በዝርዝር እመልስልዎታለሁ!"""

    private const val GENERIC_EN = """🤖 **CRVS Training AI Tutor**

Under Ethiopian CRVS Proclamation 1370/2024: Birth registration deadline is 90 days, Death is 30 days, and Marriage is 30 days. Feel free to ask about any specific training topic!"""
}
