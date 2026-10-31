package com.qcafr.gatherapp.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.SignUpRequest
import com.qcafr.gatherapp.data.preferences.AuthPreferenceManager
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: QuestionnaireViewModel,
    authManager: AuthPreferenceManager,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val region = uiState.regionConfig

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var selectedRole by remember { mutableStateOf("EXAMINEE") } // "EXAMINEE" or "ADMIN"
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var kebele by remember(region.defaultKebele) { mutableStateOf(region.defaultKebele) }
    var wereda by remember(region.defaultWereda) { mutableStateOf(region.defaultWereda) }
    var regionOrCity by remember(region.regionName) { mutableStateOf(region.regionName) }
    var country by remember(region.country) { mutableStateOf(region.country) }
    var gender by remember { mutableStateOf("ወንድ (Male)") }
    var experienceYears by remember { mutableStateOf("2.0") }
    val kebeleSuggestions = remember(region.kebeleSuggestions) { region.getKebeleList() }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doSignUp() {
        if (fullName.isBlank()) {
            errorMessage = "Full name is required (እባክዎ ሙሉ ስም ያስገቡ)"
            return
        }
        if (username.isBlank() || username.length < 3) {
            errorMessage = "Username must be at least 3 characters (የተጠቃሚ ስም ቢያንስ 3 ፊደላት)"
            return
        }
        if (password.length < 4) {
            errorMessage = "Password must be at least 4 characters (የይለፍ ቃል ቢያንስ 4 ፊደላት)"
            return
        }

        isLoading = true
        errorMessage = null

        val fullPlace = region.formatPlaceOfRegistration(
            kebele = kebele.trim(),
            wereda = wereda.trim(),
            regOrCity = regionOrCity.trim(),
            ctry = country.trim()
        )

        val req = SignUpRequest(
            username = username.trim().lowercase(),
            password = password,
            fullName = fullName.trim(),
            role = selectedRole,
            weredaKebele = if (selectedRole == "EXAMINEE") fullPlace else null,
            gender = if (selectedRole == "EXAMINEE") gender else null,
            experienceYears = if (selectedRole == "EXAMINEE") experienceYears.toFloatOrNull() ?: 1.0f else null
        )

        viewModel.signUp(req, authManager) { success, err ->
            isLoading = false
            if (success) {
                Toast.makeText(context, "መለያ በተሳካ ሁኔታ ተፈጥሯል! Account created successfully.", Toast.LENGTH_SHORT).show()
                onSignUpSuccess()
            } else {
                errorMessage = err ?: "Sign up failed. Please check information or server connection."
            }
        }
    }

    val textColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fieldContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "አዲስ መለያ መመዝገቢያ (Sign Up)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSignIn) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleTheme(!isDarkMode) }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
                            tint = if (isDarkMode) AccentAmber else PrimaryIndigo
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSecondary)
            )
        },
        containerColor = BgPrimary,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create your CRVS Account",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "የተጠቃሚ መለያ ይፍጠሩ እና ወደ ሥርዓቱ ይግቡ",
                fontSize = 13.sp,
                color = mutedColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error banner
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentDanger.copy(alpha = 0.12f))
                        .border(1.dp, AccentDanger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = AccentDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Role Selector Card
            Text(
                text = "Select Account Role / የሂሳብ ሚና ይምረጡ:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Examinee Option
                val isEx = selectedRole == "EXAMINEE"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRole = "EXAMINEE" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEx) AccentEmerald.copy(alpha = 0.15f) else BgCard,
                    border = BorderStroke(if (isEx) 2.dp else 1.dp, if (isEx) AccentEmerald else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎓",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ተፈታኝ (Examinee)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isEx) AccentEmerald else TextWhite
                            )
                            Text(
                                text = "Take CRVS knowledge exam (Results viewable by admin & manager)",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // User Manager Option
                val isUm = selectedRole == "USER_MANAGER"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRole = "USER_MANAGER" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isUm) Color(0xFF6366F1).copy(alpha = 0.15f) else BgCard,
                    border = BorderStroke(if (isUm) 2.dp else 1.dp, if (isUm) Color(0xFF6366F1) else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👥",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "የተጠቃሚዎች አስተዳዳሪ (User Manager)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isUm) Color(0xFF818CF8) else TextWhite
                            )
                            Text(
                                text = "View exam results, manage accounts & examinee roster",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Admin Option
                val isAdm = selectedRole == "ADMIN"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRole = "ADMIN" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAdm) Color(0xFF0E7490).copy(alpha = 0.15f) else BgCard,
                    border = BorderStroke(if (isAdm) 2.dp else 1.dp, if (isAdm) Color(0xFF06B6D4) else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ሥርዓት አስተዳዳሪ (Admin)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isAdm) Color(0xFF22D3EE) else TextWhite
                            )
                            Text(
                                text = "Full control: question bank, live scores & configuration",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Full Name Field
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; errorMessage = null },
                label = { Text("Full Name / ሙሉ ስም") },
                placeholder = { Text("e.g. Abebe Kebede") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = fieldContainerColor,
                    unfocusedContainerColor = fieldContainerColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = { Text("Username / የተጠቃሚ ስም") },
                placeholder = { Text("e.g. abebe12") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = fieldContainerColor,
                    unfocusedContainerColor = fieldContainerColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password / የይለፍ ቃል") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = PrimaryIndigo,
                    focusedContainerColor = fieldContainerColor,
                    unfocusedContainerColor = fieldContainerColor
                )
            )

            // Examinee-specific fields
            if (selectedRole == "EXAMINEE") {
                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ተፈታኝ መረጃ / Examinee Profile:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── PLACE OF REGISTRATION ────────────────────────
                Text(
                    text = "የምዝገባ ቦታ (Place of Registration: Country, Region/City, Wereda, Kebele)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = kebele,
                        onValueChange = { kebele = it },
                        label = { Text("Kebele (ቀበሌ) *") },
                        placeholder = { Text("e.g. Kebele 02") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = PrimaryIndigo,
                            focusedContainerColor = fieldContainerColor,
                            unfocusedContainerColor = fieldContainerColor
                        )
                    )

                    OutlinedTextField(
                        value = wereda,
                        onValueChange = { wereda = it },
                        label = { Text("Wereda (ወረዳ) *") },
                        placeholder = { Text("e.g. Wereda 01") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = PrimaryIndigo,
                            focusedContainerColor = fieldContainerColor,
                            unfocusedContainerColor = fieldContainerColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = regionOrCity,
                        onValueChange = { regionOrCity = it },
                        label = { Text("Region/City (ክልል - Optional)") },
                        placeholder = { Text("e.g. Dire Dawa") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = PrimaryIndigo,
                            focusedContainerColor = fieldContainerColor,
                            unfocusedContainerColor = fieldContainerColor
                        )
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country (አገር)") },
                        placeholder = { Text("e.g. Ethiopia") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = PrimaryIndigo,
                            focusedContainerColor = fieldContainerColor,
                            unfocusedContainerColor = fieldContainerColor
                        )
                    )
                }

                if (kebeleSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(kebeleSuggestions) { chip ->
                            val isSelected = kebele.equals(chip, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else fieldContainerColor,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
                                modifier = Modifier.clickable { kebele = chip }
                            ) {
                                Text(
                                    text = chip,
                                    fontSize = 11.sp,
                                    color = if (isSelected) PrimaryIndigo else TextMuted,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = fieldContainerColor,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📍 Place: " + region.formatPlaceOfRegistration(
                            kebele = kebele,
                            wereda = wereda,
                            regOrCity = regionOrCity,
                            ctry = country
                        ),
                        fontSize = 11.sp,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gender
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Gender (ጾታ)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    // Experience
                    OutlinedTextField(
                        value = experienceYears,
                        onValueChange = { experienceYears = it },
                        label = { Text("Exp. Years (ልምድ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Submit Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    doSignUp()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == "ADMIN") PrimaryIndigo else AccentEmerald
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("በመመዝገብ ላይ… (Creating Account…)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("ይመዝገቡ / Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back to Sign In Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSignIn() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = TextMuted
                )
                Text(
                    text = "Sign In / ይግቡ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
