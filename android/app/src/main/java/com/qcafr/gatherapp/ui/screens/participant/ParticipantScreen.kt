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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.Participant
import com.qcafr.gatherapp.ui.components.RatingScale
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantScreen(
    viewModel: QuestionnaireViewModel,
    onContinueToQuestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val region = uiState.regionConfig
    val defaultPrefix = region.regionName.trim().take(2).uppercase().ifBlank { "RG" }

    var participantCode by remember(defaultPrefix) { mutableStateOf("$defaultPrefix-REG-") }
    var kebele by remember(region.defaultKebele) { mutableStateOf(region.defaultKebele) }
    var wereda by remember(region.defaultWereda) { mutableStateOf(region.defaultWereda) }
    var regionOrCity by remember(region.regionName) { mutableStateOf(region.regionName) }
    var country by remember(region.country) { mutableStateOf(region.country) }

    var isRegionDropdownExpanded by remember { mutableStateOf(false) }
    var isGenderDropdownExpanded by remember { mutableStateOf(false) }
    var isExperienceDropdownExpanded by remember { mutableStateOf(false) }

    val ethiopianRegions = remember {
        listOf(
            "Afar",
            "Amhara",
            "Benishangul-Gumuz",
            "Central Ethiopia",
            "Gambela",
            "Harari",
            "Oromia",
            "Sidama",
            "Somali",
            "South Ethiopia",
            "South West Ethiopia Peoples' Region",
            "Tigray",
            "Addis Ababa",
            "Dire Dawa"
        )
    }

    val genderOptions = remember {
        listOf(
            "Female (ሴት)",
            "Male (ወንድ)"
        )
    }

    var selectedRole by remember { mutableStateOf("መዝጋቢ (Registrar)") }
    var selectedGender by remember { mutableStateOf("Female (ሴት)") }
    var experienceYears by remember { mutableStateOf("2.5") }
    var testType by remember { mutableStateOf("POST") }
    var confidenceDigital by remember { mutableStateOf<Int?>(4) }
    var confidenceLaw by remember { mutableStateOf<Int?>(4) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val kebeleSuggestions = remember(region.kebeleSuggestions) { region.getKebeleList() }

    val themeColors = MaterialTheme.appColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeColors.bgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "የተሳታፊ መረጃ (Participant Details)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = themeColors.textPrimary
        )
        Text(
            text = "እባክዎ መመዘኛውን ከመጀመርዎ በፊት መረጃዎን ያሟሉ",
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.textMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Assessment Mode Selector (Pre vs Post)
        Text(
            text = "የሙከራ ዓይነት (Assessment Mode) *",
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
                title = "🎯 ድህረ-ሙከራ (Post-Test)",
                isSelected = testType == "POST",
                onClick = { testType = "POST" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Participant Code Input
        Text(text = "የተሳታፊ ኮድ (Participant Code) *", color = themeColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
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
            text = "የምዝገባ ቦታ (Place of Registration: Country, Region/City, Wereda, Kebele)",
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

        // Kebele & Wereda Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = kebele,
                onValueChange = { kebele = it },
                label = { Text("Kebele (ቀበሌ) *") },
                placeholder = { Text("e.g. Kebele 02", color = themeColors.textDim) },
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

            OutlinedTextField(
                value = wereda,
                onValueChange = { wereda = it },
                label = { Text("Wereda (ወረዳ) *") },
                placeholder = { Text("e.g. Wereda 01", color = themeColors.textDim) },
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

        // Region / City (Optional) & Country Row
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
                    value = regionOrCity,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Region/City (ክልል) *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRegionDropdownExpanded) },
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
                    expanded = isRegionDropdownExpanded,
                    onDismissRequest = { isRegionDropdownExpanded = false }
                ) {
                    ethiopianRegions.forEach { reg ->
                        DropdownMenuItem(
                            text = { Text(reg, color = themeColors.textPrimary) },
                            onClick = {
                                regionOrCity = reg
                                isRegionDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country (አገር)") },
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

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestion Chips for Kebele
        if (kebeleSuggestions.isNotEmpty()) {
            Text(
                text = "⚡ ፈጣን የቀበሌ ምርጫ (Quick Kebele Suggestions):",
                color = themeColors.textDim,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(kebeleSuggestions) { suggestion ->
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
        }

        // Live Place Preview Pill
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = themeColors.bgCard,
            border = BorderStroke(1.dp, themeColors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📍 የምዝገባ ቦታ (Place): " + region.formatPlaceOfRegistration(
                    kebele = kebele,
                    wereda = wereda,
                    regOrCity = regionOrCity,
                    ctry = country
                ),
                fontSize = 11.5.sp,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Gender (Dropdown Menu Selection & Quick Touch Pills)
        Text(text = "ፆታ (Sex / Gender) *", color = themeColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
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
                label = { Text("Sex / Gender (ፆታ) *") },
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
        Text(text = "የሥራ ዓመት (Years of Experience)", color = themeColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
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
                label = { Text("Years of Experience (የሥራ ዓመት)") },
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
            text = "በራስ የመተማመን ደረጃ፦ ዲጂታል ምዝገባ ሥርዓቱን በመጠቀም (Confidence: Digital)",
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
            text = "በራስ የመተማመን ደረጃ፦ አዋጅ ቁጥር 1370/2017ን በመተግበር (Confidence: Law)",
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

        Button(
            onClick = {
                val fullPlace = region.formatPlaceOfRegistration(
                    kebele = kebele.trim(),
                    wereda = wereda.trim(),
                    regOrCity = regionOrCity.trim(),
                    ctry = country.trim()
                )
                if (participantCode.isBlank() || participantCode.trim() == "$defaultPrefix-REG-") {
                    errorMessage = "እባክዎ የተሳታፊ ኮድ ያስገቡ (Please enter participant code)"
                    return@Button
                }
                viewModel.setParticipant(
                    participant = Participant(
                        participantCode = participantCode.trim(),
                        weredaKebele = fullPlace,
                        role = selectedRole,
                        gender = selectedGender,
                        experienceYears = experienceYears.toFloatOrNull() ?: 1f
                    ),
                    testType = testType,
                    confidenceDigital = confidenceDigital,
                    confidenceLaw = confidenceLaw
                )
                onContinueToQuestions()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "ጥያቄዎችን መጠየቅ ጀምር (Start Asking) →",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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
