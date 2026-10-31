package com.qcafr.gatherapp.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.graphics.Path
import com.qcafr.gatherapp.data.model.AnswerResult
import com.qcafr.gatherapp.data.model.Participant
import com.qcafr.gatherapp.data.model.SubmissionRecordItem
import com.qcafr.gatherapp.data.model.SubmissionResponse
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PdfExporter – generates a multi-page, professional A4 PDF of the exam result,
 * as well as authentic official Birth/Civil Competency Certificates matching the
 * official state certificate template.
 */
object PdfExporter {

    private const val PAGE_WIDTH  = 595   // A4 points
    private const val PAGE_HEIGHT = 842   // A4 points
    private const val MARGIN      = 46f
    private const val CONTENT_W   = PAGE_WIDTH - MARGIN * 2   // 503 pts

    // ── Paint helpers ────────────────────────────────────────────────────────

    private fun paint(
        colorHex: String,
        size: Float = 12f,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ) = Paint().apply {
        color = Color.parseColor(colorHex)
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                   else Typeface.DEFAULT
        textAlign = align
        isAntiAlias = true
    }

    private fun fillPaint(colorHex: String) = Paint().apply {
        color = Color.parseColor(colorHex)
        style = Paint.Style.FILL
    }

    private fun strokePaint(colorHex: String, width: Float = 1f) = Paint().apply {
        color = Color.parseColor(colorHex)
        style = Paint.Style.STROKE
        strokeWidth = width
        isAntiAlias = true
    }

    // ── Public entry points ──────────────────────────────────────────────────

