package com.suman.memoryarchitect.core.debug

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory-only override of which [com.suman.memoryarchitect.domain.progression.LiveEventCatalog]
 * event id (if any) should resolve as active, checked *ahead of* Remote Config by both
 * [com.suman.memoryarchitect.domain.usecase.GetActiveLiveEventUseCase] and
 * [com.suman.memoryarchitect.data.repository.MissionRepositoryImpl]'s own `activeEvent()`, debug
 * builds only.
 *
 * [DebugTestGrantor.debugTriggerSeasonalEvent] previously only wrote `event_active_id` into the
 * Room Remote Config cache - which looked like it worked, but didn't survive contact with the
 * screen it was meant to be previewed on: [RemoteConfigRepositoryImpl.getRemoteConfig] always
 * attempts a live fetch first and writes the result straight over that same cache row, and
 * [FirebaseRemoteConfigSource]'s own `event_active_id` default is `""` - so the very next live
 * Remote Config fetch (unthrottled in debug builds) silently clobbered the override back to "no
 * event" before a tester ever saw it, indistinguishable from the button doing nothing at all. An
 * in-memory value Remote Config's write path never touches is the only override that actually
 * survives being looked at.
 */
@Singleton
class DebugLiveEventOverride @Inject constructor() {
    @Volatile
    var activeEventId: String? = null
}
