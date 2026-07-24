# Memory Architect — Soundtrack Specification

> **Status note:** the game shipped with a simpler 5-track soundtrack than the per-mode/per-phase
> brief below describes - one shared bed per mood (Home, Gameplay, Countdown, Victory, Failure),
> loaded from `app/src/main/res/raw/` via AndroidX Media3 `ExoPlayer` (see `MusicManagerImpl`),
> not the `assets/audio/` per-mode library this document was originally written against. The
> identity/instrumentation/key-family brief below is kept as-is because it's still the right
> creative reference if the soundtrack is ever expanded back toward distinct per-mode themes -
> just don't take the per-track folder paths/file list literally against the current codebase.
> See `AUDIO_LICENSES.md` for the real, current 5-track list and its (currently unverified)
> licensing status.

This is the composition brief every track in `app/src/main/assets/audio/` is written against. It
exists so a composer, a licensed commercial library search, or a commissioned original session
can produce assets that sound like **one coherent game**, not a folder of unrelated stock tracks.

## Identity

Memory Architect's soundtrack is built around a single unifying idea:

> **The Architect's Motif** — a four-note rising cell, scale degrees **1–2–3–5** (e.g. in D major:
> D–E–F♯–A), played slowly enough to feel like something being *placed down, one piece at a
> time* — a melodic echo of the game's own core action (recalling and rebuilding a scene, object
> by object). It never resolves in a hurry. Every track below either states it plainly (Home,
> Victory), implies it in the harmony without stating it melodically (Memorize phases — kept out
> of the way of concentration), or answers/completes it (Failure's soft major resolution at the
> very end).

**Instrumentation family** (every track draws only from this palette — nothing outside it, so the
whole game shares one sonic world):
- **Piano** — the emotional foundation. Close-mic'd, warm, felt hammers (not a bright concert
  grand) — intimate, never percussive-hard.
- **Flute** (concert or alto) — soft, breathy, used for accents and the motif's higher
  restatements, never a lead melody carrying a whole phrase alone.
- **String ensemble** (small: 2 violins, viola, cello, no full orchestra weight) — warm sustained
  pads and slow-bowed harmony, occasional soft pizzicato for gentle rhythmic texture.
- **Soft mallet/orchestral percussion** — vibraphone, soft frame drum, shaker — always felt, never
  struck hard; the only "rhythm section" this game has.
- **Pads/atmospheric texture** — either a soft synth pad or bowed-string drone doubling as
  sustain glue between the above; never a "synth lead" and never bass-heavy.

**Explicitly excluded everywhere**: heavy/sub bass, distorted or aggressive synths, drum kits,
harsh brass, anything with a fast attack transient loud enough to startle, alarm/siren tones,
recognizable quotations from any existing game, film, or commercial track.

**Key family**: D major is the game's "home." Practice detunes to the subdominant (G major, a
"resting" key) to feel like a safe space; Daily Challenge sits in the dominant (A major, bright,
forward-leaning, ritual-like); Level Select and Failure both touch the relative minor (B minor)
for a moment of introspection before returning home. Every key in the game is a close relative of
D major — nothing modulates far enough to feel like a different soundtrack.

---

## Track index

| Track | File | Mood in one phrase |
|---|---|---|
| Home | `music/home.mp3` | Welcoming, warm, quietly curious |
| Mode Select | `music/mode_select.mp3` | Curious anticipation |
| Level Select | `music/level_select.mp3` | Exploratory, a little wistful |
| Settings | `music/settings.mp3` | Plain, utilitarian, almost silent |
| Classic — Memorize | `ambient/classic_memorize.mp3` | Calm concentration |
| Classic — Reconstruct | `music/classic_reconstruct.mp3` | Focused momentum |
| Practice — Memorize | `ambient/practice_memorize.mp3` | The calmest moment in the game |
| Practice — Reconstruct | `music/practice_reconstruct.mp3` | Gentle, confidence-building |
| Daily Challenge — Memorize | `ambient/daily_challenge_memorize.mp3` | Bright, ritual-like calm |
| Daily Challenge — Reconstruct | `music/daily_challenge_reconstruct.mp3` | Energized but relaxed |
| Weekly Challenge — Memorize | `ambient/weekly_challenge_memorize.mp3` | Elegant, a little grander |
| Weekly Challenge — Reconstruct | `music/weekly_challenge_reconstruct.mp3` | Epic but elegant |
| Victory | `victory/victory.mp3` | Emotionally satisfying celebration |
| Failure | `failure/failure.mp3` | Gentle, warm encouragement |

