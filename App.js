import React, { useState, useCallback } from 'react';
import { SafeAreaView, View, Text, StyleSheet, StatusBar as RNStatusBar, Platform } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { colors, radius, shadow } from './src/theme';
import { Button } from './src/components/ui';
import HomeScreen from './src/screens/HomeScreen';
import NumbersScreen from './src/screens/NumbersScreen';
import LettersScreen from './src/screens/LettersScreen';

export default function App() {
  const [mode, setMode] = useState('yarisma');
  const [route, setRoute] = useState('home');
  const [duelStep, setDuelStep] = useState('letters');
  const [duelScores, setDuelScores] = useState({ letters: 0, numbers: 0 });

  const goHome = useCallback(() => setRoute('home'), []);

  const onSelect = useCallback((game) => {
    if (game === 'duel') {
      setDuelScores({ letters: 0, numbers: 0 });
      setDuelStep('letters');
      setRoute('duel');
    } else {
      setRoute(game);
    }
  }, []);

  let content;
  if (route === 'home') {
    content = <HomeScreen mode={mode} setMode={setMode} onSelect={onSelect} />;
  } else if (route === 'numbers') {
    content = <NumbersScreen key="num" mode={mode} onBack={goHome} />;
  } else if (route === 'letters') {
    content = <LettersScreen key="let" mode={mode} onBack={goHome} />;
  } else if (route === 'duel') {
    if (duelStep === 'letters') {
      content = (
        <LettersScreen
          key="duel-let"
          mode={mode}
          onBack={goHome}
          duel={{ step: 1, total: 2 }}
          onComplete={(s) => {
            setDuelScores((d) => ({ ...d, letters: s }));
            setDuelStep('numbers');
          }}
        />
      );
    } else if (duelStep === 'numbers') {
      content = (
        <NumbersScreen
          key="duel-num"
          mode={mode}
          onBack={goHome}
          duel={{ step: 2, total: 2 }}
          onComplete={(s) => {
            setDuelScores((d) => ({ ...d, numbers: s }));
            setDuelStep('result');
          }}
        />
      );
    } else {
      const total = duelScores.letters + duelScores.numbers;
      content = (
        <View style={styles.resultWrap}>
          <Text style={styles.resultEmoji}>🏆</Text>
          <Text style={styles.resultTitle}>Düello Bitti!</Text>
          <View style={[styles.scoreCard, shadow]}>
            <View style={styles.scoreRow}>
              <Text style={styles.scoreLabel}>🔤 Kelime</Text>
              <Text style={styles.scoreVal}>{duelScores.letters} puan</Text>
            </View>
            <View style={styles.scoreRow}>
              <Text style={styles.scoreLabel}>🔢 İşlem</Text>
              <Text style={styles.scoreVal}>{duelScores.numbers} puan</Text>
            </View>
            <View style={styles.divider} />
            <View style={styles.scoreRow}>
              <Text style={styles.scoreLabelTotal}>Toplam</Text>
              <Text style={styles.scoreValTotal}>{total}</Text>
            </View>
          </View>
          <Button title="Tekrar Oyna" color={colors.lavenderDeep} onPress={() => onSelect('duel')} style={{ marginTop: 24, alignSelf: 'stretch' }} />
          <Button title="Ana Menü" color={colors.surfaceSoft} textColor={colors.lavenderDeep} onPress={goHome} style={{ marginTop: 12, alignSelf: 'stretch' }} />
        </View>
      );
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <View style={styles.inner}>{content}</View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.bg,
    paddingTop: Platform.OS === 'android' ? RNStatusBar.currentHeight : 0,
  },
  inner: { flex: 1 },
  resultWrap: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 28 },
  resultEmoji: { fontSize: 64 },
  resultTitle: { fontSize: 30, fontWeight: '900', color: colors.textDark, marginTop: 8, marginBottom: 24 },
  scoreCard: { alignSelf: 'stretch', backgroundColor: colors.surface, borderRadius: radius.lg, padding: 22 },
  scoreRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 10 },
  scoreLabel: { fontSize: 18, color: colors.textMuted, fontWeight: '700' },
  scoreVal: { fontSize: 18, color: colors.textDark, fontWeight: '800' },
  divider: { height: 1, backgroundColor: colors.surfaceSoft, marginVertical: 8 },
  scoreLabelTotal: { fontSize: 20, color: colors.textDark, fontWeight: '900' },
  scoreValTotal: { fontSize: 26, color: colors.lavenderDeep, fontWeight: '900' },
});
