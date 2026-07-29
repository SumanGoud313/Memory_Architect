# victory/

Best-tier results reveal. Two kinds of file: a short one-shot music sting (`victory.mp3`, played
through `MusicManager`/ExoPlayer, replaces whatever was playing) and short one-shot SFX
(`SoundPool`) layered on top of it.

| File | Used for |
|---|---|
| `victory.mp3` | The music sting itself - plays once when the results screen reveals a Perfect/Memory Master outcome |
| `victory_sting.mp3` | SFX layer for the same moment |
| `great_sting.mp3` | SFX for the mid-tier "Great" results reveal (still a win, one notch below Victory) |
| `lucky_spin_win.wav` | Lucky Spin's own reward reveal (coins or a cosmetic) - a bigger, more festive fanfare than `victory_sting.mp3`, reserved for this one moment. Procedurally synthesized by `tools/audio/synth_sfx.js` (regenerate with `node tools/audio/synth_sfx.js app/src/main/assets/audio/sfx` - it writes this file into this sibling folder too), not recorded or sourced from anywhere, so it carries zero licensing risk. |

Resolved by `MusicTrack.assetPath()` (`victory.mp3`) and `SfxId.assetPath()`
(`victory_sting.mp3`/`great_sting.mp3`/`lucky_spin_win.wav`) in
`app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/`.
