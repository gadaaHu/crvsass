package com.qcafr.gatherapp.ui.screens.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.SubmissionResponse
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    viewModel: QuestionnaireViewModel,
    onJumpToQuestion: (Int) -> Unit,
    onSubmitSuccess: (SubmissionResponse) -> Unit,
    onBackToQuestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAmharic = uiState.isAmharic
    val currentUser by viewModel.currentUser.collectAsState()
    val canViewResult = currentUser?.role == "ADMIN" || currentUser?.role == "USER_MANAGER"
    val questions = uiState.flatQuestions
    val answers = uiState.answers

    val answeredCount = questions.count { pair ->
        val ans = answers[pair.second.id]
        ans != null && (
            !ans.selectedOptionKey.isNullOrBlank() ||
            ans.ratingValue != null ||
            !ans.answerText.isNullOrBlank()
        )
    }
    val progress = if (questions.isNotEmpty()) answeredCount.toFloat() / questions.size.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isAmharic) "መልሶችን ይከልሱ (Review Answers)" else "Review Answers",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextWhite
        )
        Text(
            text = if (isAmharic)
                "ማንኛውንም ጥያቄ ጠቅ በማድረግ መልስዎን ማስተካከል ይችላሉ"
            else
                "Click any question to edit your answer",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary pills (FlowRow ensures pills wrap naturally instead of vertically compressing)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ተሳታፊ፦ ${uiState.participant?.participantCode ?: "N/A"}",
                    fontSize = 12.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.testType == "POST") AccentEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (uiState.testType == "POST") AccentEmerald.copy(alpha = 0.3f) else AccentAmber.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (uiState.testType == "POST")
                        (if (isAmharic) "ድህረ-ሙከራ (POST)" else "Post-Test (POST)")
                    else
                        (if (isAmharic) "ቅድመ-ሙከራ (PRE)" else "Pre-Test (PRE)"),
                    fontSize = 12.sp,
                    color = if (uiState.testType == "POST") AccentEmerald else AccentAmber,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryIndigo.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAmharic) "$answeredCount / ${questions.size} ተመልሷል" else "$answeredCount / ${questions.size} answered",
                    fontSize = 12.sp,
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (answeredCount == questions.size && questions.isNotEmpty()) AccentEmerald else PrimaryIndigo,
            trackColor = BorderSubtle
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Review list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(questions) { index, pair ->
                val q = pair.second
                val ans = answers[q.id]
                val hasAns = ans != null && (
                    !ans.selectedOptionKey.isNullOrBlank() ||
                    ans.ratingValue != null ||
                    !ans.answerText.isNullOrBlank()
                )

                val answerDisplay = when {
                    !ans?.selectedOptionKey.isNullOrBlank() ->
                        if (isAmharic) "አማራጭ: ${ans?.selectedOptionKey}" else "Choice: ${ans?.selectedOptionKey}"
                    ans?.ratingValue != null ->
                        if (isAmharic) "ደረጃ: ${ans.ratingValue} / 5" else "Rating: ${ans.ratingValue} / 5"
                    !ans?.answerText.isNullOrBlank() ->
                        if (isAmharic) "ጽሑፍ: ${ans.answerText?.take(25)}..." else "Text: ${ans.answerText?.take(25)}..."
                    else ->
                        if (isAmharic) "መልስ አልተሰጠም (Skipped)" else "Skipped"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { onJumpToQuestion(index) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q${q.questionNumber}",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo,
                        fontSize = 14.sp,
                        modifier = Modifier.width(36.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAmharic) q.promptAm.ifBlank { q.promptEn } else q.promptEn.ifBlank { q.promptAm },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = answerDisplay,
                            fontSize = 12.sp,
                            color = if (hasAns) AccentEmerald else AccentDanger,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = if (isAmharic) "አስተካክል →" else "Edit →",
                        fontSize = 11.5.sp,
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Final Submit Button
        Button(
            onClick = {
                viewModel.submitAssessment { response ->
                    onSubmitSuccess(response)
                }
            },
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentEmerald,
                contentColor = Color.White,
                disabledContainerColor = AccentEmerald.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            )
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (canViewResult) "ውጤት እየተሰላ ነው..." else "ምላሾች እየተላኩ ነው...",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isSubmitting)
                        (if (canViewResult)
                            (if (isAmharic) "ውጤት እየተሰላ ነው..." else "Calculating score...")
                        else
                            (if (isAmharic) "ምላሾች እየተላኩ ነው..." else "Submitting..."))
                    else
                        (if (canViewResult)
                            (if (isAmharic) "መልሶችን ላክና ውጤት ተመልከት (Submit & View Score)" else "Submit & View Score")
                        else
                            (if (isAmharic) "መልሶችን ላክ (Submit Assessment)" else "Submit Assessment")),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onBackToQuestions,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Text(
                text = if (isAmharic) "ወደ ጥያቄዎቹ ተመለስ (Back)" else "Back to Questions",
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