---

## Home

- **Mood**: Welcoming, warm, quietly curious — the first thing a player hears; it should make
  them want to press Play without any urgency.
- **Tempo**: 72 BPM.
- **Key**: D major.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano (foreground), string pad (sustained, low-mid register), single soft
  flute phrase entering at loop midpoint.
- **Chord Progression**: I – V/vi – vi – IV (D – A/C♯ – Bm – G), a gentle circling progression
  that never fully resolves to a cadence — it always feels like it's about to continue, matching
  a menu screen you're meant to leave.
- **Melody Style**: The Architect's Motif stated once, plainly, on piano, in its original rhythm
  — the clearest statement of it anywhere in the game.
- **Layering**: Bar 1–8: piano + pad only. Bar 9–16: flute enters with a soft countermelody
  answering the motif. Bar 17–24: piano restates the motif one octave up, pad thickens slightly.
  Bar 25–32: everything thins back to piano + pad, setting up the loop.
- **Dynamic Progression**: mp → mf (flute entrance) → mp, a single gentle swell and release across
  the loop, never louder than a comfortable background level.
- **Loop Duration**: 32 bars (~26.7s at 72 BPM).
- **Transition Behaviour**: Crossfades into Mode Select/Profile on navigation (handled in code by
  `MusicManagerImpl`, 400ms).
- **Fade Duration**: 400ms in, 400ms out (crossfade), matching every other track for consistency.
- **Energy Curve**: Flat-low with one gentle rise-and-fall per loop — never spikes.

## Mode Select

