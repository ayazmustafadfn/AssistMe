import React, { useState, useCallback } from 'react';
import { SafeAreaView, View, StyleSheet, StatusBar as RNStatusBar, Platform } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { colors } from './src/theme';
import { LanguageProvider, useLang } from './src/i18n';
import HomeScreen from './src/screens/HomeScreen';
import NumbersScreen from './src/screens/NumbersScreen';
import LettersScreen from './src/screens/LettersScreen';
import ResultScreen from './src/screens/ResultScreen';
import ScoresScreen from './src/screens/ScoresScreen';
import { saveResult } from './src/storage';

const NUM_MAX = 10; // soru başına en yüksek (işlem)
const LET_MAX = 9; // soru başına en yüksek (kelime)

// duel sırası
const DUEL_SEQ = ['letters', 'numbers'];

function maxForGame(game, total) {
  if (game === 'numbers') return NUM_MAX * total;
  if (game === 'letters') return LET_MAX * total;
  if (game === 'duel') return LET_MAX + NUM_MAX; // 1 kelime + 1 işlem
  return 0;
}

function AppInner() {
  const { t, lang } = useLang();
  const [mode, setMode] = useState('yarisma');
  const [length, setLength] = useState(5);
  const [route, setRoute] = useState('home');
  const [session, setSession] = useState(null); // { kind, game, total, index, scores }
  const [result, setResult] = useState(null);

  const goHome = useCallback(() => {
    setSession(null);
    setRoute('home');
  }, []);

  const startSingle = useCallback(
    (game) => {
      setSession({ kind: 'single', game, total: length, index: 0, scores: [] });
      setRoute('play');
    },
    [length]
  );

  const startDuel = useCallback(() => {
    setSession({ kind: 'duel', game: 'duel', total: DUEL_SEQ.length, index: 0, scores: [] });
    setRoute('play');
  }, []);

  const onSelect = useCallback(
    (game) => {
      if (game === 'scores') setRoute('scores');
      else if (game === 'duel') startDuel();
      else startSingle(game);
    },
    [startDuel, startSingle]
  );

  const finalize = useCallback(
    async (scores, sess) => {
      const totalScore = scores.reduce((a, b) => a + b, 0);
      const game = sess.kind === 'duel' ? 'duel' : sess.game;
      const max = maxForGame(game, sess.total);
      let rows;
      if (sess.kind === 'duel') {
        rows = [
          { label: `🔤 ${t('gLettersTitle')}`, value: `${scores[0]} ${t('points')}` },
          { label: `🔢 ${t('gNumbersTitle')}`, value: `${scores[1]} ${t('points')}` },
        ];
      } else {
        const name = sess.game === 'numbers' ? t('gNumbersTitle') : t('gLettersTitle');
        rows = [{ label: name, value: `${sess.total} ${t('qShort')}` }];
      }
      const saved = await saveResult({ game, mode, lang, total: sess.total, score: totalScore, max });
      setResult({
        title: sess.kind === 'duel' ? t('duelOver') : t('roundOver'),
        emoji: sess.kind === 'duel' ? '🏆' : '🎉',
        rows,
        total: totalScore,
        max,
        best: saved.best,
        isRecord: saved.isRecord,
        replay: sess.kind === 'duel' ? () => startDuel() : () => startSingle(sess.game),
      });
      setRoute('result');
    },
    [mode, lang, t, startDuel, startSingle]
  );

  const handleComplete = useCallback(
    (score) => {
      if (!session) return;
      const scores = [...session.scores, score];
      if (session.index + 1 >= session.total) {
        finalize(scores, session);
      } else {
        setSession({ ...session, index: session.index + 1, scores });
      }
    },
    [session, finalize]
  );

  let content = null;

  if (route === 'home') {
    content = <HomeScreen mode={mode} setMode={setMode} length={length} setLength={setLength} onSelect={onSelect} />;
  } else if (route === 'scores') {
    content = <ScoresScreen onBack={goHome} />;
  } else if (route === 'play' && session) {
    const step = session.index + 1;
    const currentGame = session.kind === 'duel' ? DUEL_SEQ[session.index] : session.game;
    const label =
      session.kind === 'duel'
        ? currentGame === 'letters'
          ? t('duelLetters')
          : t('duelNumbers')
        : undefined;
    const series = { step, total: session.total, label, onComplete: handleComplete };
    const key = `${session.kind}-${currentGame}-${session.index}`;
    if (currentGame === 'numbers') {
      content = <NumbersScreen key={key} mode={mode} onBack={goHome} series={series} />;
    } else {
      content = <LettersScreen key={key} mode={mode} onBack={goHome} series={series} />;
    }
  } else if (route === 'result' && result) {
    content = (
      <ResultScreen
        {...result}
        onPlayAgain={result.replay}
        onHome={goHome}
        onScores={() => setRoute('scores')}
      />
    );
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <View style={styles.inner}>{content}</View>
    </SafeAreaView>
  );
}

export default function App() {
  return (
    <LanguageProvider>
      <AppInner />
    </LanguageProvider>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.bg,
    paddingTop: Platform.OS === 'android' ? RNStatusBar.currentHeight : 0,
  },
  inner: { flex: 1 },
});
