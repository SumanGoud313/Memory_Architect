// Procedurally synthesizes short, original UI sound effects as 16-bit PCM mono WAV files.
// No samples, no external audio libraries, no recorded material of any kind - every sample is
// computed from scratch by the waveform/envelope math below. Written for Memory Architect's
// object pickup/rotate/place sound-variation system (see SfxId.variantAssetPaths()).
//
// Also generates the 3 premium OBJECT_MATERIAL sound families (metallic/organic/crystalline -
// see domain/model/SfxMaterialFamily.kt) as themed variant sets, e.g. object_pickup_metallic_1.wav
// - same synth functions as the baseline, driven by a MATERIAL_PROFILES timbre profile instead of
// the neutral default, so a premium material sounds distinct without any new synthesis technique.

const fs = require("fs");
const path = require("path");

const SAMPLE_RATE = 44100;

function writeWav(filePath, samples) {
  const numSamples = samples.length;
  const byteRate = SAMPLE_RATE * 2; // mono, 16-bit
  const dataSize = numSamples * 2;
  const buffer = Buffer.alloc(44 + dataSize);

  buffer.write("RIFF", 0);
  buffer.writeUInt32LE(36 + dataSize, 4);
  buffer.write("WAVE", 8);
  buffer.write("fmt ", 12);
  buffer.writeUInt32LE(16, 16); // fmt chunk size
  buffer.writeUInt16LE(1, 20); // PCM
  buffer.writeUInt16LE(1, 22); // mono
  buffer.writeUInt32LE(SAMPLE_RATE, 24);
  buffer.writeUInt32LE(byteRate, 28);
  buffer.writeUInt16LE(2, 32); // block align
  buffer.writeUInt16LE(16, 34); // bits per sample
  buffer.write("data", 36);
  buffer.writeUInt32LE(dataSize, 40);

  for (let i = 0; i < numSamples; i++) {
    const clamped = Math.max(-1, Math.min(1, samples[i]));
    buffer.writeInt16LE(Math.round(clamped * 32767), 44 + i * 2);
  }

  fs.writeFileSync(filePath, buffer);
}

function durationSamples(seconds) {
  return Math.round(seconds * SAMPLE_RATE);
}

// Smooth attack/decay envelope: linear attack to 1, then exponential decay to ~0.
function envelope(t, durationSec, attackSec, decayRate) {
  if (t < attackSec) return t / attackSec;
  return Math.exp(-decayRate * (t - attackSec));
}

// A timbre profile drives the same synth math toward a different character - pitchMult shifts
// the fundamental, harmonicGain/harmonicMult shape the close overtone already present in the
// baseline sound, decayMult scales how fast the body dies away (>1 = shorter/more muted, <1 =
// longer/more sustained), and an optional ringGain/ringMult/ringDecayMult layers a separate,
// slower-decaying high overtone on top - the "ring" a metal or glass object has that a wood/fabric
// one doesn't. Each synth function's own default reproduces its original, already-committed
// baseline output exactly (verified identical to the pre-existing hardcoded constants below).
const NEUTRAL_PROFILE = { pitchMult: 1, harmonicGain: 0.25, harmonicMult: 2, decayMult: 1, ringGain: 0, ringMult: 1, ringDecayMult: 1 };
const PLACE_NEUTRAL_PROFILE = { ...NEUTRAL_PROFILE, harmonicGain: 0.3 };

