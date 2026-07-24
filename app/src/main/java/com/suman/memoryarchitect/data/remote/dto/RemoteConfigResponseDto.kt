package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteConfigResponseDto(
    val values: Map<String, String>,
)