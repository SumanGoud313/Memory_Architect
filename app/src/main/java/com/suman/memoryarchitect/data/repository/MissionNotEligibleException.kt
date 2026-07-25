package com.suman.memoryarchitect.data.repository

/** Thrown from inside a Firestore transaction (see [FirestoreMissionRemoteSource.claimMissionReward])
 * when a claim fails either of the two independently server-recomputable checks: the mission
 * wasn't actually part of that period's deterministic rotation (see
 * [com.suman.memoryarchitect.domain.progression.MissionCatalog.activeMissionIds]), or the
 * reported progress hasn't actually reached the mission's target. Both are treated as one
 * "this claim was never valid" condition rather than distinguished, since neither should ever
 * happen from an unmodified client - this exists to reject a stale or forged request, not to
 * give a legitimate player two different error messages to react to. */
class MissionNotEligibleException(reason: String) : RuntimeException("Mission claim rejected: $reason")
