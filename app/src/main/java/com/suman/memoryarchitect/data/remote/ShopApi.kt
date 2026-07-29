package com.suman.memoryarchitect.data.remote

import com.suman.memoryarchitect.data.remote.dto.CosmeticsStateDto
import com.suman.memoryarchitect.data.remote.dto.EquipCosmeticRequestDto
import com.suman.memoryarchitect.data.remote.dto.LuckySpinStateDto
import com.suman.memoryarchitect.data.remote.dto.PurchaseCosmeticRequestDto
import com.suman.memoryarchitect.data.remote.dto.PurchaseCosmeticResponseDto
import com.suman.memoryarchitect.data.remote.dto.SpinLuckySpinRequestDto
import com.suman.memoryarchitect.data.remote.dto.SpinLuckySpinResponseDto
import com.suman.memoryarchitect.data.remote.dto.UnequipCosmeticRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** `mock-backend/`'s counterpart to [com.suman.memoryarchitect.data.repository.FirestoreShopRemoteSource]
 * - see [com.suman.memoryarchitect.data.repository.ShopRemoteSource]'s doc. Same dev-only,
 * single-shared-state caveat as [ProgressionApi]. */
interface ShopApi {
    @GET("v1/cosmetics")
    suspend fun getCosmeticsState(): CosmeticsStateDto

    @GET("v1/cosmetics/luckySpinState")
    suspend fun getLuckySpinState(): LuckySpinStateDto

    @POST("v1/cosmetics/purchase")
    suspend fun purchase(@Body body: PurchaseCosmeticRequestDto): PurchaseCosmeticResponseDto

    @POST("v1/cosmetics/spin")
    suspend fun spin(@Body body: SpinLuckySpinRequestDto): SpinLuckySpinResponseDto

    @POST("v1/cosmetics/equip")
    suspend fun equip(@Body body: EquipCosmeticRequestDto): CosmeticsStateDto

    @POST("v1/cosmetics/unequip")
    suspend fun unequip(@Body body: UnequipCosmeticRequestDto): CosmeticsStateDto
}
