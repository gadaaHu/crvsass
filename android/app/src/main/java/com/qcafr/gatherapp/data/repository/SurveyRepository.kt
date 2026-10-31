package com.qcafr.gatherapp.data.repository

import android.util.Log
import com.qcafr.gatherapp.data.local.AppDatabase
import com.qcafr.gatherapp.data.local.entity.CompletedSubmissionEntity
import com.qcafr.gatherapp.data.local.entity.LocalSurveyEntity
import com.qcafr.gatherapp.data.local.entity.PendingSubmissionEntity
import com.qcafr.gatherapp.data.model.AnalyticsSummary
import com.qcafr.gatherapp.data.model.SubmissionInput
import com.qcafr.gatherapp.data.model.SubmissionRecordItem
import com.qcafr.gatherapp.data.model.SubmissionResponse
import com.qcafr.gatherapp.data.model.Survey
import com.qcafr.gatherapp.data.model.User
import com.qcafr.gatherapp.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SurveyRepository(private val database: AppDatabase) {

    private val api = NetworkClient.getApiService()
    private val moshi = NetworkClient.moshi
    private val surveyDao = database.surveyDao()
    private val pendingDao = database.pendingSubmissionDao()
    private val completedDao = database.completedSubmissionDao()

    val pendingCountFlow: Flow<Int> = pendingDao.getPendingCountFlow()
    val completedSubmissionsFlow: Flow<List<CompletedSubmissionEntity>> = completedDao.getAllFlow()

    /**
     * Loads survey details. Tries network first; on failure, loads from local SQLite cache.
     */
    suspend fun getSurvey(surveyId: Long): Result<Survey> = withContext(Dispatchers.IO) {
        try {
            // 1. Try remote API
            val response = api.getSurveyById(surveyId)
            if (response.isSuccessful && response.body()?.data != null) {
                val survey = response.body()!!.data!!
                // Cache into Room
                val adapter = moshi.adapter(Survey::class.java)
                val json = adapter.toJson(survey)
                surveyDao.insertSurvey(
                    LocalSurveyEntity(
                        id = survey.id,
                        code = survey.code,
                        titleAm = survey.titleAm,
                        titleEn = survey.titleEn,
                        descriptionAm = survey.descriptionAm,
                        descriptionEn = survey.descriptionEn,
                        totalPoints = survey.totalPoints,
                        passingScore = survey.passingScore,
                        rawJson = json
                    )
                )
                return@withContext Result.success(survey)
            }
        } catch (e: Exception) {
            Log.w("SurveyRepo", "Network fetch failed, attempting Room cache: ${e.message}")
        }

        // 2. Fallback to Room DB
        try {
            val local = surveyDao.getSurveyById(surveyId)
            if (local != null) {
                val adapter = moshi.adapter(Survey::class.java)
                val survey = adapter.fromJson(local.rawJson)
                if (survey != null) {
                    return@withContext Result.success(survey)
                }
            }
        } catch (e: Exception) {
            Log.e("SurveyRepo", "Cache read failed: ${e.message}")
        }

        Result.failure(Exception("Could not load survey online or from offline cache."))
    }

    /**
     * Submits assessment answers.
     * If online: sends directly to backend and receives verified score.
     * If offline: queues in Room pending database and creates provisional local evaluation.
     */
    suspend fun submitAssessment(submission: SubmissionInput): Result<SubmissionResponse> = withContext(Dispatchers.IO) {
        val adapter = moshi.adapter(SubmissionInput::class.java)
        val payloadJson = adapter.toJson(submission)

        try {
            val response = api.submitSurvey(submission)
            if (response.isSuccessful && response.body()?.data != null) {
                val res = response.body()!!.data!!
                try {
                    completedDao.insert(
                        CompletedSubmissionEntity(
                            id = res.id,
                            participantCode = res.participantCode,
                            weredaKebele = submission.participant.weredaKebele,
                            role = submission.participant.role,
                            testType = res.testType,
                            totalScore = res.totalScore,
                            maxScore = res.maxScore,
                            scorePercentage = res.scorePercentage,
                            performanceBand = res.performanceBand,
                            submittedAt = res.submittedAt
                        )
                    )
                } catch (_: Exception) {}
                return@withContext Result.success(res)
            }
        } catch (e: Exception) {
            Log.w("SurveyRepo", "Online submission failed, queuing offline: ${e.message}")
        }

        // Save into pending offline queue
        try {
            val localId = pendingDao.insertSubmission(
                PendingSubmissionEntity(
                    surveyId = submission.surveyId,
                    participantCode = submission.participant.participantCode,
                    testType = submission.testType,
                    payloadJson = payloadJson,
                    timestamp = System.currentTimeMillis(),
                    isSynced = false
                )
            )

            // Return provisional response
            val provisional = SubmissionResponse(
                id = localId,
                participantCode = submission.participant.participantCode,
                testType = submission.testType,
                totalScore = 0f,
                maxScore = 24f,
                scorePercentage = 0f,
                performanceBand = "PENDING_SYNC (በስልኩ ተቀምጧል)",
                submittedAt = "Saved Locally (Offline)",
                answers = emptyList()
            )

            try {
                completedDao.insert(
                    CompletedSubmissionEntity(
                        id = localId,
                        participantCode = provisional.participantCode,
                        weredaKebele = submission.participant.weredaKebele,
                        role = submission.participant.role,
                        testType = provisional.testType,
                        totalScore = provisional.totalScore,
                        maxScore = provisional.maxScore,
                        scorePercentage = provisional.scorePercentage,
                        performanceBand = provisional.performanceBand,
                        submittedAt = provisional.submittedAt
                    )
                )
            } catch (_: Exception) {}

            return@withContext Result.success(provisional)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synchronizes all offline pending submissions with the server.
     */
    suspend fun syncPendingSubmissions(): Result<Int> = withContext(Dispatchers.IO) {
        var syncedCount = 0
        try {
            val pendingList = pendingDao.getUnsyncedSubmissionsList()
            val inputAdapter = moshi.adapter(SubmissionInput::class.java)

            for (item in pendingList) {
                val input = inputAdapter.fromJson(item.payloadJson) ?: continue
                val response = api.submitSurvey(input)
                if (response.isSuccessful) {
                    pendingDao.markAsSynced(item.localId)
                    val res = response.body()?.data
                    if (res != null) {
                        try {
                            completedDao.insert(
                                CompletedSubmissionEntity(
                                    id = res.id,
                                    participantCode = res.participantCode,
                                    weredaKebele = input.participant.weredaKebele,
                                    role = input.participant.role,
                                    testType = res.testType,
                                    totalScore = res.totalScore,
                                    maxScore = res.maxScore,
                                    scorePercentage = res.scorePercentage,
                                    performanceBand = res.performanceBand,
                                    submittedAt = res.submittedAt
                                )
                            )
                        } catch (_: Exception) {}
                    }
                    syncedCount++
                } else {
                    pendingDao.updateSubmission(
                        item.copy(
                            syncAttempts = item.syncAttempts + 1,
                            lastError = response.errorBody()?.string()
                        )
                    )
                }
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all submitted exam results (from remote server, fallback to local database).
     */
    suspend fun getSubmissions(): Result<List<SubmissionRecordItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSubmissions(surveyId = 1L)
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!
                try {
                    completedDao.insertAll(list.map {
                        CompletedSubmissionEntity(
                            id = it.id,
                            participantCode = it.participantCode,
                            weredaKebele = it.weredaKebele,
                            role = it.role,
                            testType = it.testType,
                            totalScore = it.totalScore,
                            maxScore = it.maxScore,
                            scorePercentage = it.scorePercentage,
                            performanceBand = it.performanceBand,
                            submittedAt = it.submittedAt
                        )
                    })
                } catch (_: Exception) {}
                return@withContext Result.success(list)
            }
        } catch (e: Exception) {
            Log.w("SurveyRepo", "Remote getSubmissions failed: ${e.message}")
        }

        // Return from local completed DB
        try {
            val localList = completedDao.getAll().map {
                SubmissionRecordItem(
                    id = it.id,
                    surveyId = 1L,
                    participantCode = it.participantCode,
                    weredaKebele = it.weredaKebele,
                    role = it.role,
                    gender = null,
                    experienceYears = null,
                    testType = it.testType,
                    totalScore = it.totalScore,
                    maxScore = it.maxScore,
                    scorePercentage = it.scorePercentage,
                    performanceBand = it.performanceBand,
                    submittedAt = it.submittedAt
                )
            }
            Result.success(localList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes an examinee's submission record (remotely and locally), allowing resets/retakes.
     */
    suspend fun deleteSubmission(submissionId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        var remoteSuccess = false
        try {
            val response = api.deleteSubmission(submissionId)
            remoteSuccess = response.isSuccessful
        } catch (e: Exception) {
            Log.w("SurveyRepo", "Remote deleteSubmission failed: ${e.message}")
        }
        try {
            completedDao.deleteById(submissionId)
            Result.success(true)
        } catch (e: Exception) {
            if (remoteSuccess) Result.success(true) else Result.failure(e)
        }
    }

    /**
     * Updates question prompts, points, type, and options on the backend and refreshes cache.
     */
    suspend fun updateQuestion(
        questionId: Long,
        promptAm: String,
        promptEn: String,
        points: Int,
        type: String,
        options: List<Map<String, Any?>>?,
        rubricAm: String? = null
    ): Result<com.qcafr.gatherapp.data.model.Question> = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf<String, Any?>(
                "prompt_am" to promptAm,
                "prompt_en" to promptEn,
                "points" to points,
                "type" to type,
                "rubric_am" to rubricAm
            )
            if (options != null) {
                payload["options"] = options
            }

            val response = api.updateQuestion(questionId, payload)
            if (response.isSuccessful && response.body()?.data != null) {
                // Refresh survey cache
                getSurvey(1L)
                return@withContext Result.success(response.body()!!.data!!)
            }
            Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update question"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a question from the backend and updates cache.
     */
    suspend fun deleteQuestion(questionId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteQuestion(questionId)
            if (response.isSuccessful) {
                getSurvey(1L)
                return@withContext Result.success(true)
            }
            Result.failure(Exception("Failed to delete question"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new question in a specific section on the backend and updates cache.
     */
    suspend fun createQuestion(
        sectionId: Long,
        type: String,
        promptAm: String,
        promptEn: String,
        points: Int,
        options: List<Map<String, Any?>>? = null,
        rubricAm: String? = null,
        rubricEn: String? = null
    ): Result<com.qcafr.gatherapp.data.model.Question> = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf<String, Any?>(
                "prompt_am" to promptAm,
                "prompt_en" to promptEn,
                "type" to type,
                "points" to points,
                "rubric_am" to rubricAm,
                "rubric_en" to rubricEn
            )
            if (options != null) {
                payload["options"] = options
            }

            val response = api.createQuestion(sectionId, payload)
            if (response.isSuccessful && response.body()?.data != null) {
                getSurvey(1L)
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create question"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates user against backend API.
     */
    suspend fun signIn(username: String, password: String): Result<com.qcafr.gatherapp.data.model.AuthData> = withContext(Dispatchers.IO) {
        try {
            val response = api.signIn(com.qcafr.gatherapp.data.model.SignInRequest(username.trim(), password))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error ?: "Invalid credentials or login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error connecting to authentication server"))
        }
    }

    /**
     * Registers a new user account (Admin or Examinee).
     */
    suspend fun signUp(request: com.qcafr.gatherapp.data.model.SignUpRequest): Result<com.qcafr.gatherapp.data.model.AuthData> = withContext(Dispatchers.IO) {
        try {
            val response = api.signUp(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error during registration"))
        }
    }

    /**
     * Retrieves all registered users for user management.
     */
    suspend fun listUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val response = api.listUsers()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to fetch user list"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error fetching users"))
        }
    }

    /**
     * Updates a user's role (ADMIN, USER_MANAGER, EXAMINEE).
     */
    suspend fun updateUserRole(userId: Long, newRole: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateUserRole(userId, mapOf("role" to newRole))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to update user role"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error updating user role"))
        }
    }

    /**
     * Deletes a user by ID.
     */
    suspend fun deleteUser(userId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteUser(userId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete user (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error deleting user"))
        }
    }

    /**
     * Loads aggregated survey analytics from backend.
     */
    suspend fun getAnalytics(surveyId: Long = 1L): Result<AnalyticsSummary> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAnalytics(surveyId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to load analytics (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error loading analytics"))
        }
    }
}