    fun exportResultPdf(
        context: Context,
        response: SubmissionResponse,
        participant: Participant?,
        agencyName: String = "CRVS Agency",
        place: String = "Ethiopia"
    ): File {
        val document = PdfDocument()
        val renderer = PageRenderer(document, response, participant, agencyName, place)
        renderer.render()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "CRVS_Result_${response.participantCode}_$timestamp.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun exportAllSubmissionsPdf(
        context: Context,
        submissions: List<SubmissionRecordItem>,
        agencyName: String = "CRVS Agency",
        place: String = "Ethiopia"
    ): File {
        val document = PdfDocument()
        val renderer = AllSubmissionsPageRenderer(document, submissions, agencyName)
        renderer.render()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "CRVS_All_Examinee_Results_$timestamp.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun exportSingleSubmissionRecordPdf(
        context: Context,
        item: SubmissionRecordItem,
        agencyName: String = "CRVS Agency",
        place: String = "Ethiopia"
    ): File {
        val document = PdfDocument()
        val renderer = SingleRecordPageRenderer(document, item, context, agencyName, place)
        renderer.render()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "CRVS_Certificate_${item.participantCode}_$timestamp.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun exportCertificatePdf(
        context: Context,
        cert: Map<String, Any>,
        agencyName: String = "CRVS Agency",
        place: String = "Ethiopia"
    ): File {
        val document = PdfDocument()
        val renderer = CertificatePageRenderer(document, cert, context, agencyName, place)
        renderer.render()

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val certNo = ((cert["certificate_number"] as? String) ?: "D001456BABCD123")
            .replace("/", "_").replace(" ", "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "Certificate_${certNo}_$timestamp.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    // ── Internal multi-page renderer ─────────────────────────────────────────

    private class PageRenderer(
        private val document: PdfDocument,
        private val response: SubmissionResponse,
        private val participant: Participant?,
        private val agencyName: String = "CRVS Agency",
        private val place: String = "Ethiopia"
    ) {
        private var pageNumber = 1
        private lateinit var currentPage: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = 0f

        // Body safe zone: below header (88 pts), above footer
        private val bodyTop = 100f
        private val bodyBottom = PAGE_HEIGHT - 50f
        private val cx = PAGE_WIDTH / 2f

        fun render() {
            openPage()
            drawHeader(isFirstPage = true)
            y = bodyTop

            drawScoreSection()
            drawDivider()
            drawParticipantInfo()
            drawDivider()
            drawAnswerBreakdown()

            drawFooter()
            document.finishPage(currentPage)
        }

        // ── Page management ──────────────────────────────────────────────────

        private fun openPage() {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = document.startPage(info)
            canvas = currentPage.canvas
        }

        private fun newPage() {
            drawFooter()
            document.finishPage(currentPage)
            pageNumber++
            openPage()
            drawHeader(isFirstPage = false)
            y = bodyTop
        }

        /** Ensure there is at least [needed] pts of space; if not, start a new page. */
        private fun ensureSpace(needed: Float) {
            if (y + needed > bodyBottom) newPage()
        }

        // ── Header / footer ──────────────────────────────────────────────────

        private fun drawHeader(isFirstPage: Boolean) {
            // Indigo banner (slimmer: 88 pts instead of 108)
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 88f, fillPaint("#6366F1"))

            canvas.drawText(
                "CRVS Assessment Result",
                MARGIN, 36f,
                paint("#FFFFFF", 18f, bold = true)
            )
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
            canvas.drawText(
                "$dateStr  ·  Page $pageNumber",
                MARGIN, 56f,
                paint("#A5B4FC", 10f)
            )
            canvas.drawLine(0f, 88f, PAGE_WIDTH.toFloat(), 88f,
                strokePaint("#4F46E5", 1.5f))
        }

        private fun drawFooter() {
            canvas.drawLine(MARGIN, bodyBottom + 10f,
                PAGE_WIDTH - MARGIN, bodyBottom + 10f,
                strokePaint("#E5E7EB", 0.5f))
            canvas.drawText(
                "CRVS Gather · $agencyName  ·  Page $pageNumber",
                cx, bodyBottom + 26f,
                paint("#9CA3AF", 8.5f, align = Paint.Align.CENTER)
            )
        }

        // ── Score section (first page only) ─────────────────────────────────

        private fun drawScoreSection() {
            val r = 52f
            val circleCy = y + r + 8f

            // Circle fill + stroke
            canvas.drawCircle(cx, circleCy, r, fillPaint("#EEF2FF"))
            canvas.drawCircle(cx, circleCy, r, strokePaint("#6366F1", 5f))

            // Score text
            canvas.drawText(
                "${response.totalScore.toInt()}",
                cx, circleCy + 14f,
                paint("#4F46E5", 38f, bold = true, align = Paint.Align.CENTER)
            )
            canvas.drawText(
                "/ ${response.maxScore.toInt()} pts",
                cx, circleCy + 32f,
                paint("#6B7280", 12f, align = Paint.Align.CENTER)
            )

            y = circleCy + r + 16f

            // Performance band pill
            val bandColor = bandColor()
            val bandLabel = bandLabel()
            val pillW = 220f; val pillH = 28f
            val pillLeft = cx - pillW / 2f
            val pillRect = RectF(pillLeft, y, pillLeft + pillW, y + pillH)
            canvas.drawRoundRect(pillRect, 14f, 14f, fillPaint(bandColor).apply { alpha = 40 })
            canvas.drawRoundRect(pillRect, 14f, 14f, strokePaint(bandColor, 1f))
            canvas.drawText(
                bandLabel,
                cx, y + 19f,
                paint(bandColor, 12f, bold = true, align = Paint.Align.CENTER)
            )
            y += pillH + 8f

            // Score percentage
            canvas.drawText(
                "Score: ${response.scorePercentage.toInt()}%   |   " +
                "Test: ${if (response.testType == "PRE") "Pre-Training (ቅድመ)" else "Post-Training (ድህረ)"}",
                cx, y + 14f,
                paint("#6B7280", 11f, align = Paint.Align.CENTER)
            )
            y += 28f
        }

        // ── Participant info table ────────────────────────────────────────────

        private fun drawParticipantInfo() {
            sectionTitle("Participant Details")

            val labelPaint = paint("#6B7280", 10.5f)
            val valuePaint = paint("#111827", 10.5f, bold = true)

            fun infoRow(label: String, value: String) {
                ensureSpace(17f)
                canvas.drawText(label, MARGIN, y, labelPaint)
                canvas.drawText(value, MARGIN + 140f, y, valuePaint)
                y += 17f
            }

            infoRow("Code:", response.participantCode)
            infoRow("Test:", if (response.testType == "PRE") "Pre-Training" else "Post-Training")
            infoRow("Date:", response.submittedAt.take(10))
            if (participant != null) {
                infoRow("Place of Registration:", participant.weredaKebele ?: "—")
                infoRow("Role:", participant.role ?: "—")
            }

            y += 6f
        }

        // ── Answer breakdown table ────────────────────────────────────────────

        private fun drawAnswerBreakdown() {
            if (response.answers.isNullOrEmpty()) return

            sectionTitle("Answer Breakdown")

            // Compact summary line
            ensureSpace(20f)
            val correct = response.answers.count { it.isCorrect }
            val total   = response.answers.size
            canvas.drawText(
                "$correct / $total correct  ·  ${response.totalScore.toInt()} / ${response.maxScore.toInt()} pts",
                MARGIN, y,
                paint("#374151", 10.5f)
            )
            y += 16f

            // Table header
            ensureSpace(24f)
            val colQ   = MARGIN
            val colSel = MARGIN + 40f
            val colCor = MARGIN + 195f
            val colSc  = MARGIN + 300f
            val colMax = MARGIN + 355f

            val hdrPaint = paint("#4F46E5", 9.5f, bold = true)
            canvas.drawText("Q#",       colQ,   y, hdrPaint)
            canvas.drawText("Answer",   colSel, y, hdrPaint)
            canvas.drawText("Result",   colCor, y, hdrPaint)
            canvas.drawText("Score",    colSc,  y, hdrPaint)
            canvas.drawText("Max",      colMax, y, hdrPaint)
            y += 4f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y,
                strokePaint("#6366F1", 0.6f))
            y += 9f

            // Rows
            var rowAlt = false
            response.answers.forEachIndexed { idx, ans ->
                ensureSpace(20f)

                // Alternating row tint
                if (rowAlt) {
                    canvas.drawRect(
                        MARGIN - 4f, y - 13f,
                        PAGE_WIDTH - MARGIN + 4f, y + 5f,
                        fillPaint("#F9FAFB")
                    )
                }
                rowAlt = !rowAlt

                val rowPaint    = paint("#1F2937", 10f)
                val correctPaint = paint("#059669", 10f, bold = true)
                val wrongPaint  = paint("#DC2626", 10f)

                canvas.drawText("Q${ans.questionNumber}", colQ, y, rowPaint)

                // Truncate long answers to fit cell
                val selectedText = (ans.selectedOptionKey ?: "(open/rating)").take(22)
                canvas.drawText(selectedText, colSel, y, rowPaint)

                val (correctText, cpaint) = if (ans.isCorrect) "✓ Correct" to correctPaint
                                             else "✗ Wrong" to wrongPaint
                canvas.drawText(correctText, colCor, y, cpaint)
                canvas.drawText("${ans.scoreAwarded.toInt()}", colSc, y, rowPaint)
                canvas.drawText("${ans.maxPoints.toInt()}", colMax, y, rowPaint)
                y += 18f

                // Show correct answer key if the user got it wrong
                if (!ans.isCorrect && !ans.correctOptionKey.isNullOrBlank()) {
                    ensureSpace(14f)
                    canvas.drawText(
                        "     ✎ Correct: ${ans.correctOptionKey}",
                        colSel, y,
                        paint("#059669", 9f)
                    )
                    y += 14f
                }
            }

            y += 8f

            // Summary total row
            ensureSpace(24f)
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, strokePaint("#D1D5DB", 0.5f))
            y += 10f
            canvas.drawText(
                "TOTAL SCORE",
                MARGIN, y,
                paint("#111827", 11f, bold = true)
            )
            canvas.drawText(
                "${response.totalScore.toInt()}",
                colSc, y,
                paint("#4F46E5", 11f, bold = true)
            )
            canvas.drawText(
                "${response.maxScore.toInt()}",
                colMax, y,
                paint("#4F46E5", 11f, bold = true)
            )
            canvas.drawText(
                "= ${response.scorePercentage.toInt()}%  (${bandLabel()})",
                colMax + 30f, y,
                paint(bandColor(), 11f, bold = true)
            )
            y += 20f
        }

