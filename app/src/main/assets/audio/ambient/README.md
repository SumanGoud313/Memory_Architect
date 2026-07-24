# ambient/

Looping background beds for the **Memorize** phase specifically - deliberately the sparsest,
calmest tracks in the whole soundtrack (a memory game needs concentration, not atmosphere
competing for attention). Full musical brief in `SOUNDTRACK_SPECIFICATION.md` at the project
root.

| File | Used for |
|---|---|
| `classic_memorize.mp3` | Classic mode, Memorize phase |
| `practice_memorize.mp3` | Practice mode, Memorize phase - the calmest track in the app |
| `daily_challenge_memorize.mp3` | Daily Challenge, Memorize phase |
| `weekly_challenge_memorize.mp3` | Weekly Challenge, Memorize phase |

Resolved by `MusicTrack.assetPath()` in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/MusicManager.kt`.
