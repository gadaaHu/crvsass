package com.qcafr.gatherapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qcafr.gatherapp.data.model.*
import com.qcafr.gatherapp.data.preferences.AuthPreferenceManager
import com.qcafr.gatherapp.data.preferences.LanguagePreferenceManager
import com.qcafr.gatherapp.data.preferences.RegionConfig
import com.qcafr.gatherapp.data.preferences.RegionPreferenceManager
import com.qcafr.gatherapp.data.preferences.ThemePreferenceManager
import com.qcafr.gatherapp.data.remote.NetworkClient
import com.qcafr.gatherapp.data.remote.QcafrApiService
import com.qcafr.gatherapp.data.repository.SurveyRepository
import com.qcafr.gatherapp.util.CrvsOfflineFallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestionnaireUiState(
    val survey: Survey? = null,
    val flatQuestions: List<Pair<String, Question>> = emptyList(), // Pair(SectionTitle, Question)
    val currentIndex: Int = 0,
    val participant: Participant? = null,
    val testType: String = "PRE",
    val confidenceDigital: Int? = null,
    val confidenceLaw: Int? = null,
    val answers: Map<Long, AnswerInput> = emptyMap(), // questionId -> AnswerInput
    val isSubmitting: Boolean = false,
    val submissionResult: SubmissionResponse? = null,
    val error: String? = null,
    val isAmharic: Boolean = true,  // display language: true = Amharic, false = English
    val regionConfig: RegionConfig = RegionConfig(),  // configurable region branding
    val isPostTestUnlocked: Boolean = false, // whether manager/admin unlocked post-test for examinees
    val isAiChatAllowed: Boolean = true // whether admin authorized post-test AI Tutor chat for examinees
)

class QuestionnaireViewModel(private val repository: SurveyRepository) : ViewModel() {

    /** Public access to API service for screens that call REST directly (e.g. CertificateScreen) */
    val apiService: QcafrApiService = NetworkClient.getApiService()

    private val _uiState = MutableStateFlow(QuestionnaireUiState())
    val uiState: StateFlow<QuestionnaireUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _submissions = MutableStateFlow<List<SubmissionRecordItem>>(emptyList())
    val submissions: StateFlow<List<SubmissionRecordItem>> = _submissions.asStateFlow()

    private val _isLoadingSubmissions = MutableStateFlow(false)
    val isLoadingSubmissions: StateFlow<Boolean> = _isLoadingSubmissions.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    private val _analytics = MutableStateFlow<AnalyticsSummary?>(null)
    val analytics: StateFlow<AnalyticsSummary?> = _analytics.asStateFlow()

    private val _isLoadingAnalytics = MutableStateFlow(false)
    val isLoadingAnalytics: StateFlow<Boolean> = _isLoadingAnalytics.asStateFlow()

    private val _aiStats = MutableStateFlow<AiKnowledgeStats?>(null)
    val aiStats: StateFlow<AiKnowledgeStats?> = _aiStats.asStateFlow()

    private val _aiDocuments = MutableStateFlow<List<KnowledgeDocumentItem>>(emptyList())
    val aiDocuments: StateFlow<List<KnowledgeDocumentItem>> = _aiDocuments.asStateFlow()

    private val _isLoadingAiKnowledge = MutableStateFlow(false)
    val isLoadingAiKnowledge: StateFlow<Boolean> = _isLoadingAiKnowledge.asStateFlow()

    val pendingCount: StateFlow<Int> = repository.pendingCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    init {
        // Survey loading is intentionally deferred: observeLanguage() triggers
        // loadSurvey() once the persisted language preference is resolved, so
        // section titles are built with the correct locale on the very first load.
        loadSubmissions()
        loadUsers()
        loadAnalytics()
        // Note: loadAiKnowledge() is intentionally omitted here.
        // AiChatScreen calls it via LaunchedEffect(Unit) on first composition,
        // avoiding a redundant double call on cold start.
    }

    fun loadAiKnowledge() {
        viewModelScope.launch {
            _isLoadingAiKnowledge.value = true
            val statsRes = repository.getAiKnowledgeStats()
            statsRes.onSuccess { stats ->
                _aiStats.value = stats
            }
            val docsRes = repository.getAiDocuments()
            docsRes.onSuccess { docs ->
                _aiDocuments.value = docs
            }
            _isLoadingAiKnowledge.value = false
        }
    }

