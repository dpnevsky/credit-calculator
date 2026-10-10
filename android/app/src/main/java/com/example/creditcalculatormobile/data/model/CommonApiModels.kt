package com.example.creditcalculatormobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponseDto(
    @SerialName("timestamp")
    val timestamp: String? = null,
    @SerialName("status")
    val status: Int? = null,
    @SerialName("error")
    val error: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("path")
    val path: String? = null,
)

data class DownloadedDocument(
    val bytes: ByteArray,
    val fileName: String?,
    val mimeType: String?,
)
