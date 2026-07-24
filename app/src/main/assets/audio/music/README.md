# music/

Looping background beds for menu screens and every mode's **Reconstruct** phase. Full musical
brief (mood, tempo, key, instrumentation, etc.) for each is in `SOUNDTRACK_SPECIFICATION.md` at
the project root - this file just lists the exact filename each one resolves to.

| File | Used for |
|---|---|
| `home.mp3` | Home screen |
| `mode_select.mp3` | Mode Select and Profile screens |
| `level_select.mp3` | Level Select screen |
| `settings.mp3` | Settings screen |
| `classic_reconstruct.mp3` | Classic mode, Reconstruct phase |
| `practice_reconstruct.mp3` | Practice mode, Reconstruct phase |
| `daily_challenge_reconstruct.mp3` | Daily Challenge, Reconstruct phase |
| `weekly_challenge_reconstruct.mp3` | Weekly Challenge, Reconstruct phase |

Resolved by `MusicTrack.assetPath()` in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/MusicManager.kt`.
