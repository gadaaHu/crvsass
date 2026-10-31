package com.qcafr.gatherapp.ui.screens.questionnaire

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.QuestionType
import com.qcafr.gatherapp.ui.components.*
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionnaireScreen(
    viewModel: QuestionnaireViewModel,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAmharic = uiState.isAmharic
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Text to Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("am", "ET")
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val questions = uiState.flatQuestions
    if (questions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BgPrimary),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryIndigo)
        }
        return
    }

    // Horizontal Pager State enabling full touch swipe left/right gestures
    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex.coerceIn(0, questions.size - 1),
        pageCount = { questions.size }
    )

    // Sync ViewModel index when user swipes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentIndex) {
            viewModel.jumpToQuestion(pagerState.currentPage)
        }
    }

    // Sync Pager when index changes programmatically
    LaunchedEffect(uiState.currentIndex) {
        if (pagerState.currentPage != uiState.currentIndex) {
            pagerState.animateScrollToPage(uiState.currentIndex)
        }
    }

    val currentIndex = pagerState.currentPage
    val isLast = currentIndex == questions.size - 1
    val progress = (currentIndex + 1).toFloat() / questions.size.toFloat()

    // Question number scroll state
    val numbersListState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        numbersListState.animateScrollToItem(currentIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Top Progress Line with clean status indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAmharic) "ጥያቄ ${currentIndex + 1} / ${questions.size}" else "Question ${currentIndex + 1} / ${questions.size}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryIndigo
                )
                Text(
                    text = if (isAmharic) "${((progress) * 100).toInt()}% ተጠናቋል · 👈 Swipe 👉" else "${((progress) * 100).toInt()}% complete · 👈 Swipe 👉",
                    fontSize = 11.5.sp,
                    color = TextDim,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = PrimaryIndigo,
                trackColor = BorderSubtle
            )
        }

        // Quick Jump Question Numbers Bar (Horizontal Scroll)
        LazyRow(
            state = numbersListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(questions) { idx, item ->
                val qId = item.second.id
                val answer = uiState.answers[qId]
                val isAnswered = answer != null && (
                    !answer.selectedOptionKey.isNullOrBlank() ||
                    answer.ratingValue != null ||
                    !answer.answerText.isNullOrBlank()
                )
                val isCurrent = idx == currentIndex

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isCurrent -> PrimaryIndigo
                        isAnswered -> AccentEmerald.copy(alpha = 0.18f)
                        else -> BgCard
                    },
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = when {
                            isCurrent -> PrimaryIndigo
                            isAnswered -> AccentEmerald.copy(alpha = 0.5f)
                            else -> BorderSubtle
                        }
                    ),
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(idx)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                            color = when {
                                isCurrent -> Color.White
                                isAnswered -> AccentEmerald
                                else -> TextMuted
                            }
                        )
                        if (isAnswered && !isCurrent) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = AccentEmerald,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Pager enabling full touch swipe left/right gestures
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val pagePair = questions[page]
            val sectionTitle = pagePair.first
            val currentQuestion = pagePair.second
            val currentAnswer = uiState.answers[currentQuestion.id]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Question Header
                QuestionHeaderCard(
                    question = currentQuestion,
                    currentIndex = page,
                    totalQuestions = questions.size,
                    sectionTitle = sectionTitle,
                    isAmharic = isAmharic,
                    onSpeakClick = {
                        val promptToRead = if (isAmharic) currentQuestion.promptAm else currentQuestion.promptEn
                        tts?.speak(promptToRead, TextToSpeech.QUEUE_FLUSH, null, "Q_PROMPT")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Answer Area
                when (currentQuestion.type) {
                    QuestionType.SINGLE_CHOICE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            currentQuestion.options?.forEach { option ->
                                val isSelected = currentAnswer?.selectedOptionKey == option.optionKey
                                OptionItem(
                                    option = option,
                                    isSelected = isSelected,
                                    isAmharic = isAmharic,
                                    onSelect = {
                                        viewModel.selectOption(
                                            questionId = currentQuestion.id,
                                            questionNumber = currentQuestion.questionNumber,
                                            optionKey = option.optionKey
                                        )
                                    }
                                )
                            }
                        }
                    }
                    QuestionType.RATING_SCALE -> {
                        RatingScale(
                            selectedRating = currentAnswer?.ratingValue,
                            onRatingSelected = { rating ->
                                viewModel.setRating(
                                    questionId = currentQuestion.id,
                                    questionNumber = currentQuestion.questionNumber,
                                    rating = rating
                                )
                            }
                        )
                    }
                    QuestionType.TEXT_OPEN -> {
                        ScenarioInput(
                            value = currentAnswer?.answerText ?: "",
                            onValueChange = { text ->
                                viewModel.setTextAnswer(
                                    questionId = currentQuestion.id,
                                    questionNumber = currentQuestion.questionNumber,
                                    text = text
                                )
                            }
                        )
                    }
                    QuestionType.DEMOGRAPHIC -> {
                        // Handled in demographic screen
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Current question answer state for bottom bar
        val activeQuestion = questions[currentIndex].second
        val activeAnswer = uiState.answers[activeQuestion.id]
        val isAnswered = activeAnswer != null && (
            !activeAnswer.selectedOptionKey.isNullOrBlank() ||
            activeAnswer.ratingValue != null ||
            !activeAnswer.answerText.isNullOrBlank()
        )

        // Bottom Action Bar (Protected with navigationBarsPadding)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = BgSecondary,
            border = BorderStroke(1.dp, BorderSubtle),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                OutlinedButton(
                    onClick = {
                        if (currentIndex > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentIndex - 1)
                            }
                        }
                    },
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite,
                        disabledContentColor = TextDim
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (currentIndex > 0) BorderSubtle else BorderSubtle.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isAmharic) "ወደ ኋላ" else "Previous", fontWeight = FontWeight.SemiBold)
                }

                // Quick Status Indicator
                if (isAnswered) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Answered",
                            tint = AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAmharic) "ተመልሷል" else "Answered",
                            color = AccentEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = if (isAmharic) "ምላሽ አልተመረጠም" else "Not answered",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                }

                // Next or Review Button
                Button(
                    onClick = {
                        if (isLast) {
                            onNavigateToReview()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentIndex + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLast) AccentEmerald else PrimaryIndigo
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isLast) (if (isAmharic) "ይከልሱ (Review)" else "Review") else (if (isAmharic) "ቀጣይ" else "Next"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