        // ── Helpers ──────────────────────────────────────────────────────────

        private fun drawDivider() {
            ensureSpace(16f)
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, strokePaint("#E5E7EB", 0.8f))
            y += 14f
        }

        private fun sectionTitle(text: String) {
            ensureSpace(20f)
            canvas.drawText(text, MARGIN, y, paint("#6366F1", 11f, bold = true))
            y += 16f
        }

        private fun bandColor() = when (response.performanceBand) {
            "STRONG"               -> "#10B981"
            "SATISFACTORY"         -> "#06B6D4"
            "NEEDS_REINFORCEMENT"  -> "#F59E0B"
            else                   -> "#EF4444"
        }

        private fun bandLabel() = when (response.performanceBand) {
            "STRONG"               -> "STRONG — ከፍተኛ ብቃት"
            "SATISFACTORY"         -> "SATISFACTORY — አጥጋቢ"
            "NEEDS_REINFORCEMENT"  -> "NEEDS REINFORCEMENT — ድጋፍ ያስፈልጋል"
            else                   -> response.performanceBand
        }
    }

    // ── Multi-Examinee All Submissions Roster Renderer ────────────────────────

    private class AllSubmissionsPageRenderer(
        private val document: PdfDocument,
        private val submissions: List<SubmissionRecordItem>,
        private val agencyName: String = "CRVS Agency"
    ) {
        private var pageNumber = 1
        private lateinit var currentPage: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = 0f

        private val margin = 36f
        private val bodyTop = 118f
        private val bodyBottom = PAGE_HEIGHT - 48f
        private val cx = PAGE_WIDTH / 2f

        fun render() {
            openPage()
            drawHeader(isFirstPage = true)
            y = bodyTop

            drawExecutiveSummary()
            drawTableHeader()
            drawRows()

            drawFooter()
            document.finishPage(currentPage)
        }

        private fun openPage() {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = document.startPage(info)
            canvas = currentPage.canvas
        }

        private fun newPage() {
            drawFooter()
            document.finishPage(currentPage)
            pageNumber++
            openPage()
            drawHeader(isFirstPage = false)
            y = bodyTop
            drawTableHeader()
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed > bodyBottom) newPage()
        }

        private fun drawHeader(isFirstPage: Boolean) {
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 88f, fillPaint("#1E1B4B"))
            canvas.drawText(
                "CRVS — Master Results Roster",
                margin, 36f,
                paint("#FFFFFF", 17f, bold = true)
            )
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
            canvas.drawText(
                "$dateStr  ·  Page $pageNumber",
                margin, 56f,
                paint("#A5B4FC", 9f)
            )
            canvas.drawLine(0f, 88f, PAGE_WIDTH.toFloat(), 88f, strokePaint("#4338CA", 1.5f))
        }

        private fun drawFooter() {
            canvas.drawLine(margin, bodyBottom + 8f, PAGE_WIDTH - margin, bodyBottom + 8f, strokePaint("#E2E8F0", 0.6f))
            canvas.drawText(
                "CRVS Gather · $agencyName  ·  Page $pageNumber",
                cx, bodyBottom + 22f,
                paint("#94A3B8", 8f, align = Paint.Align.CENTER)
            )
        }

        private fun drawExecutiveSummary() {
            val total = submissions.size
            val preCount = submissions.count { it.testType.uppercase() == "PRE" }
            val postCount = submissions.count { it.testType.uppercase() == "POST" }
            val avg = if (total > 0) submissions.map { it.scorePercentage }.average().toInt() else 0
            val passCount = submissions.count { it.scorePercentage >= 60f }
            val passRate = if (total > 0) ((passCount.toFloat() / total) * 100).toInt() else 0

            val boxW = (PAGE_WIDTH - margin * 2 - 24f) / 4f
            val boxH = 46f
            val boxY = y

            val boxes = listOf(
                Pair("TOTAL EXAMINEES", "$total"),
                Pair("PRE / POST", "$preCount / $postCount"),
                Pair("AVERAGE SCORE", "$avg%"),
                Pair("PASSING RATE", "$passRate% (≥60%)")
            )

            boxes.forEachIndexed { idx, pair ->
                val bx = margin + idx * (boxW + 8f)
                canvas.drawRoundRect(RectF(bx, boxY, bx + boxW, boxY + boxH), 8f, 8f, fillPaint("#F1F5F9"))
                canvas.drawRoundRect(RectF(bx, boxY, bx + boxW, boxY + boxH), 8f, 8f, strokePaint("#CBD5E1", 0.8f))
                canvas.drawText(pair.first, bx + 8f, boxY + 16f, paint("#64748B", 8f, bold = true))
                canvas.drawText(pair.second, bx + 8f, boxY + 36f, paint("#0F172A", 14f, bold = true))
            }

            y += boxH + 16f
        }

        private fun drawTableHeader() {
            val hY = y
            canvas.drawRect(margin, hY, PAGE_WIDTH - margin, hY + 22f, fillPaint("#4338CA"))

            val p = paint("#FFFFFF", 9f, bold = true)
            canvas.drawText("#", margin + 6f, hY + 14f, p)
            canvas.drawText("Participant Code", margin + 28f, hY + 14f, p)
            canvas.drawText("Place of Registration", margin + 130f, hY + 14f, p)
            canvas.drawText("Role", margin + 250f, hY + 14f, p)
            canvas.drawText("Mode", margin + 335f, hY + 14f, p)
            canvas.drawText("Score", margin + 385f, hY + 14f, p)
            canvas.drawText("%", margin + 435f, hY + 14f, p)
            canvas.drawText("Classification", margin + 465f, hY + 14f, p)

            y += 24f
        }

        private fun drawRows() {
            submissions.forEachIndexed { index, sub ->
                ensureSpace(22f)
                val rowY = y
                val isEven = index % 2 == 0
                if (isEven) {
                    canvas.drawRect(margin, rowY, PAGE_WIDTH - margin, rowY + 20f, fillPaint("#F8FAFC"))
                }
                canvas.drawLine(margin, rowY + 20f, PAGE_WIDTH - margin, rowY + 20f, strokePaint("#F1F5F9", 0.5f))

                val textPaint = paint("#1E293B", 8.5f)
                canvas.drawText("${index + 1}", margin + 6f, rowY + 13f, paint("#64748B", 8f))
                canvas.drawText(sub.participantCode, margin + 28f, rowY + 13f, paint("#0F172A", 8.5f, bold = true))
                val wereda = sub.weredaKebele ?: agencyName
                canvas.drawText(if (wereda.length > 20) wereda.take(18) + "…" else wereda, margin + 130f, rowY + 13f, textPaint)
                val role = sub.role ?: "Registrar"
                canvas.drawText(if (role.length > 14) role.take(12) + "…" else role, margin + 250f, rowY + 13f, textPaint)

                // Mode
                val isPost = sub.testType.uppercase() == "POST"
                val modeColor = if (isPost) "#10B981" else "#F59E0B"
                canvas.drawText(if (isPost) "POST" else "PRE", margin + 335f, rowY + 13f, paint(modeColor, 8.5f, bold = true))

                // Score & %
                canvas.drawText("${sub.totalScore.toInt()}/${sub.maxScore.toInt()}", margin + 385f, rowY + 13f, paint("#334155", 8.5f, bold = true))
                canvas.drawText("${sub.scorePercentage.toInt()}%", margin + 435f, rowY + 13f, paint(if (sub.scorePercentage >= 60) "#10B981" else "#F59E0B", 8.5f, bold = true))

                // Band
                val bandColor = when (sub.performanceBand) {
                    "STRONG" -> "#10B981"
                    "SATISFACTORY" -> "#4338CA"
                    "NEEDS_REINFORCEMENT" -> "#F59E0B"
                    else -> "#EF4444"
                }
                canvas.drawText(sub.performanceBand, margin + 465f, rowY + 13f, paint(bandColor, 8f, bold = true))

                y += 21f
            }

            // Sign-off line
            ensureSpace(36f)
            y += 12f
            canvas.drawLine(margin, y, margin + 160f, y, strokePaint("#94A3B8", 0.8f))
            canvas.drawText("Authorized Signature", margin, y + 12f, paint("#94A3B8", 8f))
            y += 24f
        }
    }

    // ── Single Examinee Record Certificate Renderer ──────────────────────────

    private class SingleRecordPageRenderer(
        private val document: PdfDocument,
        private val item: SubmissionRecordItem,
        private val context: Context? = null,
        private val agencyName: String = "CRVS Agency",
        private val place: String = "Ethiopia"
    ) {
        fun render() {
            val dateStr = if (item.submittedAt.length >= 10) item.submittedAt.take(10) else SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
            val certNum = item.certificateNumber ?: "CRVS-2026-${item.participantCode.replace("-", "").takeLast(8)}"
            val certMap = mapOf<String, Any>(
                "certificate_number" to certNum,
                "participant_code" to item.participantCode,
                "full_name" to item.participantCode,
                "wereda_kebele" to (item.weredaKebele ?: agencyName),
                "role" to (item.role ?: "Certified Vital Events Registrar"),
                "gender" to (item.gender ?: "Female"),
                "survey_title_en" to "$agencyName Competency Assessment",
                "test_type" to item.testType,
                "score_percentage" to item.scorePercentage,
                "total_score" to item.totalScore,
                "max_score" to item.maxScore,
                "performance_band" to item.performanceBand,
                "issued_by" to (item.approvedBy ?: "$agencyName Director"),
                "issued_at" to dateStr,
                "submission_id" to item.id
            )
            CertificatePageRenderer(document, certMap, context, agencyName, place).render()
        }
    }

    // ── Authentic Official CRVS Competency Certificate Page Renderer ──────────

    private class CertificatePageRenderer(
        private val document: PdfDocument,
        private val cert: Map<String, Any>,
        private val context: Context? = null,
        private val agencyName: String = "CRVS Agency",
        private val place: String = "Ethiopia"
    ) {
        fun render() {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(info)
            val canvas = page.canvas

            // ── Data extraction ──────────────────────────────────────────────
            val docNo              = cert["certificate_number"] as? String ?: "N/A"
            val participantCode    = cert["participant_code"]   as? String ?: "N/A"
            val fullName           = (cert["full_name"] as? String ?: participantCode).trim()
            val facility           = (cert["wereda_kebele"] as? String)?.ifEmpty { agencyName } ?: agencyName
            val rawDate            = (cert["issued_at"] as? String)?.take(10) ?: SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date())
            val dateOfIssuance     = rawDate.ifEmpty { SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date()) }
            val rawGender          = (cert["gender"] as? String) ?: "Female"
            val gender             = if (rawGender.uppercase().startsWith("M") || rawGender.contains("ወንድ")) "Male (ወንድ)" else "Female (ሴት)"
            val role               = (cert["role"] as? String)?.ifEmpty { "Vital Events Registrar" } ?: "Vital Events Registrar"
            val testType           = ((cert["test_type"] as? String) ?: "POST").uppercase()
            val testTypeLabel      = if (testType.startsWith("POST")) "Post-Test (ድህረ-ሙከራ)" else "Pre-Test (ቅድመ-ሙከራ)"
            val scorePct           = (cert["score_percentage"] as? Number)?.toInt() ?: 0
            val totalScore         = (cert["total_score"]      as? Number)?.toInt() ?: 0
            val maxScore           = (cert["max_score"]        as? Number)?.toInt() ?: 24
            val band               = (cert["performance_band"] as? String) ?: "SATISFACTORY"
            val issuingOfficer     = cert["issued_by"] as? String ?: "$agencyName Director"
            val submissionId       = cert["submission_id"] ?: cert["id"] ?: "—"
            val cx                 = PAGE_WIDTH / 2f

            val bandColorHex = when (band) {
                "STRONG"              -> "#10B981"
                "SATISFACTORY"        -> "#0284C7"
                "NEEDS_REINFORCEMENT" -> "#F59E0B"
                else                  -> "#EF4444"
            }
            val bandLabel = when (band) {
                "STRONG"              -> "STRONG — ከፍተኛ ብቃት"
                "SATISFACTORY"        -> "SATISFACTORY — አጥጋቢ"
                "NEEDS_REINFORCEMENT" -> "NEEDS REINFORCEMENT — ተጨማሪ ድጋፍ"
                else                  -> band
            }

            // ── 1. Background: certificate_template.png ──────────────────────
            var templateDrawn = false
            if (context != null) {
                try {
                    val resId = context.resources.getIdentifier("certificate_template", "drawable", context.packageName)
                    if (resId != 0) {
                        val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                        if (bmp != null) {
                            canvas.drawBitmap(bmp, null, android.graphics.Rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT), null)
                            bmp.recycle()
                            templateDrawn = true
                        }
                    }
                } catch (_: Exception) {}
            }
            if (!templateDrawn) {
                // Fallback: light blue background + concentric circles
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), fillPaint("#EBF5FB"))
                val gp = strokePaint("#0284C7", 0.45f).apply { alpha = 26 }
                for (r in 40..500 step 16) canvas.drawCircle(cx, PAGE_HEIGHT / 2f, r.toFloat(), gp)
                canvas.drawRect(RectF(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f), strokePaint("#004C99", 2.2f))
                canvas.drawRect(RectF(24f, 24f, PAGE_WIDTH - 24f, PAGE_HEIGHT - 24f), strokePaint("#026CB6", 0.8f))
            }

            // ── 2. Header ────────────────────────────────────────────────────
            // Ethiopian flag (top-left)
            val fL = 36f; val fT = 34f; val fR = 76f; val fB = 58f
            val bh = (fB - fT) / 3f
            canvas.drawRect(RectF(fL, fT, fR, fT + bh),      fillPaint("#078930"))
            canvas.drawRect(RectF(fL, fT + bh, fR, fT + bh * 2f), fillPaint("#FCDD09"))
            canvas.drawRect(RectF(fL, fT + bh * 2f, fR, fB), fillPaint("#DA121A"))
            canvas.drawCircle(56f, 46f, 5.5f, fillPaint("#0F47AF"))
            canvas.drawText("★", 56f, 49f, paint("#FCDD09", 7f, align = Paint.Align.CENTER))

            // Center title
            canvas.drawText(
                "CERTIFICATE OF COMPETENCY",
                cx, 44f, paint("#004C99", 15f, bold = true, align = Paint.Align.CENTER)
            )
            canvas.drawText(
                agencyName.uppercase(),
                cx, 56f, paint("#0284C7", 7.5f, bold = true, align = Paint.Align.CENTER)
            )

            // Doc number (top-right)
            canvas.drawText("No. $docNo", PAGE_WIDTH - 100f, 44f, paint("#0B1F3A", 8.5f, bold = true))

            // Thin rule under header
            canvas.drawLine(36f, 64f, PAGE_WIDTH - 36f, 64f, strokePaint("#0284C7", 0.8f).apply { alpha = 120 })

            // ── 3. Recipient block ────────────────────────────────────────────
            canvas.drawText(
                "This certifies that",
                cx, 84f, paint("#0284C7", 9f, align = Paint.Align.CENTER)
            )
            canvas.drawText(
                fullName,
                cx, 112f, paint("#003D7A", 20f, bold = true, align = Paint.Align.CENTER)
            )
            canvas.drawLine(cx - 90f, 120f, cx + 90f, 120f, strokePaint("#0284C7", 0.7f).apply { alpha = 100 })
            canvas.drawText(
                "has demonstrated competency in Civil Registration and Vital Statistics (Proclamation No. 1370/2024)",
                cx, 134f, paint("#003D7A", 8.5f, align = Paint.Align.CENTER)
            )

            // ── 4. Key Info Grid (2 columns × 2 rows — 4 cells) ─────────────
            fun infoCell(label: String, value: String, x: Float, y: Float, w: Float) {
                val cellRect = RectF(x, y, x + w, y + 32f)
                canvas.drawRoundRect(cellRect, 5f, 5f, fillPaint("#003D7A").apply { alpha = 14 })
                canvas.drawText(label, x + 6f, y + 12f, paint("#0284C7", 6.5f, bold = true))
                val displayValue = if (value.length > 30) value.take(28) + "…" else value
                canvas.drawText(displayValue, x + 6f, y + 25f, paint("#003D7A", 9f, bold = true))
            }

            val col1 = 36f
            val col2 = 36f + (PAGE_WIDTH - 72f) / 2f + 4f
            val cellW = (PAGE_WIDTH - 72f - 8f) / 2f
            val row1Y = 148f
            val row2Y = 186f

            infoCell("Place of Registration", facility,       col1, row1Y, cellW)
            infoCell("Role",            role,           col2, row1Y, cellW)
            infoCell("Assessment Type", testTypeLabel,  col1, row2Y, cellW)
            infoCell("Date Issued",     dateOfIssuance, col2, row2Y, cellW)

            // ── 5. Score Band Highlight ───────────────────────────────────────
            val bandBoxY = 228f
            val bandBoxH = 56f
            val bandBoxRect = RectF(36f, bandBoxY, PAGE_WIDTH - 36f, bandBoxY + bandBoxH)
            canvas.drawRoundRect(bandBoxRect, 8f, 8f, fillPaint(bandColorHex).apply { alpha = 22 })
            canvas.drawRoundRect(bandBoxRect, 8f, 8f, strokePaint(bandColorHex, 1f).apply { alpha = 100 })

            // Score / Percentage / Band — 3 segments
            val seg = (PAGE_WIDTH - 72f) / 3f
            // Score
            canvas.drawText("$totalScore / $maxScore pts", col1 + seg * 0f + seg / 2f, bandBoxY + 26f,
                paint("#003D7A", 16f, bold = true, align = Paint.Align.CENTER))
            canvas.drawText("Score Earned", col1 + seg * 0f + seg / 2f, bandBoxY + 40f,
                paint("#0284C7", 7.5f, align = Paint.Align.CENTER))
            // Dividers
            canvas.drawLine(col1 + seg, bandBoxY + 10f, col1 + seg, bandBoxY + bandBoxH - 10f,
                strokePaint("#0284C7", 0.6f).apply { alpha = 80 })
            canvas.drawLine(col1 + seg * 2f, bandBoxY + 10f, col1 + seg * 2f, bandBoxY + bandBoxH - 10f,
                strokePaint("#0284C7", 0.6f).apply { alpha = 80 })
            // Percentage
            canvas.drawText("$scorePct%", col1 + seg + seg / 2f, bandBoxY + 26f,
                paint(bandColorHex, 16f, bold = true, align = Paint.Align.CENTER))
            canvas.drawText("Score Percentage", col1 + seg + seg / 2f, bandBoxY + 40f,
                paint("#0284C7", 7.5f, align = Paint.Align.CENTER))
            // Band
            canvas.drawText(bandLabel, col1 + seg * 2f + seg / 2f, bandBoxY + 26f,
                paint(bandColorHex, 9.5f, bold = true, align = Paint.Align.CENTER))
            canvas.drawText("Performance Band", col1 + seg * 2f + seg / 2f, bandBoxY + 40f,
                paint("#0284C7", 7.5f, align = Paint.Align.CENTER))

            // ── 6. Issuing Authority row (single full-width cell) ─────────────
            val adminY = 298f
            val adminRect = RectF(36f, adminY, PAGE_WIDTH - 36f, adminY + 28f)
            canvas.drawRoundRect(adminRect, 5f, 5f, fillPaint("#003D7A").apply { alpha = 10 })
            canvas.drawText("Issuing Authority", 42f, adminY + 11f, paint("#0284C7", 6.5f, bold = true))
            canvas.drawText("$agencyName  ·  ${cert["proclamation"] as? String ?: "Proclamation No. 1370/2024"}", 42f, adminY + 22f, paint("#003D7A", 8.5f, bold = true))

            // ── 7. QR Code placeholder ────────────────────────────────────────
            val qrL = 36f; val qrT = 342f; val qrSz = 80f
            canvas.drawRect(RectF(qrL, qrT, qrL + qrSz, qrT + qrSz), fillPaint("#FFFFFF"))
            canvas.drawRect(RectF(qrL, qrT, qrL + qrSz, qrT + qrSz), strokePaint("#94A3B8", 0.8f))
            fun drawFinder(fx: Float, fy: Float) {
                canvas.drawRect(fx, fy, fx + 22f, fy + 22f, fillPaint("#000000"))
                canvas.drawRect(fx + 3f, fy + 3f, fx + 19f, fy + 19f, fillPaint("#FFFFFF"))
                canvas.drawRect(fx + 6f, fy + 6f, fx + 16f, fy + 16f, fillPaint("#000000"))
            }
            drawFinder(qrL + 5f, qrT + 5f)
            drawFinder(qrL + qrSz - 27f, qrT + 5f)
            drawFinder(qrL + 5f, qrT + qrSz - 27f)
            val qPaint = fillPaint("#000000")
            for (r in 0..10) for (c in 0..10) {
                val px = qrL + 6f + c * 6f; val py = qrT + 6f + r * 6f
                val isC = (r < 4 && (c < 4 || c > 6)) || (r > 6 && c < 4)
                if (!isC && (r * 5 + c * 11 + r * c) % 3 == 0)
                    canvas.drawRect(px, py, px + 4.5f, py + 4.5f, qPaint)
            }
            canvas.drawText("Scan to verify", qrL + qrSz / 2f, qrT + qrSz + 10f,
                paint("#0284C7", 6.5f, align = Paint.Align.CENTER))

            // ── 8. Signature block ────────────────────────────────────────────
            val sigY = 440f
            val sigPath = android.graphics.Path().apply {
                moveTo(180f, sigY)
                cubicTo(190f, sigY - 16f, 200f, sigY - 12f, 210f, sigY - 4f)
                cubicTo(218f, sigY + 5f, 228f, sigY - 8f, 240f, sigY - 2f)
                cubicTo(252f, sigY + 4f, 268f, sigY - 10f, 290f, sigY - 4f)
                moveTo(182f, sigY + 7f)
                cubicTo(220f, sigY + 5f, 260f, sigY + 9f, 305f, sigY + 5f)
            }
            canvas.drawPath(sigPath, strokePaint("#0C4A6E", 1.6f))
            canvas.drawText(issuingOfficer, 182f, sigY - 4f, paint("#0C4A6E", 8f, bold = true))
            canvas.drawLine(180f, sigY + 12f, 320f, sigY + 12f, strokePaint("#0284C7", 0.7f))
            canvas.drawText("Authorized Signature", 180f, sigY + 22f, paint("#0284C7", 6.5f))
            canvas.drawText("Place: $place", 180f, sigY + 32f, paint("#0284C7", 6f))

            // ── 9. Official Seal ──────────────────────────────────────────────
            val sealCx = PAGE_WIDTH - 85f
            val sealCy = sigY + 8f
            canvas.save()
            canvas.rotate(-6f, sealCx, sealCy)
            canvas.drawCircle(sealCx, sealCy, 44f, strokePaint("#003D7A", 2.2f).apply { alpha = 210 })
            canvas.drawCircle(sealCx, sealCy, 40f, strokePaint("#003D7A", 0.9f).apply { alpha = 180 })
            for (deg in 0 until 360 step 12) {
                val rad = Math.toRadians(deg.toDouble())
                val dx = sealCx + (Math.cos(rad) * 36f).toFloat()
                val dy = sealCy + (Math.sin(rad) * 36f).toFloat()
                canvas.drawCircle(dx, dy, 0.7f, strokePaint("#003D7A", 0.8f).apply { alpha = 170 })
            }
            canvas.drawText(agencyName.uppercase(), sealCx, sealCy - 26f,
                paint("#003D7A", 5.5f, bold = true, align = Paint.Align.CENTER).apply { alpha = 220 })
            canvas.drawText("OFFICIAL SEAL · ማህተም", sealCx, sealCy + 33f,
                paint("#003D7A", 5.5f, bold = true, align = Paint.Align.CENTER).apply { alpha = 220 })
            // Scales emblem
            canvas.drawLine(sealCx - 18f, sealCy - 4f, sealCx + 18f, sealCy - 4f, strokePaint("#003D7A", 1.8f).apply { alpha = 210 })
            canvas.drawLine(sealCx, sealCy - 14f, sealCx, sealCy + 14f, strokePaint("#003D7A", 2.2f).apply { alpha = 210 })
            canvas.drawRect(RectF(sealCx - 9f, sealCy + 12f, sealCx + 9f, sealCy + 16f), fillPaint("#003D7A").apply { alpha = 190 })
            val panL2 = android.graphics.Path().apply { moveTo(sealCx - 22f, sealCy + 7f); quadTo(sealCx - 14f, sealCy + 13f, sealCx - 8f, sealCy + 7f); close() }
            canvas.drawPath(panL2, fillPaint("#003D7A").apply { alpha = 190 })
            val panR2 = android.graphics.Path().apply { moveTo(sealCx + 8f, sealCy + 7f); quadTo(sealCx + 14f, sealCy + 13f, sealCx + 22f, sealCy + 7f); close() }
            canvas.drawPath(panR2, fillPaint("#003D7A").apply { alpha = 190 })
            canvas.restore()

            document.finishPage(page)
        }

    }
}

