package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestDocumentsRequestDto(
    @SerialName("paymentType")
    val paymentType: PaymentTypeDto? = null,
)

@Serializable
data class RequestDocumentsResponseDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String,
)

@Serializable
data class ApplicationDocumentDto(
    @SerialName("documentId")
    val documentId: String,
    @SerialName("documentType")
    val documentType: String,
    @SerialName("format")
    val format: String,
    @SerialName("fileName")
    val fileName: String? = null,
    @SerialName("mimeType")
    val mimeType: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("generatedAt")
    val generatedAt: String? = null,
)
