package com.qcafr.gatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qcafr.gatherapp.data.model.SubmissionResponse
import com.qcafr.gatherapp.data.preferences.AuthPreferenceManager
import com.qcafr.gatherapp.data.preferences.LanguagePreferenceManager
import com.qcafr.gatherapp.data.preferences.RegionPreferenceManager
import com.qcafr.gatherapp.data.preferences.ThemePreferenceManager
import com.qcafr.gatherapp.ui.screens.aichat.AiChatScreen
import com.qcafr.gatherapp.ui.screens.analytics.AnalyticsDashboardScreen
import com.qcafr.gatherapp.ui.screens.auth.SignInScreen
import com.qcafr.gatherapp.ui.screens.auth.SignUpScreen
import com.qcafr.gatherapp.ui.screens.certificate.CertificateScreen
import com.qcafr.gatherapp.ui.screens.home.HomeScreen
import com.qcafr.gatherapp.ui.screens.participant.ParticipantScreen
import com.qcafr.gatherapp.ui.screens.questionnaire.QuestionnaireScreen
import com.qcafr.gatherapp.ui.screens.result.ResultScreen
import com.qcafr.gatherapp.ui.screens.review.ReviewScreen
import com.qcafr.gatherapp.ui.theme.CRVSTheme
import com.qcafr.gatherapp.ui.theme.BgPrimary
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: QuestionnaireViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as QcafrApplication
                @Suppress("UNCHECKED_CAST")
                return QuestionnaireViewModel(app.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themeManager  = ThemePreferenceManager.getInstance(this)
        val authManager   = AuthPreferenceManager.getInstance(this)
        val langManager   = LanguagePreferenceManager.getInstance(this)
        val regionManager = RegionPreferenceManager.getInstance(this)

        // Start all observers before setContent so initial state is ready
        viewModel.observeAuth(authManager)
        viewModel.observeLanguage(langManager)
        viewModel.observeRegion(regionManager)

        setContent {
            // Observe persisted theme preference
            var isDarkMode by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                themeManager.isDarkModeFlow.collectLatest { isDark ->
                    isDarkMode = isDark
                }
            }

            // isAmharic is collected from uiState (kept in sync by observeLanguage)
            // to avoid a dual-source-of-truth problem with a separate LaunchedEffect.
            val uiStateForLang by viewModel.uiState.collectAsState()
            val isAmharic = uiStateForLang.isAmharic

            val currentUser by viewModel.currentUser.collectAsState()

            CRVSTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgPrimary
                ) {
                    val navController = rememberNavController()
                    var lastSubmissionResponse by remember { mutableStateOf<SubmissionResponse?>(null) }

                    // Determine start destination: "home" if already logged in, otherwise "signin"
                    val startDest = if (currentUser != null) "home" else "signin"

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        // ── Sign In ───────────────────────────────────────────
                        composable("signin") {
                            SignInScreen(
                                viewModel = viewModel,
                                authManager = authManager,
                                isDarkMode = isDarkMode,
                                onToggleTheme = { newDark ->
                                    viewModel.setDarkMode(themeManager, newDark)
                                },
                                onSignInSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("signin") { inclusive = true }
                                    }
                                },
                                onNavigateToSignUp = {
                                    navController.navigate("signup")
                                }
                            )
                        }

                        // ── Sign Up ───────────────────────────────────────────
                        composable("signup") {
                            SignUpScreen(
                                viewModel = viewModel,
                                authManager = authManager,
                                isDarkMode = isDarkMode,
                                onToggleTheme = { newDark ->
                                    viewModel.setDarkMode(themeManager, newDark)
                                },
                                onSignUpSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("signin") { inclusive = true }
                                    }
                                },
                                onNavigateToSignIn = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // ── Home ──────────────────────────────────────────────
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                isDarkMode = isDarkMode,
                                isAmharic = isAmharic,
                                onToggleTheme = { newDark ->
                                    viewModel.setDarkMode(themeManager, newDark)
                                },
                                onToggleLanguage = { newAm ->
                                    viewModel.setLanguage(langManager, newAm)
                                },
                                onStartAssessment = {
                                    navController.navigate("participant")
                                },
                                onStartExamDirectly = { testType ->
                                    viewModel.startExamWithCurrentUser(testType)
                                    navController.navigate("questionnaire")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToAnalytics = {
                                    navController.navigate("analytics")
                                },
                                onNavigateToCertificate = { subId, code ->
                                    navController.navigate("certificate/$subId/$code")
                                },
                                onNavigateToAiChat = {
                                    navController.navigate("aichat")
                                },
                                onSignOut = {
                                    viewModel.signOut(authManager) {
                                        navController.navigate("signin") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // ── Analytical Dashboard ─────────────────────────────
                        composable("analytics") {
                            AnalyticsDashboardScreen(
                                viewModel = viewModel,
                                isDarkMode = isDarkMode,
                                onBack = { navController.popBackStack() },
                                onNavigateToAiChat = { navController.navigate("aichat") },
                                onSignOut = {
                                    viewModel.signOut(authManager) {
                                        navController.navigate("signin") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // ── Settings & Question Editor ────────────────────────
                        composable("settings") {
                            com.qcafr.gatherapp.ui.screens.settings.SettingsScreen(
                                viewModel = viewModel,
                                isDarkMode = isDarkMode,
                                isAmharic = isAmharic,
                                onToggleTheme = { newDark ->
                                    viewModel.setDarkMode(themeManager, newDark)
                                },
                                onToggleLanguage = { newAmharic ->
                                    viewModel.setLanguage(langManager, newAmharic)
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToAnalytics = {
                                    navController.navigate("analytics")
                                },
                                onNavigateToCertificate = { subId, code ->
                                    navController.navigate("certificate/$subId/$code")
                                },
                                onSignOut = {
                                    viewModel.signOut(authManager) {
                                        navController.navigate("signin") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }


                        // ── Participant Details (Manual setup fallback) ──────
                        composable("participant") {
                            ParticipantScreen(
                                viewModel = viewModel,
                                onContinueToQuestions = {
                                    navController.navigate("questionnaire")
                                }
                            )
                        }

                        // ── Interactive Questionnaire Wizard ──────────────────
                        composable("questionnaire") {
                            QuestionnaireScreen(
                                viewModel = viewModel,
                                onNavigateToReview = {
                                    navController.navigate("review")
                                }
                            )
                        }

                        // ── Review Screen ─────────────────────────────────────
                        composable("review") {
                            ReviewScreen(
                                viewModel = viewModel,
                                onJumpToQuestion = { index ->
                                    viewModel.jumpToQuestion(index)
                                    navController.navigate("questionnaire")
                                },
                                onBackToQuestions = {
                                    navController.popBackStack()
                                },
                                onSubmitSuccess = { response ->
                                    lastSubmissionResponse = response
                                    navController.navigate("result") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                }
                            )
                        }

                        // ── Result & PDF Screen ───────────────────────────────
                        composable("result") {
                            val resp = lastSubmissionResponse ?: SubmissionResponse(
                                id = 0,
                                participantCode = currentUser?.participantCode ?: "N/A",
                                testType = "POST",
                                totalScore = 20f,
                                maxScore = 24f,
                                scorePercentage = 83f,
                                performanceBand = "STRONG",
                                submittedAt = ""
                            )
                            ResultScreen(
                                viewModel = viewModel,
                                response = resp,
                                onNavigateToAiChat = {
                                    navController.navigate("aichat")
                                },
                                onDone = {
                                    viewModel.resetAssessment()
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                        // ── AI Tutor Chat Screen ──────────────────────────────
                        composable("aichat") {
                            AiChatScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // ── Certificate Viewer ────────────────────────────────
                        composable("certificate/{submissionId}/{code}") { backStack ->
                            val submissionId = backStack.arguments?.getString("submissionId")?.toLongOrNull() ?: 0L
                            val code = backStack.arguments?.getString("code") ?: ""
                            CertificateScreen(
                                submissionId    = submissionId,
                                participantCode = code,
                                viewModel       = viewModel,
                                onBack          = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
