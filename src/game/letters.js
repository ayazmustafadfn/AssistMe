// "Bir Kelime / Letters" — harf oyunu mantığı (TR + EN)
// 9 harf seçilir (sesli/sessiz), oyuncu en uzun anlamlı kelimeyi bulur.

// Türkçe harf dağılımı (Scrabble TR'ye yakın)
const TR_VOWELS = [
  ['a', 12], ['e', 8], ['i', 7], ['ı', 5], ['o', 3], ['u', 3], ['ü', 2], ['ö', 1],
];
const TR_CONSONANTS = [
  ['k', 7], ['l', 7], ['r', 6], ['n', 6], ['t', 5], ['d', 5], ['m', 4], ['s', 3],
  ['y', 3], ['b', 3], ['c', 2], ['ç', 2], ['ş', 2], ['z', 2], ['p', 2], ['h', 2],
  ['g', 2], ['v', 1], ['f', 1], ['j', 1], ['ğ', 1],
];

// İngilizce harf dağılımı (Scrabble EN)
const EN_VOWELS = [
  ['e', 12], ['a', 9], ['i', 9], ['o', 8], ['u', 4],
];
const EN_CONSONANTS = [
  ['n', 6], ['r', 6], ['t', 6], ['l', 4], ['s', 4], ['d', 4], ['g', 3], ['b', 2],
  ['c', 2], ['m', 2], ['p', 2], ['f', 2], ['h', 2], ['v', 2], ['w', 2], ['y', 2],
  ['k', 1], ['j', 1], ['x', 1], ['q', 1], ['z', 1],
];

const BAGS = {
  tr: { vowels: TR_VOWELS, consonants: TR_CONSONANTS, vowelChars: 'aeıioöuü' },
  en: { vowels: EN_VOWELS, consonants: EN_CONSONANTS, vowelChars: 'aeiou' },
};

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

export function drawVowel(lang) {
  return weightedDraw(BAGS[lang].vowels);
}

export function drawConsonant(lang) {
  return weightedDraw(BAGS[lang].consonants);
}

export function isVowel(ch, lang) {
  return BAGS[lang].vowelChars.includes(ch);
}

// Dengeli rastgele 9 harf (3-4 sesli)
export function randomRack(lang) {
  const vowelCount = 3 + Math.floor(Math.random() * 2);
  const rack = [];
  for (let i = 0; i < vowelCount; i++) rack.push(drawVowel(lang));
  for (let i = 0; i < TOTAL_TILES - vowelCount; i++) rack.push(drawConsonant(lang));
  for (let i = rack.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [rack[i], rack[j]] = [rack[j], rack[i]];
  }
  return rack;
}
