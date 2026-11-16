package com.qcafr.gatherapp.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.R
import com.qcafr.gatherapp.data.preferences.RegionConfig
import com.qcafr.gatherapp.data.preferences.RegionPreferenceManager
import com.qcafr.gatherapp.ui.components.CurvedFloatingNavBar
import com.qcafr.gatherapp.ui.components.CurvedNavItem
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

private fun getRegionAmharicName(regionName: String): String {
    return when (regionName.trim()) {
        "Dire Dawa" -> "የድሬዳዋ አስተዳደር"
        "Addis Ababa" -> "የአዲስ አበባ አስተዳደር"
        "Afar" -> "የአፋር ክልል"
        "Amhara" -> "የአማራ ክልል"
        "Benishangul-Gumuz" -> "የቤንሻንጉል ጉሙዝ ክልል"
        "Central Ethiopia" -> "የማዕከላዊ ኢትዮጵያ ክልል"
        "Gambela" -> "የጋምቤላ ክልል"
        "Harari" -> "የሐረሪ ክልል"
        "Oromia" -> "የኦሮሚያ ክልል"
        "Sidama" -> "የሲዳማ ክልል"
        "Somali" -> "የሶማሌ ክልል"
        "South Ethiopia" -> "የደቡብ ኢትዮጵያ ክልል"
        "South West Ethiopia Peoples' Region" -> "የደቡብ ምዕራብ ኢትዮጵያ ክልል"
        "Tigray" -> "የትግራይ ክልል"
        else -> if (regionName.isNotBlank()) "የ$regionName ክልል" else "የድሬዳዋ አስተዳደር"
    }
}

// ── Palette constants used by the new design ────────────────────────────────
private val SampleLavender       = Color(0xFFEEEEFF)   // hero card background (light mode)
private val SampleLavenderBorder = Color(0xFFD4D4F5)   // hero card border
private val SampleShieldBg       = Color(0xFFEAEAFF)   // user card avatar circle bg
private val SampleShieldTint     = Color(0xFF6366F1)   // shield icon tint (indigo)
private val SampleAdminBg        = Color(0xFFE0F2F1)   // admin pill background (teal-50)
private val SampleAdminText      = Color(0xFF00796B)   // admin pill text (teal-700)
private val SampleGreenPill      = Color(0xFFD1FAE5)   // "Dire Dawa CRVS" pill bg
private val SampleGreenText      = Color(0xFF059669)   // pill text / score label
private val SampleGreenBorder    = Color(0xFF6EE7B7)   // pill border
private val SampleScoreGreen     = Color(0xFF10B981)   // current score value


