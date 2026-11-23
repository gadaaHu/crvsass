package com.qcafr.gatherapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyRow
import com.qcafr.gatherapp.data.model.AiKnowledgeStats
import com.qcafr.gatherapp.data.model.KnowledgeDocumentItem
import com.qcafr.gatherapp.data.model.Question
import com.qcafr.gatherapp.data.model.QuestionOption
import com.qcafr.gatherapp.data.model.SubmissionRecordItem
import com.qcafr.gatherapp.data.model.User
import com.qcafr.gatherapp.data.preferences.RegionConfig
import com.qcafr.gatherapp.data.preferences.RegionPreferenceManager
import com.qcafr.gatherapp.ui.components.CurvedFloatingNavBar
import com.qcafr.gatherapp.ui.components.CurvedNavItem
import com.qcafr.gatherapp.data.remote.NetworkClient
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel
import com.qcafr.gatherapp.util.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun queryFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (_: Exception) {}
    }
    if (result == null) {
        val path = uri.path
        val cut = path?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = path?.substring(cut + 1)
        }
    }
    return result ?: "document_${System.currentTimeMillis()}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: QuestionnaireViewModel,
    isDarkMode: Boolean,
    isAmharic: Boolean = true,
    onToggleTheme: (Boolean) -> Unit,
    onToggleLanguage: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToCertificate: (submissionId: Long, code: String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAmharic = uiState.isAmharic
    val submissions by viewModel.submissions.collectAsState()
    val isLoadingSubmissions by viewModel.isLoadingSubmissions.collectAsState()
    val users by viewModel.users.collectAsState()
    val isLoadingUsers by viewModel.isLoadingUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val aiStats by viewModel.aiStats.collectAsState()
    val aiDocuments by viewModel.aiDocuments.collectAsState()
    val isLoadingAiKnowledge by viewModel.isLoadingAiKnowledge.collectAsState()
    val context = LocalContext.current
    val themeColors = MaterialTheme.appColors

    var selectedTab by remember { mutableIntStateOf(0) }
    var filterTestType by remember { mutableStateOf("ALL") } // ALL, PRE, POST
    var submissionSearchQuery by remember { mutableStateOf("") }
    var submissionBandFilter by remember { mutableStateOf("ALL") }
    var submissionToDelete by remember { mutableStateOf<SubmissionRecordItem?>(null) }
    var isExportingAllPdf by remember { mutableStateOf(false) }
    var exportingSingleId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(NetworkClient.baseUrl) }
    var editingQuestion by remember { mutableStateOf<Question?>(null) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Question?>(null) }
    var userToEditRole by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var userSearchQuery by remember { mutableStateOf("") }
    var userRoleFilter by remember { mutableStateOf("ALL") }

    var selectedDocUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocName by remember { mutableStateOf<String?>(null) }
    var selectedDocSize by remember { mutableStateOf<Long?>(null) }
    var docTitleInput by remember { mutableStateOf("") }
    var isUploadingDoc by remember { mutableStateOf(false) }
    var docToDelete by remember { mutableStateOf<KnowledgeDocumentItem?>(null) }
    var testQuestionInput by remember { mutableStateOf("") }
    var testAiAnswer by remember { mutableStateOf<String?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedDocUri = uri
            val name = queryFileName(context, uri)
            selectedDocName = name
            if (docTitleInput.isBlank()) {
                val base = name.substringBeforeLast(".")
                docTitleInput = base.replace("_", " ").replace("-", " ")
            }
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        selectedDocSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    var isSaving by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val regionManager = remember { RegionPreferenceManager.getInstance(context) }
    val currentRegion = uiState.regionConfig
    var regRegionName by remember(currentRegion.regionName) { mutableStateOf(currentRegion.regionName) }
    var regAgencyName by remember(currentRegion.agencyName) { mutableStateOf(currentRegion.agencyName) }
    var regCountry by remember(currentRegion.country) { mutableStateOf(currentRegion.country) }
    var regProclamation by remember(currentRegion.proclamation) { mutableStateOf(currentRegion.proclamation) }
    var regTrainingLabel by remember(currentRegion.trainingLabel) { mutableStateOf(currentRegion.trainingLabel) }
    var regDefaultWereda by remember(currentRegion.defaultWereda) { mutableStateOf(currentRegion.defaultWereda) }
    var regDefaultKebele by remember(currentRegion.defaultKebele) { mutableStateOf(currentRegion.defaultKebele) }
    var regKebeleSuggestions by remember(currentRegion.kebeleSuggestions) { mutableStateOf(currentRegion.kebeleSuggestions) }

    LaunchedEffect(Unit) {
        viewModel.loadSubmissions()
        viewModel.loadUsers()
        viewModel.loadAiKnowledge()
    }

    val isAdmin = currentUser?.role == "ADMIN"
    val isManager = currentUser?.role == "USER_MANAGER"
    val canManage = isAdmin || isManager

    if (!canManage) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Access Restricted", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSecondary)
                )
            },
            containerColor = BgPrimary
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isAmharic) "የተገደበ መዳረሻ" else "Access Restricted",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isAmharic) "የፈተና ውጤቶችና የተጠቃሚዎች አስተዳደር የሚታየው ለአስተዳዳሪ እና ሥራ አስኪያጅ ብቻ ነው።" else "Exam results and user management are restricted to Administrators and Managers.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text(if (isAmharic) "ወደ ዋናው ገጽ ተመለስ" else "Back to Home")
                }
            }
        }
        return
    }

    fun openPdfFile(file: java.io.File) {
        try {
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
            } catch (_: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Open Exam PDF Report"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredSubmissions = remember(submissions, filterTestType, submissionSearchQuery, submissionBandFilter) {
        submissions.filter { sub ->
            val matchesType = when (filterTestType) {
                "PRE" -> sub.testType.uppercase() == "PRE"
                "POST" -> sub.testType.uppercase() == "POST"
                else -> true
            }
            val matchesBand = when (submissionBandFilter) {
                "STRONG" -> sub.performanceBand == "STRONG"
                "SATISFACTORY" -> sub.performanceBand == "SATISFACTORY"
                "NEEDS_REINFORCEMENT" -> sub.performanceBand == "NEEDS_REINFORCEMENT"
                "CRITICAL" -> sub.performanceBand != "STRONG" && sub.performanceBand != "SATISFACTORY" && sub.performanceBand != "NEEDS_REINFORCEMENT"
                else -> true
            }
            val q = submissionSearchQuery.trim()
            val matchesSearch = if (q.isBlank()) true else {
                sub.participantCode.contains(q, ignoreCase = true) ||
                (sub.weredaKebele?.contains(q, ignoreCase = true) == true) ||
                (sub.role?.contains(q, ignoreCase = true) == true) ||
                sub.performanceBand.contains(q, ignoreCase = true)
            }
            matchesType && matchesBand && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAmharic) {
                                if (isManager) "የሥራ አስኪያጅ ማዕከል" else "የቁጥጥርና አስተዳደር ማዕከል"
                            } else {
                                if (isManager) "Manager Portal" else "Admin Portal & Control Center"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isAmharic) "የውጤቶች፣ ተጠቃሚዎች እና ጥያቄዎች ማዕከል" else "Results, Users & Question Management",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    IconButton(onClick = { onToggleLanguage(!isAmharic) }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = if (isAmharic) "Switch to English" else "ወደ አማርኛ ቀይር",
                            tint = if (isAmharic) AccentEmerald else PrimaryIndigo
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.loadSubmissions()
                            viewModel.loadUsers()
                            viewModel.loadSurvey(1L)
                            viewModel.loadAnalytics()
                            Toast.makeText(context, "Refreshing data...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSecondary)
            )
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
                            onClick = { onNavigateBack() }
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
                            onClick = { onNavigateBack() }
                        ),
                        CurvedNavItem(
                            id = "aichat",
                            icon = Icons.Default.SmartToy,
                            title = if (isAmharic) "AI ሞግዚት" else "AI Tutor",
                            hasBadge = uiState.isAiChatAllowed,
                            onClick = { onNavigateBack() }
                        ),
                        CurvedNavItem(
                            id = "signout",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = if (isAmharic) "ውጣ" else "Sign Out",
                            hasBadge = false,
                            onClick = { showSignOutConfirm = true }
                        )
                    ),
                    selectedIndex = -1
                )
            }
        },
        containerColor = BgPrimary,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Admin Tabs Header: Results | Users | Questions | System | AI Docs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
                containerColor = BgSecondary,
                contentColor = PrimaryIndigo
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (isAmharic) "ውጤቶች (${submissions.size})" else "Results (${submissions.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (isAmharic) "ተጠቃሚዎች (${users.size})" else "Users (${users.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = if (isAmharic) "ጥያቄዎች (${uiState.flatQuestions.size})" else "Questions (${uiState.flatQuestions.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = if (isAmharic) "ስርዓት" else "System",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Text(
                            text = if (isAmharic) "AI ሰነዶች (${aiDocuments.size})" else "AI Tutor & Docs (${aiDocuments.size})",
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // ── TAB 0: EXAM RESULTS & SUBMISSIONS ────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats overview banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BgCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Assessment,
                                                contentDescription = null,
                                                tint = PrimaryIndigo,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "የተመዘገቡ የፈተና ውጤቶች ማጠቃለያ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = TextWhite
                                            )
                                        }
                                        if (isLoadingSubmissions) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryIndigo, strokeWidth = 2.dp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 4 Clean, Weighted KPI Metric Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val avg = if (submissions.isNotEmpty()) "${(submissions.map { it.scorePercentage }.average()).toInt()}%" else "0%"
                                        UserStatChip(
                                            title = "Total",
                                            subtitle = "ጠቅላላ",
                                            count = "${submissions.size}",
                                            color = PrimaryIndigo,
                                            modifier = Modifier.weight(1f)
                                        )
                                        UserStatChip(
                                            title = "Pre-Test",
                                            subtitle = "ቅድመ",
                                            count = "${submissions.count { it.testType.uppercase() == "PRE" }}",
                                            color = AccentAmber,
                                            modifier = Modifier.weight(1f)
                                        )
                                        UserStatChip(
                                            title = "Post-Test",
                                            subtitle = "ድህረ",
                                            count = "${submissions.count { it.testType.uppercase() == "POST" }}",
                                            color = AccentEmerald,
                                            modifier = Modifier.weight(1f)
                                        )
                                        UserStatChip(
                                            title = "Avg Score",
                                            subtitle = "አማካይ",
                                            count = avg,
                                            color = Color(0xFF6366F1),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Master PDF Export Button
                                    Button(
                                        onClick = {
                                            if (!isExportingAllPdf) {
                                                isExportingAllPdf = true
                                                scope.launch {
                                                    try {
                                                        val targetList = if (filteredSubmissions.isNotEmpty()) filteredSubmissions else submissions
                                                        if (targetList.isEmpty()) {
                                                            Toast.makeText(context, "No exam results to export", Toast.LENGTH_SHORT).show()
                                                            return@launch
                                                        }
                                                        val file = withContext(Dispatchers.IO) {
                                                            PdfExporter.exportAllSubmissionsPdf(
                                                                context = context,
                                                                submissions = targetList,
                                                                agencyName = uiState.regionConfig.agencyName,
                                                                place = uiState.regionConfig.place
                                                            )
                                                        }
                                                        Toast.makeText(context, "Master PDF Report saved: ${file.name}", Toast.LENGTH_LONG).show()
                                                        openPdfFile(file)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        isExportingAllPdf = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        enabled = !isExportingAllPdf && submissions.isNotEmpty()
                                    ) {
                                        if (isExportingAllPdf) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isAmharic) "ሪፖርት እየተዘጋጀ ነው..." else "Generating PDF Report...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isAmharic) {
                                                    "የፈተና ውጤቶች PDF አውርድ (${if (filteredSubmissions.size != submissions.size) "${filteredSubmissions.size}/" else ""}${submissions.size})"
                                                } else {
                                                    "Download All Results PDF (${if (filteredSubmissions.size != submissions.size) "${filteredSubmissions.size}/" else ""}${submissions.size})"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                // Duplicate Open Analytical Dashboard button removed (already in bottom nav bar)
                            }
                        }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Search bar for examinees
                            OutlinedTextField(
                                value = submissionSearchQuery,
                                onValueChange = { submissionSearchQuery = it },
                                placeholder = { Text(if (isAmharic) "በተሳታፊ ኮድ፣ ወረዳ ወይም ሚና ፈልግ..." else "Search by participant code, wereda, or role...", fontSize = 12.5.sp, color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                                trailingIcon = {
                                    if (submissionSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { submissionSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = BgCard,
                                    unfocusedContainerColor = BgCard,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Filter Chips Row: Test Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = if (isAmharic) listOf("ALL" to "ሁሉም (${submissions.size})", "PRE" to "ቅድመ-ፈተና", "POST" to "ድህረ-ፈተና") else listOf("ALL" to "All (${submissions.size})", "PRE" to "Pre-Test", "POST" to "Post-Test")
                                filters.forEach { (key, label) ->
                                    val isSelected = filterTestType == key
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) PrimaryIndigo else BgCard,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
                                        modifier = Modifier.clickable { filterTestType = key }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextWhite,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter Chips Row: Performance Band
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val bandFilters = if (isAmharic) listOf(
                                    "ALL" to "ሁሉም ደረጃዎች",
                                    "STRONG" to "ከፍተኛ ውጤት",
                                    "SATISFACTORY" to "አጥጋቢ",
                                    "NEEDS_REINFORCEMENT" to "የማጠናከሪያ ድጋፍ"
                                ) else listOf(
                                    "ALL" to "All Bands",
                                    "STRONG" to "Strong",
                                    "SATISFACTORY" to "Satisfactory",
                                    "NEEDS_REINFORCEMENT" to "Needs Support"
                                )
                                bandFilters.forEach { (bKey, bLabel) ->
                                    val isSel = submissionBandFilter == bKey
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSel) PrimaryIndigo.copy(alpha = 0.25f) else BgCard,
                                        border = BorderStroke(1.dp, if (isSel) PrimaryIndigo else BorderSubtle),
                                        modifier = Modifier.clickable { submissionBandFilter = bKey }
                                    ) {
                                        Text(
                                            text = bLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) PrimaryIndigo else TextMuted,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredSubmissions.isEmpty() && !isLoadingSubmissions) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isAmharic) "ምንም የተመዘገበ የፈተና ውጤት የለም" else "No exam submissions found",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (isAmharic) "ተፈታኞች ፈተና ሲወስዱ ውጤቶቻቸው እዚህ በዝርዝር ይታያሉ።" else "When examinees take exams, their scores and details will appear here automatically.",
                                            fontSize = 12.sp,
                                            color = TextMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredSubmissions) { sub ->
                                val isPost = sub.testType.uppercase() == "POST"
                                val bandColor = when (sub.performanceBand) {
                                    "STRONG" -> AccentEmerald
                                    "SATISFACTORY" -> PrimaryIndigo
                                    "NEEDS_REINFORCEMENT" -> AccentAmber
                                    else -> AccentDanger
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // Header Row: Code, Test Type, Score
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = sub.participantCode,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = TextWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isPost) AccentEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isPost) "POST-TEST" else "PRE-TEST",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isPost) AccentEmerald else AccentAmber
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Score pill
                                            Text(
                                                text = "${sub.totalScore} / ${sub.maxScore} (${sub.scorePercentage.toInt()}%)",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = if (sub.scorePercentage >= 60) AccentEmerald else AccentAmber,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Details Row: Facility, Role, Band
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "📍 ${sub.weredaKebele ?: "${uiState.regionConfig.regionName} System"}",
                                                    fontSize = 12.sp,
                                                    color = TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "👤 ${sub.role ?: "Registrar"}",
                                                    fontSize = 11.5.sp,
                                                    color = TextDim,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            val bandLabel = when (sub.performanceBand) {
                                                "STRONG" -> if (isAmharic) "ከፍተኛ" else "Strong"
                                                "SATISFACTORY" -> if (isAmharic) "አጥጋቢ" else "Satisfactory"
                                                "NEEDS_REINFORCEMENT" -> if (isAmharic) "ማጠናከሪያ" else "Needs Support"
                                                else -> if (isAmharic) "መሻሻል ያለበት" else "Critical"
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = bandColor.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, bandColor.copy(alpha = 0.45f)),
                                                modifier = Modifier.widthIn(max = 120.dp)
                                            ) {
                                                Text(
                                                    text = bandLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = bandColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Timestamp
                                        Text(
                                            text = "🕒 Submitted: ${sub.submittedAt}",
                                            fontSize = 10.5.sp,
                                            color = TextDim
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Approval status badge
                                        val approvalStatus = (sub as? Any)?.let {
                                            try { (it.javaClass.getField("approvalStatus").get(it) as? String) } catch (_: Exception) { null }
                                        } ?: "PENDING"
                                        val canCertify = sub.scorePercentage >= 50.0
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val (statusBg, statusColor, statusLabel) = when (approvalStatus) {
                                                "APPROVED" -> Triple(AccentEmerald.copy(0.15f), AccentEmerald, "✓ APPROVED")
                                                "REJECTED" -> Triple(AccentDanger.copy(0.15f), AccentDanger, "✗ REJECTED")
                                                else -> if (canCertify)
                                                    Triple(AccentAmber.copy(0.15f), AccentAmber, "⏳ Awaiting Approval")
                                                else
                                                    Triple(BorderSubtle.copy(0.3f), TextDim, "Score < 50%")
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(statusBg)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                            }
                                        }

                                        // Action buttons: Download PDF + Approve/Certificate + Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isThisExporting = exportingSingleId == sub.id

                                            // PDF Download
                                            OutlinedButton(
                                                onClick = {
                                                    if (exportingSingleId == null) {
                                                        exportingSingleId = sub.id
                                                        scope.launch {
                                                            try {
                                                                val file = withContext(Dispatchers.IO) {
                                                                    PdfExporter.exportSingleSubmissionRecordPdf(
                                                                        context = context,
                                                                        item = sub,
                                                                        agencyName = uiState.regionConfig.agencyName,
                                                                        place = uiState.regionConfig.place
                                                                    )
                                                                }
                                                                Toast.makeText(context, "PDF generated: ${file.name}", Toast.LENGTH_SHORT).show()
                                                                openPdfFile(file)
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            } finally {
                                                                exportingSingleId = null
                                                            }
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                if (isThisExporting) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PrimaryIndigo, strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Approve / Certificate button
                                            if (canCertify) {
                                                val certColor = if (approvalStatus == "APPROVED") AccentEmerald else Color(0xFF00529C)
                                                val certLabel = if (approvalStatus == "APPROVED") "🏆 Cert" else "✅ Approve"
                                                OutlinedButton(
                                                    onClick = {
                                                        if (approvalStatus == "APPROVED") {
                                                            // Navigate to certificate viewer
                                                            val subIdLong = sub.id?.toLong() ?: 0L
                                                            onNavigateToCertificate(subIdLong, sub.participantCode)
                                                        } else {
                                                            // Trigger approval
                                                            scope.launch {
                                                                try {
                                                                    val subIdLong = sub.id?.toLong() ?: 0L
                                                                    val body = mapOf("approved_by" to (currentUser?.fullName ?: currentUser?.username ?: "Manager"))
                                                                    val res = viewModel.apiService.approveSubmission(subIdLong, body)
                                                                    if (res.isSuccessful) {
                                                                        Toast.makeText(context, "✅ Certificate issued!", Toast.LENGTH_LONG).show()
                                                                        viewModel.loadSubmissions()
                                                                        val subIdLong2 = sub.id?.toLong() ?: 0L
                                                                        onNavigateToCertificate(subIdLong2, sub.participantCode)
                                                                    } else {
                                                                        val errBody = res.errorBody()?.string() ?: "Unknown"
                                                                        Toast.makeText(context, "Failed: $errBody", Toast.LENGTH_LONG).show()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, certColor.copy(alpha = 0.7f)),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = certColor),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text(certLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            // Delete
                                            OutlinedButton(
                                                onClick = { submissionToDelete = sub },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, AccentDanger.copy(alpha = 0.5f)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentDanger),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                1 -> {
                    // ── TAB 1: USER MANAGEMENT ─────────────────────────────────
                    val filteredUsers = remember(users, userSearchQuery, userRoleFilter) {
                        users.filter { user ->
                            val matchesRole = when (userRoleFilter) {
                                "ADMIN" -> user.role == "ADMIN"
                                "USER_MANAGER" -> user.role == "USER_MANAGER"
                                "EXAMINEE" -> user.role == "EXAMINEE"
                                else -> true
                            }
                            val q = userSearchQuery.trim().lowercase()
                            val matchesQuery = q.isEmpty() ||
                                user.username.lowercase().contains(q) ||
                                (user.weredaKebele ?: "").lowercase().contains(q) ||
                                (user.participantCode ?: "").lowercase().contains(q) ||
                                user.fullName.lowercase().contains(q)
                            matchesRole && matchesQuery
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))

                            // 4 summary badges: Total | Admins | Managers | Examinees
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UserStatChip(
                                    title = if (isAmharic) "ጠቅላላ" else "Total",
                                    count = users.size.toString(),
                                    color = PrimaryIndigo,
                                    modifier = Modifier.weight(1f)
                                )
                                UserStatChip(
                                    title = if (isAmharic) "አስተዳዳሪ" else "Admins",
                                    count = users.count { it.role == "ADMIN" }.toString(),
                                    color = Color(0xFF00796B),
                                    modifier = Modifier.weight(1f)
                                )
                                UserStatChip(
                                    title = if (isAmharic) "ማኔጀሮች" else "Managers",
                                    count = users.count { it.role == "USER_MANAGER" }.toString(),
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.weight(1f)
                                )
                                UserStatChip(
                                    title = if (isAmharic) "ተፈታኞች" else "Examinees",
                                    count = users.count { it.role == "EXAMINEE" }.toString(),
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Search Bar
                        item {
                            OutlinedTextField(
                                value = userSearchQuery,
                                onValueChange = { userSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search by name, username, phone, or code...", fontSize = 12.5.sp, color = TextDim) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    if (userSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { userSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextDim, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = BgCard,
                                    unfocusedContainerColor = BgCard,
                                    focusedTextColor = themeColors.textPrimary,
                                    unfocusedTextColor = themeColors.textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter chips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "ALL" to "All (${users.size})",
                                    "ADMIN" to "Admins",
                                    "USER_MANAGER" to "Managers",
                                    "EXAMINEE" to "Examinees"
                                ).forEach { (filterVal, label) ->
                                    val isSelected = userRoleFilter == filterVal
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) PrimaryIndigo else BgCard,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
                                        modifier = Modifier.clickable { userRoleFilter = filterVal }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else TextMuted,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Loading or Empty or User list
                        if (isLoadingUsers) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = PrimaryIndigo)
                                }
                            }
                        } else if (filteredUsers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAmharic) {
                                            if (userSearchQuery.isNotBlank() || userRoleFilter != "ALL") "ምንም ተጠቃሚ አልተገኘም" else "ምንም የተመዘገቡ ተጠቃሚዎች የሉም"
                                        } else {
                                            if (userSearchQuery.isNotBlank() || userRoleFilter != "ALL") "No matching users found" else "No registered users"
                                        },
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredUsers, key = { it.id }) { user ->
                                UserCardItem(
                                    user = user,
                                    isCurrentUser = user.id == currentUser?.id,
                                    onEditRole = { userToEditRole = user },
                                    onDelete = { userToDelete = user }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                2 -> {
                    // ── TAB 2: QUESTION BANK & EDITOR ────────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isAmharic) "የጥያቄዎች ባንክ" else "Question Bank",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (isAmharic) "${uiState.flatQuestions.size} ጥያቄዎች · የቀጥታ ጥያቄ ማዘጋጃ" else "${uiState.flatQuestions.size} Questions · Live question editor",
                                        fontSize = 11.5.sp,
                                        color = TextDim
                                    )
                                }
                                Button(
                                    onClick = { showAddQuestionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Question", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // List of questions with Edit Button and Delete Button
                        items(uiState.flatQuestions) { pair ->
                            val q = pair.second

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BgCard)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(PrimaryIndigo.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Q${q.questionNumber}",
                                                    color = PrimaryIndigo,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${q.points} Pt${if (q.points > 1) "s" else ""}",
                                                color = AccentEmerald,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = BorderSubtle
                                            ) {
                                                Text(
                                                    text = when (q.type.name) {
                                                        "SINGLE_CHOICE" -> "MCQ"
                                                        "TEXT_OPEN" -> "SCENARIO"
                                                        "RATING_SCALE" -> "RATING"
                                                        else -> q.type.name
                                                    },
                                                    fontSize = 10.sp,
                                                    color = TextDim,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Edit Button
                                            IconButton(
                                                onClick = { editingQuestion = q },
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(BorderSubtle)
                                                    .size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Question",
                                                    tint = PrimaryIndigo,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Delete Button
                                            IconButton(
                                                onClick = { questionToDelete = q },
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(AccentDanger.copy(alpha = 0.12f))
                                                    .size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Question",
                                                    tint = AccentDanger,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = q.promptAm,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = TextWhite,
                                        lineHeight = 20.sp
                                    )

                                    if (q.promptEn.isNotBlank() && q.promptEn != q.promptAm) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = q.promptEn,
                                            fontSize = 12.5.sp,
                                            color = TextMuted,
                                            lineHeight = 17.sp
                                        )
                                    }

                                    // Options preview
                                    if (!q.options.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            q.options.forEach { opt ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "${opt.optionKey}. ",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (opt.isCorrect) AccentEmerald else TextDim
                                                    )
                                                    Text(
                                                        text = opt.labelAm,
                                                        fontSize = 12.sp,
                                                        color = if (opt.isCorrect) AccentEmerald else TextMuted,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (opt.isCorrect) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Correct",
                                                            tint = AccentEmerald,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else if (!q.rubricAm.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BorderSubtle.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "Rubric: ${q.rubricAm}",
                                                fontSize = 11.5.sp,
                                                color = TextDim,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                3 -> {
                    // ── TAB 2: SYSTEM CONFIG & SETTINGS ──────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Appearance Card (Light / Dark toggle)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BgCard)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isDarkMode) PrimaryIndigo.copy(alpha = 0.15f)
                                                    else AccentAmber.copy(alpha = 0.15f)
                                                )
                                                .padding(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                contentDescription = null,
                                                tint = if (isDarkMode) PrimaryIndigo else AccentAmber,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = if (isAmharic) {
                                                    if (isDarkMode) "የጨለማ ገጽታ" else "የብርሃን ገጽታ"
                                                } else {
                                                    if (isDarkMode) "Dark Mode" else "Light Mode"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = if (isAmharic) {
                                                    if (isDarkMode) "ወደ ብርሃን ገጽታ ለመቀየር ይጫኑ" else "ወደ ጨለማ ገጽታ ለመቀየር ይጫኑ"
                                                } else {
                                                    if (isDarkMode) "Tap to switch to light theme" else "Tap to switch to dark theme"
                                                },
                                                fontSize = 12.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { onToggleTheme(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PrimaryIndigo,
                                            checkedTrackColor = PrimaryIndigo.copy(alpha = 0.4f),
                                            uncheckedThumbColor = AccentAmber,
                                            uncheckedTrackColor = AccentAmber.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 🤖 AI Tutor Permission Card (Admin / Manager Control)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BgCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF0F766E).copy(alpha = 0.2f))
                                                .padding(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = Color(0xFF2DD4BF),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = if (isAmharic) "የድህረ-ፈተና AI ረዳት ፈቃድ" else "Post-Test AI Tutor Chat",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = if (isAmharic) "ተፈታኞች ድህረ-ፈተና ካጠናቀቁ በኋላ በ AI ጥያቄዎችን እንዲጠይቁ መፍቀጃ"
                                                else "Authorize examinees to consult AI Tutor on training topics after Post-Test",
                                                fontSize = 12.sp,
                                                color = TextMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = uiState.isAiChatAllowed,
                                        onCheckedChange = { allowed ->
                                            viewModel.setAiChatAllowed(regionManager, allowed)
                                            Toast.makeText(
                                                context,
                                                if (allowed) {
                                                    if (isAmharic) "የ AI ውይይት ለተፈታኞች ተፈቅዷል! ✓" else "AI Chat authorized for examinees! ✓"
                                                } else {
                                                    if (isAmharic) "የ AI ውይይት ለተፈታኞች ተዘግቷል!" else "AI Chat disabled for examinees!"
                                                },
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF2DD4BF),
                                            checkedTrackColor = Color(0xFF0F766E).copy(alpha = 0.5f),
                                            uncheckedThumbColor = Color(0xFF64748B),
                                            uncheckedTrackColor = Color(0xFF334155)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 🏢 Region & Agency Jurisdiction Configuration Card
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
                                        text = if (isAmharic) "🏢 የክልል እና ኤጀንሲ መረጃ" else "🏢 Region & Civil Registry Jurisdiction",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAmharic) "ስርዓቱ ለተለያዩ የኢትዮጵያ ክልሎች እንዲሰራ የክልልና ኤጀንሲ መረጃ እዚህ ይቀይሩ።" else "Configure civil registry jurisdiction for certificates, PDFs, and agency branding.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Active Region Pill
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PrimaryIndigo.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isAmharic) "📍 አሁን እየሰራበት ያለ፡" else "📍 Active Jurisdiction:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryIndigo
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = currentRegion.agencyName,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Presets row
                                    Text(
                                        text = if (isAmharic) "⚡ ፈጣን የክልል ምርጫዎች፡" else "⚡ Quick Region Presets:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(RegionPreferenceManager.PRESETS) { preset ->
                                            val isSelected = regRegionName.equals(preset.regionName, ignoreCase = true)
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isSelected) PrimaryIndigo else BgSecondary,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) PrimaryIndigo else BorderSubtle
                                                ),
                                                modifier = Modifier.clickable {
                                                    regRegionName = preset.regionName
                                                    regAgencyName = preset.agencyName
                                                    regCountry = preset.country
                                                    regProclamation = preset.proclamation
                                                    regTrainingLabel = preset.trainingLabel
                                                    regDefaultWereda = preset.defaultWereda
                                                    regDefaultKebele = preset.defaultKebele
                                                    regKebeleSuggestions = preset.kebeleSuggestions
                                                }
                                            ) {
                                                Text(
                                                    text = preset.regionName,
                                                    color = if (isSelected) Color.White else TextWhite,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Agency & Authority Inputs
                                    OutlinedTextField(
                                        value = regRegionName,
                                        onValueChange = {
                                            regRegionName = it
                                            if (regAgencyName.isBlank() || regAgencyName.endsWith("CRVS Agency") || regAgencyName.endsWith("CRVS Regional Agency")) {
                                                regAgencyName = if (it.isNotBlank()) "$it CRVS Agency" else "CRVS Agency"
                                            }
                                        },
                                        label = { Text(if (isAmharic) "የክልል ወይም የከተማ ስም (አማራጭ)" else "Region or City Name (Optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = themeColors.textPrimary,
                                            unfocusedTextColor = themeColors.textPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regAgencyName,
                                        onValueChange = { regAgencyName = it },
                                        label = { Text(if (isAmharic) "የኤጀንሲ ስም (ለምሳሌ፡ የድሬዳዋ ሲቪል ምዝገባ ኤጀንሲ)" else "Agency Name (e.g. Dire Dawa CRVS Agency)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = themeColors.textPrimary,
                                            unfocusedTextColor = themeColors.textPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = regCountry,
                                            onValueChange = { regCountry = it },
                                            label = { Text(if (isAmharic) "አገር" else "Country") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryIndigo,
                                                unfocusedBorderColor = BorderSubtle,
                                                focusedTextColor = themeColors.textPrimary,
                                                unfocusedTextColor = themeColors.textPrimary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = regProclamation,
                                            onValueChange = { regProclamation = it },
                                            label = { Text(if (isAmharic) "አዋጅ" else "Proclamation") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryIndigo,
                                                unfocusedBorderColor = BorderSubtle,
                                                focusedTextColor = themeColors.textPrimary,
                                                unfocusedTextColor = themeColors.textPrimary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regTrainingLabel,
                                        onValueChange = { regTrainingLabel = it },
                                        label = { Text(if (isAmharic) "የስልጠናው ርዕስ ወይም ፕሮግራም" else "Training Title / Program Label") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = themeColors.textPrimary,
                                            unfocusedTextColor = themeColors.textPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // ── PLACE OF REGISTRATION SUBSECTION ────────────────────────
                                    Text(
                                        text = if (isAmharic) "📍 የምዝገባ ቦታ ቅንብር (አገር፣ ክልል/ከተማ፣ ወረዳ፣ ቀበሌ)" else "📍 Place of Registration (Country, Region/City, Wereda, Kebele)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAmharic) "የተጠቃሚዎች እና ተፈታኞች የምዝገባ ቦታ ነባሪ ወረዳ፣ ቀበሌ እና ፈጣን አማራጮች ያዘጋጁ።" else "Configure default Wereda, Kebele, and quick suggestions for participants.",
                                        fontSize = 11.5.sp,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = regDefaultWereda,
                                            onValueChange = { regDefaultWereda = it },
                                            label = { Text(if (isAmharic) "ነባሪ ወረዳ" else "Default Wereda") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryIndigo,
                                                unfocusedBorderColor = BorderSubtle,
                                                focusedTextColor = themeColors.textPrimary,
                                                unfocusedTextColor = themeColors.textPrimary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = regDefaultKebele,
                                            onValueChange = { regDefaultKebele = it },
                                            label = { Text(if (isAmharic) "ነባሪ ቀበሌ" else "Default Kebele") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryIndigo,
                                                unfocusedBorderColor = BorderSubtle,
                                                focusedTextColor = themeColors.textPrimary,
                                                unfocusedTextColor = themeColors.textPrimary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regKebeleSuggestions,
                                        onValueChange = { regKebeleSuggestions = it },
                                        label = { Text(if (isAmharic) "ፈጣን የቀበሌ ጥቆማዎች (በኮማ ይለዩ)" else "Quick Kebele Suggestions (Comma-separated)") },
                                        singleLine = false,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = themeColors.textPrimary,
                                            unfocusedTextColor = themeColors.textPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Live Place of Registration Preview
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = AccentEmerald.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isAmharic) "📍 የምዝገባ ቦታ ቅድመ-ዕይታ፡" else "📍 Place of Registration Preview:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentEmerald
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = currentRegion.formatPlaceOfRegistration(
                                                    kebele = regDefaultKebele,
                                                    wereda = regDefaultWereda,
                                                    regOrCity = regRegionName,
                                                    ctry = regCountry
                                                ),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextWhite,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            val newConfig = RegionConfig(
                                                regionName = regRegionName.trim(),
                                                country = regCountry.trim().ifBlank { "Ethiopia" },
                                                agencyName = regAgencyName.trim().ifBlank { if (regRegionName.isNotBlank()) "${regRegionName.trim()} CRVS Agency" else "CRVS Agency" },
                                                proclamation = regProclamation.trim().ifBlank { "Proclamation No. 1370/2024" },
                                                trainingLabel = regTrainingLabel.trim().ifBlank { "CRVS Registrar Training" },
                                                defaultWereda = regDefaultWereda.trim().ifBlank { "Wereda 01" },
                                                defaultKebele = regDefaultKebele.trim().ifBlank { "Kebele 01" },
                                                kebeleSuggestions = regKebeleSuggestions.trim()
                                            )
                                            viewModel.setRegion(regionManager, newConfig)
                                            Toast.makeText(context, if (isAmharic) "የክልል እና የምዝገባ ቦታ ቅንብሮች ተቀምጠዋል!" else "Jurisdiction settings saved successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isAmharic) "የምዝገባ ቦታ እና የክልል መረጃ መዝግብ" else "Save Jurisdiction Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Server Config Card
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
                                        text = if (isAmharic) "🌐 የዋናው ሰርቨር ግንኙነት" else "🌐 Backend Server Connection",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAmharic) "መረጃዎችን ለማመሳሰል የሰርቨሩን አድራሻ ያስገቡ" else "Specify the backend API address for field synchronization",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = serverUrl,
                                        onValueChange = { serverUrl = it },
                                        label = { Text(if (isAmharic) "የሰርቨር አድራሻ (API Base URL)" else "API Base URL") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = themeColors.textPrimary,
                                            unfocusedTextColor = themeColors.textPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            NetworkClient.updateBaseUrl(serverUrl.trim())
                                            viewModel.loadSurvey(1L)
                                            viewModel.loadSubmissions()
                                            Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = Color.White)
                                    ) {
                                        Text(if (isAmharic) "አድስና ተገናኝ" else "Update & Reconnect", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }
                4 -> {
                    // ── TAB 4: AI TUTOR & KNOWLEDGE DOCUMENTS ────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))

                            // AI Knowledge Overview Banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BgCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryIndigo.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SmartToy,
                                                    contentDescription = null,
                                                    tint = PrimaryIndigo,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isAmharic) "የ AI ማስተማሪያ ሰነዶች ማዕከል" else "AI Tutor & Documents",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = TextWhite
                                                )
                                                Text(
                                                    text = if (isAmharic) "የኢትዮጵያ ወሳኝ ኩነቶች ህግና መመሪያዎች" else "Ethiopian CRVS Legal Base & SOPs",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.loadAiKnowledge()
                                                Toast.makeText(context, if (isAmharic) "የ AI ማህደረ-ትውስታ ታድሷል" else "AI Memory refreshed", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh AI Memory",
                                                tint = AccentEmerald,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 4 KPI Stat Badges in 2 Rows
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Questions Learned
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary,
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "${aiStats?.questionsCount ?: uiState.flatQuestions.size}",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextWhite
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isAmharic) "የተማራቸው ጥያቄዎች" else "Curriculum Questions",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = PrimaryIndigo,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (isAmharic) "ከጥያቄ ባንክ የተወሰደ" else "Indexed from Question Bank",
                                                    fontSize = 9.sp,
                                                    color = TextMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // Documents Learned
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary,
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "${aiStats?.documentsCount ?: aiDocuments.size}",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentTeal
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isAmharic) "የስልጠና ሰነዶች" else "Training Manuals",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AccentTeal,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (isAmharic) "PDF, Word እና Text" else "PDF, DOCX & Text files",
                                                    fontSize = 9.sp,
                                                    color = TextMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Knowledge Characters
                                        val totalChars = aiStats?.totalKnowledgeChars ?: 0L
                                        val charFormatted = if (totalChars >= 1000) String.format("%.1fk", totalChars / 1000.0) else totalChars.toString()
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary,
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = charFormatted,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentInfo
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isAmharic) "የእውቀት ፊደላት" else "Knowledge Chars",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AccentInfo,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (isAmharic) "በማህደረ-ትውስታ የተያዙ" else "Stored in memory",
                                                    fontSize = 9.sp,
                                                    color = TextMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // Status
                                        val statusText = aiStats?.status ?: "ONLINE"
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary,
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = statusText,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentEmerald
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isAmharic) "የ AI ሁኔታ" else "AI Tutor Status",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AccentEmerald,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (isAmharic) "ለተፈታኞች ጥያቄ ዝግጁ" else "Ready for examinee chat",
                                                    fontSize = 9.sp,
                                                    color = TextMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Upload Document Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BgCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = AccentTeal,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAmharic) "የስልጠና ሰነድ ስቀል (PDF / Word)" else "Upload Training Document",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextWhite
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AccentTeal.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "PDF · DOCX · TXT",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentTeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Drop/Picker Area
                                    if (selectedDocUri == null) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary.copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    filePickerLauncher.launch("*/*")
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.UploadFile,
                                                    contentDescription = null,
                                                    tint = PrimaryIndigo,
                                                    modifier = Modifier.size(42.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (isAmharic) "ሰነድ ከስልክዎ ለመምረጥ እዚህ ይጫኑ" else "Tap here to select training document",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextWhite
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isAmharic) "አዋጅ 1370 PDF፣ የወሳኝ ኩነቶች Word (.docx) ማኑዋል ወይም ማስታወሻ" else "Supports Ethiopian Proclamation PDFs, Word (.docx) SOPs, and notes",
                                                    fontSize = 11.sp,
                                                    color = TextMuted,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = { filePickerLauncher.launch("*/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (isAmharic) "ፋይል ምረጥ" else "Browse File", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        // Selected File Preview Card
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary,
                                            border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isPdf = selectedDocName?.endsWith(".pdf", ignoreCase = true) == true
                                                val isDocx = selectedDocName?.endsWith(".docx", ignoreCase = true) == true || selectedDocName?.endsWith(".doc", ignoreCase = true) == true
                                                Icon(
                                                    imageVector = if (isPdf) Icons.Default.PictureAsPdf else if (isDocx) Icons.Default.Description else Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = if (isPdf) AccentDanger else if (isDocx) PrimaryIndigo else AccentEmerald,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = selectedDocName ?: "Selected File",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val sizeKb = (selectedDocSize ?: 0L) / 1024
                                                    Text(
                                                        text = if (sizeKb > 1024) "${String.format("%.1f", sizeKb / 1024.0)} MB" else "$sizeKb KB",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                                TextButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                                    Text(if (isAmharic) "ቀይር" else "Change", color = PrimaryIndigo, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Title field
                                    OutlinedTextField(
                                        value = docTitleInput,
                                        onValueChange = { docTitleInput = it },
                                        label = {
                                            Text(if (isAmharic) "የሰነዱ ርዕስ (Document Title)" else "Document Title / Label", fontSize = 12.sp)
                                        },
                                        placeholder = {
                                            Text("e.g. Proclamation 1370 Guidelines (የልደትና ሞት መመሪያ)", fontSize = 11.sp, color = TextDim)
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryIndigo,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedLabelColor = PrimaryIndigo,
                                            unfocusedLabelColor = TextMuted,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedContainerColor = BgSecondary,
                                            unfocusedContainerColor = BgSecondary
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Upload Button
                                    Button(
                                        onClick = {
                                            if (selectedDocUri == null) {
                                                Toast.makeText(context, if (isAmharic) "እባክዎ መጀመሪያ ሰነድ ይምረጡ" else "Please select a document first", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val uri = selectedDocUri!!
                                            val filename = selectedDocName ?: "document.pdf"
                                            val title = docTitleInput.trim().ifBlank { filename }
                                            isUploadingDoc = true

                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val stream = context.contentResolver.openInputStream(uri)
                                                    val bytes = stream?.use { it.readBytes() }
                                                    if (bytes == null || bytes.isEmpty()) {
                                                        withContext(Dispatchers.Main) {
                                                            isUploadingDoc = false
                                                            Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
                                                        }
                                                        return@launch
                                                    }
                                                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                                    withContext(Dispatchers.Main) {
                                                        viewModel.uploadAiDocument(filename, title, base64) { success, err ->
                                                            isUploadingDoc = false
                                                            if (success) {
                                                                Toast.makeText(context, if (isAmharic) "ሰነዱ በተሳካ ሁኔታ ተምሯል!" else "Document learned successfully by AI!", Toast.LENGTH_LONG).show()
                                                                selectedDocUri = null
                                                                selectedDocName = null
                                                                selectedDocSize = null
                                                                docTitleInput = ""
                                                            } else {
                                                                Toast.makeText(context, err ?: "Upload failed", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        isUploadingDoc = false
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isUploadingDoc,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isUploadingDoc) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isAmharic) "ሰነዱን በማንበብና እያስተማረ ነው..." else "Extracting & Learning...", fontSize = 13.sp)
                                        } else {
                                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isAmharic) "🚀 ሰነዱን ጨምርና AI አስተምር" else "Teach AI from Document", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Real-Time AI Tester Section
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BgCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAmharic) "የ AI ረዳቱን በቀጥታ ይሞክሩ" else "Test AI Tutor in Real-Time",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextWhite
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PrimaryIndigo.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "አማርኛ / EN",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryIndigo,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAmharic) "የተሰቀሉትን ሰነዶች እና የስርዓተ-ትምህርት ጥያቄዎች እንዴት እንደተማረ ለመሞከር ማንኛውንም ጥያቄ ይጠይቁ።" else "Ask any question to test how the AI combines curriculum questions with your uploaded training documents.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick prompt chips
                                    val testerChips = if (isAmharic) listOf(
                                        "👶 የልደት ምዝገባ ጊዜ ገደብ?",
                                        "⚰️ የሞት ምዝገባ አስፈላጊ ሰነዶች?",
                                        "📜 አዋጅ ቁጥር 1370/2024",
                                        "💍 የጋብቻ ምዝገባ ህጋዊ እድሜ?"
                                    ) else listOf(
                                        "👶 Birth registration deadline?",
                                        "⚰️ Death registration documents?",
                                        "📜 Proclamation 1370 summary?",
                                        "💍 Marriage legal age?"
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(testerChips) { chip ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = BgSecondary,
                                                border = BorderStroke(1.dp, BorderSubtle),
                                                modifier = Modifier.clickable {
                                                    testQuestionInput = chip
                                                }
                                            ) {
                                                Text(
                                                    text = chip,
                                                    fontSize = 10.5.sp,
                                                    color = TextWhite,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Input row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = testQuestionInput,
                                            onValueChange = { testQuestionInput = it },
                                            placeholder = {
                                                Text(
                                                    if (isAmharic) "ጥያቄዎን እዚህ ያስገቡ..." else "Ask any training question...",
                                                    fontSize = 11.5.sp,
                                                    color = TextDim
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryIndigo,
                                                unfocusedBorderColor = BorderSubtle,
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite,
                                                focusedContainerColor = BgSecondary,
                                                unfocusedContainerColor = BgSecondary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val q = testQuestionInput.trim()
                                                if (q.isBlank() || isTestingAi) return@Button
                                                isTestingAi = true
                                                scope.launch {
                                                    val reply = viewModel.askAiTutor(q)
                                                    testAiAnswer = reply
                                                    isTestingAi = false
                                                }
                                            },
                                            enabled = !isTestingAi && testQuestionInput.isNotBlank(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            if (isTestingAi) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                            } else {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    // AI Answer Preview
                                    if (testAiAnswer != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BgSecondary,
                                            border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (isAmharic) "የ AI መልስ" else "AI Tutor Answer",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = AccentEmerald
                                                        )
                                                    }
                                                    TextButton(onClick = { testAiAnswer = null }, contentPadding = PaddingValues(0.dp)) {
                                                        Text("✕", color = TextDim, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = testAiAnswer ?: "",
                                                    fontSize = 12.sp,
                                                    color = TextWhite,
                                                    lineHeight = 17.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Learned Documents Section Header
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isAmharic) "የተማራቸው ሰነዶች ዝርዝር (${aiDocuments.size})" else "Learned Documents (${aiDocuments.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (isAmharic) "ለተፈታኞች ስልጠና በ AI ማህደረ-ትውስታ የተያዙ" else "Active documents stored in AI knowledge base",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        // Documents List
                        if (aiDocuments.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = TextDim, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isAmharic) "እስካሁን የተሰቀለ ተጨማሪ ሰነድ የለም" else "No custom documents added yet",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isAmharic) "የስርዓተ-ትምህርት ጥያቄዎችና መደበኛ የወሳኝ ኩነቶች መመሪያዎች ብቻ እየሰሩ ነው።" else "Only curriculum questions and standard CRVS base rules are active.",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(aiDocuments) { doc ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BgCard,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isPdf = doc.fileType.equals("pdf", ignoreCase = true) || doc.filename.endsWith(".pdf", ignoreCase = true)
                                        val isDocx = doc.fileType.equals("docx", ignoreCase = true) || doc.filename.endsWith(".docx", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isPdf) AccentDanger.copy(alpha = 0.15f)
                                                    else if (isDocx) PrimaryIndigo.copy(alpha = 0.15f)
                                                    else AccentTeal.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPdf) Icons.Default.PictureAsPdf else if (isDocx) Icons.Default.Description else Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = if (isPdf) AccentDanger else if (isDocx) PrimaryIndigo else AccentTeal,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = BgSecondary
                                                ) {
                                                    Text(
                                                        text = doc.fileType.uppercase(),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isPdf) AccentDanger else if (isDocx) PrimaryIndigo else AccentTeal,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${doc.charCount} chars",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                                if (!doc.uploadedAt.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• ${doc.uploadedAt}",
                                                        fontSize = 10.sp,
                                                        color = TextDim,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = doc.filename,
                                                fontSize = 10.sp,
                                                color = TextDim,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { docToDelete = doc }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Document",
                                                tint = AccentDanger,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }

    // Delete Document Dialog (Admin control to remove knowledge document from AI memory)
    if (docToDelete != null) {
        val targetDoc = docToDelete!!
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = {
                Text(
                    text = if (isAmharic) "ሰነዱን ከ AI ማህደረ-ትውስታ ማጥፋት" else "Remove Document from AI Memory",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isAmharic) {
                            "እርግጠኛ ነዎት '${targetDoc.title}' የተሰኘውን ሰነድ ከ AI ማህደረ-ትውስታ ማጥፋት ይፈልጋሉ?"
                        } else {
                            "Are you sure you want to remove '${targetDoc.title}' from the AI knowledge base?"
                        },
                        color = TextWhite,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAmharic) {
                            "ይህ እርምጃ ሰነዱን ከ AI ትውስታ ያጠፋል፤ AI ሞግዚቱ ለተፈታኞች ጥያቄዎችን ሲመልስ ይህን ሰነድ መጠቀም ያቆማል።"
                        } else {
                            "This will remove the document text from AI memory and the AI Tutor will no longer reference it in answers."
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val docId = targetDoc.id
                        docToDelete = null
                        viewModel.deleteAiDocument(docId) { success, err ->
                            if (success) {
                                Toast.makeText(context, if (isAmharic) "ሰነዱ ከ AI ማህደረ-ትውስታ ተሰርዟል!" else "Document removed from AI memory!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "Failed to delete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text(if (isAmharic) "ሰርዝ" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text(if (isAmharic) "ተው" else "Cancel", color = TextWhite)
                }
            },
            containerColor = BgSecondary
        )
    }

    // Delete Submission Dialog (Manager / Admin control to reset examinee result)
    if (submissionToDelete != null) {
        val target = submissionToDelete!!
        AlertDialog(
            onDismissRequest = { submissionToDelete = null },
            title = {
                Text(
                    text = if (isAmharic) "ውጤት ሰርዝ / ድገም" else "Delete Submission Record",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isAmharic) {
                            "የተሳታፊ ${target.participantCode} (${target.testType}) የፈተና ውጤት መሰረዝ እርግጠኛ ነዎት?"
                        } else {
                            "Are you sure you want to delete the exam record for participant ${target.participantCode} (${target.testType})?"
                        },
                        color = TextWhite,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAmharic) {
                            "ይህ እርምጃ የተሳታፊውን የፈተና ውጤት ከስርዓቱ ያጠፋል፤ ተፈታኙ ፈተናውን በድጋሚ እንዲወስድ ያስችለዋል።"
                        } else {
                            "This action permanently removes this submission, allowing the participant to retake the exam."
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDeleteId = target.id
                        submissionToDelete = null
                        viewModel.deleteSubmission(toDeleteId) { success, err ->
                            if (success) {
                                Toast.makeText(context, if (isAmharic) "የፈተና ውጤቱ ተሰርዟል!" else "Exam record deleted successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: (if (isAmharic) "ስህተት ተከስቷል፤ ማጥፋት አልተቻለም" else "Failed to delete record"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text(if (isAmharic) "ሰርዝ" else "Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { submissionToDelete = null }) {
                    Text(if (isAmharic) "ተው" else "Cancel", color = TextWhite)
                }
            },
            containerColor = BgSecondary
        )
    }

    // Edit Question Dialog
    if (editingQuestion != null) {
        val q = editingQuestion!!
        var promptAm by remember { mutableStateOf(q.promptAm) }
        var promptEn by remember { mutableStateOf(q.promptEn) }
        var points by remember { mutableStateOf(q.points.toString()) }
        val initialOptions: List<QuestionOption> = q.options ?: emptyList()
        var editableOptions by remember { mutableStateOf(initialOptions) }

        Dialog(onDismissRequest = { if (!isSaving) editingQuestion = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BgSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (isAmharic) "ጥያቄ Q${q.questionNumber} ማስተካከያ" else "Edit Question Q${q.questionNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amharic Prompt
                    OutlinedTextField(
                        value = promptAm,
                        onValueChange = { promptAm = it },
                        label = { Text(if (isAmharic) "የጥያቄው ፅሁፍ (በአማርኛ)" else "Amharic Prompt") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // English Prompt
                    OutlinedTextField(
                        value = promptEn,
                        onValueChange = { promptEn = it },
                        label = { Text("English Translation") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Points
                    OutlinedTextField(
                        value = points,
                        onValueChange = { points = it },
                        label = { Text(if (isAmharic) "ነጥብ" else "Points") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Options Editor if MCQ
                    if (editableOptions.isNotEmpty()) {
                        Text(
                            text = "Options (Tap circle to set correct):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDim
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            editableOptions.forEachIndexed { index, opt ->
                                val isCorrect = opt.isCorrect
                                val key = opt.optionKey
                                val label = opt.labelAm

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCorrect) AccentEmerald.copy(alpha = 0.15f) else BgCard)
                                        .border(1.dp, if (isCorrect) AccentEmerald else BorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable {
                                            editableOptions = editableOptions.mapIndexed { i, o ->
                                                o.copy(isCorrect = i == index)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$key.",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect) AccentEmerald else TextWhite,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = TextWhite,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCorrect) {
                                        Text(
                                            text = "✓ Correct",
                                            fontSize = 11.sp,
                                            color = AccentEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { editingQuestion = null },
                            enabled = !isSaving
                        ) {
                            Text("Cancel", color = TextDim)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                isSaving = true
                                val finalOptions = if (editableOptions.isNotEmpty()) {
                                    editableOptions.mapIndexed { idx, o ->
                                        mapOf<String, Any>(
                                            "option_key" to o.optionKey,
                                            "label_am" to o.labelAm,
                                            "label_en" to o.labelEn,
                                            "is_correct" to o.isCorrect,
                                            "order_index" to idx
                                        )
                                    }
                                } else null

                                viewModel.updateQuestion(
                                    questionId = q.id,
                                    promptAm = promptAm.trim(),
                                    promptEn = promptEn.trim(),
                                    points = points.toIntOrNull() ?: q.points,
                                    type = q.type.name,
                                    options = finalOptions,
                                    rubricAm = q.rubricAm
                                ) { success, error ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "Question Q${q.questionNumber} updated live!", Toast.LENGTH_SHORT).show()
                                        editingQuestion = null
                                    } else {
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextWhite)
                            } else {
                                Text("Save Changes ✓")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = {
                Text(
                    text = if (isAmharic) "ከመለያ መውጣት" else "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = if (isAmharic) "እርግጠኛ ነዎት ከመለያዎ መውጣት ይፈልጋሉ?" else "Are you sure you want to sign out?",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text(if (isAmharic) "አዎ ውጣ" else "Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(if (isAmharic) "ይቅር" else "Cancel", color = TextWhite)
                }
            },
            containerColor = BgCard
        )
    }

    // ── Delete Question Confirmation Dialog ──────────────────────────────
    if (questionToDelete != null) {
        val q = questionToDelete!!
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = {
                Text("Delete Question Q${q.questionNumber}", fontWeight = FontWeight.Bold, color = TextWhite)
            },
            text = {
                Text("Are you sure you want to delete question Q${q.questionNumber}? This will remove it from all assessments.", color = TextMuted)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQuestion(q.id) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Question Q${q.questionNumber} deleted", Toast.LENGTH_SHORT).show()
                                questionToDelete = null
                            } else {
                                Toast.makeText(context, "Delete failed: $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToDelete = null }) {
                    Text("Cancel", color = TextWhite)
                }
            },
            containerColor = BgCard
        )
    }

    // ── Edit User Role Dialog ─────────────────────────────────────────────
    if (userToEditRole != null) {
        val targetUser = userToEditRole!!
        var selectedRole by remember { mutableStateOf(targetUser.role) }

        Dialog(onDismissRequest = { userToEditRole = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BgSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isAmharic) "የተጠቃሚ ሚና ማስተካከያ" else "Change User Role",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "User: @${targetUser.username} (${targetUser.weredaKebele ?: (if (isAmharic) "መዝጋቢ" else "Registrar")})",
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    listOf(
                        Triple(
                            "ADMIN",
                            if (isAmharic) "👑 ሥርዓት አስተዳዳሪ" else "👑 System Administrator",
                            if (isAmharic) "ሙሉ ፈቃድ፡ ጥያቄዎች፣ ውጤቶች እና ቅንብሮችን ይቆጣጠራል" else "Full permissions: Questions, results, exams & settings"
                        ),
                        Triple(
                            "USER_MANAGER",
                            if (isAmharic) "👥 የተጠቃሚዎች አስተዳዳሪ" else "👥 User Manager",
                            if (isAmharic) "የፈተና ውጤቶችን ማየት፣ መለያዎችን መቆጣጠር እና ማስተዳደር ይችላል" else "Can view exam results, manage accounts & monitor users"
                        ),
                        Triple(
                            "EXAMINEE",
                            if (isAmharic) "✍️ ተፈታኝ" else "✍️ Examinee",
                            if (isAmharic) "ቅድመና ድህረ ፈተናዎችን መውሰድ ይችላል" else "Access to take Pre & Post exams"
                        )
                    ).forEach { (rKey, rTitle, rDesc) ->
                        val isSelected = selectedRole == rKey
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryIndigo.copy(alpha = 0.18f) else BgCard,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = rKey }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedRole = rKey },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isSelected) TextWhite else TextMuted
                                    )
                                }
                                Text(
                                    text = rDesc,
                                    fontSize = 11.5.sp,
                                    color = TextDim,
                                    modifier = Modifier.padding(start = 36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { userToEditRole = null }) {
                            Text("Cancel", color = TextDim)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateUserRole(targetUser.id, selectedRole) { success, err ->
                                    if (success) {
                                        Toast.makeText(context, "Role updated to $selectedRole!", Toast.LENGTH_SHORT).show()
                                        userToEditRole = null
                                    } else {
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Role ✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── Delete User Confirmation Dialog ───────────────────────────────────
    if (userToDelete != null) {
        val targetUser = userToDelete!!
        val isSelf = targetUser.id == currentUser?.id

        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = if (isSelf) "Cannot Delete Self" else "Delete User Account",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = if (isSelf)
                        "You cannot delete your own account while you are signed in."
                    else
                        "Are you sure you want to permanently delete @${targetUser.username} (${targetUser.weredaKebele ?: "User"})? This action cannot be undone.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                if (!isSelf) {
                    Button(
                        onClick = {
                            viewModel.deleteUser(targetUser.id) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show()
                                    userToDelete = null
                                } else {
                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                    ) {
                        Text("Delete User", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(if (isSelf) "Close" else "Cancel", color = TextWhite)
                }
            },
            containerColor = BgCard
        )
    }

    // ── Add Question Dialog (Admin role can add ALL kinds of questions from mobile) ──
    if (showAddQuestionDialog) {
        val sections = uiState.survey?.sections ?: emptyList()
        var selectedSectionId by remember { mutableLongStateOf(sections.firstOrNull()?.id ?: 1L) }
        var questionType by remember { mutableStateOf("SINGLE_CHOICE") } // SINGLE_CHOICE, TEXT_OPEN, RATING_SCALE
        var promptAm by remember { mutableStateOf("") }
        var promptEn by remember { mutableStateOf("") }
        var pointsText by remember { mutableStateOf("1") }
        var rubricAm by remember { mutableStateOf("") }
        var rubricEn by remember { mutableStateOf("") }

        // Default options for MCQ
        var newOptions by remember {
            mutableStateOf(
                listOf(
                    Triple("A", "", true),
                    Triple("B", "", false),
                    Triple("C", "", false),
                    Triple("D", "", false)
                )
            )
        }

        Dialog(onDismissRequest = { if (!isSaving) showAddQuestionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BgSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Add New Question",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "አዲስ ጥያቄ ወደ ፈተናው ማካተቻ",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = { if (!isSaving) showAddQuestionDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDim)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Target Section Selector
                    Text(
                        text = if (isAmharic) "የጥያቄው ክፍል *" else "Target Section *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val availableSections = if (sections.isNotEmpty()) sections else listOf(
                            com.qcafr.gatherapp.data.model.Section(1L, 1L, "SECTION_A", "Part A - Legal Framework", "Part A - Legal Framework"),
                            com.qcafr.gatherapp.data.model.Section(2L, 1L, "SECTION_B", "Part B - Digital CRVS", "Part B - Digital CRVS"),
                            com.qcafr.gatherapp.data.model.Section(3L, 1L, "SECTION_C", "Part C - Practical Scenarios", "Part C - Practical Scenarios")
                        )
                        availableSections.forEach { sec ->
                            val isSecSelected = selectedSectionId == sec.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSecSelected) PrimaryIndigo.copy(alpha = 0.18f) else BgCard,
                                border = BorderStroke(1.dp, if (isSecSelected) PrimaryIndigo else BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSectionId = sec.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSecSelected,
                                        onClick = { selectedSectionId = sec.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sec.titleAm.takeIf { it.isNotBlank() } ?: sec.titleEn,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSecSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSecSelected) TextWhite else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Question Type Selector (Single Choice MCQ, Open Scenario, Rating Scale)
                    Text(
                        text = if (isAmharic) "የጥያቄ ዓይነት *" else "Question Type *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("SINGLE_CHOICE", if (isAmharic) "ምርጫ\n(MCQ)" else "Multiple\nChoice", Color(0xFF6366F1)),
                            Triple("TEXT_OPEN", if (isAmharic) "ክፍት\nሁኔታ" else "Scenario\n(Open)", Color(0xFF10B981)),
                            Triple("RATING_SCALE", if (isAmharic) "ደረጃ\n(Rating)" else "Rating\nScale", Color(0xFFF59E0B))
                        ).forEach { (typeVal, label, color) ->
                            val isTypeSelected = questionType == typeVal
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { questionType = typeVal },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isTypeSelected) color.copy(alpha = 0.2f) else BgCard,
                                border = BorderStroke(if (isTypeSelected) 2.dp else 1.dp, if (isTypeSelected) color else BorderSubtle)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isTypeSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isTypeSelected) TextWhite else TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Amharic Prompt
                    OutlinedTextField(
                        value = promptAm,
                        onValueChange = { promptAm = it },
                        label = { Text(if (isAmharic) "የጥያቄው ፅሁፍ (በአማርኛ) *" else "Question Prompt (Amharic) *") },
                        placeholder = { Text("የጥያቄውን ጽሑፍ እዚህ ያስገቡ...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = BgCard,
                            unfocusedContainerColor = BgCard,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. English Prompt
                    OutlinedTextField(
                        value = promptEn,
                        onValueChange = { promptEn = it },
                        label = { Text("English Translation (Optional)") },
                        placeholder = { Text("Enter English question text...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = BgCard,
                            unfocusedContainerColor = BgCard,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Points
                    OutlinedTextField(
                        value = pointsText,
                        onValueChange = { pointsText = it },
                        label = { Text(if (isAmharic) "ነጥብ (ነባሪ: 1)" else "Points (Default: 1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = BgCard,
                            unfocusedContainerColor = BgCard,
                            focusedTextColor = themeColors.textPrimary,
                            unfocusedTextColor = themeColors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6. Dynamic Type-Specific Editor
                    when (questionType) {
                        "SINGLE_CHOICE" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAmharic) "የምርጫ አማራጮች፡" else "Multiple Choice Options:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDim
                                )
                                TextButton(
                                    onClick = {
                                        val nextKey = ('A' + newOptions.size).toString()
                                        newOptions = newOptions + Triple(nextKey, "", false)
                                    }
                                ) {
                                    Text("+ Add Option", fontSize = 11.5.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = if (isAmharic) "ትክክለኛውን ይፋዊ መልስ ይምረጡ" else "Tap radio to mark the correct official answer",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                newOptions.forEachIndexed { index, (key, label, isCorrect) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCorrect) AccentEmerald.copy(alpha = 0.12f) else BgCard)
                                            .border(1.dp, if (isCorrect) AccentEmerald else BorderSubtle, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = isCorrect,
                                            onClick = {
                                                newOptions = newOptions.mapIndexed { i, opt ->
                                                    opt.copy(third = i == index)
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = AccentEmerald)
                                        )
                                        Text(
                                            text = "$key.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCorrect) AccentEmerald else TextWhite
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        OutlinedTextField(
                                            value = label,
                                            onValueChange = { newText ->
                                                newOptions = newOptions.mapIndexed { i, opt ->
                                                    if (i == index) opt.copy(second = newText) else opt
                                                }
                                            },
                                            placeholder = { Text("Choice $key text...", fontSize = 12.sp, color = TextDim) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = if (isCorrect) AccentEmerald else PrimaryIndigo,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedTextColor = themeColors.textPrimary,
                                                unfocusedTextColor = themeColors.textPrimary
                                            )
                                        )
                                        if (newOptions.size > 2) {
                                            IconButton(
                                                onClick = {
                                                    val updated = newOptions.filterIndexed { i, _ -> i != index }
                                                    val reindexed = updated.mapIndexed { i, opt ->
                                                        val k = ('A' + i).toString()
                                                        Triple(k, opt.second, opt.third)
                                                    }
                                                    newOptions = if (reindexed.none { it.third } && reindexed.isNotEmpty()) {
                                                        reindexed.mapIndexed { i, opt -> opt.copy(third = i == 0) }
                                                    } else reindexed
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextDim, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "TEXT_OPEN" -> {
                            Text(
                                text = if (isAmharic) "የነጥብ አሰጣጥ መመሪያ፡" else "Scoring Rubric:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDim
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = rubricAm,
                                onValueChange = { rubricAm = it },
                                label = { Text(if (isAmharic) "የማረሚያ መስፈርት (በአማርኛ)" else "Scoring Rubric (Amharic)") },
                                placeholder = { Text("የተሟላ መልስ መስፈርቶች እና የነጥብ ክፍፍል...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = BgCard,
                                    unfocusedContainerColor = BgCard,
                                    focusedTextColor = themeColors.textPrimary,
                                    unfocusedTextColor = themeColors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = rubricEn,
                                onValueChange = { rubricEn = it },
                                label = { Text("Rubric in English (Optional)") },
                                minLines = 1,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = BgCard,
                                    unfocusedContainerColor = BgCard,
                                    focusedTextColor = themeColors.textPrimary,
                                    unfocusedTextColor = themeColors.textPrimary
                                )
                            )
                        }

                        "RATING_SCALE" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BgCard,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "⭐ 1 to 5 Rating Scale",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = AccentAmber
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAmharic) "ተፈታኞች ደረጃቸውን ከ1 (በጣም አልስማማም) እስከ 5 (በጣም እስማማለሁ) ይመርጣሉ።" else "Examinees will select their level from 1 (Strongly Disagree) to 5 (Strongly Agree).",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddQuestionDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Cancel", color = TextDim)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                if (promptAm.isBlank()) {
                                    Toast.makeText(context, "Please enter the question prompt in Amharic", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (questionType == "SINGLE_CHOICE") {
                                    if (newOptions.any { it.second.isBlank() }) {
                                        Toast.makeText(context, "Please fill in all multiple choice options", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (newOptions.none { it.third }) {
                                        Toast.makeText(context, "Please select which choice is the correct answer", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                }

                                isSaving = true
                                val finalOptions = if (questionType == "SINGLE_CHOICE") {
                                    newOptions.mapIndexed { idx, (k, lbl, isCorr) ->
                                        mapOf<String, Any>(
                                            "option_key" to k,
                                            "label_am" to lbl.trim(),
                                            "label_en" to lbl.trim(),
                                            "is_correct" to isCorr,
                                            "order_index" to idx
                                        )
                                    }
                                } else null

                                viewModel.createQuestion(
                                    sectionId = selectedSectionId,
                                    type = questionType,
                                    promptAm = promptAm.trim(),
                                    promptEn = if (promptEn.isNotBlank()) promptEn.trim() else promptAm.trim(),
                                    points = pointsText.toIntOrNull() ?: 1,
                                    options = finalOptions,
                                    rubricAm = rubricAm.takeIf { it.isNotBlank() },
                                    rubricEn = rubricEn.takeIf { it.isNotBlank() }
                                ) { success, error ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "Question added to exam successfully! ✓", Toast.LENGTH_SHORT).show()
                                        showAddQuestionDialog = false
                                    } else {
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextWhite)
                            } else {
                                Text("Add Question ✓", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatChip(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UserCardItem(
    user: User,
    isCurrentUser: Boolean,
    onEditRole: () -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = user.role == "ADMIN"
    val isUserManager = user.role == "USER_MANAGER"

    val roleBadgeBg = when {
        isAdmin -> Color(0xFFE0F2F1)
        isUserManager -> Color(0xFFFEF3C7)
        else -> PrimaryIndigo.copy(alpha = 0.12f)
    }
    val roleBadgeText = when {
        isAdmin -> Color(0xFF00796B)
        isUserManager -> Color(0xFFB45309)
        else -> PrimaryIndigo
    }
    val roleLabel = when {
        isAdmin -> "ADMIN"
        isUserManager -> "USER MGR"
        else -> "EXAMINEE"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(roleBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isAdmin -> Icons.Default.AdminPanelSettings
                        isUserManager -> Icons.Default.Group
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = roleBadgeText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.weredaKebele?.takeIf { it.isNotBlank() } ?: user.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryIndigo.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "@${user.username} · Code: ${user.participantCode ?: "N/A"}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (user.fullName.isNotBlank()) {
                    Text(
                        text = "Name: ${user.fullName}",
                        fontSize = 11.sp,
                        color = TextDim,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Role Badge + Actions
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = roleBadgeBg,
                    modifier = Modifier.clickable { onEditRole() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            color = roleBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Role",
                            tint = roleBadgeText,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditRole,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = "Manage Role",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!isCurrentUser) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete User",
                                tint = AccentDanger,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

