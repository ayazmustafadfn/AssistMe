// Kod ile meydan okuma: sorular bir seed'den deterministik üretilir,
// böylece iki oyuncu da BİREBİR aynı soruları görür. Backend gerekmez.
import { generatePuzzle } from './numbers';
import { randomRack } from './letters';
import { bestWords } from './dictionary';

// Hızlı, taşınabilir PRNG (mulberry32) — JS motorları arasında aynı sonuç verir
export function mulberry32(a) {
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Soru başına ayrı ama deterministik tohum
function questionSeed(seed, i) {
  return (Math.imul(seed ^ (i + 1), 0x9e3779b1) >>> 0);
}

function seededRack(lang, rng) {
  for (let i = 0; i < 10; i++) {
    const r = randomRack(lang, rng);
    if (bestWords(r, lang).length >= 3) return r;
  }
  return randomRack(lang, rng);
}

const DUEL_SEQ = ['letters', 'numbers'];

// Bir meydan okumanın tüm sorularını deterministik üretir.
// challenge: { game, lang, total, seed }
export function buildQuestions({ game, lang, total, seed }) {
  const seq = game === 'duel' ? DUEL_SEQ : Array(total).fill(game);
  return seq.map((type, i) => {
    const rng = mulberry32(questionSeed(seed, i));
    if (type === 'numbers') return { type: 'numbers', puzzle: generatePuzzle({ rng }) };
    return { type: 'letters', rack: seededRack(lang, rng) };
  });
}

// ---- Kod kodlama / çözme ----
const GAME_C = { numbers: 'N', letters: 'L', duel: 'D' };
const GAME_R = { N: 'numbers', L: 'letters', D: 'duel' };
const TOTAL_C = { 2: '2', 5: '5', 10: 'X' };
const TOTAL_R = { 2: 2, 5: 5, X: 10 };

function b36(n, len) {
  let s = Math.floor(n).toString(36).toUpperCase();
  while (s.length < len) s = '0' + s;
  return s;
}

function checksum(s) {
  let sum = 0;
  for (let i = 0; i < s.length; i++) sum += s.charCodeAt(i);
  return (sum % 36).toString(36).toUpperCase();
}

// payload: { game, mode, lang, total, seed, score }
export function encodeChallenge(p) {
  const body =
    '1' +
    GAME_C[p.game] +
    (p.mode === 'yarisma' ? 'T' : 'R') +
    (p.lang === 'tr' ? 'T' : 'E') +
    TOTAL_C[p.total] +
    b36(p.seed >>> 0, 7) +
    b36(Math.max(0, Math.min(46655, p.score || 0)), 3);
  return body + checksum(body);
}

export function decodeChallenge(raw) {
  if (!raw) return null;
  const s = raw.toUpperCase().replace(/[^0-9A-Z]/g, '');
  if (s.length !== 16) return null;
  const body = s.slice(0, 15);
  if (checksum(body) !== s[15]) return null;
  if (s[0] !== '1') return null;
  const game = GAME_R[s[1]];
  const total = TOTAL_R[s[4]];
  if (!game || !total) return null;
  const seed = parseInt(s.slice(5, 12), 36) >>> 0;
  const score = parseInt(s.slice(12, 15), 36);
  if (Number.isNaN(seed) || Number.isNaN(score)) return null;
  return {
    game,
    mode: s[2] === 'T' ? 'yarisma' : 'rahat',
    lang: s[3] === 'T' ? 'tr' : 'en',
    total: game === 'duel' ? 2 : total,
    seed,
    score,
  };
}

export function randomSeed() {
  return (Math.floor(Math.random() * 0xffffffff) >>> 0);
}