data class ModuleDetail(
    val sectionNumber: String,
    val titleEn: String,
    val titleAm: String,
    val questionsRange: String,
    val points: Int,
    val startIndex: Int,
    val accentColor: Color,
    val descriptionEn: String,
    val descriptionAm: String,
    val topicsEn: List<String>,
    val topicsAm: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QuestionnaireViewModel,
    isDarkMode: Boolean,
    isAmharic: Boolean = true,
    onToggleTheme: (Boolean) -> Unit,
    onToggleLanguage: (Boolean) -> Unit = {},
    onStartAssessment: () -> Unit,
    onStartExamDirectly: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToCertificate: (Long, String) -> Unit = { _, _ -> },
    onNavigateToAiChat: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val survey = uiState.survey
    val region = uiState.regionConfig
    val isAdminUser = currentUser?.role == "ADMIN"
    val isUserManager = currentUser?.role == "USER_MANAGER"
    val canManage = isAdminUser || isUserManager

    val isPostTestAllowed = canManage || uiState.isPostTestUnlocked
    var selectedTestType by remember(isPostTestAllowed) { mutableStateOf("PRE") } // Examinees default to PRE
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRegionSelectorDialog by remember { mutableStateOf(false) }
    var showPostTestLockedDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val submissions by viewModel.submissions.collectAsState()
    val hasCompletedPostTest = remember(submissions, currentUser, uiState.submissionResult) {
        submissions.any { sub ->
            sub.testType.equals("POST", ignoreCase = true) &&
            (currentUser == null || sub.participantCode == currentUser?.participantCode)
        } || (uiState.submissionResult?.testType == "POST")
    }
    var showAiChatLockedDialog by remember { mutableStateOf(false) }
    var activeModuleDetail by remember { mutableStateOf<ModuleDetail?>(null) }
    var showModulesSheet by remember { mutableStateOf(false) }

    if (showAiChatLockedDialog) {
        AlertDialog(
            onDismissRequest = { showAiChatLockedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color(0xFF2DD4BF),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isAmharic) "የ AI ውይይት ተቆልፏል" else "AI Chat Locked",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isAmharic) "ተፈታኞች ድህረ-ፈተናውን (Post-Test) ከወሰዱ በኋላ ብቻ በ AI አማካኝነት የስልጠና እና የአሰራር ጥያቄዎችን እንዲጠይቁ ይፈቀድላቸዋል።\n\nእባክዎ መጀመሪያ ድህረ-ፈተናውን ያጠናቁ።"
                    else "Examinees are only authorized to consult the AI Tutor after completing the Post-Test assessment.\n\nPlease complete the Post-Test first."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAiChatLockedDialog = false }) {
                    Text(if (isAmharic) "እሺ" else "OK")
                }
            }
        )
    }

    // ── Module Detail & Launch Dialog ─────────────────────────────────────
    if (activeModuleDetail != null) {
        val mod = activeModuleDetail!!
        AlertDialog(
            onDismissRequest = { activeModuleDetail = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(mod.accentColor.copy(alpha = if (isDarkMode) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = mod.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = mod.accentColor.copy(alpha = if (isDarkMode) 0.2f else 0.12f)
                    ) {
                        Text(
                            text = "${mod.sectionNumber} · ${mod.questionsRange} (${mod.points} ${if (isAmharic) "ነጥብ" else "Pts"})",
                            color = mod.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isAmharic) mod.titleAm else mod.titleEn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAmharic) mod.descriptionAm else mod.descriptionEn,
                        fontSize = 13.5.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isAmharic) "ዋና ዋና ርዕሶች፦" else "Key Competencies Covered:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                    )
                    Spacer(Modifier.height(6.dp))
                    val topics = if (isAmharic) mod.topicsAm else mod.topicsEn
                    topics.forEach { topic ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("✓ ", color = mod.accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = topic,
                                fontSize = 12.5.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetStart = mod.startIndex
                        activeModuleDetail = null
                        viewModel.startExamWithCurrentUser(selectedTestType)
                        viewModel.jumpToQuestion(targetStart)
                        onStartExamDirectly(selectedTestType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = mod.accentColor)
                ) {
                    Text(
                        text = if (isAmharic) "ይህን ክፍል ጀምር" else "Start This Section",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { activeModuleDetail = null }) {
                    Text(if (isAmharic) "ዝጋ" else "Close")
                }
            },
            containerColor = if (isDarkMode) Color(0xFF131B2E) else Color.White
        )
    }

    // ── Training Modules Bottom Sheet (Triggered from Footer Modules Tab) ──
    if (showModulesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModulesSheet = false },
            containerColor = if (isDarkMode) Color(0xFF131B2E) else Color(0xFFFFFFFF),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo.copy(alpha = if (isDarkMode) 0.25f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isAmharic) "የስልጠና ሞጁሎች" else "Training Modules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryIndigo.copy(alpha = if (isDarkMode) 0.2f else 0.1f)
                    ) {
                        Text(
                            text = if (isAmharic) "4 ሞጁሎች · 29 ነጥብ" else "4 Modules · 29 Pts",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                SectionOverviewItem(
                    sectionNumber = "Section A",
                    title = if (isAmharic) "የሲቪል እና የቤተሰብ ምዝገባ አዋጅ 1370/2016" else "Civil & Family Registration Proc. 1370/2024",
                    details = if (isAmharic) "ጥያቄ 1–8 · የሕግ ማዕቀፍ እና የምዝገባ መርሆዎች (8 ነጥብ)" else "Q 1–8 · Legal framework & principles (8 pts)",
                    accentColor = Color(0xFF6366F1),
                    isDark = isDarkMode,
                    onClick = {
                        showModulesSheet = false
                        activeModuleDetail = ModuleDetail(
                            sectionNumber = "Section A",
                            titleEn = "Civil & Family Registration (Proc. 1370/2024)",
                            titleAm = "የሲቪል እና የቤተሰብ ምዝገባ አዋጅ 1370/2016",
                            questionsRange = "Q 1 – 8",
                            points = 8,
                            startIndex = 0,
                            accentColor = Color(0xFF6366F1),
                            descriptionEn = "Legal foundations of Ethiopian civil registration, mandatory timelines, late registration rules, and vital event declarations.",
                            descriptionAm = "የኢትዮጵያ ሲቪል ምዝገባ ሕጋዊ መሠረቶች፣ የግዴታ የምዝገባ ጊዜያት፣ የዘገየ ምዝገባ ደንቦች እና የሕይወት ክስተቶች መግለጫ።",
                            topicsEn = listOf("Birth & Death timelines (90 / 30 days)", "Marriage & Divorce legal documentation", "Vital event certifiers & requirements"),
                            topicsAm = listOf("የልደት እና የሞት ምዝገባ ጊዜ ገደብ (90 እና 30 ቀናት)", "የጋብቻ እና የፍቺ ሕጋዊ ሰነዶች", "የክስተት አሳዋቂዎች እና ምስክሮች መስፈርት")
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SectionOverviewItem(
                    sectionNumber = "Section B",
                    title = if (isAmharic) "አዲሱ የዲጂታል ሲቪል ምዝገባ ሥርዓት" else "New CRVS Digital Registration System",
                    details = if (isAmharic) "ጥያቄ 9–16 · የሥርዓቱ አጠቃቀም፣ ፍለጋ እና ደህንነት (8 ነጥብ)" else "Q 9–16 · System usage, search & security (8 pts)",
                    accentColor = Color(0xFF06B6D4),
                    isDark = isDarkMode,
                    onClick = {
                        showModulesSheet = false
                        activeModuleDetail = ModuleDetail(
                            sectionNumber = "Section B",
                            titleEn = "New CRVS Digital Registration System",
                            titleAm = "አዲሱ የዲጂታል ሲቪል ምዝገባ ሥርዓት",
                            questionsRange = "Q 9 – 16",
                            points = 8,
                            startIndex = 8,
                            accentColor = Color(0xFF06B6D4),
                            descriptionEn = "System architecture, Fayda National ID biometric integration, database search, audit logs, and data privacy safeguards.",
                            descriptionAm = "የሥርዓቱ አሠራር፣ የፋይዳ ብሔራዊ መታወቂያ ባዮሜትሪክ ቅንጅት፣ የመረጃ ፍለጋ፣ የኦዲት መዝገብ እና የመረጃ ደህንነት።",
                            topicsEn = listOf("Fayda ID verification & deduplication", "Digital certificate generation & QR codes", "Role-based access & audit security"),
                            topicsAm = listOf("የፋይዳ መታወቂያ ማረጋገጫ እና ድግግሞሽ መከላከል", "ዲጂታል ሰርተፍኬት ማመንጨት እና QR ኮድ", "በኃላፊነት የተገደበ መዳረሻ እና የኦዲት ደህንነት")
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SectionOverviewItem(
                    sectionNumber = "Section C",
                    title = if (isAmharic) "ተግባራዊ የጉዳይ ትንተናዎች" else "Practical Case Scenarios",
                    details = if (isAmharic) "ጥያቄ 17–20 · የውሳኔ አሰጣጥ እና የጉዳይ ትንተና (8 ነጥብ)" else "Q 17–20 · Decision-making & case analysis (8 pts)",
                    accentColor = Color(0xFF8B5CF6),
                    isDark = isDarkMode,
                    onClick = {
                        showModulesSheet = false
                        activeModuleDetail = ModuleDetail(
                            sectionNumber = "Section C",
                            titleEn = "Practical Case Scenarios",
                            titleAm = "ተግባራዊ የጉዳይ ትንተናዎች",
                            questionsRange = "Q 17 – 20",
                            points = 8,
                            startIndex = 16,
                            accentColor = Color(0xFF8B5CF6),
                            descriptionEn = "Real-world registrar problem solving: conflicting documents, unverified parentage, out-of-kebele births, and delayed death declarations.",
                            descriptionAm = "የተግባር ፈተናዎች፡ የተዛቡ ሰነዶች፣ ያልተረጋገጠ ወላጅነት፣ ከቀበሌ ውጭ የተከናወኑ ልደቶች እና የዘገዩ የሞት ምዝገባዎች።",
                            topicsEn = listOf("Cross-wereda vital event verification", "Late registration court clearance protocols", "Disputed parentage & evidence handling"),
                            topicsAm = listOf("የወረዳዎች መካከል የክስተት ማረጋገጫ", "የዘገየ ምዝገባ የፍርድ ቤት ውሳኔ አሠራር", "አወዛጋቢ የወላጅነት መዛግብት አያያዝ")
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SectionOverviewItem(
                    sectionNumber = "Section D",
                    title = if (isAmharic) "የቴክኒክ ብቃት እና ዝግጁነት መመዘኛ" else "Technical Assessment Checklist",
                    details = if (isAmharic) "ጥያቄ 21–25 · የሥራ ዝግጁነት እና ደረጃዎች (5 ነጥብ)" else "Q 21–25 · Operational readiness & standards (5 pts)",
                    accentColor = Color(0xFF10B981),
                    isDark = isDarkMode,
                    onClick = {
                        showModulesSheet = false
                        activeModuleDetail = ModuleDetail(
                            sectionNumber = "Section D",
                            titleEn = "Technical Assessment Checklist",
                            titleAm = "የቴክኒክ ብቃት እና ዝግጁነት መመዘኛ",
                            questionsRange = "Q 21 – 25",
                            points = 5,
                            startIndex = 20,
                            accentColor = Color(0xFF10B981),
                            descriptionEn = "Operational readiness: offline sync protocols, hardware operation, system fault recovery, and daily reconciliation reports.",
                            descriptionAm = "የሥራ ላይ ዝግጁነት፡ ያለ ኢንተርኔት መሥራት እና ማመሳሰል፣ የመሣሪያ አጠቃቀም፣ የብልሽት ማስተካከል እና የዕለት ሪፖርት።",
                            topicsEn = listOf("Offline cache & cloud synchronisation", "Biometric scanner troubleshooting", "Daily registration reconciliation checklist"),
                            topicsAm = listOf("ያለ ኢንተርኔት መመዝገብ እና ዳታ ማመሳሰል", "የጣት አሻራ መሣሪያ ብልሽት መፍቻ", "የዕለት ተዕለት ምዝገባ ማጠቃለያ ቼክሊስት")
                        )
                    }
                )
            }
        }
    }

    val context = LocalContext.current
    val regionManager = remember { RegionPreferenceManager.getInstance(context) }

    var examineeCertificate by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoadingCert by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.participantCode) {
        val code = currentUser?.participantCode
        if (!canManage && !code.isNullOrBlank()) {
            isLoadingCert = true
            try {
                val res = viewModel.apiService.getCertificatesForParticipant(code)
                if (res.isSuccessful && res.body() != null) {
                    @Suppress("UNCHECKED_CAST")
                    val list = res.body()!!["data"] as? List<Map<String, Any>>
                    examineeCertificate = list?.firstOrNull()
                }
            } catch (_: Exception) {}
            isLoadingCert = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgPrimary,
        topBar = {
            // ── 1. FIXED HEADER (Pinned TopBar / Not Movable) ───────────────
            Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkMode) Color(0xFF0F172A) else Color.White,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: logo + title/subtitle
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "ICS CRVS Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CRVS",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isAmharic) "የምዘና እና ማረጋገጫ ስርዓት" else "Assessment & Certification",
                                fontSize = 11.5.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right: action icons (Sync, Language, Theme, Settings)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (pendingCount > 0) {
                            IconButton(onClick = { viewModel.syncPending() }) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = AccentAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Language toggle: አ (Amharic) / EN (English)
                        IconButton(onClick = { onToggleLanguage(!isAmharic) }) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = if (isAmharic) "Switch to English" else "ወደ አማርኛ ቀይር",
                                tint = if (isAmharic) AccentEmerald else AccentAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Moon / sun toggle
                        IconButton(onClick = { onToggleTheme(!isDarkMode) }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkMode) AccentAmber else PrimaryIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Top header settings icon removed to prevent duplication with footer
                    }
                }
            }
        }
    },
    bottomBar = {
        // Modern floating curved pill nav bar matching reference design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 8.dp)
        ) {
            CurvedFloatingNavBar(
                isDarkMode = isDarkMode,
                items = listOf(
                    CurvedNavItem(
                        id = "home",
                        icon = Icons.Default.Home,
                        title = if (isAmharic) "መነሻ" else "Home",
                        hasBadge = false,
                        onClick = { /* Already on Home */ }
                    ),
                    CurvedNavItem(
                        id = "analytics",
                        icon = Icons.Default.Assessment,
                        title = if (isAmharic) "ዳሽቦርድ" else "Analytics",
                        hasBadge = false,
                        onClick = { onNavigateToAnalytics() }
                    ),
                    CurvedNavItem(
                        id = "curriculum",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = if (isAmharic) "ስልጠና" else "Modules",
                        hasBadge = false,
                        onClick = { showModulesSheet = true }
                    ),
                    CurvedNavItem(
                        id = "aichat",
                        icon = Icons.Default.SmartToy,
                        title = if (isAmharic) "AI ሞግዚት" else "AI Tutor",
                        hasBadge = uiState.isAiChatAllowed,
                        onClick = {
                            if (canManage || hasCompletedPostTest || uiState.isAiChatAllowed) {
                                onNavigateToAiChat()
                            } else {
                                showAiChatLockedDialog = true
                            }
                        }
                    ),
                    CurvedNavItem(
                        id = "signout",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = if (isAmharic) "ውጣ" else "Sign Out",
                        hasBadge = false,
                        onClick = { showLogoutDialog = true }
                    )
                ),
                selectedIndex = 0
            )
        }
    }
) { innerPadding ->
        // ── 3. SCROLLABLE BODY CONTENT (Strictly Scrolls In Between) ─────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

        // ── User Profile Card ───────────────────────────────────────────────
        if (currentUser != null) {
            val user = currentUser!!
            val isAdmin = user.role == "ADMIN"
            val isUserManager = user.role == "USER_MANAGER"

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkMode) Color(0xFF131B2E) else Color(0xFFFFFFFF),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0x26FFFFFF) else Color(0xFFE2E8F0)),
                shadowElevation = if (isDarkMode) 2.dp else 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shield avatar with rich gradient
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // System name + code
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.fullName.ifBlank { user.weredaKebele ?: "${region.regionName} System" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Code: ",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = user.participantCode ?: "${region.regionName.trim().take(2).uppercase().ifBlank { "RG" }}-USER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Role pill with icon and polished badge styling
                    val pillBg = when {
                        isAdmin -> Color(0xFF10B981).copy(alpha = 0.15f)
                        isUserManager -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFF6366F1).copy(alpha = 0.12f)
                    }
                    val pillText = when {
                        isAdmin -> Color(0xFF059669)
                        isUserManager -> Color(0xFFD97706)
                        else -> Color(0xFF4F46E5)
                    }
                    val roleLabel = when {
                        isAdmin -> "🛡️ ADMIN"
                        isUserManager -> "👥 MGR"
                        else -> "🎓 EXAM"
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pillBg,
                        border = BorderStroke(1.dp, pillText.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = roleLabel,
                            color = pillText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Examinee Official Certificate Card (Delivered upon Manager Approval) ──
        if (!canManage && examineeCertificate != null) {
            val cert = examineeCertificate!!
            val certNumber = cert["certificate_number"] as? String ?: "—"
            val scorePct = (cert["score_percentage"] as? Number)?.toInt() ?: 0
            val subIdLong = (cert["submission_id"] as? Number)?.toLong() ?: 0L
            val testType = cert["test_type"] as? String ?: "POST"
            val issuedAt = (cert["issued_at"] as? String)?.take(10) ?: ""

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkMode) Color(0xFF0C241D) else Color(0xFFF0FDF4),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "OFFICIAL CERTIFICATE ISSUED",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        Text(
                            text = "Score: $scorePct%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = if (isAmharic) "የብቃት ምስክር ወረቀት" else "Certificate of Competency",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) TextWhite else Color(0xFF0F172A)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Doc. No: $certNumber · ${if (testType == "POST") "Post-Test" else "Pre-Test"} · Approved by Manager",
                        fontSize = 12.sp,
                        color = if (isDarkMode) TextMuted else Color(0xFF64748B)
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val codeStr = currentUser?.participantCode ?: ""
                            onNavigateToCertificate(subIdLong, codeStr)
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00529C))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isAmharic) "የምስክር ወረቀት አውርድ" else "View & Download Certificate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Hero / Survey Card ──────────────────────────────────────────────
        val heroBgBrush = if (isDarkMode) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E1B4B), // Deep indigo slate
                    Color(0xFF0F172A), // Dark slate
                    Color(0xFF131A2B)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF8FAFC),
                    Color(0xFFF1F5F9)
                )
            )
        }

        val heroBorder = if (isDarkMode) {
            BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF6366F1).copy(alpha = 0.5f), Color(0xFF06B6D4).copy(alpha = 0.2f))))
        } else {
            BorderStroke(1.dp, Color(0xFFE2E8F0))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            border = heroBorder,
            shadowElevation = if (isDarkMode) 3.dp else 6.dp
        ) {
            Box(
                modifier = Modifier
                    .background(heroBgBrush)
                    .fillMaxWidth()
            ) {
                // Top decorative accent gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF10B981), // Emerald
                                    Color(0xFF6366F1), // Indigo
                                    Color(0xFF8B5CF6), // Purple
                                    Color(0xFF06B6D4)  // Cyan
                                )
                            )
                        )
                )

                Column(modifier = Modifier.padding(22.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // ── 3D Banner with Changeable Region & Minimized Size ───
                    val currentAmharicRegion = remember(region.regionName) {
                        getRegionAmharicName(region.regionName)
                    }
                    val dynamicTitle = if (isAmharic) {
                        "የቅድመ-ስልጠና እና የድህረ-ስልጠና የእውቀት መመዘኛ - $currentAmharicRegion የሲአርቪኤስ (CRVS) መዝጋቢዎች ስልጠና"
                    } else {
                        "Pre & Post Training Knowledge Assessment - ${region.regionName} CRVS Registrars Training"
                    }

                    // 3D Plaque container with depth extrusion
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // 1. Lower 3D extrusion ledge (creates physical depth & thickness)
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(y = 5.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isDarkMode) Color(0xFF090D16) else Color(0xFFCBD5E1)
                                )
                        )

                        // 2. Main 3D Card surface with specular gradient border
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            shadowElevation = if (isDarkMode) 8.dp else 4.dp,
                            border = BorderStroke(
                                1.2.dp,
                                Brush.verticalGradient(
                                    if (isDarkMode) listOf(
                                        Color(0xFF6366F1).copy(alpha = 0.6f),
                                        Color(0xFF0F172A).copy(alpha = 0.3f)
                                    ) else listOf(
                                        Color.White,
                                        Color(0xFFE2E8F0)
                                    )
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            if (isDarkMode) listOf(
                                                Color(0xFF1E293B),
                                                Color(0xFF0F172A)
                                            ) else listOf(
                                                Color(0xFFFFFFFF),
                                                Color(0xFFF8FAFC)
                                            )
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Column {
                                    // Top Row: Interactive 3D Region Selector Pill + 3D Mode Tag
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Changeable Region 3D Button
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isDarkMode) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDarkMode) Color(0xFF818CF8).copy(alpha = 0.45f) else Color(0xFFC7D2FE)
                                            ),
                                            shadowElevation = 2.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { showRegionSelectorDialog = true }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            ) {
                                                Text(text = "📍", fontSize = 11.5.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAmharic) currentAmharicRegion else region.regionName,
                                                    color = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF4338CA),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "Change Region",
                                                    tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF4338CA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (isAmharic) "ቀይር" else "Change",
                                                    color = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF6366F1),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Proclamation Badge (Strictly single-line, unshrinkable, fixed height)
                                        Surface(
                                            modifier = Modifier.height(28.dp).wrapContentWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFD1FAE5),
                                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF6EE7B7)),
                                            shadowElevation = 1.dp
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981))
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Proc. 1370/2024",
                                                    color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF065F46),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Minimized 3D Title (Clean & Compact)
                                    Text(
                                        text = dynamicTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                                        lineHeight = 19.sp,
                                        letterSpacing = (-0.1).sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Subtitle line
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "🇪🇹", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${region.agencyName} · National Standard Evaluation",
                                            fontSize = 11.sp,
                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── 3 Modern Stat Micro-Cards ───────────────────────────
                    val totalQCount = (survey?.sections?.flatMap { it.questions ?: emptyList() }?.size ?: 25)
                    val totalPts = survey?.totalPoints ?: 29
                    val passPts = survey?.passingScore ?: 18

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tile 1: Points
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL POINTS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalPts",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Max 100%",
                                    fontSize = 10.5.sp,
                                    color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Tile 2: Questions
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "QUESTIONS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalQCount",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "4 Categories",
                                    fontSize = 10.5.sp,
                                    color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Tile 3: Benchmark / Score
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFA7F3D0))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (canManage) "TARGET PASS" else "PASS SCORE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (canManage) "≥ 60%" else "$passPts pts",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Benchmark",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── Role-Specific Action Card ───────────────────────────────────────
        if (canManage) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = if (isDarkMode) Color(0xFF131B2E) else Color(0xFFFFFFFF),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF6366F1).copy(alpha = 0.35f) else Color(0xFFE2E8F0)),
                shadowElevation = if (isDarkMode) 3.dp else 5.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isAdminUser) Icons.Default.AdminPanelSettings else Icons.Default.SupervisorAccount,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isAdminUser) "Admin Control Center" else "Manager Control Center",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Exam Governance & Review",
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color(0xFF10B981),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isAdminUser)
                            "Review examinee submissions, manage registered users, and edit live question rubric parameters."
                        else
                            "Review examinee scores, monitor participants, and manage certificates.",
                        fontSize = 12.5.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manager / Admin Control: Post-Test Authorization Switch
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isPostTestUnlocked) AccentEmerald.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isPostTestUnlocked) AccentEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (uiState.isPostTestUnlocked) "🔓" else "🔒", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (uiState.isPostTestUnlocked) "Post-Test: Allowed for Examinees" else "Post-Test: Locked (Pre-Test Only)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (uiState.isPostTestUnlocked) "Examinees can take both Pre & Post-Test" else "Examinees see Pre-Test first until unlocked",
                                        fontSize = 11.sp,
                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.isPostTestUnlocked,
                                onCheckedChange = { viewModel.setPostTestUnlocked(regionManager, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentEmerald,
                                    checkedTrackColor = AccentEmerald.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BgCard,
                border = BorderStroke(1.5.dp, AccentEmerald.copy(alpha = 0.4f)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Take CRVS Exam",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select test mode and begin your assessment:",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPre = selectedTestType == "PRE"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTestType = "PRE" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPre) AccentAmber.copy(alpha = 0.15f) else BgCard,
                            border = BorderStroke(
                                width = if (isPre) 2.dp else 1.dp,
                                color = if (isPre) AccentAmber else BorderSubtle
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⏱️ Pre-Test",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isPre) AccentAmber else TextMuted
                                    )
                                    if (!isPostTestAllowed) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AccentAmber.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AccentAmber,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val isPost = selectedTestType == "POST"
                        if (isPostTestAllowed) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTestType = "POST" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPost) AccentEmerald.copy(alpha = 0.15f) else BgCard,
                                border = BorderStroke(
                                    width = if (isPost) 2.dp else 1.dp,
                                    color = if (isPost) AccentEmerald else BorderSubtle
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🎯 Post-Test",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isPost) AccentEmerald else TextMuted
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showPostTestLockedDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = BgCard.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, BorderSubtle.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🔒 Post-Test",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = TextDim
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(Locked)",
                                            fontSize = 10.sp,
                                            color = TextDim
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartExamDirectly(selectedTestType) },
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 3.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF0F766E),
                                            Color(0xFF10B981)
                                        )
                                    )
                                )
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedTestType == "PRE") {
                                        if (isAmharic) "የቅድመ-ስልጠና ፈተና ጀምር" else "Start Pre-Test Assessment"
                                    } else {
                                        if (isAmharic) "የድህረ-ስልጠና ፈተና ጀምር" else "Start Post-Test Assessment"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Tutor is exclusively accessed via the dedicated AI Tutor tab in the floating footer
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(24.dp))
    }
}



    // ── Logout dialog ───────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out?",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextWhite)
                }
            },
            containerColor = BgCard
        )
    }

    // ── Region Selection Dialog ──────────────────────────────────────────────
    if (showRegionSelectorDialog) {
        val allRegions = listOf(
            "Chartered Cities (Astedader)" to listOf(
                "Addis Ababa" to "አዲስ አበባ",
                "Dire Dawa" to "ድሬዳዋ"
            ),
            "Regional States (Kililoch)" to listOf(
                "Afar" to "አፋር",
                "Amhara" to "አማራ",
                "Benishangul-Gumuz" to "ቤንሻንጉል ጉሙዝ",
                "Central Ethiopia" to "ማዕከላዊ ኢትዮጵያ",
                "Gambela" to "ጋምቤላ",
                "Harari" to "ሐረሪ",
                "Oromia" to "ኦሮሚያ",
                "Sidama" to "ሲዳማ",
                "Somali" to "ሶማሌ",
                "South Ethiopia" to "ደቡብ ኢትዮጵያ",
                "South West Ethiopia Peoples' Region" to "ደቡብ ምዕራብ ኢትዮጵያ",
                "Tigray" to "ትግራይ"
            )
        )

        AlertDialog(
            onDismissRequest = { showRegionSelectorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAmharic) "ክልል / ከተማ አስተዳደር ይምረጡ" else "Select Region / City",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allRegions.forEach { (category, list) ->
                        Text(
                            text = category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4338CA),
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                        list.forEach { (regEn, regAm) ->
                            val isSelected = region.regionName.equals(regEn, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    if (isDarkMode) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF)
                                } else Color.Transparent,
                                border = if (isSelected) BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF818CF8) else Color(0xFF6366F1)
                                ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val preset = RegionPreferenceManager.PRESETS.find {
                                            it.regionName.equals(regEn, ignoreCase = true)
                                        } ?: RegionConfig(regionName = regEn, agencyName = "$regEn CRVS Agency")
                                        viewModel.setRegion(regionManager, preset)
                                        showRegionSelectorDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "$regEn ($regAm)",
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDarkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = getRegionAmharicName(regEn),
                                            fontSize = 11.sp,
                                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionSelectorDialog = false }) {
                    Text(if (isAmharic) "ዝጋ" else "Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)
        )
    }

    // ── Post-Test Locked Dialog (for Examinees) ──────────────────────────────
    if (showPostTestLockedDialog) {
        AlertDialog(
            onDismissRequest = { showPostTestLockedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAmharic) "የድህረ-ስልጠና መመዘኛ አልተፈቀደም" else "Post-Test Locked",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                }
            },
            text = {
                Text(
                    text = if (isAmharic)
                        "የድህረ-ስልጠና መመዘኛ (Post-Test) የሚፈቀደው ስልጠናው ሲጠናቀቅ በተፈቀደላቸው ስራ አስኪያጆች (Manager) ወይም አስተዳዳሪ (Admin) ሲከፈት ብቻ ነው።\n\nእባክዎ መጀመሪያ የቅድመ-ስልጠና ፈተናውን (Pre-Test) ያጠናቁ።"
                    else
                        "The Post-Test is locked until training is completed and authorized by an Admin or Training Manager.\n\nPlease take and complete your Pre-Test assessment first.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTestType = "PRE"
                    showPostTestLockedDialog = false
                }) {
                    Text(if (isAmharic) "እሺ፣ ቅድመ-ፈተናውን እጀምራለሁ" else "Understood (Take Pre-Test)", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BgCard
        )
    }
}

@Composable
fun SectionOverviewItem(
    sectionNumber: String,
    title: String,
    details: String,
    accentColor: Color = PrimaryIndigo,
    isDark: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, if (isDark) accentColor.copy(alpha = 0.25f) else Color(0xFFE2E8F0)),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.4f else 0.25f))
            ) {
                Text(
                    text = sectionNumber,
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.12f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

