package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EquipCosmeticRequestDto(
    val category: String,
    val sku: String,
)
