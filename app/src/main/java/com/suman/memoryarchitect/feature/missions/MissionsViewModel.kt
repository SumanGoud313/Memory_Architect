package com.suman.memoryarchitect.feature.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logEventStartedClientView
import com.suman.memoryarchitect.core.analytics.logInventoryItemGranted
import com.suman.memoryarchitect.core.analytics.logMemoryJourneyPointsEarned
import com.suman.memoryarchitect.core.analytics.logMissionAssigned
import com.suman.memoryarchitect.core.analytics.logMissionCompleted
import com.suman.memoryarchitect.core.analytics.logMissionProgressed
import com.suman.memoryarchitect.core.analytics.logMissionRewardClaimed
import com.suman.memoryarchitect.core.analytics.logMonthlyGoalCompleted
import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.MemoryJourneyRules
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.MissionCatalog
import com.suman.memoryarchitect.domain.usecase.ClaimMissionCategoryBonusUseCase
import com.suman.memoryarchitect.domain.usecase.ClaimMissionRewardUseCase
import com.suman.memoryarchitect.domain.usecase.GetActiveLiveEventUseCase
import com.suman.memoryarchitect.domain.usecase.GetActiveMissionsUseCase
import com.suman.memoryarchitect.domain.usecase.GrantInventoryItemUseCase
import com.suman.memoryarchitect.domain.usecase.UnlockAllMissionsEarlyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Backs the dedicated Missions screen - loads today's active Daily/Weekly/Monthly set (a pure,
 * local, deterministic computation, see [GetActiveMissionsUseCase]'s doc) and handles claiming.
 *
 * Also the one place `mission_assigned`/`mission_progressed`/`mission_completed` get logged -
 * client-side, by diffing consecutive [ActiveMission] snapshots across calls to [refresh] (see
 * [logMissionDiff]) - rather than threading analytics calls through every
 * [com.suman.memoryarchitect.domain.repository.MissionRepository.recordMissionEvent] call site
 * scattered across the app's gameplay/shop/collections ViewModels. */
