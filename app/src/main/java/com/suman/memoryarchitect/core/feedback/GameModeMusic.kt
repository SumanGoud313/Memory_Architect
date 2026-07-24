package com.suman.memoryarchitect.core.feedback

import com.suman.memoryarchitect.core.feedback.audio.MusicTrack
import com.suman.memoryarchitect.domain.model.GameMode

/**
 * One shared gameplay bed for every mode and both phases - Memorize and Reconstruct (including a
 * Rewatch replay's own Memorize) all resolve to the exact same [MusicTrack.GAMEPLAY] value, so
 * [com.suman.memoryarchitect.core.feedback.audio.MusicManagerImpl.setTrack] never sees a
 * track-identity change on a phase transition and the bed plays on uninterrupted rather than
 * crossfading into itself. [GameMode] is accepted (rather than these being plain constants) only
 * so call sites read the same way regardless of mode and this stays the one place that would
 * change if per-mode beds are ever reintroduced.
 */
fun GameMode.gameplayMusicTrack(): MusicTrack = MusicTrack.GAMEPLAY