- **Mood**: Curious anticipation — "what will I choose to play."
- **Tempo**: 76 BPM.
- **Key**: D major (same harmonic world as Home, one register brighter).
- **Time Signature**: 4/4.
- **Instrumentation**: Piano, string pad, flute (more present than Home's).
- **Chord Progression**: I – iii – IV – V (D – F♯m – G – A), a slightly more forward-moving
  progression than Home's circling one — implies choice/direction.
- **Melody Style**: Motif implied harmonically (the I–iii–IV–V outlines the same 1-2-3-5 shape a
  third apart) rather than stated outright — Home already claimed the clean statement.
- **Layering**: Piano and pad present from bar 1; flute answers every second phrase rather than
  entering once and staying, giving a call-and-response feel appropriate to "considering options."
- **Dynamic Progression**: Even mp throughout — no big swells, since this screen is usually a
  brief stop.
- **Loop Duration**: 24 bars (~18.9s).
- **Transition Behaviour**: Crossfades in from Home/screen-open; crossfades out to Level Select or
  Gameplay.
- **Fade Duration**: 400ms crossfade.
- **Energy Curve**: Flat, slightly brighter than Home, no dynamic events.

## Level Select

- **Mood**: Exploratory, a little wistful — looking down a long road of 100 levels.
- **Tempo**: 80 BPM.
- **Key**: B minor (relative minor of D major), resolving briefly to D major every 8 bars.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano (slightly more rhythmic, arpeggiated left hand), string pad, no flute
  (kept for Home/Mode Select's warmth — Level Select's minor color carries its own character).
- **Chord Progression**: i – VI – III – VII (Bm – G – D – A), with the final bar of every 8-bar
  phrase landing on D major (the "VII" resolving up into the relative major) before circling back
  to Bm — a road that keeps almost-arriving.
- **Melody Style**: The motif's rhythm (long-short-short-long) appears in the piano's arpeggiation
  pattern rather than as a singable melody — texture, not tune.
- **Layering**: Arpeggiated piano throughout; pad swells only under the D major resolution bars.
- **Dynamic Progression**: A small swell exactly on each D major resolution, otherwise flat mp.
- **Loop Duration**: 32 bars (~24s).
- **Transition Behaviour**: Standard 400ms crossfade in/out.
- **Fade Duration**: 400ms.
- **Energy Curve**: Gently oscillating (gets on this pattern of tension/release every 8 bars),
  never climbing overall.

## Settings

- **Mood**: Plain, utilitarian, almost silent — a utility screen shouldn't carry emotional weight.
- **Tempo**: Free/rubato (no strict pulse — the sparsest track in the game).
- **Key**: D major, voiced as a single sustained Dsus2 (no third — deliberately unresolved/neutral).
- **Time Signature**: N/A (free time).
- **Instrumentation**: Solo piano, single sustained soft pedal chord, occasionally re-struck very
  quietly. Nothing else.
- **Chord Progression**: None — one sustained sonority (Dsus2) held and gently re-articulated.
- **Melody Style**: None.
- **Layering**: One layer only.
- **Dynamic Progression**: Constant ppp throughout.
- **Loop Duration**: 20s (long enough that the quiet re-articulation doesn't feel mechanically
  regular).
- **Transition Behaviour**: 400ms crossfade in; per the brief, Settings **never interrupts**
  whatever was already playing elsewhere — the game only ever asks for this track when Settings is
  the actual active screen (`MusicTrack.SETTINGS`), never as an override.
- **Fade Duration**: 400ms.
- **Energy Curve**: Flat-zero.

## Classic — Memorize

- **Mood**: Calm concentration — supports focus, never distracts.
- **Tempo**: 66 BPM.
- **Key**: D major, sparse voicing (piano + pad only, no motion).
- **Time Signature**: 4/4 (felt, not emphasized).
- **Instrumentation**: Piano (very sparse, long sustained notes rather than a moving line), string
  pad underneath. No flute, no percussion — this is the quietest gameplay moment in the game.
- **Chord Progression**: I – IV (D – G), two chords only, alternating every 4 bars — deliberately
  under-eventful.
- **Melody Style**: None - texture only, so nothing competes with the player's visual memorization.
- **Layering**: Single static layer for the whole loop - no build, no development.
- **Dynamic Progression**: Flat pp throughout.
- **Loop Duration**: 16 bars (~14.5s) - short and unremarkable on purpose, since Memorize windows
  are themselves short.
- **Transition Behaviour**: Crossfades in the moment the Memorize phase starts (`GameplayViewModel
  .startMemorizePhase`); crossfades to Classic — Reconstruct the moment Reconstruct starts.
- **Fade Duration**: 400ms.
- **Energy Curve**: Flat-zero.

## Classic — Reconstruct

- **Mood**: Focused momentum - "light rhythmic ambience," per the brief; energized just enough to
  feel like forward motion without becoming exciting enough to distract.
- **Tempo**: 78 BPM.
- **Key**: D major, fuller voicing than Memorize.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano (now moving, a simple quarter-note pulse in the left hand), string
  pad, soft mallet percussion (vibraphone) entering every other bar with a single soft note - the
  "light rhythmic ambience."
- **Chord Progression**: I – V – vi – IV (D – A – Bm – G) - the classic circling pop progression,
  deliberately familiar/comfortable rather than a novel shape, since the player's attention should
  be on the puzzle, not the harmony.
- **Melody Style**: Motif implied by the vibraphone's entrances tracing 1-2-3-5 across the
  progression, very quietly.
- **Layering**: Piano pulse + pad from bar 1; vibraphone enters at bar 5 and stays for the rest of
  the loop.
- **Dynamic Progression**: A slow, single mp → mf rise across the full loop, resetting at the loop
  point - by design this reads as "quietly building," which pairs with
  `FeedbackManagerImpl`'s separate countdown-urgency volume swell in the final 5 seconds of the
  phase without the two ever feeling like they're fighting each other.
- **Loop Duration**: 24 bars (~18.5s).
- **Transition Behaviour**: Crossfades in from Classic — Memorize; crossfades to Victory/Failure
  at Submit.
- **Fade Duration**: 400ms.
- **Energy Curve**: Gentle single rise per loop, capped well below anything urgent.

## Practice — Memorize

- **Mood**: The calmest moment in the entire game - Practice is "no stakes," and its Memorize
  phase should feel like the safest possible place to be.
- **Tempo**: 60 BPM.
- **Key**: G major (subdominant of D - a "resting," settled key).
- **Time Signature**: 4/4 (felt, not emphasized).
- **Instrumentation**: Solo piano only, extremely sparse, wide open voicing with long pedal.
- **Chord Progression**: I (G) held, occasionally coloured with a IV (C) for one bar - almost a
  drone.
- **Melody Style**: None.
- **Layering**: Single layer.
- **Dynamic Progression**: Flat ppp.
- **Loop Duration**: 16 bars (~16s).
- **Transition Behaviour**: 400ms crossfade in/out.
- **Fade Duration**: 400ms.
- **Energy Curve**: Flat-zero, the lowest-energy track in the whole soundtrack.

## Practice — Reconstruct

- **Mood**: Gentle, confidence-building - a warm-up should feel encouraging, not tense.
- **Tempo**: 70 BPM.
- **Key**: G major.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano (light, moving), string pad. No percussion (unlike Classic's
  Reconstruct) - Practice never needs the extra "momentum" texture.
- **Chord Progression**: I – vi – IV – V (G – Em – C – D), a warm, textbook-comfortable
  progression - deliberately the least adventurous harmony in the game, matching Practice's "warm
  up, no stakes" identity.
- **Melody Style**: A very simple, singable 4-note descending answer phrase on piano - the most
  "hummable" moment in the soundtrack, meant to build familiarity/confidence over repeat plays.
- **Layering**: Piano + pad throughout, no build.
- **Dynamic Progression**: Flat mp.
- **Loop Duration**: 20 bars (~17s).
- **Transition Behaviour**: 400ms crossfade in/out.
- **Fade Duration**: 400ms.
- **Energy Curve**: Flat-low, no swell.

## Daily Challenge — Memorize

- **Mood**: Bright, ritual-like calm - "today's puzzle," a small daily occasion.
- **Tempo**: 68 BPM.
- **Key**: A major (dominant of D - brighter, forward-leaning).
- **Time Signature**: 4/4.
- **Instrumentation**: Piano (sparse, as with all Memorize tracks), string pad, one very quiet
  flute note sustained under the pad (the "shimmer" - a touch of occasion without adding motion).
- **Chord Progression**: I – IV (A – D), two chords, matching Classic's Memorize's simplicity but
  in the brighter key.
- **Melody Style**: None (texture only, consistent with every Memorize track).
- **Layering**: Static single layer plus the one sustained flute note.
- **Dynamic Progression**: Flat pp.
- **Loop Duration**: 14 bars (~12.4s) - Daily Challenge's Memorize window is fixed and short, so
  the loop stays tight.
- **Transition Behaviour**: 400ms crossfade in/out.
- **Fade Duration**: 400ms.
- **Energy Curve**: Flat-zero, only the sustained flute note distinguishes it from silence.

## Daily Challenge — Reconstruct

- **Mood**: Energized but relaxed - a little more presence than Classic's Reconstruct, still never
  urgent.
- **Tempo**: 82 BPM.
- **Key**: A major.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano, string pad, soft mallet percussion (as Classic's Reconstruct), plus
  the flute shimmer carried over from its own Memorize track for continuity across the two phases.
- **Chord Progression**: I – V – vi – iii (A – E – F♯m – C♯m), one step more harmonically colorful
  than Classic's I–V–vi–IV, matching "a touch more energetic than campaign."
- **Melody Style**: Flute restates the motif's rhythm softly every 8 bars.
- **Layering**: Piano + pad + percussion from bar 1 (busier immediately than Classic, which
  builds); flute enters at bar 9.
- **Dynamic Progression**: mp → mf steady rise, slightly faster arrival than Classic's.
- **Loop Duration**: 20 bars (~14.6s).
- **Transition Behaviour**: 400ms crossfade in/out; crossfades to Victory/Failure at Submit.
- **Fade Duration**: 400ms.
- **Energy Curve**: A touch higher ceiling than Classic's Reconstruct, still well short of Weekly's.

## Weekly Challenge — Memorize

- **Mood**: Elegant, a little grander - Weekly's higher stakes deserve more presence even at rest.
- **Tempo**: 70 BPM.
- **Key**: D major, full voicing (unlike every other Memorize track, the string section is present
  from the start rather than just a pad).
- **Time Signature**: 4/4.
- **Instrumentation**: Piano, full small string ensemble (sustained, warm), no percussion, no
  flute yet (saved for the Reconstruct half).
- **Chord Progression**: I – IV – I – V (D – G – D – A), still simple, but the fuller string
  voicing makes it read as richer than Classic's equivalent two-chord Memorize.
- **Melody Style**: None (texture only, per every Memorize track's concentration-first rule), but
  the string voicing itself outlines the motif's interval shape as a chord (1-3-5 stacked).
- **Layering**: Full ensemble from bar 1, no build - Weekly starts "already elegant."
- **Dynamic Progression**: Flat mp - louder at rest than any other Memorize track, but still
  static (no swell).
- **Loop Duration**: 16 bars (~13.7s).
- **Transition Behaviour**: 400ms crossfade in/out.
- **Fade Duration**: 400ms.
- **Energy Curve**: Flat but elevated baseline - "presence without motion."

## Weekly Challenge — Reconstruct

- **Mood**: Epic but elegant - a sense of importance without overwhelming the player.
- **Tempo**: 86 BPM.
- **Key**: D major, the fullest orchestration in the game short of Victory itself.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano, full string ensemble, soft mallet percussion, flute - every
  instrument family present simultaneously, the only Reconstruct track where that's true.
- **Chord Progression**: I – V – vi – IV – I – iii – IV – V (D – A – Bm – G – D – F♯m – G – A),
  an 8-chord extended progression (double the length of Classic's) so the "epic" feeling comes
  from harmonic scope, not volume.
- **Melody Style**: The clearest Reconstruct-phase statement of the Architect's Motif anywhere in
  the game, carried by flute over the string pad, restated once per loop at bar 9.
- **Layering**: Full ensemble from bar 1 (matching its Memorize half); flute's motif statement at
  bar 9 is the single "event" of the loop.
- **Dynamic Progression**: mf baseline, swelling to f exactly under the flute's motif statement,
  settling back to mf.
- **Loop Duration**: 24 bars (~16.7s).
- **Transition Behaviour**: 400ms crossfade in/out; crossfades to Victory/Failure at Submit.
- **Fade Duration**: 400ms.
- **Energy Curve**: The highest sustained baseline of any looping track, with one clear peak per
  loop (the motif statement) - "important," never "urgent."

## Victory

- **Mood**: Emotionally satisfying celebration - uplifting piano, warm strings, a subtle
  orchestral flourish, per the brief. One-shot, not looped.
- **Tempo**: 92 BPM, with a slight rallentando (slowing) into the final chord for a "landing"
  feeling rather than an abrupt stop.
- **Key**: D major, full and resolved - the only track in the game that reaches a complete,
  unambiguous perfect cadence.
- **Time Signature**: 4/4.
- **Instrumentation**: Piano, full string ensemble, soft mallet percussion (a single bright
  vibraphone flourish), flute (the motif's final, triumphant statement).
- **Chord Progression**: vi – IV – I – V – I (Bm – G – D – A – D), building through a classic
  "climb to the cadence" shape and landing on a full, sustained D major chord.
- **Melody Style**: The Architect's Motif in augmentation (played slower/grander than anywhere
  else) on flute over the full ensemble, immediately followed by a bright ascending piano
  flourish (a single fast run up to the final chord) - the "subtle orchestral flourish" the brief
  asks for, kept to one gesture rather than a barrage.
- **Layering**: Full ensemble present from the first note (this is a celebration, not a build);
  the flourish is the one added event two-thirds of the way through.
- **Dynamic Progression**: mf → f at the flourish → f sustained through the final chord, then a
  gentle 400ms fade rather than a hard stop.
- **Loop Duration**: N/A - one-shot, ~9 seconds total.
- **Transition Behaviour**: Replaces whatever was playing (crossfade in, 400ms); once finished, the
  next screen's own `TrackScreenView` call takes over music naturally - nothing needs to resume it.
- **Fade Duration**: 400ms in, 400ms out at the very end of the one-shot.
- **Energy Curve**: Single arc: confident start, one clear peak (the flourish), warm landing.

## Failure

- **Mood**: Gentle, encouraging - motivates a retry rather than making the low-star run feel
  punishing. One-shot, not looped.
- **Tempo**: 66 BPM, unhurried.
- **Key**: B minor, resolving to D major in the final two bars - the arc is the whole point:
  starts a little wistful, ends genuinely warm, never stays sad.
- **Time Signature**: 4/4.
- **Instrumentation**: Solo piano for the first half, joined by a single sustained string note
  entering under the resolution to D major - deliberately the sparsest of the one-shot stings,
  since this is a quiet, private moment, not a public celebration.
- **Chord Progression**: i – VII – i – (resolving) V/III – III→I (Bm – A – Bm – F♯/A♯ – D), the
  final chord is a clean D major, not a minor "sad ending."
- **Melody Style**: The Architect's Motif started but left incomplete (only the first three notes,
  1-2-3, no leap to 5) over the minor harmony - it sounds like a question - then the fourth note
  (the "5", completing the shape) finally arrives softly on the D major resolution, answering it.
- **Layering**: Piano alone for bars 1-4; a single sustained string note enters at bar 5 under the
  resolution and holds through the end.
- **Dynamic Progression**: mp throughout, no swell - the warmth comes from the harmonic resolution,
  not from volume.
- **Loop Duration**: N/A - one-shot, ~7 seconds total.
- **Transition Behaviour**: Replaces whatever was playing (crossfade in, 400ms); the next screen's
  own music takes over naturally once the player continues.
- **Fade Duration**: 400ms in, 400ms out.
- **Energy Curve**: Flat-low throughout - the "event" here is harmonic (minor resolving to major),
  not dynamic.
