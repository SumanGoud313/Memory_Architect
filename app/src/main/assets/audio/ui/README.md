# ui/

Menu/navigation one-shot sound effects, played via `SoundPool`. Every button, card, and dialog in
the app routes through one shared tap handler (`rememberHapticsTick()` /
`FeedbackManager.onUiTap()`), so `tap.mp3` in particular is the single most frequently played
sound in the entire app - it must hold up to being heard hundreds of times a session without
becoming fatiguing (soft, short, unobtrusive).

| File | Used for |
|---|---|
| `tap.mp3` | Generic button/card tap - the most frequent sound in the app |
| `confirm.mp3` | Affirmative action (e.g. a dialog's confirm button) |
| `back.mp3` | Navigating back |
| `dialog_open.mp3` | A dialog appears |
| `dialog_close.mp3` | A dialog dismisses |
| `screen_open.mp3` | Navigating to a new top-level screen |

Resolved by `SfxId.assetPath()` in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/SfxId.kt`.
