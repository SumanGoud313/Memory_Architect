// Procedurally synthesizes short, original UI sound effects as 16-bit PCM mono WAV files.
// No samples, no external audio libraries, no recorded material of any kind - every sample is
// computed from scratch by the waveform/envelope math below. Written for Memory Architect's
// object pickup/rotate/place sound-variation system (see SfxId.variantAssetPaths()).

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

// A soft, satisfying "pop/lift" - rising pitch sweep with a fast attack and quick decay.
// Used for OBJECT_PICKUP - signals "this is now in hand."
function synthPickup(baseFreq, seed) {
  const durationSec = 0.16;
  const n = durationSamples(durationSec);
  const out = new Float32Array(n);
  for (let i = 0; i < n; i++) {
    const t = i / SAMPLE_RATE;
    const sweep = baseFreq + (baseFreq * 0.5) * (t / durationSec); // rises over the sound's life
    const env = envelope(t, durationSec, 0.006, 26);
    const tone = Math.sin(2 * Math.PI * sweep * t);
    const harmonic = 0.25 * Math.sin(2 * Math.PI * sweep * 2 * t);
    out[i] = env * (tone + harmonic) * 0.6;
  }
  return out;
}

// A crisp, brief mechanical "tick" - two quick filtered clicks close together, like a ratchet
// notch. Used for OBJECT_ROTATE - a light, low-commitment sound for a reversible action.
function synthRotate(baseFreq, seed) {
  const durationSec = 0.09;
  const n = durationSamples(durationSec);
  const out = new Float32Array(n);
  const clickAt = 0.012; // second micro-click offset, gives the "ratchet" character
  for (let i = 0; i < n; i++) {
    const t = i / SAMPLE_RATE;
    const env1 = envelope(t, durationSec, 0.002, 90);
    const tone1 = Math.sin(2 * Math.PI * baseFreq * t);
    let s = env1 * tone1;
    if (t >= clickAt) {
      const t2 = t - clickAt;
      const env2 = envelope(t2, durationSec, 0.002, 90) * 0.55;
      s += env2 * Math.sin(2 * Math.PI * (baseFreq * 0.82) * t2);
    }
    out[i] = s * 0.55;
  }
  return out;
}

// A grounded, low "thud/clunk" with a brief higher-pitched contact transient on top - signals
// "this has landed" without implying correctness. Used for OBJECT_PLACE.
function synthPlace(baseFreq, seed) {
  const durationSec = 0.2;
  const n = durationSamples(durationSec);
  const out = new Float32Array(n);
  for (let i = 0; i < n; i++) {
    const t = i / SAMPLE_RATE;
    const bodyEnv = envelope(t, durationSec, 0.004, 16);
    const body = Math.sin(2 * Math.PI * baseFreq * t) + 0.3 * Math.sin(2 * Math.PI * baseFreq * 2 * t);
    const contactEnv = envelope(t, durationSec, 0.001, 140); // very fast, just the initial "tap"
    const contact = Math.sin(2 * Math.PI * (baseFreq * 5.5) * t);
    out[i] = (bodyEnv * body * 0.6 + contactEnv * contact * 0.25);
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
    const samples = spec.synth(freq, index);
    const filePath = path.join(outDir, `${spec.name}_${index + 1}.wav`);
    writeWav(filePath, samples);
    console.log(`wrote ${filePath} (${samples.length} samples, ${(samples.length / SAMPLE_RATE).toFixed(3)}s)`);
  });
}
