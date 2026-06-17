// Dil-duyarlı sözlük yardımcıları (TR + EN)
import TR from '../data/words.json';
import EN from '../data/words_en.json';

const LISTS = { tr: TR, en: EN };
const SETS = {}; // dil başına Set tembel kurulur

function getSet(lang) {
  if (!SETS[lang]) SETS[lang] = new Set(LISTS[lang]);
  return SETS[lang];
}

export function lower(s, lang) {
  return lang === 'tr' ? s.toLocaleLowerCase('tr') : s.toLowerCase();
}

export function upper(s, lang) {
  return lang === 'tr' ? s.toLocaleUpperCase('tr') : s.toUpperCase();
}

export function isValidWord(w, lang) {
  if (!w) return false;
  return getSet(lang).has(lower(w.trim(), lang));
}

function canBuild(word, rackCount) {
  const need = {};
  for (const ch of word) {
    need[ch] = (need[ch] || 0) + 1;
    if (!rackCount[ch] || need[ch] > rackCount[ch]) return false;
  }
  return true;
}

// Verilen harflerden kurulabilecek en uzun kelimeleri bulur.
export function bestWords(rack, lang, limit = 5) {
  const list = LISTS[lang];
  const rackCount = {};
  for (const ch of rack) {
    const c = lower(ch, lang);
    rackCount[c] = (rackCount[c] || 0) + 1;
  }
  const rackLen = rack.length;
  let found = [];
  let bestLen = 0;
  for (let i = 0; i < list.length; i++) {
    const w = list[i];
    if (w.length < bestLen) continue;
    if (w.length > rackLen) continue;
    if (canBuild(w, rackCount)) {
      if (w.length > bestLen) {
        bestLen = w.length;
        found = [w];
      } else if (w.length === bestLen) {
        found.push(w);
      }
    }
  }
  if (found.length > limit) {
    for (let i = found.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [found[i], found[j]] = [found[j], found[i]];
    }
    found = found.slice(0, limit);
  }
  return { words: found, length: bestLen };
}

export function wordCount(lang) {
  return LISTS[lang].length;
}
