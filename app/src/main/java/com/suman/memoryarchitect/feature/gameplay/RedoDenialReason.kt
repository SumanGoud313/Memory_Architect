package com.suman.memoryarchitect.feature.gameplay

/** Why a requested Redo was rejected - [NOTHING_TO_UNDO] takes priority over [OUT_OF_USES] when
 * both are true, since the redo budget is moot if there's nothing placed yet to undo. */
enum class RedoDenialReason {
    NOTHING_TO_UNDO,
    OUT_OF_USES,
}
