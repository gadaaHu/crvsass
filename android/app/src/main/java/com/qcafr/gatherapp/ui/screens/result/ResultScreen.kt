package com.qcafr.gatherapp.ui.screens.result

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.qcafr.gatherapp.data.model.SubmissionResponse
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel
import com.qcafr.gatherapp.util.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResultScreen(
    viewModel: QuestionnaireViewModel,
    response: SubmissionResponse,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canViewResult = currentUser?.role == "ADMIN" || currentUser?.role == "USER_MANAGER"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        if (canViewResult) {
            // ── ADMIN & MANAGER VIEW: Full Result, Scores & PDF Export ──
            // Circular Score Badge
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                PrimaryIndigo.copy(alpha = 0.35f),
                                AccentEmerald.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(3.dp, PrimaryIndigo, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${response.totalScore.toInt()}",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        lineHeight = 48.sp
                    )
                    Text(
                        text = "/ ${response.maxScore.toInt()} pts",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "መመዘኛው ተጠናቋል! (Assessment Completed)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Performance Classification Band
            val bandColor = when (response.performanceBand) {
                "STRONG" -> AccentEmerald
                "SATISFACTORY" -> AccentCyan
                "NEEDS_REINFORCEMENT" -> AccentAmber
                else -> AccentDanger
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bandColor.copy(alpha = 0.15f))
                    .border(1.dp, bandColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val bandLabel = when (response.performanceBand) {
                    "STRONG" -> "⭐ STRONG (ጠንካራ) — ከፍተኛ ብቃት"
                    "SATISFACTORY" -> "✅ SATISFACTORY (አጥጋቢ)"
                    "NEEDS_REINFORCEMENT" -> "⚠ NEEDS REINFORCEMENT (የማጠናከሪያ ድጋፍ)"
                    else -> response.performanceBand
                }
                Text(
                    text = bandLabel,
                    color = bandColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "የውጤት መቶኛ፦ ${response.scorePercentage.toInt()}%",
                fontSize = 15.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Breakdown Card (from actual answers)
            if (!response.answers.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "የውጤት ዝርዝር (Answer Summary)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val correct = response.answers.count { it.isCorrect }
                        val total = response.answers.size

                        ResultStatRow("ትክክለኛ መልሶች (Correct)", "$correct / $total")
                        Spacer(modifier = Modifier.height(10.dp))
                        ResultStatRow("ጠቅላላ ነጥብ (Total Score)", "${response.totalScore.toInt()} / ${response.maxScore.toInt()}")
                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini answer grid
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Per-Question Results:",
                            fontSize = 12.sp,
                            color = TextDim,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        response.answers.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { ans ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (ans.isCorrect) AccentEmerald.copy(alpha = 0.15f)
                                                else AccentDanger.copy(alpha = 0.12f)
                                            )
                                            .border(
                                                1.dp,
                                                if (ans.isCorrect) AccentEmerald.copy(alpha = 0.5f)
                                                else AccentDanger.copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Q${ans.questionNumber}",
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (ans.isCorrect) "✓" else "✗",
                                                fontSize = 16.sp,
                                                color = if (ans.isCorrect) AccentEmerald else AccentDanger
                                            )
                                        }
                                    }
                                }
                                repeat(4 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "የውጤት ዝርዝር በክፍል (Section Scores)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        ResultStatRow("ክፍል ሀ – አዋጅ ቁጥር 1370/2017 (ሕግ)", "8 / 8 ነጥቦች")
                        Spacer(modifier = Modifier.height(10.dp))
                        ResultStatRow("ክፍል ለ – የሲአርቪኤስ ዲጂታል ሥርዓት", "7 / 8 ነጥቦች")
                        Spacer(modifier = Modifier.height(10.dp))
                        ResultStatRow("ክፍል ሐ – ተግባራዊ ሁኔታዎች (Scenarios)", "6 / 8 ነጥቦች")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── PDF Download Button (Admin & Manager Only) ──────────────────────
            OutlinedButton(
                onClick = {
                    if (!isExporting) {
                        isExporting = true
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    PdfExporter.exportResultPdf(
                                        context = context,
                                        response = response,
                                        participant = uiState.participant,
                                        agencyName = uiState.regionConfig.agencyName,
                                        place = uiState.regionConfig.place
                                    )
                                }
                                Toast.makeText(context, "PDF saved: ${file.name}", Toast.LENGTH_LONG).show()

                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "PDF saved to Documents. Install a PDF viewer to open.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExporting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentEmerald)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AccentEmerald,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generating PDF…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ውጤት ሪፖርት ዳውንሎድ (Download PDF Report)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // ── EXAMINEE VIEW: Secure Completion & Submission Confirmation ──
            // Notice: Results and scores are seen ONLY by Admin and Manager
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald.copy(alpha = 0.15f))
                    .border(3.dp, AccentEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Submission Success",
                    tint = AccentEmerald,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ምዘናው በተሳካ ሁኔታ ተጠናቋል!\n(Assessment Submitted)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Restricted Results Notice Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.12f))
                    .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted",
                            tint = AccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "የውጤት ገደብ (Restricted Result)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "የፈተናው ውጤት የሚታየው ለአስተዳዳሪ (Admin) እና ለሥራ አስኪያጅ (Manager) ብቻ ነው።",
                            fontSize = 12.5.sp,
                            color = AccentAmber,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exam scores and answer breakdown are accessible only to authorized administrators and managers.",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Submission Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "የተመዘገበው ማረጋገጫ (Submission Record)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ResultStatRow("የተሳታፊ መለያ (Participant Code)", response.participantCode.ifBlank { currentUser?.participantCode ?: "N/A" })
                    Spacer(modifier = Modifier.height(10.dp))
                    ResultStatRow("የፈተና ዓይነት (Assessment Type)", if (response.testType == "POST") "ድህረ-ምዘና (POST-TEST)" else "ቅድመ-ምዘና (PRE-TEST)")
                    Spacer(modifier = Modifier.height(10.dp))
                    ResultStatRow("ሁኔታ (Submission Status)", "በስኬት ተመዝግቧል ✓")
                    Spacer(modifier = Modifier.height(10.dp))
                    ResultStatRow("የተመለሱ ጥያቄዎች (Questions Answered)", "${response.answers?.size ?: uiState.flatQuestions.size} ጥያቄዎች")
                }
            }

            // Certificate Delivery Notice Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentEmerald.copy(alpha = 0.10f))
                    .border(1.dp, AccentEmerald.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "የምስክር ወረቀት አሰጣጥ (Certificate Delivery)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AccentEmerald
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "የፈተናው ውጤት 50% እና ከዚያ በላይ ከሆነ፣ ሥራ አስኪያጁ ሲያጸድቀው ይፋዊው ምስክር ወረቀት በቀጥታ ወደ እርስዎ ፕሮፋይል ይላካል (Certificate will be sent to your profile once approved by manager).",
                            fontSize = 11.5.sp,
                            color = TextWhite.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
        ) {
            Text(
                text = "ተጠናቋል / ወደ መነሻ ተመለስ (Back to Home)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = androidx.compose.ui.graphics.Color.White
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun ResultStatRow(label: String, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextMuted, modifier = Modifier.weight(1f))
        Text(text = score, fontSize = 13.5.sp, color = TextWhite, fontWeight = FontWeight.Bold)
    }
}
