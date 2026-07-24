# timer/

Countdown one-shot sounds, played via `SoundPool`. The countdown is deliberately audio-only - no
vibration is ever tied to the timer (see `FeedbackManagerImpl.onTimerTick`'s doc comment). Ticks
fire on an accelerating schedule in the last 5 seconds of either the Memorize or Reconstruct
phase (interval shrinks from ~1000ms down to ~150ms as the clock approaches zero), so
`tick_urgent.mp3` in particular will be heard many times in quick succession near the end of a
round - it must stay pleasant under rapid repetition, not read as an alarm.

| File | Used for |
|---|---|
| `tick_soft.mp3` | Countdown, 10s-5s remaining (calm, once per second) |
| `tick_urgent.mp3` | Countdown, last 5s (accelerating - fired far more often than `tick_soft.mp3`) |
| `final_warning.mp3` | The exact moment the timer hits zero - fires once |

Resolved by `SfxId.assetPath()` in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/SfxId.kt`.
