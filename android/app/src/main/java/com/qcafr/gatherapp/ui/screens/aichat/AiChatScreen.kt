package com.qcafr.gatherapp.ui.screens.aichat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: QuestionnaireViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAmharic = uiState.isAmharic
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    // Initial welcome greeting
    val welcomeMessage = remember(isAmharic) {
        if (isAmharic) {
            "👋 **ሰላም! እኔ የ CRVS ስልጠና AI አማካሪ ነኝ።**\n\n" +
            "ድህረ-ፈተናዎን በማጠናቀቅዎ እንኳን ደስ አለዎት! ስለ ወሳኝ ኩነቶች ምዝገባ አዋጅ ቁጥር 1370/2024፣ የልደት፣ ሞት፣ ጋብቻ፣ ፍቺ አሰራሮች ወይም ስለ ሲስተሙ አጠቃቀም ማንኛውንም ጥያቄ ሊጠይቁኝ ይችላሉ።"
        } else {
            "👋 **Hello! I am your CRVS Training AI Tutor.**\n\n" +
            "Congratulations on taking your Post-Test! Feel free to ask any question regarding Ethiopian Proclamation 1370/2024, Birth, Death, Marriage, Divorce registration guidelines, or digital software workflows."
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(text = welcomeMessage, isUser = false)
        )
    }

    // Quick prompt chips
    val quickPrompts = remember(isAmharic) {
        if (isAmharic) listOf(
            "👶 የልደት ምዝገባ ጊዜ ገደብ?",
            "⚰️ የሞት ምዝገባ አስፈላጊ ሰነዶች?",
            "📜 አዋጅ ቁጥር 1370/2024 ዋና ዋና ነጥቦች?",
            "💍 የጋብቻ ምዝገባ ህጋዊ እድሜ?",
            "⚖️ የፍቺ ምዝገባ ቅድመ ሁኔታ?",
            "💻 ከመስመር ውጭ (Offline) አሰራር?"
        ) else listOf(
            "👶 Birth registration deadline?",
            "⚰️ Death registration documents?",
            "📜 Proclamation 1370/2024 summary?",
            "💍 Marriage legal age requirements?",
            "⚖️ Divorce registration criteria?",
            "💻 Offline system sync?"
        )
    }

    fun sendMessage(textToSend: String) {
        val query = textToSend.trim()
        if (query.isBlank() || isThinking) return

        messages.add(ChatMessage(text = query, isUser = true))
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val reply = viewModel.askAiTutor(query)
            messages.add(ChatMessage(text = reply, isUser = false))
            isThinking = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2DD4BF), Color(0xFF0F766E))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isAmharic) "የ CRVS AI ረዳት" else "CRVS AI Tutor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextWhite
                            )
                            Text(
                                text = if (isAmharic) "የስልጠና እና ህጎች አማካሪ" else "Curriculum & Legal Assistant",
                                fontSize = 11.sp,
                                color = Color(0xFF2DD4BF)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(ChatMessage(text = welcomeMessage, isUser = false))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSecondary)
            )
        },
        bottomBar = {
            // Message input bar with keyboard padding
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = BgSecondary,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Quick suggestion chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.clickable { sendMessage(prompt) }
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Input Field & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (isAmharic) "ስለ ስልጠናው ማንኛውንም ጥያቄ ይጠይቁ..." else "Ask any question about CRVS training...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 110.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2DD4BF),
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedContainerColor = BgCard,
                                unfocusedContainerColor = BgCard
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) }),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { sendMessage(inputText) },
                            enabled = inputText.isNotBlank() && !isThinking,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !isThinking) Color(0xFF2DD4BF)
                                    else Color(0xFF334155)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() && !isThinking) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = BgPrimary,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg, isAmharic = isAmharic)
            }

            if (isThinking) {
                item {
                    ThinkingIndicator(isAmharic = isAmharic)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isAmharic: Boolean) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F766E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) PrimaryIndigo else BgCard,
            border = BorderStroke(
                1.dp,
                if (message.isUser) PrimaryIndigo.copy(alpha = 0.5f) else Color(0xFF2DD4BF).copy(alpha = 0.25f)
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    fontSize = 13.5.sp,
                    color = TextWhite,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = if (message.isUser) Color.White.copy(alpha = 0.65f) else TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun ThinkingIndicator(isAmharic: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F766E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = Color(0xFF2DD4BF),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BgCard,
            border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.25f)),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFF2DD4BF),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAmharic) "AI እያሰበ ነው..." else "AI is thinking...",
                    fontSize = 12.sp,
                    color = Color(0xFF2DD4BF)
                )
            }
        }
    }
}