const MATERIAL_PROFILES = {
  // Royal/Luxury/Cyber - hard, resonant, metal-on-metal. Brighter overtone plus a slow-decaying
  // high ring that the baseline timbre has none of.
  metallic: { pitchMult: 1.12, harmonicGain: 0.4, harmonicMult: 2.8, decayMult: 0.55, ringGain: 0.22, ringMult: 4.2, ringDecayMult: 0.35 },
  // Nature/Founder's Pack/Starter Bundle - soft, warm, wood/fabric/earth. Lower pitch, a duller
  // overtone, faster decay (muted, no ring at all).
  organic: { pitchMult: 0.88, harmonicGain: 0.15, harmonicMult: 1.5, decayMult: 1.35, ringGain: 0, ringMult: 1, ringDecayMult: 1 },
  // Space Collection - bright, glassy, chime-like. Highest pitch and the longest, brightest ring
  // of the three families.
  crystalline: { pitchMult: 1.4, harmonicGain: 0.5, harmonicMult: 3.6, decayMult: 0.5, ringGain: 0.3, ringMult: 5.5, ringDecayMult: 0.3 },
};

// A soft, satisfying "pop/lift" - rising pitch sweep with a fast attack and quick decay.
// Used for OBJECT_PICKUP - signals "this is now in hand."
function synthPickup(baseFreq, profile = NEUTRAL_PROFILE) {
  const durationSec = 0.16;
  const n = durationSamples(durationSec);
  const out = new Float32Array(n);
  const freq = baseFreq * profile.pitchMult;
  for (let i = 0; i < n; i++) {
    const t = i / SAMPLE_RATE;
    const sweep = freq + (freq * 0.5) * (t / durationSec); // rises over the sound's life
    const env = envelope(t, durationSec, 0.006, 26 * profile.decayMult);
    const tone = Math.sin(2 * Math.PI * sweep * t);
    const harmonic = profile.harmonicGain * Math.sin(2 * Math.PI * sweep * profile.harmonicMult * t);
    let s = env * (tone + harmonic);
    if (profile.ringGain > 0) {
      const ringEnv = envelope(t, durationSec, 0.006, 26 * profile.ringDecayMult);
      s += profile.ringGain * ringEnv * Math.sin(2 * Math.PI * sweep * profile.ringMult * t);
    }
    out[i] = s * 0.6;
  }
  return out;
}

// A crisp, brief mechanical "tick" - two quick filtered clicks close together, like a ratchet
// notch. Used for OBJECT_ROTATE - a light, low-commitment sound for a reversible action.
function synthRotate(baseFreq, profile = NEUTRAL_PROFILE) {
  const durationSec = 0.09;
  const n = durationSamples(durationSec);
  const out = new Float32Array(n);
  const freq = baseFreq * profile.pitchMult;
  const clickAt = 0.012; // second micro-click offset, gives the "ratchet" character
  for (let i = 0; i < n; i++) {
    const t = i / SAMPLE_RATE;
    const env1 = envelope(t, durationSec, 0.002, 90 * profile.decayMult);
    const tone1 = Math.sin(2 * Math.PI * freq * t);
    let s = env1 * tone1;
    if (t >= clickAt) {
      const t2 = t - clickAt;
      const env2 = envelope(t2, durationSec, 0.002, 90 * profile.decayMult) * 0.55;
      s += env2 * Math.sin(2 * Math.PI * (freq * 0.82) * t2);
    }
    if (profile.ringGain > 0) {
      const ringEnv = envelope(t, durationSec, 0.002, 90 * profile.ringDecayMult);
      s += profile.ringGain * ringEnv * Math.sin(2 * Math.PI * freq * profile.ringMult * t);
    }
    out[i] = s * 0.55;
  }
  return out;
}

// A grounded, low "thud/clunk" with a brief higher-pitched contact transient on top - signals
// "this has landed" without implying correctness. Used for OBJECT_PLACE.
function synthPlace(baseFreq, profile = PLACE_NEUTRAL_PROFILE) {
  const durationSec = 0.2;
  const n = durationSamples(durationSec);
  const out = new Float32Array(n);
  const freq = baseFreq * profile.pitchMult;
  for (let i = 0; i < n; i++) {
    const t = i / SAMPLE_RATE;
    const bodyEnv = envelope(t, durationSec, 0.004, 16 * profile.decayMult);
    const body = Math.sin(2 * Math.PI * freq * t) + profile.harmonicGain * Math.sin(2 * Math.PI * freq * profile.harmonicMult * t);
    const contactEnv = envelope(t, durationSec, 0.001, 140); // very fast, just the initial "tap"
    const contact = Math.sin(2 * Math.PI * (freq * 5.5) * t);
    let s = bodyEnv * body * 0.6 + contactEnv * contact * 0.25;
    if (profile.ringGain > 0) {
      const ringEnv = envelope(t, durationSec, 0.004, 16 * profile.ringDecayMult);
      s += profile.ringGain * ringEnv * Math.sin(2 * Math.PI * freq * profile.ringMult * t);
    }
    out[i] = s;
  }
  return out;
}

