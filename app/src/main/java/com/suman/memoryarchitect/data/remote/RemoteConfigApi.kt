package com.suman.memoryarchitect.data.remote

import com.suman.memoryarchitect.data.remote.dto.RemoteConfigResponseDto
import retrofit2.http.GET

interface RemoteConfigApi {
    @GET("v1/config/remote")
    suspend fun getRemoteConfig(): RemoteConfigResponseDto
}