    fun uploadAiDocument(
        filename: String,
        title: String,
        contentBase64: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoadingAiKnowledge.value = true
            val res = repository.uploadAiDocument(filename, title, contentBase64)
            res.onSuccess {
                loadAiKnowledge()
                onResult(true, null)
            }.onFailure { err ->
                _isLoadingAiKnowledge.value = false
                onResult(false, err.message ?: "Failed to upload document")
            }
        }
    }

    fun deleteAiDocument(documentId: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoadingAiKnowledge.value = true
            val res = repository.deleteAiDocument(documentId)
            res.onSuccess {
                loadAiKnowledge()
                onResult(true, null)
            }.onFailure { err ->
                _isLoadingAiKnowledge.value = false
                onResult(false, err.message ?: "Failed to delete document")
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            val res = repository.listUsers()
            res.onSuccess { list ->
                _users.value = list
            }
            _isLoadingUsers.value = false
        }
    }

    fun updateUserRole(userId: Long, newRole: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateUserRole(userId, newRole)
            res.onSuccess {
                loadUsers()
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to update role")
            }
        }
    }

    fun deleteUser(userId: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteUser(userId)
            res.onSuccess {
                loadUsers()
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to delete user")
            }
        }
    }

    fun loadSubmissions() {
        viewModelScope.launch {
            _isLoadingSubmissions.value = true
            val res = repository.getSubmissions()
            res.onSuccess { list ->
                _submissions.value = list
                if (_analytics.value == null) {
                    loadAnalytics()
                }
            }
            _isLoadingSubmissions.value = false
        }
    }

    fun loadAnalytics(surveyId: Long = 1L) {
        viewModelScope.launch {
            _isLoadingAnalytics.value = true
            val res = repository.getAnalytics(surveyId)
            res.onSuccess { summary ->
                _analytics.value = summary
            }.onFailure {
                // Offline / fallback analytics calculation from local submissions
                val fallback = computeLocalAnalytics(_submissions.value)
                _analytics.value = fallback
            }
            _isLoadingAnalytics.value = false
        }
    }

    fun computeLocalAnalytics(subs: List<SubmissionRecordItem>): AnalyticsSummary {
        val pre = subs.filter { it.testType.equals("PRE", ignoreCase = true) }
        val post = subs.filter { it.testType.equals("POST", ignoreCase = true) }

        val avgPre = if (pre.isNotEmpty()) pre.map { it.scorePercentage }.average().toFloat() else 0f
        val avgPost = if (post.isNotEmpty()) post.map { it.scorePercentage }.average().toFloat() else 0f
        val delta = if (pre.isNotEmpty() && post.isNotEmpty()) avgPost - avgPre else 0f

        val passPre = if (pre.isNotEmpty()) (pre.count { it.scorePercentage >= 60f } * 100 / pre.size) else 0
        val passPost = if (post.isNotEmpty()) (post.count { it.scorePercentage >= 60f } * 100 / post.size) else 0

        var strong = 0
        var satisfactory = 0
        var needsReinforcement = 0
        var needsIntensive = 0

        val targetList = if (post.isNotEmpty()) post else subs
        for (item in targetList) {
            when {
                item.scorePercentage >= 80f -> strong++
                item.scorePercentage >= 60f -> satisfactory++
                item.scorePercentage >= 40f -> needsReinforcement++
                else -> needsIntensive++
            }
        }

        val facilityMap = subs.groupBy { it.weredaKebele?.ifBlank { "General / Unassigned" } ?: "General / Unassigned" }
        val facilityBreakdown = facilityMap.map { (fac, items) ->
            val avg = items.map { it.scorePercentage }.average().toFloat()
            val passRate = (items.count { it.scorePercentage >= 60f } * 100f / items.size)
            val certified = items.count { it.approvalStatus == "APPROVED" || it.certificateNumber != null }
            FacilityBreakdownItem(
                facility = fac,
                totalSubmissions = items.size,
                participantCount = items.map { it.participantCode }.distinct().size,
                avgScorePct = avg,
                passRatePct = passRate,
                certifiedCount = certified
            )
        }.sortedByDescending { it.totalSubmissions }

        val roleMap = subs.groupBy { it.role?.ifBlank { "Registrar" } ?: "Registrar" }
        val roleBreakdown = roleMap.map { (r, items) ->
            val avg = items.map { it.scorePercentage }.average().toFloat()
            val passRate = (items.count { it.scorePercentage >= 60f } * 100f / items.size)
            RoleBreakdownItem(
                role = r,
                totalCount = items.size,
                avgScorePct = avg,
                passRatePct = passRate
            )
        }.sortedByDescending { it.totalCount }

        val eligible = targetList.count { it.scorePercentage >= 60f }
        val approved = targetList.count { it.approvalStatus == "APPROVED" || it.certificateNumber != null }
        val pending = targetList.count { it.scorePercentage >= 60f && (it.approvalStatus == "PENDING" || it.approvalStatus == null) && it.certificateNumber == null }
        val pipeline = CertificationPipelineSummary(
            totalEligible = eligible,
            totalApproved = approved,
            pendingApproval = pending,
            needsReinforcement = targetList.size - eligible
        )

        return AnalyticsSummary(
            surveyId = 1L,
            totalSubmissions = subs.size,
            preTestCount = pre.size,
            postTestCount = post.size,
            avgPreScore = avgPre,
            avgPostScore = avgPost,
            avgImprovementDelta = delta,
            passRatePre = passPre,
            passRatePost = passPost,
            performanceBands = PerformanceBandsSummary(
                strong = strong,
                satisfactory = satisfactory,
                needsReinforcement = needsReinforcement,
                needsIntensiveSupport = needsIntensive
            ),
            facilityBreakdown = facilityBreakdown,
            roleBreakdown = roleBreakdown,
            certificationPipeline = pipeline
        )
    }

    fun deleteSubmission(submissionId: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteSubmission(submissionId)
            result.onSuccess {
                loadSubmissions()
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to delete submission")
            }
        }
    }

    fun observeAuth(authManager: AuthPreferenceManager) {
        viewModelScope.launch {
            authManager.tokenFlow.collectLatest { token ->
                NetworkClient.authToken = token
            }
        }
        viewModelScope.launch {
            authManager.currentUserFlow.collectLatest { user ->
                _currentUser.value = user
            }
        }
    }

    fun signIn(
        username: String,
        password: String,
        authManager: AuthPreferenceManager,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.signIn(username, password)
            result.onSuccess { authData ->
                authManager.saveUserSession(authData.user, authData.token)
                _currentUser.value = authData.user
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(
        request: SignUpRequest,
        authManager: AuthPreferenceManager,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.signUp(request)
            result.onSuccess { authData ->
                authManager.saveUserSession(authData.user, authData.token)
                _currentUser.value = authData.user
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message ?: "Sign up failed")
            }
        }
    }

    fun signOut(authManager: AuthPreferenceManager, onComplete: () -> Unit) {
        viewModelScope.launch {
            authManager.clearUserSession()
            _currentUser.value = null
            resetAssessment()
            onComplete()
        }
    }

    fun startExamWithCurrentUser(testType: String = "POST") {
        val user = _currentUser.value
        val region = _uiState.value.regionConfig
        val defaultPrefix = region.regionName.trim().take(2).uppercase().ifBlank { "RG" }
        val participant = Participant(
            participantCode = user?.participantCode ?: "$defaultPrefix-${System.currentTimeMillis().toString().takeLast(4)}",
            weredaKebele = user?.weredaKebele ?: region.formatPlaceOfRegistration(),
            role = if (user?.role == "ADMIN") "Administrator" else "${region.regionName} CRVS Registrar",
            gender = user?.gender ?: "Unspecified",
            experienceYears = user?.experienceYears ?: 1.0f
        )
        setParticipant(
            participant = participant,
            testType = testType,
            confidenceDigital = 4,
            confidenceLaw = 4
        )
    }


    /**
     * Load (or reload) a survey, building [flatQuestions] with the supplied
     * [isAmharic] flag.  Callers must pass the flag explicitly so that async
     * network calls always use the value that was current *when the reload was
     * requested*, not whatever happens to be in [_uiState] when the response
     * eventually arrives.
     *
     * If [isAmharic] is null the current state value is used as a fallback
     * (e.g. manual calls from question-edit screens).
     */
    fun loadSurvey(surveyId: Long, isAmharic: Boolean? = null) {
        viewModelScope.launch {
            val result = repository.getSurvey(surveyId)
            result.onSuccess { survey ->
                // Use the explicitly supplied flag; fall back to current state.
                val isAm = isAmharic ?: _uiState.value.isAmharic
                val flat = mutableListOf<Pair<String, Question>>()
                for (section in survey.sections) {
                    for (q in section.questions) {
                        flat.add(Pair(if (isAm) section.titleAm else section.titleEn, q))
                    }
                }
                _uiState.value = _uiState.value.copy(
                    survey = survey,
                    flatQuestions = flat,
                    error = null
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
            }
        }
    }

    fun setParticipant(
        participant: Participant,
        testType: String,
        confidenceDigital: Int?,
        confidenceLaw: Int?
    ) {
        _uiState.value = _uiState.value.copy(
            participant = participant,
            testType = testType,
            confidenceDigital = confidenceDigital,
            confidenceLaw = confidenceLaw,
            currentIndex = 0
        )
    }

    fun selectOption(questionId: Long, questionNumber: Int, optionKey: String) {
        val currentAnswers = _uiState.value.answers.toMutableMap()
        currentAnswers[questionId] = AnswerInput(
            questionId = questionId,
            questionNumber = questionNumber,
            selectedOptionKey = optionKey
        )
        _uiState.value = _uiState.value.copy(answers = currentAnswers)
    }

    fun setRating(questionId: Long, questionNumber: Int, rating: Int) {
        val currentAnswers = _uiState.value.answers.toMutableMap()
        currentAnswers[questionId] = AnswerInput(
            questionId = questionId,
            questionNumber = questionNumber,
            ratingValue = rating
        )
        _uiState.value = _uiState.value.copy(answers = currentAnswers)
    }

    fun setTextAnswer(questionId: Long, questionNumber: Int, text: String) {
        val currentAnswers = _uiState.value.answers.toMutableMap()
        currentAnswers[questionId] = AnswerInput(
            questionId = questionId,
            questionNumber = questionNumber,
            answerText = text
        )
        _uiState.value = _uiState.value.copy(answers = currentAnswers)
    }

    fun nextQuestion(): Boolean {
        val nextIdx = _uiState.value.currentIndex + 1
        if (nextIdx < _uiState.value.flatQuestions.size) {
            _uiState.value = _uiState.value.copy(currentIndex = nextIdx)
            return true
        }
        return false // reached end
    }

    fun prevQuestion(): Boolean {
        val prevIdx = _uiState.value.currentIndex - 1
        if (prevIdx >= 0) {
            _uiState.value = _uiState.value.copy(currentIndex = prevIdx)
            return true
        }
        return false
    }

    fun jumpToQuestion(index: Int) {
        if (index in _uiState.value.flatQuestions.indices) {
            _uiState.value = _uiState.value.copy(currentIndex = index)
        }
    }

    fun submitAssessment(onComplete: (SubmissionResponse) -> Unit) {
        val state = _uiState.value
        val participant = state.participant ?: return
        val survey = state.survey ?: return

        _uiState.value = state.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            val answersList = state.answers.values.toList()
            val input = SubmissionInput(
                surveyId = survey.id,
                participant = participant,
                testType = state.testType,
                confidenceDigital = state.confidenceDigital,
                confidenceLaw = state.confidenceLaw,
                answers = answersList
            )

            val result = repository.submitAssessment(input)
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submissionResult = response
                )
                onComplete(response)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Submission error: ${err.message}"
                )
            }
        }
    }

    fun syncPending() {
        viewModelScope.launch {
            repository.syncPendingSubmissions()
        }
    }

    fun resetAssessment() {
        _uiState.value = _uiState.value.copy(
            currentIndex = 0,
            answers = emptyMap(),
            submissionResult = null,
            error = null
        )
    }

    fun updateQuestion(
        questionId: Long,
        promptAm: String,
        promptEn: String,
        points: Int,
        type: String,
        options: List<Map<String, Any?>>?,
        rubricAm: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateQuestion(
                questionId = questionId,
                promptAm = promptAm,
                promptEn = promptEn,
                points = points,
                type = type,
                options = options,
                rubricAm = rubricAm
            )
            result.onSuccess {
                loadSurvey(1L)
                onComplete(true, null)
            }.onFailure { err ->
                onComplete(false, err.message)
            }
        }
    }

    fun deleteQuestion(questionId: Long, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteQuestion(questionId)
            result.onSuccess {
                loadSurvey(1L)
                onComplete(true, null)
            }.onFailure { err ->
                onComplete(false, err.message)
            }
        }
    }

    fun createQuestion(
        sectionId: Long,
        type: String,
        promptAm: String,
        promptEn: String,
        points: Int,
        options: List<Map<String, Any?>>? = null,
        rubricAm: String? = null,
        rubricEn: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createQuestion(
                sectionId = sectionId,
                type = type,
                promptAm = promptAm,
                promptEn = promptEn,
                points = points,
                options = options,
                rubricAm = rubricAm,
                rubricEn = rubricEn
            )
            result.onSuccess {
                loadSurvey(1L)
                onComplete(true, null)
            }.onFailure { err ->
                onComplete(false, err.message)
            }
        }
    }

    fun setDarkMode(themeManager: ThemePreferenceManager, isDark: Boolean) {
        viewModelScope.launch {
            themeManager.setDarkMode(isDark)
        }
    }

    /**
     * Observe and persist language preference; reloads the survey whenever the
     * language changes so that section titles are rebuilt with the correct locale.
     *
     * The [isAm] value is passed *directly* into [loadSurvey] rather than being
     * re-read from [_uiState] later, which avoids a race condition where the
     * async network call could pick up a stale or intermediate flag value.
     */
    fun observeLanguage(langManager: LanguagePreferenceManager) {
        viewModelScope.launch {
            langManager.isAmharicFlow.collectLatest { isAm ->
                // Update UI state first so all Composables re-render immediately.
                _uiState.value = _uiState.value.copy(isAmharic = isAm)
                // Pass isAm explicitly to avoid a race with the async network call.
                val surveyId = _uiState.value.survey?.id ?: 1L
                loadSurvey(surveyId, isAmharic = isAm)
            }
        }
    }

    fun setLanguage(langManager: LanguagePreferenceManager, isAmharic: Boolean) {
        viewModelScope.launch {
            langManager.setAmharic(isAmharic)
        }
    }

    // ── Region configuration ──────────────────────────────────────────────────

    /**
     * Observe the persisted region config and push it into [uiState] so all
     * screens automatically re-render when the admin changes the region.
     */
    fun observeRegion(regionManager: RegionPreferenceManager) {
        viewModelScope.launch {
            regionManager.regionConfigFlow.collectLatest { config ->
                _uiState.value = _uiState.value.copy(regionConfig = config)
            }
        }
        viewModelScope.launch {
            regionManager.isPostTestUnlockedFlow.collectLatest { unlocked ->
                _uiState.value = _uiState.value.copy(isPostTestUnlocked = unlocked)
            }
        }
        viewModelScope.launch {
            regionManager.isAiChatAllowedFlow.collectLatest { allowed ->
                _uiState.value = _uiState.value.copy(isAiChatAllowed = allowed)
            }
        }
    }

    fun setAiChatAllowed(regionManager: RegionPreferenceManager, allowed: Boolean) {
        viewModelScope.launch {
            regionManager.saveAiChatAllowed(allowed)
        }
    }

    suspend fun askAiTutor(question: String): String {
        val isAm = _uiState.value.isAmharic
        val participantCode = _uiState.value.participant?.participantCode ?: ""
        // 1. Try remote backend API
        val result = repository.askAiTutor(
            message = question,
            language = if (isAm) "am" else "en",
            participantCode = participantCode
        )
        if (result.isSuccess && result.getOrNull()?.isNotBlank() == true) {
            return result.getOrNull()!!
        }
        // 2. Authoritative local offline fallback
        return getOfflineAiResponse(question, isAm)
    }

    private fun getOfflineAiResponse(question: String, isAm: Boolean): String =
        CrvsOfflineFallback.getResponse(question, isAm)

    fun setRegion(regionManager: RegionPreferenceManager, config: RegionConfig) {
        viewModelScope.launch {
            regionManager.saveRegion(config)
        }
    }

    fun setPostTestUnlocked(regionManager: RegionPreferenceManager, unlocked: Boolean) {
        viewModelScope.launch {
            regionManager.savePostTestUnlocked(unlocked)
        }
    }
}
