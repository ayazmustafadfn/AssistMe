// Türkçe sözlük yardımcıları (TDK tabanlı, temizlenmiş liste)
import WORDS from '../data/words.json';

const WORD_SET = new Set(WORDS);

export function trLower(s) {
  return s.toLocaleLowerCase('tr');
}

export function isValidWord(w) {
  if (!w) return false;
  return WORD_SET.has(trLower(w.trim()));
}

// Harf çoklu-kümesinden (rack) bir kelime kurulabilir mi?
function canBuild(word, rackCount) {
  const need = {};
  for (const ch of word) {
    need[ch] = (need[ch] || 0) + 1;
    if (!rackCount[ch] || need[ch] > rackCount[ch]) return false;
  }
  return true;
}

// Verilen harflerden kurulabilecek en uzun kelimeleri bulur.
// rack: harf dizisi (örn. ['a','r','k',...])
export function bestWords(rack, limit = 5) {
  const rackCount = {};
  for (const ch of rack) {
    const c = trLower(ch);
    rackCount[c] = (rackCount[c] || 0) + 1;
  }
  const rackLen = rack.length;
  let found = [];
  let bestLen = 0;
  for (let i = 0; i < WORDS.length; i++) {
    const w = WORDS[i];
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
  // Aynı uzunlukta birden çok varsa rastgele birkaçını döndür
  if (found.length > limit) {
    // basit karıştır
    for (let i = found.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [found[i], found[j]] = [found[j], found[i]];
    }
    found = found.slice(0, limit);
  }
  return { words: found, length: bestLen };
}

export const WORD_COUNT = WORDS.length;
