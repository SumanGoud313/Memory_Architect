# Audio licenses

## Current status: 5 music files present, license source UNVERIFIED - action needed before publishing

Music now plays from 5 real files in `app/src/main/res/raw/` (`home_theme.mp3`,
`gameplay_theme.mp3`, `countdown_theme.mp3`, `victory_theme.mp3`, `failure_theme.mp3`), loaded via
AndroidX Media3 `ExoPlayer`/`RawResourceDataSource` - see `MusicManagerImpl`. **These files were
placed directly into the project; nothing in this repository or its history records where they
came from, who created them, or under what license.** That means every row in the Music table
below is `FILE PRESENT - LICENSE UNVERIFIED`, not `VERIFIED` - the file existing is not the same
claim as the file being cleared for commercial distribution. **Do not submit this app to Google
Play until a human confirms the actual source/license of each file and this table is updated
accordingly** (see "How to add an asset" below - the same verification steps apply retroactively
to a file that's already in the tree). Sound effects (`SfxId`) are a separate case: all but three
remain genuinely absent, same as before. The object pickup/rotate/place sounds are the one
exception - those 9 files (3 randomly-varied takes each, see `SfxId.variantAssetPaths()`) are
procedurally synthesized in-code (sine/envelope waveform synthesis, no samples, no recordings, no
external source of any kind) specifically so they carry zero licensing risk - see their rows below,
marked `VERIFIED - Original`.

This document is the tracking sheet for these assets - every row below should move to `VERIFIED`
only once its license has actually been confirmed commercially safe.

## How to add an asset

1. Source or commission the file per its row below and `SOUNDTRACK_SPECIFICATION.md`'s brief.
2. Place it at the exact path listed (e.g. `app/src/main/assets/audio/music/home.mp3`).
3. Fill in this file's **Source** and **License** columns for that row with the real, verifiable
   details (composer name + commission agreement, or the exact commercial library + license tier
   purchased).
4. Update **Verification Status** to `VERIFIED` only after confirming the license explicitly
   covers: worldwide distribution, a commercial (paid or ad-monetized) mobile game, unlimited
   installs, and no attribution requirement in-app (or, if attribution is required, add it to the
   app's credits screen before shipping and note that requirement here).
5. Never mark a row `VERIFIED` on the assumption a source is safe - confirm the actual license
   terms first.

## Acceptable sources

- **Commissioned original composition** - a composer/sound designer engaged directly for this
  project, with a written work-for-hire or exclusive-license agreement transferring full
  commercial rights to this game.
- **A properly licensed commercial royalty-free library** *at a tier that explicitly permits
  commercial game distribution with unlimited installs* - many "royalty-free" libraries only cover
  personal/non-commercial use or cap install counts at their free tier; the paid/commercial tier's
  exact terms must be read, not assumed.
- **Original AI-generated audio, only if the generating service's terms explicitly grant full
  commercial usage rights** for the specific output, confirmed in writing/ToS at the time of
  generation.

## Unacceptable sources (never use, under any circumstance)

- Any copyrighted music or sound effect, however small the excerpt.
- Anything downloaded from YouTube, TikTok, Instagram, or any social platform, regardless of the
  uploader's claimed license.
- Music or sound effects from any movie, TV show, or commercial game - including anything that
  merely imitates a recognizable theme, motif, or signature sound design from one.
- Any "free for personal use" library tier used in a commercial release.

## Music (`MusicTrack`, resolved via `MusicTrack.rawResId()`)

One shared track per mood now, not a per-mode/per-phase library - see `MusicTrack.kt`'s doc for
why. `Countdown` is not a `MusicTrack.setTrack()` target; it plays as a bounded overlay during a
Reconstruct round's final 10 seconds (see `MusicManager.startCountdownOverlay`).

| Track | Path | Purpose | Source | License | Commercial Use | Attribution | Status |
|---|---|---|---|---|---|---|---|
| Home | `res/raw/home_theme.mp3` | Every non-gameplay screen (Home, Mode Select, Level Select, Profile, Settings) | *(unknown - not recorded)* | *(unknown)* | — | — | FILE PRESENT - LICENSE UNVERIFIED |
| Gameplay | `res/raw/gameplay_theme.mp3` | Every mode, both Memorize and Reconstruct | *(unknown - not recorded)* | *(unknown)* | — | — | FILE PRESENT - LICENSE UNVERIFIED |
| Countdown | `res/raw/countdown_theme.mp3` | Reconstruct's final 10 seconds, overlaid on Gameplay | *(unknown - not recorded)* | *(unknown)* | — | — | FILE PRESENT - LICENSE UNVERIFIED |
| Victory | `res/raw/victory_theme.mp3` | Results screen, round passed | *(unknown - not recorded)* | *(unknown)* | — | — | FILE PRESENT - LICENSE UNVERIFIED |
| Failure | `res/raw/failure_theme.mp3` | Results screen, round not passed | *(unknown - not recorded)* | *(unknown)* | — | — | FILE PRESENT - LICENSE UNVERIFIED |

## Sound effects (`SfxId`, resolved via `SfxId.assetPath()`)

| Sound | Path | Purpose | Source | License | Commercial Use | Attribution | Status |
|---|---|---|---|---|---|---|---|
| UI Tap | `ui/tap.mp3` | Generic button/card tap | *(none yet)* | *(none yet)* | — | — | PENDING |
| UI Confirm | `ui/confirm.mp3` | Affirmative dialog action | *(none yet)* | *(none yet)* | — | — | PENDING |
| UI Back | `ui/back.mp3` | Navigating back | *(none yet)* | *(none yet)* | — | — | PENDING |
| Dialog Open | `ui/dialog_open.mp3` | A dialog appears | *(none yet)* | *(none yet)* | — | — | PENDING |
| Dialog Close | `ui/dialog_close.mp3` | A dialog dismisses | *(none yet)* | *(none yet)* | — | — | PENDING |
| Screen Open | `ui/screen_open.mp3` | Navigating to a new screen | *(none yet)* | *(none yet)* | — | — | PENDING |
| Object Pickup (3 variants) | `sfx/object_pickup_{1,2,3}.wav` | Picking an object up (random no-repeat variant per play) | Procedurally synthesized in-code (sine sweep + harmonic, envelope-shaped) | Original work - full commercial rights, no attribution required | Yes | No | VERIFIED - Original |
| Object Rotate (3 variants) | `sfx/object_rotate_{1,2,3}.wav` | Rotating a placed object (random no-repeat variant per play) | Procedurally synthesized in-code (dual micro-click waveform, envelope-shaped) | Original work - full commercial rights, no attribution required | Yes | No | VERIFIED - Original |
| Object Place (3 variants) | `sfx/object_place_{1,2,3}.wav` | Dropping an object on a slot (random no-repeat variant per play) | Procedurally synthesized in-code (low body tone + contact transient, envelope-shaped) | Original work - full commercial rights, no attribution required | Yes | No | VERIFIED - Original |
| Combo Step | `sfx/combo_step.mp3` | Results-screen combo reveal | *(none yet)* | *(none yet)* | — | — | PENDING |
| Coin Awarded | `sfx/coin_awarded.mp3` | Coins granted | *(none yet)* | *(none yet)* | — | — | PENDING |
| XP Awarded | `sfx/xp_awarded.mp3` | XP granted | *(none yet)* | *(none yet)* | — | — | PENDING |
| Star Awarded | `sfx/star_awarded.mp3` | Star earned | *(none yet)* | *(none yet)* | — | — | PENDING |
| Achievement Unlocked | `sfx/achievement_unlocked.mp3` | Achievement unlock | *(none yet)* | *(none yet)* | — | — | PENDING |
| Level Unlocked | `sfx/level_unlocked.mp3` | Next campaign level unlocked | *(none yet)* | *(none yet)* | — | — | PENDING |
| Daily Reward Claimed | `sfx/daily_reward_claimed.mp3` | Daily login reward claimed | *(none yet)* | *(none yet)* | — | — | PENDING |
| Weekly Reward Claimed | `sfx/weekly_reward_claimed.mp3` | Reserved, not yet wired to a call site | *(none yet)* | *(none yet)* | — | — | PENDING |
| Timer Tick (soft) | `timer/tick_soft.mp3` | Countdown, 10s-5s remaining | *(none yet)* | *(none yet)* | — | — | PENDING |
| Timer Tick (urgent) | `timer/tick_urgent.mp3` | Countdown, last 5s (accelerating) | *(none yet)* | *(none yet)* | — | — | PENDING |
| Timer Final Warning | `timer/final_warning.mp3` | The instant the timer hits zero | *(none yet)* | *(none yet)* | — | — | PENDING |
| Victory Sting | `victory/victory_sting.mp3` | SFX layer under the Victory music sting | *(none yet)* | *(none yet)* | — | — | PENDING |
| Great Sting | `victory/great_sting.mp3` | SFX for the mid-tier "Great" reveal | *(none yet)* | *(none yet)* | — | — | PENDING |
| Encourage Sting | `failure/encourage_sting.mp3` | SFX layer under the Failure music sting | *(none yet)* | *(none yet)* | — | — | PENDING |

## Haptics

Not an audio asset, so not tracked in this table, but documented here for completeness since it's
part of the same feedback system: vibration patterns are generated in code via
`android.os.VibrationEffect` (`core/feedback/haptics/HapticsManagerImpl.kt`), a first-party
Android platform API, not a third-party asset - no license concern applies. See that file for the
current pattern set (four strength tiers; nothing tied to the timer, music, or ambient sound).
