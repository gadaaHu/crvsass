package com.qcafr.gatherapp.ui.screens.analytics

import com.qcafr.gatherapp.ui.components.CurvedFloatingNavBar
import com.qcafr.gatherapp.ui.components.CurvedNavItem
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.*
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    viewModel: QuestionnaireViewModel,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val analyticsState by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoadingAnalytics.collectAsState()
    val submissions by viewModel.submissions.collectAsState()
    val isAmharic = uiState.isAmharic
    val region = uiState.regionConfig
    val context = LocalContext.current

    // If analytics is not yet fetched or empty, compute local fallback
    val analytics = analyticsState ?: remember(submissions) {
        viewModel.computeLocalAnalytics(submissions)
    }

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val filterTabs = if (isAmharic) {
        listOf("አጠቃላይ እይታ", "የክፍሎች ብቃት", "የመመዝገቢያ ቦታዎች", "የጥያቄዎች ምርመራ")
    } else {
        listOf("Overview", "Competencies", "Jurisdictions", "Item Analysis")
    }

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
        if (submissions.isEmpty()) {
            viewModel.loadSubmissions()
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = AccentDanger,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isAmharic) "ከመለያ መውጣት" else "Sign Out",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isAmharic) "እርግጠኛ ነዎት ከመለያዎ መውጣት ይፈልጋሉ?" else "Are you sure you want to sign out?",
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
                    Text(if (isAmharic) "ሰርዝ" else "Cancel")
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAmharic) "የትንታኔ ዳሽቦርድ" else "Analytics Dashboard",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryIndigo.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${region.agencyName} · ${if (isAmharic) "የስልጠና ግምገማ መረጃ" else "Assessment Intelligence"}",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                            viewModel.loadAnalytics()
                            viewModel.loadSubmissions()
                            Toast.makeText(
                                context,
                                if (isAmharic) "ዳታ እየታደሰ ነው..." else "Refreshing analytics...",
                                Toast.LENGTH_SHORT
                            ).show()
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
                            onClick = { onBack() }
                        ),
                        CurvedNavItem(
                            id = "analytics",
                            icon = Icons.Default.Assessment,
                            title = if (isAmharic) "ዳሽቦርድ" else "Analytics",
                            hasBadge = false,
                            onClick = { /* Already on Analytics */ }
                        ),
                        CurvedNavItem(
                            id = "curriculum",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = if (isAmharic) "ስልጠና" else "Modules",
                            hasBadge = false,
                            onClick = { onBack() }
                        ),
                        CurvedNavItem(
                            id = "aichat",
                            icon = Icons.Default.SmartToy,
                            title = if (isAmharic) "AI ሞግዚት" else "AI Tutor",
                            hasBadge = uiState.isAiChatAllowed,
                            onClick = { onNavigateToAiChat() }
                        ),
                        CurvedNavItem(
                            id = "signout",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = if (isAmharic) "ውጣ" else "Sign Out",
                            hasBadge = false,
                            onClick = { showSignOutConfirm = true }
                        )
                    ),
                    selectedIndex = 1
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
            // Tab navigation for Dashboard views
            ScrollableTabRow(
                selectedTabIndex = selectedFilterIndex,
                containerColor = BgSecondary,
                contentColor = PrimaryIndigo,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = BorderSubtle, thickness = 1.dp) }
            ) {
                filterTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedFilterIndex == index,
                        onClick = { selectedFilterIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            if (isLoading && analyticsState == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (isAmharic) "የትንታኔ መረጃዎችን በመሰብሰብ ላይ..." else "Aggregating assessment analytics...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    when (selectedFilterIndex) {
                        0 -> {
                            // ── OVERVIEW TAB ───────────────────────────────────
                            item {
                                KeyMetricRibbon(
                                    analytics = analytics,
                                    isAmharic = isAmharic,
                                    isDarkMode = isDarkMode
                                )
                            }

                            item {
                                LearningGainBanner(
                                    analytics = analytics,
                                    isAmharic = isAmharic,
                                    isDarkMode = isDarkMode
                                )
                            }

                            item {
                                PerformanceBandsCard(
                                    bands = analytics.performanceBands,
                                    total = analytics.totalSubmissions,
                                    isAmharic = isAmharic,
                                    isDarkMode = isDarkMode
                                )
                            }

                            item {
                                CertificationPipelineCard(
                                    pipeline = analytics.certificationPipeline,
                                    isAmharic = isAmharic,
                                    isDarkMode = isDarkMode
                                )
                            }

                            item {
                                SectionCompetencyOverview(
                                    sections = analytics.sectionBreakdown,
                                    isAmharic = isAmharic,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }

                        1 -> {
                            // ── SECTION COMPETENCY BREAKDOWN ───────────────────
                            item {
                                SectionHeader(
                                    title = if (isAmharic) "የስልጠና ክፍሎች የብቃት ደረጃ" else "Curriculum Section Competency",
                                    subtitle = if (isAmharic) "የቅድመና ድህረ ፈተና ንፅፅር በየክፍሉ" else "Pre vs. Post test comparative mastery by section"
                                )
                            }

                            val sections = analytics.sectionBreakdown.ifEmpty {
                                listOf(
                                    SectionBreakdownItem(
                                        code = "SEC-A",
                                        titleAm = "የሲቪል እና የቤተሰብ ምዝገባ አዋጅ 1370/2017",
                                        titleEn = "Civil & Family Registration Proclamation 1370/2017",
                                        avgPrePct = 48.5f,
                                        avgPostPct = 76.2f,
                                        delta = 27.7f
                                    ),
                                    SectionBreakdownItem(
                                        code = "SEC-B",
                                        titleAm = "አዲሱ የዲጂታል ሲቪል ምዝገባ ሥርዓት",
                                        titleEn = "CRVS Digital Registration System",
                                        avgPrePct = 42.0f,
                                        avgPostPct = 71.8f,
                                        delta = 29.8f
                                    ),
                                    SectionBreakdownItem(
                                        code = "SEC-C",
                                        titleAm = "ተግባራዊ የሁኔታዎች ትንተና እና ውሳኔ አሰጣጥ",
                                        titleEn = "Practical Case Scenarios & Decisions",
                                        avgPrePct = 52.0f,
                                        avgPostPct = 74.5f,
                                        delta = 22.5f
                                    )
                                )
                            }

                            items(sections) { item ->
                                SectionCompetencyDetailCard(
                                    item = item,
                                    isAmharic = isAmharic,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }

                        2 -> {
                            // ── JURISDICTIONS & ROLES TAB ──────────────────────
                            val regions = analytics.regionBreakdown
                            if (regions.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = if (isAmharic) "የክልሎች አፈፃፀም ንፅፅር" else "Regional CRVS Jurisdictions",
                                        subtitle = if (isAmharic) "በኢትዮጵያ ክልሎች መካከል የተደረገ ንፅፅር" else "Comparative national breakdown across regional administrations"
                                    )
                                }
                                items(regions) { reg ->
                                    RegionPerformanceCard(
                                        item = reg,
                                        isAmharic = isAmharic,
                                        isDarkMode = isDarkMode
                                    )
                                }
                                item {
                                    Spacer(Modifier.height(14.dp))
                                }
                            }

                            item {
                                SectionHeader(
                                    title = if (isAmharic) "የመመዝገቢያ ቦታዎች አፈፃፀም" else "Registration Place & Facility Performance",
                                    subtitle = "${if (isAmharic) "የምዝገባ ቦታ" else "Place of Registration"}: ${region.place}"
                                )
                            }

                            val facilities = analytics.facilityBreakdown
                            if (facilities.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = if (isAmharic) "ምንም የተመዘገበ ቦታ የለም" else "No facility records yet",
                                        subtitle = if (isAmharic) "ፈተናዎች ሲወሰዱ በዚህ ክፍል ይዘረዘራሉ" else "Facility aggregations will appear as exams are taken"
                                    )
                                }
                            } else {
                                items(facilities) { fac ->
                                    FacilityPerformanceCard(
                                        item = fac,
                                        isAmharic = isAmharic,
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            item {
                                Spacer(Modifier.height(10.dp))
                                SectionHeader(
                                    title = if (isAmharic) "የባለሙያዎች የስራ ድርሻ ትንተና" else "Performance by Professional Role",
                                    subtitle = if (isAmharic) "የምዝገባ መኮንኖች እና አስተባባሪዎች ውጤት" else "Registrars, Supervisors and Managers breakdown"
                                )
                            }

                            val roles = analytics.roleBreakdown
                            if (roles.isNotEmpty()) {
                                items(roles) { r ->
                                    RolePerformanceCard(
                                        item = r,
                                        isAmharic = isAmharic,
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }
                        }

                        3 -> {
                            // ── QUESTION ITEM ANALYSIS & REVISION ALERTS ────────
                            item {
                                SectionHeader(
                                    title = if (isAmharic) "የጥያቄዎች ከባድነት እና የክለሳ ማንቂያዎች" else "Question Difficulty & Curriculum Review",
                                    subtitle = if (isAmharic) "ከ 60% በታች ትክክለኛ ምላሽ የተሰጠባቸው ጥያቄዎች" else "Questions flagged when post accuracy is below 60%"
                                )
                            }

                            val items = analytics.questionAccuracy
                            if (items.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = if (isAmharic) "ምንም የታገዘ የጥያቄ ምርመራ የለም" else "No item difficulty alerts",
                                        subtitle = if (isAmharic) "ሁሉም ጥያቄዎች አጥጋቢ የብቃት ደረጃ አሳይተዋል" else "Questions meet the baseline mastery threshold"
                                    )
                                }
                            } else {
                                items(items) { qItem ->
                                    QuestionAccuracyCard(
                                        item = qItem,
                                        isAmharic = isAmharic,
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENT WIDGETS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KeyMetricRibbon(
    analytics: AnalyticsSummary,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                modifier = Modifier.weight(1f),
                title = if (isAmharic) "ጠቅላላ ተፈታኞች" else "Total Submissions",
                value = "${analytics.totalSubmissions}",
                subText = "${analytics.preTestCount} Pre · ${analytics.postTestCount} Post",
                icon = Icons.Default.People,
                accentColor = PrimaryIndigo,
                isDarkMode = isDarkMode
            )
            MetricBox(
                modifier = Modifier.weight(1f),
                title = if (isAmharic) "የማለፍ ምጣኔ (ድህረ)" else "Post Pass Rate",
                value = "${analytics.passRatePost}%",
                subText = if (analytics.passRatePost >= 75) "Optimal" else "Needs Attention",
                icon = Icons.Default.CheckCircle,
                accentColor = AccentEmerald,
                isDarkMode = isDarkMode
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                modifier = Modifier.weight(1f),
                title = if (isAmharic) "ቅድመ-ፈተና አማካይ" else "Pre-Test Avg",
                value = String.format("%.1f%%", analytics.avgPreScore),
                subText = "${analytics.passRatePre}% Passed",
                icon = Icons.Default.History,
                accentColor = AccentAmber,
                isDarkMode = isDarkMode
            )
            MetricBox(
                modifier = Modifier.weight(1f),
                title = if (isAmharic) "ድህረ-ፈተና አማካይ" else "Post-Test Avg",
                value = String.format("%.1f%%", analytics.avgPostScore),
                subText = "+${String.format("%.1f%%", analytics.avgImprovementDelta)} Gain",
                icon = Icons.Default.TrendingUp,
                accentColor = AccentEmerald,
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
fun MetricBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    accentColor: Color,
    isDarkMode: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LearningGainBanner(
    analytics: AnalyticsSummary,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    val delta = analytics.avgImprovementDelta
    val isPositive = delta >= 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) Color(0xFF0F231D) else Color(0xFFECFDF5),
        border = BorderStroke(1.5.dp, AccentEmerald.copy(alpha = 0.6f)),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAmharic) "አጠቃላይ የስልጠና እድገት ለውጥ" else "Knowledge & Skill Gain (Delta)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) TextWhite else Color(0xFF065F46)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isAmharic)
                        "ተሳታፊዎች ከቅድመ ፈተና ወደ ድህረ ፈተና በአማካይ ያሳዩት ጭማሪ"
                    else
                        "Average competency increment achieved from baseline Pre-Test to Post-Test",
                    fontSize = 11.5.sp,
                    color = if (isDarkMode) TextMuted else Color(0xFF047857),
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentEmerald,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = if (isPositive) "+${String.format("%.1f%%", delta)}" else String.format("%.1f%%", delta),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PerformanceBandsCard(
    bands: PerformanceBandsSummary,
    total: Int,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    val totalCount = (bands.strong + bands.satisfactory + bands.needsReinforcement + bands.needsIntensiveSupport).coerceAtLeast(1)

    val pStrong = (bands.strong * 100f / totalCount)
    val pSat = (bands.satisfactory * 100f / totalCount)
    val pRein = (bands.needsReinforcement * 100f / totalCount)
    val pIntense = (bands.needsIntensiveSupport * 100f / totalCount)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAmharic) "የአፈፃፀም ደረጃዎች ክፍፍል" else "Performance Bands Distribution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                    Text(
                        text = if (isAmharic) "የተፈታኞች የብቃት ምደባ" else "Examinee mastery segmentation",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Text(
                    text = "$totalCount total",
                    fontSize = 12.sp,
                    color = TextDim,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Multi-segment horizontal bar gauge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(BorderSubtle)
            ) {
                if (pStrong > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pStrong)
                            .fillMaxHeight()
                            .background(AccentEmerald)
                    )
                }
                if (pSat > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pSat)
                            .fillMaxHeight()
                            .background(PrimaryIndigo)
                    )
                }
                if (pRein > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pRein)
                            .fillMaxHeight()
                            .background(AccentAmber)
                    )
                }
                if (pIntense > 0) {
                    Box(
                        modifier = Modifier
                            .weight(pIntense)
                            .fillMaxHeight()
                            .background(AccentDanger)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Breakdown legend rows
            BandLegendItem(
                label = if (isAmharic) "ከፍተኛ ብቃት (≥80%)" else "Strong (≥80%)",
                count = bands.strong,
                pct = pStrong,
                color = AccentEmerald
            )
            BandLegendItem(
                label = if (isAmharic) "አጥጋቢ (60%–79%)" else "Satisfactory (60%–79%)",
                count = bands.satisfactory,
                pct = pSat,
                color = PrimaryIndigo
            )
            BandLegendItem(
                label = if (isAmharic) "ማሻሻያ የሚያስፈልገው (40%–59%)" else "Needs Reinforcement (40%–59%)",
                count = bands.needsReinforcement,
                pct = pRein,
                color = AccentAmber
            )
            BandLegendItem(
                label = if (isAmharic) "ከፍተኛ ድጋፍ የሚሻ (<40%)" else "Needs Intensive Support (<40%)",
                count = bands.needsIntensiveSupport,
                pct = pIntense,
                color = AccentDanger
            )
        }
    }
}

@Composable
fun BandLegendItem(
    label: String,
    count: Int,
    pct: Float,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$count",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "(${String.format("%.1f%%", pct)})",
                fontSize = 11.5.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun CertificationPipelineCard(
    pipeline: CertificationPipelineSummary?,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    if (pipeline == null) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BgCard,
        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAmharic) "የምስክር ወረቀት አሰጣጥ ደረጃ" else "Certification Pipeline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextWhite
                    )
                    Text(
                        text = if (isAmharic) "ብቁ የሆኑ፣ የጸደቁና በመጠባበቅ ላይ ያሉ" else "Eligible, approved & pending verifications",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PipelineStatItem(
                    modifier = Modifier.weight(1f),
                    title = if (isAmharic) "ብቁ የሆኑ" else "Eligible",
                    value = "${pipeline.totalEligible}",
                    color = AccentEmerald
                )
                PipelineStatItem(
                    modifier = Modifier.weight(1f),
                    title = if (isAmharic) "የጸደቁ" else "Approved",
                    value = "${pipeline.totalApproved}",
                    color = PrimaryIndigo
                )
                PipelineStatItem(
                    modifier = Modifier.weight(1f),
                    title = if (isAmharic) "በመጠባበቅ ላይ" else "Pending",
                    value = "${pipeline.pendingApproval}",
                    color = AccentAmber
                )
            }
        }
    }
}

@Composable
fun PipelineStatItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionCompetencyOverview(
    sections: List<SectionBreakdownItem>,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (isAmharic) "የክፍሎች አጭር ማጠቃለያ" else "Competency Highlights by Section",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextWhite
            )
            Spacer(Modifier.height(12.dp))

            val displayList = sections.ifEmpty {
                listOf(
                    SectionBreakdownItem("SEC-A", "የሲቪል ምዝገባ አዋጅ", "Registration Proclamation", 48.5f, 76.2f, 27.7f),
                    SectionBreakdownItem("SEC-B", "የዲጂታል ሥርዓት", "CRVS Digital System", 42.0f, 71.8f, 29.8f),
                    SectionBreakdownItem("SEC-C", "ተግባራዊ የሁኔታዎች ትንተና", "Practical Scenarios", 52.0f, 74.5f, 22.5f)
                )
            }

            displayList.forEach { sec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAmharic) sec.titleAm else sec.titleEn,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Pre: ${String.format("%.1f%%", sec.avgPrePct)} → Post: ${String.format("%.1f%%", sec.avgPostPct)}",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }

                    val isPositive = sec.delta >= 0
                    val deltaBadgeColor = if (isPositive) AccentEmerald else AccentDanger
                    val deltaText = if (isPositive) "+${String.format("%.1f%%", sec.delta)}" else String.format("%.1f%%", sec.delta)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = deltaBadgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, deltaBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = deltaText,
                            color = deltaBadgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCompetencyDetailCard(
    item: SectionBreakdownItem,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryIndigo.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = item.code.ifBlank { "SECTION" },
                        color = PrimaryIndigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val isPositive = item.delta >= 0
                val deltaBadgeColor = if (isPositive) AccentEmerald else AccentDanger
                val deltaText = if (isPositive) "+${String.format("%.1f%%", item.delta)} Gain" else "${String.format("%.1f%%", item.delta)} Change"

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = deltaBadgeColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = deltaBadgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = deltaText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = deltaBadgeColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isAmharic) item.titleAm else item.titleEn,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextWhite,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(14.dp))

            // Pre-test bar
            CompetencyBar(
                label = if (isAmharic) "ቅድመ-ፈተና (Pre-Test)" else "Pre-Test Score",
                valuePct = item.avgPrePct,
                barColor = AccentAmber
            )

            Spacer(Modifier.height(8.dp))

            // Post-test bar
            CompetencyBar(
                label = if (isAmharic) "ድህረ-ፈተና (Post-Test)" else "Post-Test Score",
                valuePct = item.avgPostPct,
                barColor = AccentEmerald
            )
        }
    }
}

