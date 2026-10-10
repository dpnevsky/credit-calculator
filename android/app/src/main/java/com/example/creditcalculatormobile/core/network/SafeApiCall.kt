package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.data.model.ApiErrorResponseDto
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

sealed interface ApiResult<out T> {
    data class Success<T>(
        val value: T,
        val code: Int,
    ) : ApiResult<T>

    data class Failure(
        val error: ApiFailure,
    ) : ApiResult<Nothing>
}

sealed class ApiFailure(
    open val message: String,
    open val cause: Throwable? = null,
) {
    data class Network(
        override val message: String = "Не удалось выполнить сетевой запрос",
        override val cause: Throwable? = null,
    ) : ApiFailure(message, cause)

    data class Http(
        val code: Int,
        override val message: String,
        val errorBody: ApiErrorResponseDto? = null,
        override val cause: Throwable? = null,
    ) : ApiFailure(message, cause)

    data class Serialization(
        override val message: String = "Ошибка обработки ответа сервера",
        override val cause: Throwable? = null,
    ) : ApiFailure(message, cause)

    data class Unknown(
        override val message: String = "Непредвиденная ошибка",
        override val cause: Throwable? = null,
    ) : ApiFailure(message, cause)
}

suspend inline fun <T> safeApiCall(
    json: Json,
    crossinline request: suspend () -> Response<T>,
): ApiResult<T> {
    return try {
        val response = request()

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(
                    value = body,
                    code = response.code(),
                )
            } else {
                ApiResult.Failure(
                    ApiFailure.Http(
                        code = response.code(),
                        message = "Сервер вернул пустой ответ",
                    ),
                )
            }
        } else {
            ApiResult.Failure(
                response.toApiFailure(json),
            )
        }
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
    } catch (exception: HttpException) {
        ApiResult.Failure(
            ApiFailure.Http(
                code = exception.code(),
                message = exception.message(),
                cause = exception,
            ),
        )
    } catch (exception: Exception) {
        ApiResult.Failure(
            ApiFailure.Unknown(cause = exception),
        )
    }
}

fun ApiFailure.readableMessage(): String = when (this) {
    is ApiFailure.Network -> message
    is ApiFailure.Http -> message
    is ApiFailure.Serialization -> message
    is ApiFailure.Unknown -> message
}

fun <T> ApiResult<T>.getOrNull(): T? = when (this) {
    is ApiResult.Success -> value
    is ApiResult.Failure -> null
}

fun <T> ApiResult<T>.errorOrNull(): ApiFailure? = when (this) {
    is ApiResult.Success -> null
    is ApiResult.Failure -> error
}

@PublishedApi
internal fun <T> Response<T>.toApiFailure(json: Json): ApiFailure.Http {
    val rawError = try {
        errorBody()?.string()
    } catch (_: Exception) {
        null
    }

    val parsedError = rawError
        ?.takeIf { it.isNotBlank() }
        ?.let { body ->
            runCatching {
                json.decodeFromString(ApiErrorResponseDto.serializer(), body)
            }.getOrNull()
        }

    val message = parsedError?.message
        ?: parsedError?.error
        ?: when (code()) {
            400 -> "Некорректный запрос"
            401 -> "Сессия истекла или токен недействителен"
            403 -> "Недостаточно прав для выполнения операции"
            404 -> "Запрошенный ресурс не найден"
            409 -> "Конфликт данных"
            422 -> "Запрос не прошёл валидацию"
            in 500..599 -> "Сервер временно недоступен"
            else -> "Ошибка HTTP ${code()}"
        }

    return ApiFailure.Http(
        code = code(),
        message = message,
        errorBody = parsedError,
    )
}
