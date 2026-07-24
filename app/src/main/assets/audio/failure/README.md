# failure/

Low-tier results reveal - deliberately framed as encouragement to retry, never punishment (this
app never shows red/harsh styling for a low-star run, and the audio matches that). Two kinds of
file: a short one-shot music sting (`failure.mp3`, through `MusicManager`/ExoPlayer) and a short
one-shot SFX (`SoundPool`) layered on top.

| File | Used for |
|---|---|
| `failure.mp3` | The music sting itself - plays once when the results screen reveals a low-tier outcome |
| `encourage_sting.mp3` | SFX layer for the same moment - warm, gentle, motivating, never a "fail" buzzer |

Resolved by `MusicTrack.assetPath()` (`failure.mp3`) and `SfxId.assetPath()`
(`encourage_sting.mp3`) in `app/src/main/java/com/suman/memoryarchitect/core/feedback/audio/`.
