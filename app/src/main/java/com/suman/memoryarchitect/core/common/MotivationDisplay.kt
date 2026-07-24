package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.motivation.MotivationInsight

fun MotivationInsight.toDisplayMessage(context: Context): String = when (this) {
    is MotivationInsight.NewPersonalBest -> context.getString(R.string.motivation_new_personal_best)
    is MotivationInsight.LevelsToNextRank -> {
        val nextRankName = nextRank.toDisplayName(context)
        if (levels == 1) {
            context.getString(R.string.motivation_levels_to_next_rank, levels, nextRankName)
        } else {
            context.getString(R.string.motivation_levels_to_next_rank_plural, levels, nextRankName)
        }
    }
    is MotivationInsight.AccuracyImproved -> context.getString(R.string.motivation_accuracy_improved, percentPoints)
    is MotivationInsight.ObjectsMemorizedMilestone -> context.getString(R.string.motivation_objects_solved, total)
    is MotivationInsight.TopPercentThisWeek -> context.getString(R.string.motivation_top_percent_week, percent)
}
