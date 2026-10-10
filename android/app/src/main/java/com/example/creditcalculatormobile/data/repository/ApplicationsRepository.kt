package com.example.creditcalculatormobile.data.repository

import com.example.creditcalculatormobile.core.network.ApiResult
import com.example.creditcalculatormobile.core.network.safeApiCall
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
import com.example.creditcalculatormobile.data.remote.ApplicationsApi
import kotlinx.serialization.json.Json

class ApplicationsRepository(
    private val applicationsApi: ApplicationsApi,
    private val json: Json,
) {
    suspend fun createApplication(
        request: CreateApplicationRequestDto,
    ): ApiResult<CreateApplicationResponseDto> = safeApiCall(json) {
        applicationsApi.createApplication(request)
    }

    suspend fun getApplications(): ApiResult<List<GetApplicationResponseDto>> = safeApiCall(json) {
        applicationsApi.getApplications()
    }

    suspend fun getApplication(
        applicationId: String,
    ): ApiResult<GetApplicationResponseDto> = safeApiCall(json) {
        applicationsApi.getApplication(applicationId)
    }

    suspend fun updateApplication(
        applicationId: String,
        request: UpdateApplicationRequestDto,
    ): ApiResult<UpdateApplicationResponseDto> = safeApiCall(json) {
        applicationsApi.updateApplication(applicationId, request)
    }

    suspend fun submitApplication(
        applicationId: String,
        request: SubmitApplicationRequestDto,
    ): ApiResult<SubmitApplicationResponseDto> = safeApiCall(json) {
        applicationsApi.submitApplication(applicationId, request)
    }

    suspend fun getScoringResult(
        applicationId: String,
    ): ApiResult<ScoringResultDto> = safeApiCall(json) {
        applicationsApi.getScoringResult(applicationId)
    }

    suspend fun getOffers(
        applicationId: String,
    ): ApiResult<List<OfferDto>> = safeApiCall(json) {
        applicationsApi.getOffers(applicationId)
    }

    suspend fun selectOffer(
        applicationId: String,
        offerId: String,
        request: SelectOfferRequestDto? = null,
    ): ApiResult<SelectOfferResponseDto> = safeApiCall(json) {
        applicationsApi.selectOffer(applicationId, offerId, request)
    }

    suspend fun requestDocuments(
        applicationId: String,
        request: RequestDocumentsRequestDto? = null,
    ): ApiResult<RequestDocumentsResponseDto> = safeApiCall(json) {
        applicationsApi.requestDocuments(applicationId, request)
    }

    suspend fun getDocuments(
        applicationId: String,
    ): ApiResult<List<ApplicationDocumentDto>> = safeApiCall(json) {
        applicationsApi.getDocuments(applicationId)
    }

    suspend fun signContract(
        applicationId: String,
    ): ApiResult<ContractResponseDto> = safeApiCall(json) {
        applicationsApi.signContract(applicationId)
    }
}
