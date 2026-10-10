package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ContractResponseDto(
    val applicationId: String,
    val contractNumber: String? = null,
    val contractStatus: String,
    val applicationStatus: String,
    val signed: Boolean,
    val signedAt: String? = null,
    val signatureId: String? = null,
)
