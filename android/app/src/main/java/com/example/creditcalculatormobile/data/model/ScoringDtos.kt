package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitApplicationResponseDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("status")
    val status: String,
    @SerialName("decision")
    val decision: String,
    @SerialName("scoreValue")
    val scoreValue: Double,
    @SerialName("riskGrade")
    val riskGrade: String? = null,
    @SerialName("rulesVersion")
    val rulesVersion: String,
    @SerialName("approvedAmount")
    val approvedAmount: Double? = null,
    @SerialName("approvedTermMonths")
    val approvedTermMonths: Int? = null,
    @SerialName("approvedRate")
    val approvedRate: Double? = null,
    @SerialName("reasons")
    val reasons: List<String> = emptyList(),
)

@Serializable
data class ScoringResultDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("decision")
    val decision: String,
    @SerialName("scoreValue")
    val scoreValue: Double,
    @SerialName("riskGrade")
    val riskGrade: String? = null,
    @SerialName("rulesVersion")
    val rulesVersion: String,
    @SerialName("approvedAmount")
    val approvedAmount: Double? = null,
    @SerialName("approvedTermMonths")
    val approvedTermMonths: Int? = null,
    @SerialName("approvedRate")
    val approvedRate: Double? = null,
    @SerialName("reasons")
    val reasons: List<String> = emptyList(),
    @SerialName("scoredAt")
    val scoredAt: String? = null,
)
