import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';

const STR = {
  tr: {
    badge: 'KELİME & ZEKÂ OYUNU',
    appTitle1: 'Bir Kelime',
    appTitle2: 'Bir İşlem',
    tagline: 'Harflerle kelime kur, sayılarla hedefi yakala.',

    secLang: 'DİL',
    secMode: 'OYUN MODU',
    secLength: 'SORU SAYISI',
    secSelect: 'TUR SEÇ',

    modeTimed: 'Yarışma',
    modeRelaxed: 'Rahat',
    qShort: 'soru',

    gNumbersTitle: 'Bir İşlem',
    gNumbersSub: '6 sayı • hedefe ulaş',
    gLettersTitle: 'Bir Kelime',
    gLettersSub: '9 harf • en uzun kelime',
    gDuelTitle: 'Düello',
    gDuelSub: 'Önce kelime, sonra işlem',

    scores: 'Skorlar',
    bestShort: 'Rekor',
    footTimed: 'Yarışma modu: her tur süreli, doğru cevaba puan.',
    footRelaxed: 'Rahat mod: süre yok, dilediğin kadar düşün.',
    footDict: '{n} kelimelik sözlük',

    // ortak
    back: 'Geri',
    seconds: 'saniye',
    question: 'Soru',
    points: 'puan',
    total: 'Toplam',
    playAgain: 'Tekrar Oyna',
    home: 'Ana Menü',
    next: 'Sonraki →',
    seeResults: 'Sonuçlara Geç →',
    newQuestion: 'Yeni Soru',
    newRound: 'Yeni Tur',

    // numbers
    target: 'HEDEF',
    numbers: 'SAYILAR',
    operation: 'İŞLEM',
    undo: '↶ Geri Al',
    reset: 'Sıfırla',
    showSolution: 'Çözümü Göster',
    endRound: 'Turu Bitir →',
    exampleSolution: 'Örnek Çözüm',
    targetIs: 'Hedef: {n}',
    reached: '✓ Hedefe ulaştın! +{n} puan',
    notDivisible: 'Tam bölünmüyor',
    mustBePositive: 'Sonuç negatif olamaz',

    // letters
    pick9: '9 harf seç',
    pickHint: 'Sesli mi sessiz mi? Karışımı sana kalmış — sonra en uzun kelimeyi kur.',
    vowel: '🅰 Sesli',
    consonant: '🅱 Sessiz',
    randomAuto: '🎲 Karışık (otomatik)',
    yourWord: 'KELİMEN',
    wordPlaceholder: 'Harflere dokunarak kelime kur',
    letters: 'HARFLER',
    shuffle: '🔀 Karıştır',
    clear: 'Temizle',
    submit: 'Onayla',
    finishShow: 'Bitir & Çözümü Göster',
    validWord: '✓ "{w}" geçerli · +{n} puan',
    invalidWord: '✗ "{w}" sözlükte yok',
    longestWord: 'En uzun kelime ({n} harf)',
    longestWords: 'En uzun kelimeler ({n} harf)',
    noWord: 'Bu harflerden uygun kelime bulunamadı.',

    // duel / summary / scores
    duelLetters: 'Düello • Kelime',
    duelNumbers: 'Düello • İşlem',
    duelOver: 'Düello Bitti!',
    roundOver: 'Tur Bitti!',
    yourScore: 'Skorun',
    newRecord: '🎉 Yeni rekor!',
    bestLabel: 'En iyi',
    scoresTitle: 'Skorlar',
    bestScores: 'EN YÜKSEK SKORLAR',
    history: 'GEÇMİŞ',
    noGames: 'Henüz oynanmış oyun yok.',
    clearHistory: 'Geçmişi Temizle',
    of: '/',
  },
  en: {
    badge: 'WORD & NUMBER GAME',
    appTitle1: 'Letters',
    appTitle2: '& Numbers',
    tagline: 'Build words from letters, hit the target with numbers.',

    secLang: 'LANGUAGE',
    secMode: 'GAME MODE',
    secLength: 'QUESTIONS',
    secSelect: 'CHOOSE A ROUND',

    modeTimed: 'Timed',
    modeRelaxed: 'Relaxed',
    qShort: 'q',

    gNumbersTitle: 'Numbers',
    gNumbersSub: '6 numbers • reach the target',
    gLettersTitle: 'Letters',
    gLettersSub: '9 letters • longest word',
    gDuelTitle: 'Duel',
    gDuelSub: 'Letters, then numbers',

    scores: 'Scores',
    bestShort: 'Best',
    footTimed: 'Timed mode: each round is timed, points for correct answers.',
    footRelaxed: 'Relaxed mode: no timer, take your time.',
    footDict: '{n}-word dictionary',

    back: 'Back',
    seconds: 'seconds',
    question: 'Question',
    points: 'points',
    total: 'Total',
    playAgain: 'Play Again',
    home: 'Home',
    next: 'Next →',
    seeResults: 'See Results →',
    newQuestion: 'New Question',
    newRound: 'New Round',

    target: 'TARGET',
    numbers: 'NUMBERS',
    operation: 'OPERATION',
    undo: '↶ Undo',
    reset: 'Reset',
    showSolution: 'Show Solution',
    endRound: 'End Round →',
    exampleSolution: 'Example Solution',
    targetIs: 'Target: {n}',
    reached: '✓ You hit the target! +{n} points',
    notDivisible: 'Not divisible',
    mustBePositive: 'Result must be positive',

    pick9: 'Pick 9 letters',
    pickHint: 'Vowel or consonant? The mix is up to you — then find the longest word.',
    vowel: '🅰 Vowel',
    consonant: '🅱 Consonant',
    randomAuto: '🎲 Random (auto)',
    yourWord: 'YOUR WORD',
    wordPlaceholder: 'Tap letters to build a word',
    letters: 'LETTERS',
    shuffle: '🔀 Shuffle',
    clear: 'Clear',
    submit: 'Submit',
    finishShow: 'Finish & Show Answer',
    validWord: '✓ "{w}" is valid · +{n} points',
    invalidWord: '✗ "{w}" is not in the dictionary',
    longestWord: 'Longest word ({n} letters)',
    longestWords: 'Longest words ({n} letters)',
    noWord: 'No valid word from these letters.',

    duelLetters: 'Duel • Letters',
    duelNumbers: 'Duel • Numbers',
    duelOver: 'Duel Over!',
    roundOver: 'Round Over!',
    yourScore: 'Your Score',
    newRecord: '🎉 New record!',
    bestLabel: 'Best',
    scoresTitle: 'Scores',
    bestScores: 'BEST SCORES',
    history: 'HISTORY',
    noGames: 'No games played yet.',
    clearHistory: 'Clear History',
    of: '/',
  },
};

function format(str, params) {
  if (!params) return str;
  return str.replace(/\{(\w+)\}/g, (_, k) => (params[k] != null ? params[k] : `{${k}}`));
}

const LangContext = createContext(null);

export function LanguageProvider({ children }) {
  const [lang, setLang] = useState('tr');
  const t = useCallback((key, params) => format(STR[lang][key] ?? key, params), [lang]);
  const value = useMemo(() => ({ lang, setLang, t }), [lang, t]);
  return <LangContext.Provider value={value}>{children}</LangContext.Provider>;
}

export function useLang() {
  const ctx = useContext(LangContext);
  if (!ctx) throw new Error('useLang must be used within LanguageProvider');
  return ctx;
}
