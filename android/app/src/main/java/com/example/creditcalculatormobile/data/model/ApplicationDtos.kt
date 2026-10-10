package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentTypeDto {
    @SerialName("ANNUITY")
    ANNUITY,

    @SerialName("DIFFERENTIAL")
    DIFFERENTIAL,
}

@Serializable
enum class GenderDto {
    @SerialName("MALE")
    MALE,

    @SerialName("FEMALE")
    FEMALE,

    @SerialName("NON_BINARY")
    NON_BINARY,
}

@Serializable
enum class MaritalStatusDto {
    @SerialName("SINGLE")
    SINGLE,

    @SerialName("MARRIED")
    MARRIED,

    @SerialName("DIVORCED")
    DIVORCED,

    @SerialName("WIDOWED")
    WIDOWED,
}

@Serializable
enum class EmploymentStatusDto {
    @SerialName("EMPLOYED")
    EMPLOYED,

    @SerialName("UNEMPLOYED")
    UNEMPLOYED,

    @SerialName("SELF_EMPLOYED")
    SELF_EMPLOYED,

    @SerialName("RETIRED")
    RETIRED,

    @SerialName("BUSINESS_OWNER")
    BUSINESS_OWNER,

    @SerialName("STUDENT")
    STUDENT,
}

@Serializable
enum class PositionDto {
    @SerialName("MID_MANAGER")
    MID_MANAGER,

    @SerialName("TOP_MANAGER")
    TOP_MANAGER,

    @SerialName("JUNIOR_MANAGER")
    JUNIOR_MANAGER,

    @SerialName("DEVELOPER")
    DEVELOPER,

    @SerialName("SALES")
    SALES,

    @SerialName("ACCOUNTANT")
    ACCOUNTANT,

    @SerialName("HR")
    HR,

    @SerialName("OTHER")
    OTHER,
}

@Serializable
data class CreateApplicationRequestDto(
    @SerialName("amount")
    val amount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String,
    @SerialName("middleName")
    val middleName: String? = null,
    @SerialName("birthDate")
    val birthDate: String,
    @SerialName("passportSeries")
    val passportSeries: String,
    @SerialName("passportNumber")
    val passportNumber: String,
)

@Serializable
data class UpdateApplicationRequestDto(
    @SerialName("amount")
    val amount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String,
    @SerialName("middleName")
    val middleName: String? = null,
    @SerialName("email")
    val email: String,
    @SerialName("birthDate")
    val birthDate: String,
    @SerialName("passportSeries")
    val passportSeries: String,
    @SerialName("passportNumber")
    val passportNumber: String,
)

@Serializable
data class CreateApplicationResponseDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("status")
    val status: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("prescoringReasons")
    val prescoringReasons: List<String> = emptyList(),
    @SerialName("preliminaryOffers")
    val preliminaryOffers: List<PreliminaryOfferDto> = emptyList(),
)

@Serializable
data class UpdateApplicationResponseDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("status")
    val status: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("prescoringReasons")
    val prescoringReasons: List<String> = emptyList(),
    @SerialName("preliminaryOffers")
    val preliminaryOffers: List<PreliminaryOfferDto> = emptyList(),
)

@Serializable
data class SubmitApplicationRequestDto(
    @SerialName("insuranceEnabled")
    val insuranceEnabled: Boolean,
    @SerialName("salaryClient")
    val salaryClient: Boolean,
    @SerialName("gender")
    val gender: GenderDto,
    @SerialName("maritalStatus")
    val maritalStatus: MaritalStatusDto,
    @SerialName("dependentAmount")
    val dependentAmount: Int,
    @SerialName("passportIssueDate")
    val passportIssueDate: String,
    @SerialName("passportIssueBranch")
    val passportIssueBranch: String,
    @SerialName("accountNumber")
    val accountNumber: String? = null,
    @SerialName("employment")
    val employment: EmploymentRequestDto,
) {
    @Serializable
    data class EmploymentRequestDto(
        @SerialName("employmentStatus")
        val employmentStatus: EmploymentStatusDto,
        @SerialName("employerInn")
        val employerInn: String? = null,
        @SerialName("salary")
        val salary: Double,
        @SerialName("position")
        val position: PositionDto,
        @SerialName("workExperienceTotal")
        val workExperienceTotal: Int,
        @SerialName("workExperienceCurrent")
        val workExperienceCurrent: Int,
    )
}

@Serializable
data class GetApplicationResponseDto(
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("status")
    val status: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("termMonths")
    val termMonths: Int,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String,
    @SerialName("middleName")
    val middleName: String? = null,
    @SerialName("email")
    val email: String,
    @SerialName("birthDate")
    val birthDate: String? = null,
    @SerialName("passportSeries")
    val passportSeries: String,
    @SerialName("passportNumber")
    val passportNumber: String,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("paymentType")
    val paymentType: PaymentTypeDto? = null,
    @SerialName("submitData")
    val submitData: SubmitDataDto? = null,
) {
    @Serializable
    data class SubmitDataDto(
        @SerialName("insuranceEnabled")
        val insuranceEnabled: Boolean? = null,
        @SerialName("salaryClient")
        val salaryClient: Boolean? = null,
        @SerialName("gender")
        val gender: GenderDto? = null,
        @SerialName("maritalStatus")
        val maritalStatus: MaritalStatusDto? = null,
        @SerialName("dependentAmount")
        val dependentAmount: Int? = null,
        @SerialName("passportIssueDate")
        val passportIssueDate: String? = null,
        @SerialName("passportIssueBranch")
        val passportIssueBranch: String? = null,
        @SerialName("accountNumber")
        val accountNumber: String? = null,
        @SerialName("employment")
        val employment: EmploymentDto? = null,
        @SerialName("submittedAt")
        val submittedAt: String? = null,
    )

    @Serializable
    data class EmploymentDto(
        @SerialName("employmentStatus")
        val employmentStatus: EmploymentStatusDto? = null,
        @SerialName("employerInn")
        val employerInn: String? = null,
        @SerialName("salary")
        val salary: Double? = null,
        @SerialName("position")
        val position: PositionDto? = null,
        @SerialName("workExperienceTotal")
        val workExperienceTotal: Int? = null,
        @SerialName("workExperienceCurrent")
        val workExperienceCurrent: Int? = null,
    )
}
