# sfx/

Gameplay and reward one-shot sound effects, played via `SoundPool` (short, low-latency, can
overlap). Each should be well under 1 second - satisfying and immediate, never a musical phrase.

The three `object_*` rows are the only files in this folder that actually exist today - all
procedurally synthesized by `tools/audio/synth_sfx.js` (run with `node tools/audio/synth_sfx.js
app/src/main/assets/audio/sfx`, regenerable any time), not recorded or sourced from anywhere, so
they carry zero licensing risk. Everything else below is still `PENDING` per `AUDIO_LICENSES.md`.

| File | Used for |
|---|---|
| `object_pickup_1.wav` / `_2.wav` / `_3.wav` | Picking an object up off the tray or off a placed slot - `GameAudioManagerImpl` plays a random one of the three, never repeating the previous pick, so a flurry of pickups doesn't sound identical every time |
| `object_rotate_1.wav` / `_2.wav` / `_3.wav` | Rotating a placed object one step - same random no-repeat variant selection |
| `object_place_1.wav` / `_2.wav` / `_3.wav` | Dropping an object onto a slot (deliberately correctness-neutral - never reveals right/wrong) - same random no-repeat variant selection |
| `combo_step.mp3` | One step of the results-screen combo reveal (played once per combo step, replayed at rising volume) |
| `coin_awarded.mp3` | Coins granted |
| `xp_awarded.mp3` | XP granted |
| `star_awarded.mp3` | Star earned |
| `achievement_unlocked.mp3` | Achievement unlock |
| `level_unlocked.mp3` | Next campaign level unlocked |
| `daily_reward_claimed.mp3` | Daily login-streak reward claimed |
| `weekly_reward_claimed.mp3` | Reserved for a future weekly reward-chest feature - not yet wired to a call site, but the asset slot is ready |

Resolved by `SfxId.assetPath()` in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/SfxId.kt`.
