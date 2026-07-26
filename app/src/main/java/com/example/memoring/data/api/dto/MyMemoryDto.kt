package com.example.memoring.data.api.dto

import com.google.gson.annotations.SerializedName

data class MyMemoryResponse(
    @SerializedName("responseData") val responseData: ResponseData?,
    @SerializedName("responseStatus") val responseStatus: Int?
)

data class ResponseData(
    @SerializedName("translatedText") val translatedText: String?,
    @SerializedName("match") val match: Double?
)