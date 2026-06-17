import AsyncStorage from '@react-native-async-storage/async-storage';

const HISTORY_KEY = '@bkbi/history/v1';
const HIGH_KEY = '@bkbi/highscores/v1';
const HISTORY_LIMIT = 50;

// En yüksek skorlar, oyun + soru sayısı kırılımında tutulur: "numbers-5", "duel-2" ...
function highKey(game, total) {
  return `${game}-${total}`;
}

export async function loadHistory() {
  try {
    const raw = await AsyncStorage.getItem(HISTORY_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    return [];
  }
}

export async function loadHighScores() {
  try {
    const raw = await AsyncStorage.getItem(HIGH_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch (e) {
    return {};
  }
}

// Bir oyun sonucunu kaydeder. entry: { game, mode, lang, total, score, max }
// Geriye { isRecord, best } döner.
export async function saveResult(entry) {
  const record = { ...entry, id: Date.now(), date: new Date().toISOString() };

  const history = await loadHistory();
  history.unshift(record);
  if (history.length > HISTORY_LIMIT) history.length = HISTORY_LIMIT;
  await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(history));

  const highs = await loadHighScores();
  const key = highKey(entry.game, entry.total);
  const prevBest = highs[key]?.score ?? -1;
  let isRecord = false;
  if (entry.score > prevBest) {
    highs[key] = { score: entry.score, max: entry.max, date: record.date, lang: entry.lang };
    isRecord = true;
    await AsyncStorage.setItem(HIGH_KEY, JSON.stringify(highs));
  }
  return { isRecord, best: Math.max(entry.score, prevBest) };
}

export async function getBest(game, total) {
  const highs = await loadHighScores();
  return highs[highKey(game, total)] || null;
}

export async function clearAll() {
  await AsyncStorage.multiRemove([HISTORY_KEY, HIGH_KEY]);
}
