package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CosmeticsStateDto(
    val ownedSkus: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap(),
)
