package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreliminaryOfferDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("requestedAmount")
    val requestedAmount: Double,
    @SerialName("totalAmount")
    val totalAmount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("monthlyPayment")
    val monthlyPayment: Double,
    @SerialName("rate")
    val rate: Double,
    @SerialName("insuranceEnabled")
    val insuranceEnabled: Boolean,
    @SerialName("salaryClient")
    val salaryClient: Boolean,
)

@Serializable
data class OfferDto(
    @SerialName("offerId")
    val offerId: String,
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("requestedAmount")
    val requestedAmount: Double,
    @SerialName("totalAmount")
    val totalAmount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("monthlyPayment")
    val monthlyPayment: Double,
    @SerialName("rate")
    val rate: Double,
    @SerialName("insuranceEnabled")
    val insuranceEnabled: Boolean,
    @SerialName("salaryClient")
    val salaryClient: Boolean,
    @SerialName("selected")
    val selected: Boolean = false,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

@Serializable
data class SelectOfferRequestDto(
    @SerialName("paymentType")
    val paymentType: PaymentTypeDto? = null,
    @SerialName("accountNumber")
    val accountNumber: String? = null,
)

@Serializable
data class SelectOfferResponseDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("offerId")
    val offerId: String,
    @SerialName("applicationStatus")
    val applicationStatus: String,
    @SerialName("message")
    val message: String,
)
