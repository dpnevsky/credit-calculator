package com.example.creditcalculatormobile.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface DocumentsApi {

    @Streaming
    @GET("api/documents/{documentId}/download")
    suspend fun downloadDocument(
        @Path("documentId") documentId: String,
    ): Response<ResponseBody>
}
