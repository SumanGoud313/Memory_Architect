package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.progression.PlayerRank

fun PlayerRank.toDisplayName(context: Context): String = when (this) {
    PlayerRank.BRONZE -> context.getString(R.string.rank_bronze)
    PlayerRank.SILVER -> context.getString(R.string.rank_silver)
    PlayerRank.GOLD -> context.getString(R.string.rank_gold)
    PlayerRank.PLATINUM -> context.getString(R.string.rank_platinum)
    PlayerRank.DIAMOND -> context.getString(R.string.rank_diamond)
    PlayerRank.MASTER -> context.getString(R.string.rank_master)
    PlayerRank.GRANDMASTER -> context.getString(R.string.rank_grandmaster)
    PlayerRank.LEGEND -> context.getString(R.string.rank_legend)
}
