package com.suman.memoryarchitect.domain.model

data class RemoteConfig(
    val values: Map<String, String>,
    val fetchedAt: Long,
)