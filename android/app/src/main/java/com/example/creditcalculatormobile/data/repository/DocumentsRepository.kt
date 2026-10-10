package com.example.creditcalculatormobile.data.repository

import com.example.creditcalculatormobile.core.network.ApiFailure
import com.example.creditcalculatormobile.core.network.ApiResult
import com.example.creditcalculatormobile.data.model.DownloadedDocument
import com.example.creditcalculatormobile.data.remote.DocumentsApi
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

class DocumentsRepository(
    private val documentsApi: DocumentsApi,
) {
    suspend fun downloadDocument(
        documentId: String,
    ): ApiResult<DownloadedDocument> {
        return try {
            val response = documentsApi.downloadDocument(documentId)
            if (!response.isSuccessful) {
                return ApiResult.Failure(
                    ApiFailure.Http(
                        code = response.code(),
                        message = when (response.code()) {
                            401 -> "Сессия истекла или токен недействителен"
                            403 -> "Недостаточно прав для скачивания документа"
                            404 -> "Документ не найден"
                            else -> "Не удалось скачать документ"
                        },
                    ),
                )
            }

            val responseBody = response.body()
            if (responseBody == null) {
                return ApiResult.Failure(
                    ApiFailure.Http(
                        code = response.code(),
                        message = "Сервер вернул пустой документ",
                    ),
                )
            }

            val fileName = extractFileName(
                response.headers()["Content-Disposition"],
            )

            ApiResult.Success(
                value = DownloadedDocument(
                    bytes = withContext(Dispatchers.IO) { responseBody.use { it.bytes() } },
                    fileName = fileName,
                    mimeType = response.headers()["Content-Type"],
                ),
                code = response.code(),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            ApiResult.Failure(
                ApiFailure.Network(cause = exception),
            )
        } catch (exception: SerializationException) {
            ApiResult.Failure(
                ApiFailure.Serialization(cause = exception),
            )
        } catch (exception: Exception) {
            ApiResult.Failure(
                ApiFailure.Unknown(cause = exception),
            )
        }
    }

    private fun extractFileName(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null

        val utf8Match = Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE)
            .find(contentDisposition)
            ?.groupValues
            ?.getOrNull(1)

        if (!utf8Match.isNullOrBlank()) {
            return java.net.URLDecoder.decode(utf8Match, Charsets.UTF_8.name())
        }

        val plainMatch = Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE)
            .find(contentDisposition)
            ?.groupValues
            ?.getOrNull(1)

        return plainMatch?.takeIf { it.isNotBlank() }
    }
}
