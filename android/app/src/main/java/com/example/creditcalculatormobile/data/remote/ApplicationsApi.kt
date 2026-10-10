package com.example.creditcalculatormobile.data.remote

import com.example.creditcalculatormobile.core.network.ApiPaths
import com.example.creditcalculatormobile.data.model.ApplicationDocumentDto
import com.example.creditcalculatormobile.data.model.ContractResponseDto
import com.example.creditcalculatormobile.data.model.CreateApplicationRequestDto
import com.example.creditcalculatormobile.data.model.CreateApplicationResponseDto
import com.example.creditcalculatormobile.data.model.GetApplicationResponseDto
import com.example.creditcalculatormobile.data.model.OfferDto
import com.example.creditcalculatormobile.data.model.RequestDocumentsRequestDto
import com.example.creditcalculatormobile.data.model.RequestDocumentsResponseDto
import com.example.creditcalculatormobile.data.model.ScoringResultDto
import com.example.creditcalculatormobile.data.model.SelectOfferRequestDto
import com.example.creditcalculatormobile.data.model.SelectOfferResponseDto
import com.example.creditcalculatormobile.data.model.SubmitApplicationRequestDto
import com.example.creditcalculatormobile.data.model.SubmitApplicationResponseDto
import com.example.creditcalculatormobile.data.model.UpdateApplicationRequestDto
import com.example.creditcalculatormobile.data.model.UpdateApplicationResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApplicationsApi {

    @POST(ApiPaths.APPLICATIONS)
    suspend fun createApplication(
        @Body request: CreateApplicationRequestDto,
    ): Response<CreateApplicationResponseDto>

    @GET(ApiPaths.APPLICATIONS)
    suspend fun getApplications(): Response<List<GetApplicationResponseDto>>

    @GET("api/applications/{applicationId}")
    suspend fun getApplication(
        @Path("applicationId") applicationId: String,
    ): Response<GetApplicationResponseDto>

    @PATCH("api/applications/{applicationId}")
    suspend fun updateApplication(
        @Path("applicationId") applicationId: String,
        @Body request: UpdateApplicationRequestDto,
    ): Response<UpdateApplicationResponseDto>

    @POST("api/applications/{applicationId}/submit")
    suspend fun submitApplication(
        @Path("applicationId") applicationId: String,
        @Body request: SubmitApplicationRequestDto,
    ): Response<SubmitApplicationResponseDto>

    @GET("api/applications/{applicationId}/scoring-result")
    suspend fun getScoringResult(
        @Path("applicationId") applicationId: String,
    ): Response<ScoringResultDto>

    @GET("api/applications/{applicationId}/offers")
    suspend fun getOffers(
        @Path("applicationId") applicationId: String,
    ): Response<List<OfferDto>>

    @POST("api/applications/{applicationId}/offers/{offerId}/select")
    suspend fun selectOffer(
        @Path("applicationId") applicationId: String,
        @Path("offerId") offerId: String,
        @Body request: SelectOfferRequestDto? = null,
    ): Response<SelectOfferResponseDto>

    @POST("api/applications/{applicationId}/request-documents")
    suspend fun requestDocuments(
        @Path("applicationId") applicationId: String,
        @Body request: RequestDocumentsRequestDto? = null,
    ): Response<RequestDocumentsResponseDto>

    @GET("api/applications/{applicationId}/documents")
    suspend fun getDocuments(
        @Path("applicationId") applicationId: String,
    ): Response<List<ApplicationDocumentDto>>

    @POST("api/applications/{applicationId}/contract/sign")
    suspend fun signContract(
        @Path("applicationId") applicationId: String,
    ): Response<ContractResponseDto>
}
