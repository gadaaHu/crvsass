package com.qcafr.gatherapp.ui.screens.participant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.Participant
import com.qcafr.gatherapp.data.preferences.RegionConfig
import com.qcafr.gatherapp.data.preferences.RegionPreferenceManager
import com.qcafr.gatherapp.ui.components.RatingScale
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

data class RegionJurisdiction(
    val regionName: String,
    val amharicName: String,
    val defaultWereda: String,
    val defaultKebele: String,
    val weredas: List<String>,
    val kebeles: List<String>,
    val codePrefix: String
)

private val ETHIOPIAN_JURISDICTIONS = listOf(
    RegionJurisdiction(
        regionName = "Dire Dawa",
        amharicName = "ድሬዳዋ",
        defaultWereda = "Sabian Wereda",
        defaultKebele = "Kebele 02",
        weredas = listOf("Sabian Wereda", "Genda Kore Wereda", "Melka Jebdu Wereda", "Dechatu Wereda", "B-Wereda", "Dire Dawa Rural Wereda"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Kebele 04", "Kebele 05", "Genda Kore", "Melka"),
        codePrefix = "DD"
    ),
    RegionJurisdiction(
        regionName = "Addis Ababa",
        amharicName = "አዲስ አበባ",
        defaultWereda = "Bole Subcity",
        defaultKebele = "Wereda 03",
        weredas = listOf("Bole Subcity", "Kirkos Subcity", "Arada Subcity", "Yeka Subcity", "Lideta Subcity", "Nifas Silk Subcity", "Kolfe Keranio Subcity", "Gullele Subcity", "Akaki Kality Subcity", "Addis Ketema Subcity"),
        kebeles = listOf("Wereda 01", "Wereda 02", "Wereda 03", "Wereda 04", "Wereda 05", "Wereda 06", "Wereda 07", "Wereda 08"),
        codePrefix = "AA"
    ),
    RegionJurisdiction(
        regionName = "Afar",
        amharicName = "አፋር",
        defaultWereda = "Semera Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Semera Wereda", "Asaita Wereda", "Dubti Wereda", "Awash 7 Kilo Wereda", "Logiya Wereda", "Mille Wereda"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Semera Town", "Dubti Central", "Logiya"),
        codePrefix = "AF"
    ),
    RegionJurisdiction(
        regionName = "Amhara",
        amharicName = "አማራ",
        defaultWereda = "Bahir Dar Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Bahir Dar Wereda", "Gondar Wereda", "Dessie Wereda", "Debre Birhan Wereda", "Woldiya Wereda", "Debre Markos Wereda", "Kombolcha Wereda"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Kebele 04", "Fasiledes", "Tana", "Abay"),
        codePrefix = "AM"
    ),
    RegionJurisdiction(
        regionName = "Benishangul-Gumuz",
        amharicName = "ቤንሻንጉል ጉሙዝ",
        defaultWereda = "Assosa Wereda",
        defaultKebele = "Assosa 01",
        weredas = listOf("Assosa Wereda", "Bambasi Wereda", "Kurmuk Wereda", "Menge Wereda", "Kamashi Wereda", "Metekel Wereda"),
        kebeles = listOf("Assosa 01", "Assosa 02", "Assosa 03", "Bambasi 01", "Kurmuk Town"),
        codePrefix = "BG"
    ),
    RegionJurisdiction(
        regionName = "Central Ethiopia",
        amharicName = "ማዕከላዊ ኢትዮጵያ",
        defaultWereda = "Hosanna Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Hosanna Wereda", "Butajira Wereda", "Worabe Wereda", "Hadiya Zone", "Gurage Zone", "Silte Zone", "Halaba Zone"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Hosanna Town", "Butajira Central"),
        codePrefix = "CE"
    ),
    RegionJurisdiction(
        regionName = "Gambela",
        amharicName = "ጋምቤላ",
        defaultWereda = "Gambela Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Gambela Wereda", "Itang Wereda", "Abobo Wereda", "Gog Wereda", "Jor Wereda", "Dima Wereda"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Kebele 04", "Kebele 05", "Itang Town"),
        codePrefix = "GM"
    ),
    RegionJurisdiction(
        regionName = "Harari",
        amharicName = "ሐረሪ",
        defaultWereda = "Harar Wereda",
        defaultKebele = "Abadir",
        weredas = listOf("Harar Wereda", "Amir Nur Wereda", "Shenkor Wereda", "Jin'Eala Wereda", "Sofi Wereda", "Erer Wereda"),
        kebeles = listOf("Abadir", "Amir Nur", "Shenkor", "Jin'Eala", "Sofi", "Erer", "Dire Teyara"),
        codePrefix = "HR"
    ),
    RegionJurisdiction(
        regionName = "Oromia",
        amharicName = "ኦሮሚያ",
        defaultWereda = "Adama Wereda",
        defaultKebele = "Ganda 01",
        weredas = listOf("Adama Wereda", "Bishoftu Wereda", "Jimma Wereda", "Shashemene Wereda", "Ambo Wereda", "Nekemte Wereda", "Batu Wereda", "Asella Wereda"),
        kebeles = listOf("Ganda 01", "Ganda 02", "Ganda 03", "Ganda 04", "Bole", "Melka", "Dukem"),
        codePrefix = "OR"
    ),
    RegionJurisdiction(
        regionName = "Sidama",
        amharicName = "ሲዳማ",
        defaultWereda = "Hawassa Wereda",
        defaultKebele = "Tabor",
        weredas = listOf("Hawassa Wereda", "Yirgalem Wereda", "Aleta Wondo Wereda", "Leku Wereda", "Dale Wereda", "Bensa Wereda"),
        kebeles = listOf("Tabor", "Menahariya", "Misrak", "Hayk Dar", "Bahil Adarash", "Mehal Ketema"),
        codePrefix = "SD"
    ),
    RegionJurisdiction(
        regionName = "Somali",
        amharicName = "ሶማሌ",
        defaultWereda = "Jigjiga Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Jigjiga Wereda", "Degehabur Wereda", "Gode Wereda", "Kebri Dehar Wereda", "Shinile Wereda", "Fafan Wereda"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Kebele 04", "Kebele 05", "Kebele 06"),
        codePrefix = "SM"
    ),
    RegionJurisdiction(
        regionName = "South Ethiopia",
        amharicName = "ደቡብ ኢትዮጵያ",
        defaultWereda = "Wolaita Sodo Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Wolaita Sodo Wereda", "Arba Minch Wereda", "Dilla Wereda", "Jinka Wereda", "Sawla Wereda", "Karat Wereda"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Kebele 03", "Wolaita Sodo", "Arba Minch", "Dilla"),
        codePrefix = "SE"
    ),
    RegionJurisdiction(
        regionName = "South West Ethiopia Peoples' Region",
        amharicName = "ደቡብ ምዕራብ ኢትዮጵያ",
        defaultWereda = "Bonga Wereda",
        defaultKebele = "Kebele 01",
        weredas = listOf("Bonga Wereda", "Mizan Aman Wereda", "Tepi Wereda", "Tercha Wereda", "Keffa Zone", "Bench Sheko Zone"),
        kebeles = listOf("Kebele 01", "Kebele 02", "Bonga", "Mizan Aman", "Tepi"),
        codePrefix = "SW"
    ),
    RegionJurisdiction(
        regionName = "Tigray",
        amharicName = "ትግራይ",
        defaultWereda = "Mekelle Wereda",
        defaultKebele = "Hawelti",
        weredas = listOf("Mekelle Wereda", "Axum Wereda", "Adigrat Wereda", "Shire Wereda", "Alamata Wereda", "Adwa Wereda"),
        kebeles = listOf("Hawelti", "Kedamay Weyane", "Ayder", "Hadnet", "Semien", "Quiha"),
        codePrefix = "TG"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantScreen(
    viewModel: QuestionnaireViewModel,
    onContinueToQuestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val regionManager = remember { RegionPreferenceManager.getInstance(context) }
    val uiState by viewModel.uiState.collectAsState()
    val isAmharic = uiState.isAmharic
    val region = uiState.regionConfig

    var regionOrCity by remember(region.regionName) { mutableStateOf(region.regionName) }
    var currentJurisdiction by remember(regionOrCity) {
        mutableStateOf(
            ETHIOPIAN_JURISDICTIONS.find { it.regionName.equals(regionOrCity, ignoreCase = true) }
                ?: ETHIOPIAN_JURISDICTIONS.first()
        )
    }

    var wereda by remember(currentJurisdiction.regionName) { mutableStateOf(currentJurisdiction.defaultWereda) }
    var kebele by remember(currentJurisdiction.regionName) { mutableStateOf(currentJurisdiction.defaultKebele) }
    var country by remember(region.country) { mutableStateOf(region.country) }
    var participantCode by remember(currentJurisdiction.codePrefix) { mutableStateOf("${currentJurisdiction.codePrefix}-REG-") }

    var isRegionDropdownExpanded by remember { mutableStateOf(false) }
    var isWeredaDropdownExpanded by remember { mutableStateOf(false) }
    var isGenderDropdownExpanded by remember { mutableStateOf(false) }
    var isExperienceDropdownExpanded by remember { mutableStateOf(false) }

    val ethiopianRegions = remember { ETHIOPIAN_JURISDICTIONS }

    val genderOptions = remember {
        listOf(
            "Female (ሴት)",
            "Male (ወንድ)"
        )
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val isManagerOrAdmin = currentUser?.role == "ADMIN" || currentUser?.role == "USER_MANAGER"
    val isPostTestAllowed = isManagerOrAdmin || uiState.isPostTestUnlocked
    var showPostTestLockedDialog by remember { mutableStateOf(false) }


    var selectedRole by remember { mutableStateOf("መዝጋቢ (Registrar)") }
    var selectedGender by remember { mutableStateOf("Female (ሴት)") }
    var experienceYears by remember { mutableStateOf("2.5") }
    var testType by remember(uiState.testType, isPostTestAllowed) {
        mutableStateOf(if (isPostTestAllowed) uiState.testType else "PRE")
    }
    var confidenceDigital by remember { mutableStateOf<Int?>(4) }
    var confidenceLaw by remember { mutableStateOf<Int?>(4) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val kebeleSuggestions = remember(region.kebeleSuggestions) { region.getKebeleList() }

    val themeColors = MaterialTheme.appColors

    if (showPostTestLockedDialog) {
        AlertDialog(
            onDismissRequest = { showPostTestLockedDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber) },
            title = {
                Text(
                    text = if (isAmharic) "ድህረ-ሙከራ ተቆልፏል" else "Post-Test Locked",
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary
                )
            },
            text = {
                Text(
                    text = if (isAmharic) "ተፈታኞች ድህረ-ሙከራ መውሰድ የሚችሉት ስልጠናው ተጠናቆ በስራ አስኪያጅ ወይም በአስተዳዳሪ ሲፈቀድ ብቻ ነው።\n\nእባክዎ አሁን ቅድመ-ሙከራውን ያጠናቁ።" else "Examinees can only take the Post-Test after training completion once explicitly authorized by a Manager or Admin.\n\nPlease complete the Pre-Test first.",
                    fontSize = 13.5.sp,
                    color = themeColors.textMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        testType = "PRE"
                        showPostTestLockedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("ቅድመ-ሙከራ ቀጥል (Continue Pre-Test)", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.bgPrimary)
    ) {
        // ── Pinned Header (Not Movable) ──────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = themeColors.bgCard,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isAmharic) "የተሳታፊ መረጃ" else "Participant Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColors.textPrimary
                )
                Text(
                    text = "እባክዎ መመዘኛውን ከመጀመርዎ በፊት መረጃዎን ያሟሉ",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.textMuted
                )
            }
        }
        HorizontalDivider(color = themeColors.borderSubtle, thickness = 0.8.dp)

        // ── Scrollable Form Body ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

        // Assessment Mode Selector (Pre vs Post)
        Text(
            text = if (isAmharic) "የሙከራ ዓይነት *" else "Assessment Mode *",
            color = themeColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeSelectPill(
                title = "⏱️ ቅድመ-ሙከራ (Pre-Test)",
                isSelected = testType == "PRE",
                onClick = { testType = "PRE" },
                modifier = Modifier.weight(1f)
            )
            ModeSelectPill(
                title = if (isPostTestAllowed) "🎯 ድህረ-ሙከራ (Post-Test)" else "🔒 ድህረ-ሙከራ (Locked)",
                isSelected = testType == "POST",
                onClick = {
                    if (isPostTestAllowed) {
                        testType = "POST"
                    } else {
                        showPostTestLockedDialog = true
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (!isPostTestAllowed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isAmharic) "ℹ️ ድህረ-ሙከራው በአስተዳዳሪ ወይም ኃላፊ እስኪፈቀድ ዝግ ነው" else "ℹ️ Post-test is locked until authorized by manager",
                color = themeColors.textDim,
                fontSize = 11.5.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Participant Code Input
        Text(text = if (isAmharic) "የተሳታፊ ኮድ *" else "Participant Code *", color = themeColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = participantCode,
            onValueChange = { participantCode = it },
            placeholder = { Text("e.g. DD-REG-008", color = themeColors.textDim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = themeColors.borderSubtle,
                focusedTextColor = themeColors.textPrimary,
                unfocusedTextColor = themeColors.textPrimary,
                cursorColor = PrimaryIndigo,
                focusedContainerColor = themeColors.bgCard,
                unfocusedContainerColor = themeColors.bgCard
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── PLACE OF REGISTRATION (የምዝገባ ቦታ) ────────────────────────
        Text(
            text = if (isAmharic) "የምዝገባ ቦታ (አገር፣ ክልል/ከተማ፣ ወረዳ፣ ቀበሌ)" else "Place of Registration (Country, Region, Wereda, Kebele)",
            color = themeColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )
        Text(
            text = "ቀበሌ፣ ወረዳ፣ ክልል (አማራጭ) እና አገርዎን ያረጋግጡ",
            color = themeColors.textDim,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ── 1. Region & Country (Primary Jurisdiction Selection) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = isRegionDropdownExpanded,
                onExpandedChange = { isRegionDropdownExpanded = !isRegionDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = "${currentJurisdiction.regionName} (${currentJurisdiction.amharicName})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isAmharic) "ክልል/ከተማ *" else "Region/City *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRegionDropdownExpanded) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = themeColors.borderSubtle,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        cursorColor = PrimaryIndigo,
                        focusedContainerColor = themeColors.bgCard,
                        unfocusedContainerColor = themeColors.bgCard
                    )
                )
                ExposedDropdownMenu(
                    expanded = isRegionDropdownExpanded,
                    onDismissRequest = { isRegionDropdownExpanded = false }
                ) {
                    ethiopianRegions.forEach { jurisdiction ->
                        val isCurrent = regionOrCity.equals(jurisdiction.regionName, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${jurisdiction.regionName} (${jurisdiction.amharicName})",
                                    color = if (isCurrent) PrimaryIndigo else themeColors.textPrimary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                regionOrCity = jurisdiction.regionName
                                currentJurisdiction = jurisdiction
                                // Automatically match Wereda and Kebele to the selected Region!
                                wereda = jurisdiction.defaultWereda
                                kebele = jurisdiction.defaultKebele
                                participantCode = "${jurisdiction.codePrefix}-REG-"
                                isRegionDropdownExpanded = false

                                // Update global region preferences
                                val preset = RegionPreferenceManager.PRESETS.find {
                                    it.regionName.equals(jurisdiction.regionName, ignoreCase = true)
                                } ?: RegionConfig(regionName = jurisdiction.regionName, agencyName = "${jurisdiction.regionName} CRVS Agency")
                                viewModel.setRegion(regionManager, preset)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text(if (isAmharic) "አገር" else "Country") },
                placeholder = { Text("e.g. Ethiopia", color = themeColors.textDim) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = themeColors.borderSubtle,
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = themeColors.bgCard,
                    unfocusedContainerColor = themeColors.bgCard
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 2. Wereda & Kebele (Matched to selected Region) ──────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Wereda Dropdown & Input
            ExposedDropdownMenuBox(
                expanded = isWeredaDropdownExpanded,
                onExpandedChange = { isWeredaDropdownExpanded = !isWeredaDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = wereda,
                    onValueChange = { wereda = it },
                    label = { Text(if (isAmharic) "ወረዳ *" else "Wereda *") },
                    placeholder = { Text(currentJurisdiction.defaultWereda, color = themeColors.textDim) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWeredaDropdownExpanded) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = themeColors.borderSubtle,
                        focusedTextColor = themeColors.textPrimary,
                        unfocusedTextColor = themeColors.textPrimary,
                        cursorColor = PrimaryIndigo,
                        focusedContainerColor = themeColors.bgCard,
                        unfocusedContainerColor = themeColors.bgCard
                    )
                )
                ExposedDropdownMenu(
                    expanded = isWeredaDropdownExpanded,
                    onDismissRequest = { isWeredaDropdownExpanded = false }
                ) {
                    currentJurisdiction.weredas.forEach { wOption ->
                        DropdownMenuItem(
                            text = { Text(wOption, color = themeColors.textPrimary) },
                            onClick = {
                                wereda = wOption
                                isWeredaDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Kebele Input
            OutlinedTextField(
                value = kebele,
                onValueChange = { kebele = it },
                label = { Text(if (isAmharic) "ቀበሌ *" else "Kebele *") },
                placeholder = { Text(currentJurisdiction.defaultKebele, color = themeColors.textDim) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = themeColors.borderSubtle,
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = themeColors.bgCard,
                    unfocusedContainerColor = themeColors.bgCard
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestion Chips for Kebele & Facilities in this Region
        Text(
            text = "⚡ ፈጣን የቀበሌ እና ወረዳ ምርጫ (${currentJurisdiction.regionName}):",
            color = themeColors.textDim,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(currentJurisdiction.kebeles) { suggestion ->
                val isSelected = kebele.equals(suggestion, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else themeColors.bgCard,
                    border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else themeColors.borderSubtle),
                    modifier = Modifier.clickable { kebele = suggestion }
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.5.sp,
                        color = if (isSelected) PrimaryIndigo else themeColors.textMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Live Place Preview Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = themeColors.bgCard,
            border = BorderStroke(1.dp, themeColors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isAmharic) "📍 የምዝገባ ቦታ፡ $kebele, $wereda, ${currentJurisdiction.regionName}, $country" else "📍 Place: $kebele, $wereda, ${currentJurisdiction.regionName}, $country",
                fontSize = 11.5.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Gender (Dropdown Menu Selection & Quick Touch Pills)
        Text(text = if (isAmharic) "ፆታ *" else "Sex / Gender *", color = themeColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = isGenderDropdownExpanded,
            onExpandedChange = { isGenderDropdownExpanded = !isGenderDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedGender,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (isAmharic) "ፆታ *" else "Sex / Gender *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderDropdownExpanded) },
                singleLine = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = themeColors.borderSubtle,
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = themeColors.bgCard,
                    unfocusedContainerColor = themeColors.bgCard
                )
            )
            ExposedDropdownMenu(
                expanded = isGenderDropdownExpanded,
                onDismissRequest = { isGenderDropdownExpanded = false }
            ) {
                genderOptions.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g, color = themeColors.textPrimary) },
                        onClick = {
                            selectedGender = g
                            isGenderDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeSelectPill(
                title = "👩 ሴት (Female)",
                isSelected = selectedGender.contains("Female") || selectedGender.contains("ሴት"),
                onClick = { selectedGender = "Female (ሴት)" },
                modifier = Modifier.weight(1f)
            )
            ModeSelectPill(
                title = "🧑 ወንድ (Male)",
                isSelected = selectedGender.contains("Male") || selectedGender.contains("ወንድ"),
                onClick = { selectedGender = "Male (ወንድ)" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Experience Years - Dropdown
        Text(text = if (isAmharic) "የሥራ ዓመት" else "Years of Experience", color = themeColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = isExperienceDropdownExpanded,
            onExpandedChange = { isExperienceDropdownExpanded = !isExperienceDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = if (experienceYears.isBlank()) "" else "$experienceYears ዓመት (Years)",
                onValueChange = {},
                readOnly = true,
                label = { Text(if (isAmharic) "የሥራ ዓመት" else "Years of Experience") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExperienceDropdownExpanded) },
                singleLine = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(color = themeColors.textPrimary, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = themeColors.borderSubtle,
                    focusedTextColor = themeColors.textPrimary,
                    unfocusedTextColor = themeColors.textPrimary,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = themeColors.bgCard,
                    unfocusedContainerColor = themeColors.bgCard
                )
            )
            ExposedDropdownMenu(
                expanded = isExperienceDropdownExpanded,
                onDismissRequest = { isExperienceDropdownExpanded = false }
            ) {
                listOf("1", "2", "3", "5", "10", "15", "20+").forEach { yrs ->
                    DropdownMenuItem(
                        text = { Text("$yrs ዓመት (Years)", color = themeColors.textPrimary) },
                        onClick = {
                            experienceYears = yrs
                            isExperienceDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Confidence Ratings
        Text(
            text = if (isAmharic) "በራስ የመተማመን ደረጃ፦ ዲጂታል ምዝገባ ሥርዓቱን በመጠቀም" else "Confidence Level: Using Digital CRVS System",
            color = themeColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        RatingScale(
            selectedRating = confidenceDigital,
            onRatingSelected = { confidenceDigital = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isAmharic) "በራስ የመተማመን ደረጃ፦ አዋጅ ቁጥር 1370/2024ን በመተግበር" else "Confidence Level: Implementing Proclamation 1370/2024",
            color = themeColors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        RatingScale(
            selectedRating = confidenceLaw,
            onRatingSelected = { confidenceLaw = it }
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage!!, color = AccentDanger, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Spacer(modifier = Modifier.height(20.dp))
    }

    // ── Pinned Footer (Not Movable) ──────────────────────────────────────────
    HorizontalDivider(color = themeColors.borderSubtle, thickness = 0.8.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = themeColors.bgCard,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Button(
                onClick = {
                    val fullPlace = region.formatPlaceOfRegistration(
                        kebele = kebele.trim(),
                        wereda = wereda.trim(),
                        regOrCity = regionOrCity.trim(),
                        ctry = country.trim()
                    )
                    if (participantCode.isBlank() || participantCode.trim() == "${currentJurisdiction.codePrefix}-REG-") {
                        errorMessage = if (isAmharic) "እባክዎ የተሳታፊ ኮድ ያስገቡ" else "Please enter participant code"
                        return@Button
                    }
                    val activeTestType = if (isPostTestAllowed) testType else "PRE"
                    viewModel.setParticipant(
                        participant = Participant(
                            participantCode = participantCode.trim(),
                            weredaKebele = fullPlace,
                            role = selectedRole,
                            gender = selectedGender,
                            experienceYears = experienceYears.toFloatOrNull() ?: 1f
                        ),
                        testType = activeTestType,
                        confidenceDigital = confidenceDigital,
                        confidenceLaw = confidenceLaw
                    )
                    onContinueToQuestions()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = if (isAmharic) "ጥያቄዎችን መጠየቅ ጀምር →" else "Start Assessment →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}
}

@Composable
fun ModeSelectPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = MaterialTheme.appColors
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryIndigo.copy(alpha = 0.18f) else themeColors.bgCard,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PrimaryIndigo else themeColors.borderSubtle
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (isSelected) PrimaryIndigo else themeColors.textMuted,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
