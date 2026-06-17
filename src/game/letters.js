// "Bir Kelime" — harf oyunu mantığı
// 9 harf seçilir (sesli/sessiz), oyuncu en uzun anlamlı Türkçe kelimeyi bulur.

// Türkçe harf dağılımı (Scrabble TR'ye yakın frekanslar)
const VOWELS = [
  ['a', 12],
  ['e', 8],
  ['i', 7],
  ['ı', 5],
  ['o', 3],
  ['u', 3],
  ['ü', 2],
  ['ö', 1],
];

const CONSONANTS = [
  ['k', 7],
  ['l', 7],
  ['r', 6],
  ['n', 6],
  ['t', 5],
  ['m', 4],
  ['d', 5],
  ['s', 3],
  ['y', 3],
  ['b', 3],
  ['c', 2],
  ['ç', 2],
  ['ş', 2],
  ['z', 2],
  ['p', 2],
  ['h', 2],
  ['g', 2],
  ['v', 1],
  ['f', 1],
  ['j', 1],
  ['ğ', 1],
];

export const TOTAL_TILES = 9;

function weightedDraw(bag) {
  let total = 0;
  for (const [, w] of bag) total += w;
  let r = Math.random() * total;
  for (const [ch, w] of bag) {
    r -= w;
    if (r <= 0) return ch;
  }
  return bag[bag.length - 1][0];
}

export function drawVowel() {
  return weightedDraw(VOWELS);
}

export function drawConsonant() {
  return weightedDraw(CONSONANTS);
}

export function isVowel(ch) {
  return 'aeıioöuü'.includes(ch);
}

// Dengeli, oynanabilir rastgele bir 9 harf üretir (en az 3 sesli, en az 3 sessiz)
export function randomRack() {
  const vowelCount = 3 + Math.floor(Math.random() * 2); // 3 veya 4 sesli
  const rack = [];
  for (let i = 0; i < vowelCount; i++) rack.push(drawVowel());
  for (let i = 0; i < TOTAL_TILES - vowelCount; i++) rack.push(drawConsonant());
  // karıştır
  for (let i = rack.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [rack[i], rack[j]] = [rack[j], rack[i]];
  }
  return rack;
}
