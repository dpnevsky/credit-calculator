package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentUserProfileDto(
    @SerialName("id")
    val id: String,
    @SerialName("email")
    val email: String,
    @SerialName("firstName")
    val firstName: String? = null,
    @SerialName("lastName")
    val lastName: String? = null,
    @SerialName("middleName")
    val middleName: String? = null,
    @SerialName("birthDate")
    val birthDate: String? = null,
    @SerialName("roles")
    val roles: List<String> = emptyList(),
) {
    val fullName: String
        get() = listOfNotNull(
            lastName?.takeIf { it.isNotBlank() },
            firstName?.takeIf { it.isNotBlank() },
            middleName?.takeIf { it.isNotBlank() },
        ).joinToString(" ").ifBlank { email }
}

data class CurrentSession(
    val user: CurrentUserProfileDto,
    val tokens: StoredAuthTokens,
)