// A 5.000s "wheel spinning" ratchet for the Lucky Spin wheel (SfxId.LUCKY_SPIN_ROTATE) - a
// sequence of short percussive clicks whose spacing starts fast (rapid ticking) and eases longer
// near the end (decelerating, via a power curve on the gap-between-clicks), mirroring
// LuckySpinWheel's own continuous-then-settling animation. Pitch drops slightly as it slows, the
// same "a real wheel sounds lower as it loses speed" cue a physical ratchet has. Exactly 5.000s so
// it plays once, unlooped, for the wheel's entire spin.
function synthSpinRotate() {
  const totalDurationSec = 5.0;
  const n = durationSamples(totalDurationSec);
  const out = new Float32Array(n);

  const minGap = 0.035;
  const maxGap = 0.34;
  const clickDurationSec = 0.05;
  const clickN = durationSamples(clickDurationSec);
  let t = 0;
  while (t < totalDurationSec) {
    const progress = t / totalDurationSec;
    const clickFreq = 850 - progress * 250;
    const startSample = Math.round(t * SAMPLE_RATE);
    for (let i = 0; i < clickN && startSample + i < n; i++) {
      const ct = i / SAMPLE_RATE;
      const env = envelope(ct, clickDurationSec, 0.002, 70);
      out[startSample + i] += env * Math.sin(2 * Math.PI * clickFreq * ct) * 0.5;
    }
    const gap = minGap + (maxGap - minGap) * Math.pow(progress, 2.2);
    t += gap;
  }
  return out;
}

// A ~1.8s festive reward fanfare for a Lucky Spin reveal (SfxId.LUCKY_SPIN_WIN) - bigger and more
// ornamented than the existing victory_sting: a quick rising major arpeggio (root/third/fifth/
// octave, each a bright bell tone with harmonic + slow-decaying "ring") followed by a shimmering
// high-partial sparkle tail so the fanfare doesn't just stop dead after the last note. Normalized
// at the end since the overlapping notes/shimmer can otherwise sum past full scale.
function synthLuckySpinWin() {
  const totalDurationSec = 1.8;
  const n = durationSamples(totalDurationSec);
  const out = new Float32Array(n);

  const baseFreq = 440; // A4
  const ratios = [1, 1.25, 1.5, 2]; // major arpeggio: root, third, fifth, octave
  const noteStartSec = [0, 0.14, 0.28, 0.44];
  const noteDurationSec = 0.9;
  ratios.forEach((ratio, index) => {
    const freq = baseFreq * ratio;
    const startSample = Math.round(noteStartSec[index] * SAMPLE_RATE);
    const noteN = durationSamples(noteDurationSec);
    for (let i = 0; i < noteN && startSample + i < n; i++) {
      const t = i / SAMPLE_RATE;
      const env = envelope(t, noteDurationSec, 0.008, 3.2);
      const tone = Math.sin(2 * Math.PI * freq * t);
      const harmonic = 0.3 * Math.sin(2 * Math.PI * freq * 2 * t);
      const ringEnv = envelope(t, noteDurationSec, 0.008, 1.6);
      const ring = 0.22 * ringEnv * Math.sin(2 * Math.PI * freq * 3 * t);
      out[startSample + i] += env * (tone + harmonic) * 0.35 + ring * 0.35;
    }
  });

  const shimmerStartSec = 0.5;
  const shimmerDurationSec = 1.3;
  const shimmerStartSample = Math.round(shimmerStartSec * SAMPLE_RATE);
  const shimmerN = durationSamples(shimmerDurationSec);
  for (let i = 0; i < shimmerN && shimmerStartSample + i < n; i++) {
    const t = i / SAMPLE_RATE;
    const env = envelope(t, shimmerDurationSec, 0.05, 2.4) * 0.18;
    const sparkleFreq = 2600 + 900 * Math.sin(2 * Math.PI * 3.2 * t);
    out[shimmerStartSample + i] += env * Math.sin(2 * Math.PI * sparkleFreq * t);
  }

  let peak = 0;
  for (let i = 0; i < n; i++) peak = Math.max(peak, Math.abs(out[i]));
  if (peak > 1) {
    for (let i = 0; i < n; i++) out[i] /= peak;
  }
  return out;
}

