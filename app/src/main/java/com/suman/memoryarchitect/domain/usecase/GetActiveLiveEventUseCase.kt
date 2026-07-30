package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.LiveEvent
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.LiveEventCatalog
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Resolves the currently-live [LiveEvent], if any, by combining [RemoteConfigRepository] (which
 * `id`/window is live right now - see [LiveEventCatalog.activeEvent]'s doc) with [LiveEventCatalog]
 * (what that `id` actually looks like). A [RemoteConfigRepository] failure with no cached fallback
 * is treated the same as "no event active" here - a banner that fails to show is much less harmful
 * than one that crashes a screen, and every other Remote Config consumer already tolerates staleness.
 */
class GetActiveLiveEventUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): LiveEvent? {
        val remoteConfig = (remoteConfigRepository.getRemoteConfig() as? Outcome.Success)?.data ?: return null
        return LiveEventCatalog.activeEvent(remoteConfig, Instant.now(clock).epochSecond)
    }
}
