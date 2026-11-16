package com.qcafr.gatherapp.data.remote

import com.qcafr.gatherapp.data.model.ApiResponse
import com.qcafr.gatherapp.data.model.AuthData
import com.qcafr.gatherapp.data.model.SignInRequest
import com.qcafr.gatherapp.data.model.SignUpRequest
import com.qcafr.gatherapp.data.model.SubmissionInput
import com.qcafr.gatherapp.data.model.SubmissionResponse
import com.qcafr.gatherapp.data.model.Survey
import com.qcafr.gatherapp.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface QcafrApiService {

    @POST("auth/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<ApiResponse<AuthData>>

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<ApiResponse<AuthData>>

    @GET("auth/users")
    suspend fun listUsers(): Response<ApiResponse<List<User>>>

    @retrofit2.http.PATCH("auth/users/{id}/role")
    suspend fun updateUserRole(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<ApiResponse<User>>

    @retrofit2.http.DELETE("auth/users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Response<Map<String, Any>>

    @POST("ai/chat")
    suspend fun askAiTutor(
        @Body request: Map<String, String>
    ): Response<ApiResponse<Map<String, Any>>>

    @GET("health")
    suspend fun getHealth(): Response<Map<String, Any>>

    @GET("surveys")
    suspend fun getSurveys(): Response<ApiResponse<List<Survey>>>

    @GET("surveys/{id}")
    suspend fun getSurveyById(@Path("id") id: Long): Response<ApiResponse<Survey>>

    @POST("submissions")
    suspend fun submitSurvey(@Body input: SubmissionInput): Response<ApiResponse<SubmissionResponse>>

    @GET("submissions")
    suspend fun getSubmissions(
        @retrofit2.http.Query("survey_id") surveyId: Long? = null,
        @retrofit2.http.Query("test_type") testType: String? = null,
        @retrofit2.http.Query("participant_code") participantCode: String? = null
    ): Response<ApiResponse<List<com.qcafr.gatherapp.data.model.SubmissionRecordItem>>>

    @GET("questions/{id}")
    suspend fun getQuestionById(@Path("id") id: Long): Response<ApiResponse<com.qcafr.gatherapp.data.model.Question>>

    @retrofit2.http.PUT("questions/{id}")
    suspend fun updateQuestion(
        @Path("id") id: Long,
        @Body data: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<com.qcafr.gatherapp.data.model.Question>>

    @POST("sections/{sectionId}/questions")
    suspend fun createQuestion(
        @Path("sectionId") sectionId: Long,
        @Body data: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<com.qcafr.gatherapp.data.model.Question>>

    @retrofit2.http.DELETE("questions/{id}")
    suspend fun deleteQuestion(@Path("id") id: Long): Response<Map<String, Any>>

    @retrofit2.http.DELETE("submissions/{id}")
    suspend fun deleteSubmission(@Path("id") id: Long): Response<Map<String, Any>>

    @retrofit2.http.PATCH("submissions/{id}/approve")
    suspend fun approveSubmission(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @retrofit2.http.PATCH("submissions/{id}/reject")
    suspend fun rejectSubmission(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("submissions/{id}/certificate")
    suspend fun getCertificate(@Path("id") id: Long): Response<Map<String, Any>>

    @GET("certificates/participant/{code}")
    suspend fun getCertificatesForParticipant(@Path("code") code: String): Response<Map<String, Any>>

    @GET("analytics/{surveyId}")
    suspend fun getAnalytics(@Path("surveyId") surveyId: Long): Response<ApiResponse<com.qcafr.gatherapp.data.model.AnalyticsSummary>>
}