const outDir = process.argv[2];
if (!outDir) {
  console.error("usage: node synth_sfx.js <output-dir>");
  process.exit(1);
}
fs.mkdirSync(outDir, { recursive: true });

// 3 variants per sound: same character, gently varied base pitch so repeats never sound
// identical - the whole point of the no-consecutive-repeat system this feeds.
const specs = [
  { name: "object_pickup", synth: synthPickup, baseFreqs: [560, 620, 500] },
  { name: "object_rotate", synth: synthRotate, baseFreqs: [980, 1080, 900] },
  { name: "object_place", synth: synthPlace, baseFreqs: [210, 240, 190] },
];

for (const spec of specs) {
  spec.baseFreqs.forEach((freq, index) => {
    const samples = spec.synth(freq); // default (neutral) profile - untheme'd baseline
    const filePath = path.join(outDir, `${spec.name}_${index + 1}.wav`);
    writeWav(filePath, samples);
    console.log(`wrote ${filePath} (${samples.length} samples, ${(samples.length / SAMPLE_RATE).toFixed(3)}s)`);
  });
}

// Premium OBJECT_MATERIAL themed variant sets - same 3 sounds x 3 variants, once per family, run
// through the exact same synth functions with that family's MATERIAL_PROFILES timbre instead of
// the neutral default. See SfxId.variantAssetPaths()'s themed naming convention.
for (const [familyName, profile] of Object.entries(MATERIAL_PROFILES)) {
  for (const spec of specs) {
    spec.baseFreqs.forEach((freq, index) => {
      const samples = spec.synth(freq, profile);
      const filePath = path.join(outDir, `${spec.name}_${familyName}_${index + 1}.wav`);
      writeWav(filePath, samples);
      console.log(`wrote ${filePath} (${samples.length} samples, ${(samples.length / SAMPLE_RATE).toFixed(3)}s)`);
    });
  }
}

// Lucky Spin's two dedicated sounds - single files, no variants. lucky_spin_win.wav lives in a
// sibling victory/ folder (not sfx/), matching SfxId.assetPath()'s existing "victory/" prefix -
// outDir itself is expected to be .../assets/audio/sfx, so victory/ is outDir's own sibling.
{
  const rotateSamples = synthSpinRotate();
  const rotatePath = path.join(outDir, "lucky_spin_rotate.wav");
  writeWav(rotatePath, rotateSamples);
  console.log(`wrote ${rotatePath} (${rotateSamples.length} samples, ${(rotateSamples.length / SAMPLE_RATE).toFixed(3)}s)`);

  const victoryDir = path.join(path.dirname(outDir), "victory");
  fs.mkdirSync(victoryDir, { recursive: true });
  const winSamples = synthLuckySpinWin();
  const winPath = path.join(victoryDir, "lucky_spin_win.wav");
  writeWav(winPath, winSamples);
  console.log(`wrote ${winPath} (${winSamples.length} samples, ${(winSamples.length / SAMPLE_RATE).toFixed(3)}s)`);
}