@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val getActiveMissions: GetActiveMissionsUseCase,
    private val claimMissionReward: ClaimMissionRewardUseCase,
    private val grantInventoryItem: GrantInventoryItemUseCase,
    private val getActiveLiveEvent: GetActiveLiveEventUseCase,
    private val claimMissionCategoryBonus: ClaimMissionCategoryBonusUseCase,
    private val unlockAllMissionsEarly: UnlockAllMissionsEarlyUseCase,
    private val analyticsLogger: AnalyticsLogger,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()

    private val _claimEvents = MutableSharedFlow<MissionClaimResult>(extraBufferCapacity = 1)
    val claimEvents: SharedFlow<MissionClaimResult> = _claimEvents.asSharedFlow()

    /** Fires once a [claimCategoryBonusIfNeeded] call actually grants something (never for the
     * routine already-granted case) - what [com.suman.memoryarchitect.ui.screens.missions.MissionsScreen]
     * uses to show a brief "Daily bonus: +100 Coins!"-style confirmation. */
    private val _categoryBonusEvents = MutableSharedFlow<Pair<MissionPeriod, MissionReward>>(extraBufferCapacity = 1)
    val categoryBonusEvents: SharedFlow<Pair<MissionPeriod, MissionReward>> = _categoryBonusEvents.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val previousMissions = _uiState.value.missions
            // getActiveLiveEvent() runs first and deliberately: it performs a live Remote Config
            // fetch-then-cache (see RemoteConfigRepositoryImpl.getRemoteConfig), while
            // getActiveMissions()'s own internal event lookup (MissionRepositoryImpl.activeEvent)
            // only ever reads whatever's already in that same Room cache, never fetching live
            // itself. On a cold cache, calling getActiveMissions() first used to see an empty
            // cache (no event, so no Event missions computed) even though the live fetch moments
            // later would show the banner - the two disagreeing until the next refresh. Fetching
            // live first warms the cache getActiveMissions() then reads, so both agree within this
            // one refresh instead of racing across two.
            val activeEvent = getActiveLiveEvent()
            if (activeEvent != null) analyticsLogger.logEventStartedClientView(activeEvent.id)
            val missions = getActiveMissions()
            logMissionDiff(previousMissions, missions)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                missions = missions,
                activeEvent = activeEvent,
                nextRotationEpochSecondByPeriod = nextRotationEpochSecondsFor(missions),
            )
            // Opportunistic, not the only trigger - claim()'s own post-claim check below is what
            // normally fires this the instant the third mission in a period completes. This also
            // catches a period that was already fully claimed before this exact screen visit (e.g.
            // the app was closed mid-flow last time) - claimMissionCategoryBonus's own
            // MissionAlreadyClaimedException handling makes a redundant call here harmless.
            MissionPeriod.entries.filter { it != MissionPeriod.EVENT }.forEach { period ->
                if (_uiState.value.isCategoryComplete(period)) claimCategoryBonusIfNeeded(period)
            }
        }
    }

    /** No-op if a claim is already in flight, or [missionId] isn't actually claimable - the UI
     * only ever shows an enabled claim affordance for [com.suman.memoryarchitect.domain.model.ActiveMission.canClaim],
     * but this guards the same invariant here so a double-tap can never double-claim. */
    fun claim(missionId: MissionId) {
        val current = _uiState.value
        if (current.claimingMissionId != null) return
        val mission = current.missions.firstOrNull { it.definition.id == missionId } ?: return
        if (!mission.canClaim) return

        _uiState.value = current.copy(claimingMissionId = missionId, claimError = null)
        viewModelScope.launch {
            when (val outcome = claimMissionReward(missionId)) {
                is Outcome.Success -> {
                    val result = outcome.data
                    grantInventoryItem(result.inventory)
                    logClaim(mission, result)
                    val refreshed = getActiveMissions()
                    _uiState.value = _uiState.value.copy(missions = refreshed, claimingMissionId = null)
                    _claimEvents.emit(result)
                    // The moment this claim completes the whole period's set, grant the category
                    // bonus automatically - "if user completing all 3 daily tasks they need to get
                    // only coins" describes an automatic outcome, not a separate claim action.
                    if (_uiState.value.isCategoryComplete(mission.definition.period)) {
                        claimCategoryBonusIfNeeded(mission.definition.period)
                    }
                }
                is Outcome.Error -> {
                    _uiState.value = _uiState.value.copy(claimingMissionId = null, claimError = outcome.error)
                }
            }
        }
    }

    /** Keyed by (missionId, periodKey) so a mission rotating back into a *new* period reads as a
     * fresh assignment, exactly matching [com.suman.memoryarchitect.data.repository.MissionRepositoryImpl.currentProgress]'s
     * own "stale periodKey means fresh, not stale progress" rule. */
    private fun logMissionDiff(previous: List<ActiveMission>, current: List<ActiveMission>) {
        val previousByKey = previous.associateBy { it.definition.id to it.periodKey }
        current.forEach { mission ->
            val prior = previousByKey[mission.definition.id to mission.periodKey]
            val period = mission.definition.period.name
            when {
                prior == null -> analyticsLogger.logMissionAssigned(mission.definition.id.name, period)
                mission.currentCount > prior.currentCount ->
                    analyticsLogger.logMissionProgressed(mission.definition.id.name, period, mission.currentCount, mission.definition.targetCount)
            }
            if (mission.isComplete && prior?.isComplete != true) {
                analyticsLogger.logMissionCompleted(mission.definition.id.name, period)
                if (mission.definition.period == MissionPeriod.MONTHLY) {
                    analyticsLogger.logMonthlyGoalCompleted(mission.definition.id.name)
                }
            }
        }
    }

    private fun logClaim(mission: ActiveMission, result: MissionClaimResult) {
        analyticsLogger.logMissionRewardClaimed(mission.definition.id.name, mission.definition.period.name, result.reward.coins, result.reward.xp)
        result.reward.inventoryGrants.forEach { (kind, quantity) ->
            analyticsLogger.logInventoryItemGranted(kind.name, quantity, source = "mission_claim")
        }
        analyticsLogger.logMemoryJourneyPointsEarned(source = "mission_claim", points = journeyPointsForPeriod(mission.definition.period))
    }

    /** Mirrors [com.suman.memoryarchitect.data.repository.FirestoreMissionRemoteSource]'s own
     * private `journeyPointsForClaim` - a second, analytics-only copy of the same small mapping,
     * the same "independent copies" convention [MemoryJourneyRules]' doc already documents for
     * this exact table across Kotlin/JS/TS. */
    private fun journeyPointsForPeriod(period: MissionPeriod): Long {
        val rules = MemoryJourneyRules.Default
        return when (period) {
            MissionPeriod.DAILY -> rules.pointsPerDailyMissionClaimed
            MissionPeriod.WEEKLY -> rules.pointsPerWeeklyMissionClaimed
            MissionPeriod.MONTHLY -> rules.pointsPerMonthlyMissionClaimed
            MissionPeriod.EVENT -> rules.pointsPerEventMissionClaimed
        }
    }

    /** One instance per rotating period (Daily/Weekly/Monthly - never Event, which has no rotation
     * boundary of its own, see [MissionCatalog.nextPeriodStartEpochSecond]'s doc), keyed off
     * whatever periodKey that period's own already-loaded missions carry - never re-derived
     * independently, so this can never disagree with what [MissionCatalog.effectivePeriodKey]
     * (inside [GetActiveMissionsUseCase]) already resolved. */
    private fun nextRotationEpochSecondsFor(missions: List<ActiveMission>): Map<MissionPeriod, Long> =
        MissionPeriod.entries.filter { it != MissionPeriod.EVENT }.mapNotNull { period ->
            val periodKey = missions.firstOrNull { it.definition.period == period }?.periodKey ?: return@mapNotNull null
            period to MissionCatalog.nextPeriodStartEpochSecond(period, periodKey, clock.zone)
        }.toMap()

    /** Fires the automatic category-completion bonus - see [claim]'s and [refresh]'s own trigger
     * docs for when this gets called. [ClaimMissionCategoryBonusUseCase] (via
     * [com.suman.memoryarchitect.data.repository.MissionRepositoryImpl.claimCategoryBonus]) already
     * caches the coin/inventory grant locally on success - this only needs to log it and notify the
     * UI. A repeat call once the bonus is already granted resolves to
     * [com.suman.memoryarchitect.data.repository.MissionAlreadyClaimedException] (mapped to a 409
     * here), the routine, expected case - silently ignored, never surfaced as [MissionsUiState.claimError]
     * (that field is reserved for an *individual mission* claim failure the player just attempted). */
    private fun claimCategoryBonusIfNeeded(period: MissionPeriod) {
        val periodKey = _uiState.value.missions.firstOrNull { it.definition.period == period }?.periodKey ?: return
        viewModelScope.launch {
            when (val outcome = claimMissionCategoryBonus(period, periodKey)) {
                is Outcome.Success -> {
                    val reward = outcome.data
                    analyticsLogger.logMissionRewardClaimed("CATEGORY_BONUS_${period.name}", period.name, reward.coins, reward.xp)
                    reward.inventoryGrants.forEach { (kind, quantity) ->
                        analyticsLogger.logInventoryItemGranted(kind.name, quantity, source = "mission_category_bonus")
                    }
                    _categoryBonusEvents.emit(period to reward)
                }
                is Outcome.Error -> Unit
            }
        }
    }

    /** Spends 1,000 coins to immediately roll fresh Daily+Weekly+Monthly sets - only ever callable
     * once [MissionsUiState.canUnlockAllNow] is true (every mission in all three periods already
     * claimed), but this re-checks the same guard here rather than trusting the button's enabled
     * state alone, the same double-tap/stale-state defense [claim] already applies. */
    fun unlockAllMissionsNow() {
        val current = _uiState.value
        if (current.isUnlockingAll || !current.canUnlockAllNow) return
        val dailyPeriodKey = current.missions.first { it.definition.period == MissionPeriod.DAILY }.periodKey
        val weeklyPeriodKey = current.missions.first { it.definition.period == MissionPeriod.WEEKLY }.periodKey
        val monthlyPeriodKey = current.missions.first { it.definition.period == MissionPeriod.MONTHLY }.periodKey

        _uiState.value = current.copy(isUnlockingAll = true, unlockAllError = null)
        viewModelScope.launch {
            when (val outcome = unlockAllMissionsEarly(dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey)) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(isUnlockingAll = false)
                    refresh()
                }
                is Outcome.Error -> {
                    _uiState.value = _uiState.value.copy(isUnlockingAll = false, unlockAllError = outcome.error)
                }
            }
        }
    }
}