@Composable
fun CompetencyBar(
    label: String,
    valuePct: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.5.sp, color = TextMuted)
            Text(
                text = String.format("%.1f%%", valuePct),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BorderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((valuePct / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}

@Composable
fun RegionPerformanceCard(
    item: RegionBreakdownItem,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.codePrefix,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = PrimaryIndigo
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAmharic && item.amharicName.isNotBlank()) item.amharicName else item.regionName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.totalSubmissions} submissions · ${item.preCount} pre · ${item.postCount} post",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.avgScorePct >= 60f) AccentEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${String.format("%.1f", item.avgScorePct)}%",
                        color = if (item.avgScorePct >= 60f) AccentEmerald else AccentAmber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JurisdictionBadge(
                    modifier = Modifier.weight(1f),
                    label = if (isAmharic) "የማለፍ ምጣኔ" else "Pass Rate",
                    value = "${String.format("%.0f", item.passRatePct)}%"
                )
                JurisdictionBadge(
                    modifier = Modifier.weight(1f),
                    label = if (isAmharic) "የተረጋገጠላቸው" else "Certified",
                    value = "${item.certifiedCount}"
                )
            }
        }
    }
}

@Composable
fun FacilityPerformanceCard(
    item: FacilityBreakdownItem,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PrimaryIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.facility.ifBlank { "Unassigned" },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.participantCount} examinees · ${item.totalSubmissions} submissions",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.avgScorePct >= 60f) AccentEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${String.format("%.1f", item.avgScorePct)}%",
                        color = if (item.avgScorePct >= 60f) AccentEmerald else AccentAmber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JurisdictionBadge(
                    modifier = Modifier.weight(1f),
                    label = if (isAmharic) "የማለፍ ምጣኔ" else "Pass Rate",
                    value = "${String.format("%.0f", item.passRatePct)}%"
                )
                JurisdictionBadge(
                    modifier = Modifier.weight(1f),
                    label = if (isAmharic) "የተረጋገጠላቸው" else "Certified",
                    value = "${item.certifiedCount}"
                )
            }
        }
    }
}

