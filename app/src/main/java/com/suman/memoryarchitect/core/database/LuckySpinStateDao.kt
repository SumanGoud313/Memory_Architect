package com.suman.memoryarchitect.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LuckySpinStateDao {

    @Query("SELECT * FROM lucky_spin_state WHERE id = ${LuckySpinStateEntity.SINGLETON_ID}")
    suspend fun get(): LuckySpinStateEntity?

    @Upsert
    suspend fun upsert(entity: LuckySpinStateEntity)
}
