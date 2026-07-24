# Audio assets

> **Music has moved.** Background music now loads from `app/src/main/res/raw/` (5 files:
> `home_theme.mp3`, `gameplay_theme.mp3`, `countdown_theme.mp3`, `victory_theme.mp3`,
> `failure_theme.mp3`) via AndroidX Media3 `ExoPlayer`/`RawResourceDataSource`, not from
> `music/`/`ambient/`/`victory/`/`failure/` below - those folders (and `MusicTrack.assetPath()`
> they used to describe) are retired; see `MusicManagerImpl.kt` and `AUDIO_LICENSES.md`. **Sound
> effects are unaffected** and still work exactly as described below - `sfx/`/`ui/`/`timer/`
> remain the real, current plan for `SfxId`/`GameAudioManager`.

This app plays **only real audio files from this folder** (sound effects now - see above for
music) - there is no procedural/generated audio anywhere in the codebase. Until a real file exists
at the exact path a manager looks for, that sound simply doesn't play (silent no-op, never a
crash, never a substitute generated tone) - see `AudioAssetManager.assetExists()` in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/`.

**Right now every folder below is empty of real audio** - this repository ships with the full
playback architecture (ExoPlayer for music, SoundPool for SFX - see `MusicManagerImpl.kt`/
`GameAudioManagerImpl.kt`) wired up and ready, waiting for real files to be dropped in. Nothing
needs to change in code when that happens - drop a correctly-named file into the right folder and
it plays.

## Folders

| Folder | Contents | Player |
|---|---|---|
| `music/` | Looping background beds for menus and the Reconstruct phase of every game mode | ExoPlayer, seamless loop |
| `ambient/` | Looping background beds for the Memorize phase specifically - deliberately the sparsest, calmest tracks in the app | ExoPlayer, seamless loop |
| `sfx/` | Gameplay and reward one-shot sound effects | SoundPool |
| `ui/` | Menu/navigation one-shot sound effects | SoundPool |
| `victory/` | Best-tier results reveal (music sting + SFX) | ExoPlayer (sting) / SoundPool (SFX) |
| `failure/` | Low-tier results reveal, framed as encouragement (music sting + SFX) | ExoPlayer (sting) / SoundPool (SFX) |
| `timer/` | Countdown tick/warning one-shots | SoundPool |

Each folder has its own `README.md` listing the exact filename, format, and duration expected for
every file - that's the full manifest `SfxId.assetPath()` / `MusicTrack.assetPath()`
(`core/feedback/audio/SfxId.kt`, `core/feedback/audio/MusicManager.kt`) resolve against.

## Format guidance

- **Format**: MP3 (44.1kHz, 192kbps CBR is plenty for both music and SFX) - universally supported
  by both ExoPlayer and SoundPool with no extra Gradle configuration. OGG Vorbis is an equally
  valid alternative if that's what a sourced/composed track arrives as.
- **Music loop files**: export with the loop point already trimmed to the sample - ExoPlayer's
  `REPEAT_MODE_ONE` restarts the file exactly at 0:00, so any silence or fade baked into the
  file's own start/end will be audible as a seam on every loop. See
  `SOUNDTRACK_SPECIFICATION.md`'s "Loop Duration"/"Transition Behaviour" fields for what a
  composer/sourcing engineer needs per track.
- **One-shot SFX**: keep genuinely short (the "Timer/UI/SFX" one-shot families in
  `SOUNDTRACK_SPECIFICATION.md` are all under 1 second by design) so `SoundPool`'s low-latency
  playback stays snappy.

## Sourcing - see `AUDIO_LICENSES.md`

Every file placed here must be commercially safe for a worldwide Google Play release before it's
added - `AUDIO_LICENSES.md` at the project root tracks exactly that, per file, once real assets
exist.