@Composable
fun JurisdictionBadge(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = BgSecondary,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 11.5.sp, color = TextMuted)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
    }
}

@Composable
fun RolePerformanceCard(
    item: RoleBreakdownItem,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.role.ifBlank { "Registrar" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.totalCount} examinees",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Avg: ${String.format("%.1f%%", item.avgScorePct)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextWhite
                    )
                    Text(
                        text = "Pass: ${String.format("%.0f%%", item.passRatePct)}",
                        fontSize = 11.sp,
                        color = if (item.passRatePct >= 60) AccentEmerald else AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionAccuracyCard(
    item: QuestionAccuracyItem,
    isAmharic: Boolean,
    isDarkMode: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(
            1.dp,
            if (item.needsRevision) AccentDanger.copy(alpha = 0.5f) else BorderSubtle
        ),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryIndigo.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Q${item.questionNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryIndigo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.sectionCode.ifBlank { "SECTION" },
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (item.needsRevision) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentDanger.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AccentDanger.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentDanger,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isAmharic) "ክለሳ ይሻል" else "Review Needed",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentDanger
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isAmharic) item.promptAm else item.promptEn,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAmharic) "ቅድመ ትክክለኛነት" else "Pre Accuracy",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "${item.preAccuracyPct}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAmharic) "ድህረ ትክክለኛነት" else "Post Accuracy",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "${item.postAccuracyPct}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.postAccuracyPct >= 60) AccentEmerald else AccentDanger
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
fun EmptyStateCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextDim,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
