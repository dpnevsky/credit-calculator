package com.example.creditcalculatormobile.data.remote

import com.example.creditcalculatormobile.core.network.ApiPaths
import com.example.creditcalculatormobile.data.model.AuthResponseDto
import com.example.creditcalculatormobile.data.model.CurrentUserProfileDto
import com.example.creditcalculatormobile.data.model.LoginRequestDto
import com.example.creditcalculatormobile.data.model.LogoutRequestDto
import com.example.creditcalculatormobile.data.model.RefreshRequestDto
import com.example.creditcalculatormobile.data.model.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST(ApiPaths.AUTH_LOGIN)
    suspend fun login(
        @Body request: LoginRequestDto,
    ): Response<AuthResponseDto>

    @POST(ApiPaths.AUTH_REGISTER)
    suspend fun register(
        @Body request: RegisterRequestDto,
    ): Response<AuthResponseDto>

    @POST(ApiPaths.AUTH_REFRESH)
    suspend fun refresh(
        @Body request: RefreshRequestDto,
    ): Response<AuthResponseDto>

    @GET(ApiPaths.AUTH_ME)
    suspend fun getCurrentUser(
        @Header("Authorization") bearerToken: String,
    ): Response<CurrentUserProfileDto>

    @POST(ApiPaths.AUTH_LOGOUT)
    suspend fun logout(
        @Body request: LogoutRequestDto,
    ): Response<Unit>
}
