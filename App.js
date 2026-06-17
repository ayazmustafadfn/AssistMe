import React, { useState, useCallback } from 'react';
import { SafeAreaView, View, StyleSheet, StatusBar as RNStatusBar, Platform, Share } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { colors } from './src/theme';
import { LanguageProvider, useLang } from './src/i18n';
import HomeScreen from './src/screens/HomeScreen';
import NumbersScreen from './src/screens/NumbersScreen';
import LettersScreen from './src/screens/LettersScreen';
import ResultScreen from './src/screens/ResultScreen';
import ScoresScreen from './src/screens/ScoresScreen';
import ChallengeScreen from './src/screens/ChallengeScreen';
import { saveResult } from './src/storage';
import { buildQuestions, encodeChallenge, randomSeed } from './src/game/challenge';

const NUM_MAX = 10; // soru başına en yüksek (işlem)
const LET_MAX = 9; // soru başına en yüksek (kelime)
const DUEL_SEQ = ['letters', 'numbers'];

function maxForGame(game, total) {
  if (game === 'numbers') return NUM_MAX * total;
  if (game === 'letters') return LET_MAX * total;
  if (game === 'duel') return LET_MAX + NUM_MAX;
  return 0;
}

function AppInner() {
  const { t, lang, setLang } = useLang();
  const [mode, setMode] = useState('yarisma');
  const [length, setLength] = useState(5);
  const [route, setRoute] = useState('home');
  const [session, setSession] = useState(null);
  const [result, setResult] = useState(null);

  const goHome = useCallback(() => {
    setSession(null);
    setRoute('home');
  }, []);

  const startSingle = useCallback(
    (game) => {
      setSession({ kind: 'single', game, total: length, index: 0, scores: [], mode, lang });
      setRoute('play');
    },
    [length, mode, lang]
  );

  const startDuel = useCallback(() => {
    setSession({ kind: 'duel', game: 'duel', total: DUEL_SEQ.length, index: 0, scores: [], mode, lang });
    setRoute('play');
  }, [mode, lang]);

  // Meydan okuma (kod): role 'creator' | 'challengee'
  const startChallenge = useCallback((p, role, challengerScore) => {
    const questions = buildQuestions(p);
    setSession({
      kind: 'challenge',
      role,
      game: p.game,
      total: p.total,
      index: 0,
      scores: [],
      mode: p.mode,
      lang: p.lang,
      seed: p.seed,
      questions,
      challengerScore,
      params: p,
    });
    setRoute('play');
  }, []);

  const onSelect = useCallback(
    (game) => {
      if (game === 'scores') setRoute('scores');
      else if (game === 'challenge') setRoute('challenge');
      else if (game === 'duel') startDuel();
      else startSingle(game);
    },
    [startDuel, startSingle]
  );

  const onCreateChallenge = useCallback(
    (game) => {
      const total = game === 'duel' ? 2 : length;
      startChallenge({ game, mode, lang, total, seed: randomSeed() }, 'creator');
    },
    [length, mode, lang, startChallenge]
  );

  const onPlayCode = useCallback(
    (decoded) => {
      if (decoded.lang !== lang) setLang(decoded.lang);
      startChallenge(
        { game: decoded.game, mode: decoded.mode, lang: decoded.lang, total: decoded.total, seed: decoded.seed },
        'challengee',
        decoded.score
      );
    },
    [lang, setLang, startChallenge]
  );

  const finalize = useCallback(
    async (scores, sess) => {
      const totalScore = scores.reduce((a, b) => a + b, 0);
      const game = sess.game;
      const max = maxForGame(game, sess.total);

      // satırlar
      let rows;
      if (sess.kind === 'challenge' && sess.role === 'challengee') {
        rows = [
          { label: `🆚 ${t('chOpponent')}`, value: `${sess.challengerScore}` },
          { label: `🙋 ${t('chYou')}`, value: `${totalScore}` },
        ];
      } else if (game === 'duel') {
        rows = [
          { label: `🔤 ${t('gLettersTitle')}`, value: `${scores[0]} ${t('points')}` },
          { label: `🔢 ${t('gNumbersTitle')}`, value: `${scores[1]} ${t('points')}` },
        ];
      } else {
        const name = game === 'numbers' ? t('gNumbersTitle') : t('gLettersTitle');
        rows = [{ label: name, value: `${sess.total} ${t('qShort')}` }];
      }

      // skor kaydı (meydan okumayı da kaydet; rekor takibi oyun+uzunluk bazında)
      const saved = await saveResult({ game, mode: sess.mode, lang: sess.lang, total: sess.total, score: totalScore, max });

      let title = sess.kind === 'duel' ? t('duelOver') : t('roundOver');
      let emoji = sess.kind === 'duel' ? '🏆' : '🎉';
      let verdict = null;
      let shareCode = null;
      let onShare = null;

      if (sess.kind === 'challenge') {
        if (sess.role === 'creator') {
          title = t('challenge');
          emoji = '📨';
          shareCode = encodeChallenge({ ...sess.params, score: totalScore });
          onShare = () => Share.share({ message: t('chShareMsg', { code: shareCode }) }).catch(() => {});
        } else {
          title = t('challenge');
          if (totalScore > sess.challengerScore) { verdict = t('chWon'); emoji = '🎉'; }
          else if (totalScore < sess.challengerScore) { verdict = t('chLost'); emoji = '😅'; }
          else { verdict = t('chTie'); emoji = '🤝'; }
        }
      }

      const replay =
        sess.kind === 'duel'
          ? () => startDuel()
          : sess.kind === 'challenge'
          ? () => startChallenge(sess.params, sess.role, sess.challengerScore)
          : () => startSingle(game);

      setResult({ title, emoji, rows, total: totalScore, max, best: saved.best, isRecord: saved.isRecord, verdict, shareCode, onShare, replay });
      setRoute('result');
    },
    [t, mode, startDuel, startSingle, startChallenge]
  );

  const handleComplete = useCallback(
    (score) => {
      if (!session) return;
      const scores = [...session.scores, score];
      if (session.index + 1 >= session.total) finalize(scores, session);
      else setSession({ ...session, index: session.index + 1, scores });
    },
    [session, finalize]
  );

  let content = null;

  if (route === 'home') {
    content = <HomeScreen mode={mode} setMode={setMode} length={length} setLength={setLength} onSelect={onSelect} />;
  } else if (route === 'scores') {
    content = <ScoresScreen onBack={goHome} />;
  } else if (route === 'challenge') {
    content = <ChallengeScreen onCreate={onCreateChallenge} onPlayCode={onPlayCode} onBack={goHome} />;
  } else if (route === 'play' && session) {
    const step = session.index + 1;
    const q = session.questions ? session.questions[session.index] : null;
    const currentGame = q ? q.type : session.kind === 'duel' ? DUEL_SEQ[session.index] : session.game;
    const label =
      session.kind === 'duel' || (session.kind === 'challenge' && session.game === 'duel')
        ? currentGame === 'letters'
          ? t('duelLetters')
          : t('duelNumbers')
        : undefined;
    const series = { step, total: session.total, label, onComplete: handleComplete };
    const key = `${session.kind}-${currentGame}-${session.index}`;
    if (currentGame === 'numbers') {
      content = <NumbersScreen key={key} mode={session.mode} onBack={goHome} series={series} puzzle={q ? q.puzzle : undefined} />;
    } else {
      content = <LettersScreen key={key} mode={session.mode} onBack={goHome} series={series} fixedRack={q ? q.rack : undefined} />;
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
