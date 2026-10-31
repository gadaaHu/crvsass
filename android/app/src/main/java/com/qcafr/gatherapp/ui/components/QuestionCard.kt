package com.qcafr.gatherapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.Question
import com.qcafr.gatherapp.ui.theme.*

@Composable
fun QuestionHeaderCard(
    question: Question,
    currentIndex: Int,
    totalQuestions: Int,
    sectionTitle: String,
    onSpeakClick: () -> Unit,
    isAmharic: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.appColors.isDark

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = sectionTitle,
                        color = if (isDark) Color(0xFFA5B4FC) else PrimaryIndigo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AccentEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${question.points} Pt${if (question.points > 1) "s" else ""}",
                        color = AccentEmerald,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Question counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0x26FFFFFF) else Color(0x140F172A)
                ) {
                    Text(
                        text = "Q${question.questionNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = PrimaryIndigo,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAmharic)
                        "ጥያቄ ${currentIndex + 1} ከ $totalQuestions"
                    else
                        "Question ${currentIndex + 1} of $totalQuestions",
                    color = TextDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Prompt (Amharic or English based on selection)
            Text(
                text = if (isAmharic) question.promptAm else question.promptEn,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
                lineHeight = 27.sp
            )

            // Secondary Translation Prompt (always shown if different)
            val secondaryPrompt = if (isAmharic) question.promptEn else question.promptAm
            if (secondaryPrompt.isNotBlank() && secondaryPrompt != (if (isAmharic) question.promptAm else question.promptEn)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = secondaryPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speech button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0x26FFFFFF) else Color(0x120F172A),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.clickable { onSpeakClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak Question",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAmharic) "አድምጥ / Listen" else "Listen / አድምት",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                }
            }

            // Rubric guideline for scenario questions
            if (!question.rubricAm.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentEmerald.copy(alpha = 0.08f))
                        .border(1.dp, AccentEmerald.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "የነጥብ አሰጣጥ መመሪያ (Rubric):\n${question.rubricAm}",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
