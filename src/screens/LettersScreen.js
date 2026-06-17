import React, { useState, useCallback } from 'react';
import { Text, View, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { colors, radius, shadowSoft } from '../theme';
import { Header, Button, Card } from '../components/ui';
import { useCountdown } from '../components/useCountdown';
import { useLang } from '../i18n';
import { drawVowel, drawConsonant, isVowel, randomRack, TOTAL_TILES } from '../game/letters';
import { isValidWord, bestWords, upper } from '../game/dictionary';

const ROUND_SECONDS = 40;
export const LETTERS_MAX_PER_Q = 9;

export default function LettersScreen({ mode, onBack, series }) {
  const { t, lang } = useLang();
  const timed = mode === 'yarisma';
  const U = (s) => (s ? upper(s, lang) : '');

  const [phase, setPhase] = useState('pick'); // 'pick' | 'play'
  const [rack, setRack] = useState([]); // [{id, ch, used}]
  const [currentPick, setCurrentPick] = useState([]);
  const [picked, setPicked] = useState(0);
  const [word, setWord] = useState([]); // rack id dizisi
  const [result, setResult] = useState(null);
  const [roundBest, setRoundBest] = useState(0);
  const [revealed, setRevealed] = useState(null);
  const [finished, setFinished] = useState(false);

  function playableRack() {
    for (let i = 0; i < 8; i++) {
      const r = randomRack(lang);
      if (bestWords(r, lang).length >= 3) return r;
    }
    return randomRack(lang);
  }

  const { remaining, reset } = useCountdown(ROUND_SECONDS, timed && phase === 'play' && !finished, () => endRound());

  const startPlay = useCallback(
    (letters) => {
      setRack(letters.map((ch, i) => ({ id: i, ch, used: false })));
      setWord([]);
      setResult(null);
      setRoundBest(0);
      setRevealed(null);
      setFinished(false);
      setPhase('play');
      reset(ROUND_SECONDS);
    },
    [reset]
  );

  function addLetter(kind) {
    if (picked >= TOTAL_TILES) return;
    const ch = kind === 'v' ? drawVowel(lang) : drawConsonant(lang);
    const next = [...currentPick, ch];
    setCurrentPick(next);
    setPicked(next.length);
    if (next.length === TOTAL_TILES) startPlay(next);
  }

  function autoFill() {
    startPlay(playableRack());
    setPicked(TOTAL_TILES);
  }

  function tapRackTile(tile) {
    if (finished || tile.used) return;
    setRack((prev) => prev.map((x) => (x.id === tile.id ? { ...x, used: true } : x)));
    setWord((w) => [...w, tile.id]);
    setResult(null);
  }

  function removeFromWord(idAtPos, pos) {
    if (finished) return;
    setRack((prev) => prev.map((x) => (x.id === idAtPos ? { ...x, used: false } : x)));
    setWord((w) => w.filter((_, i) => i !== pos));
    setResult(null);
  }

  function rackChar(id) {
    return rack.find((x) => x.id === id)?.ch;
  }

  const currentWord = word.map(rackChar).join('');

  function shuffleRack() {
    if (finished) return;
    setRack((prev) => {
      const free = prev.filter((x) => !x.used);
      for (let i = free.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [free[i].ch, free[j].ch] = [free[j].ch, free[i].ch];
      }
      return [...prev];
    });
  }

  function clearWord() {
    if (finished) return;
    setRack((prev) => prev.map((x) => ({ ...x, used: false })));
    setWord([]);
    setResult(null);
  }

  function submit() {
    if (finished || word.length < 2) return;
    const valid = isValidWord(currentWord, lang);
    if (valid && word.length > roundBest) setRoundBest(word.length);
    setResult({ valid, len: valid ? word.length : 0, word: currentWord });
  }

  function endRound() {
    setFinished(true);
    setRevealed(bestWords(rack.map((tt) => tt.ch), lang));
  }

  function complete() {
    series && series.onComplete(roundBest);
  }

  const title = series?.label || t('gLettersTitle');

  // ---- PICK PHASE ----
  if (phase === 'pick') {
    return (
      <View style={styles.root}>
        <Header title={title} onBack={onBack} />
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {series && (
            <Text style={styles.counter}>
              {t('question')} {series.step}/{series.total}
            </Text>
          )}
          <Text style={styles.lead}>{t('pick9')}</Text>
          <Text style={styles.sub}>{t('pickHint')}</Text>

          <View style={styles.pickPreview}>
            {Array.from({ length: TOTAL_TILES }).map((_, i) => (
              <View key={i} style={[styles.slot, currentPick[i] && styles.slotFilled]}>
                <Text style={styles.slotText}>{currentPick[i] ? U(currentPick[i]) : ''}</Text>
              </View>
            ))}
          </View>

          <Text style={styles.counterMid}>{picked}/{TOTAL_TILES}</Text>

          <View style={styles.pickRow}>
            <Button title={t('vowel')} color={colors.peach} textColor={colors.textDark} onPress={() => addLetter('v')} style={styles.flexBtn} disabled={picked >= TOTAL_TILES} />
            <Button title={t('consonant')} color={colors.blue} textColor={colors.textDark} onPress={() => addLetter('c')} style={styles.flexBtn} disabled={picked >= TOTAL_TILES} />
          </View>
          <Button title={t('randomAuto')} color={colors.surfaceSoft} textColor={colors.lavenderDeep} onPress={autoFill} style={{ marginTop: 12 }} />
        </ScrollView>
      </View>
    );
  }

  // ---- PLAY PHASE ----
  return (
    <View style={styles.root}>
      <Header title={title} onBack={onBack} />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {series && (
          <Text style={styles.counter}>
            {t('question')} {series.step}/{series.total}
          </Text>
        )}
        {timed && (
          <View style={[styles.timerBar, remaining <= 10 && styles.timerDanger]}>
            <Text style={[styles.timerText, remaining <= 10 && { color: colors.danger }]}>⏱ {remaining} {t('seconds')}</Text>
          </View>
        )}

        <Text style={styles.sectionLabel}>{t('yourWord')}</Text>
        <View style={styles.wordArea}>
          {word.length === 0 ? (
            <Text style={styles.wordPlaceholder}>{t('wordPlaceholder')}</Text>
          ) : (
            word.map((id, pos) => (
              <TouchableOpacity key={pos} onPress={() => removeFromWord(id, pos)} style={styles.wordTile} activeOpacity={0.8}>
                <Text style={styles.wordTileText}>{U(rackChar(id))}</Text>
              </TouchableOpacity>
            ))
          )}
        </View>

        {result && (
          <View style={[styles.banner, { backgroundColor: result.valid ? colors.successSoft : colors.dangerSoft }]}>
            <Text style={[styles.bannerText, { color: result.valid ? '#2E8B6B' : colors.danger }]}>
              {result.valid ? t('validWord', { w: U(result.word), n: result.len }) : t('invalidWord', { w: U(result.word) })}
            </Text>
          </View>
        )}

        <Text style={styles.sectionLabel}>{t('letters')}</Text>
        <View style={styles.rackGrid}>
          {rack.map((tile) => (
            <TouchableOpacity
              key={tile.id}
              activeOpacity={0.8}
              disabled={tile.used || finished}
              onPress={() => tapRackTile(tile)}
              style={[styles.tile, isVowel(tile.ch, lang) ? styles.tileVowel : styles.tileCons, tile.used && styles.tileUsed]}
            >
              <Text style={[styles.tileText, tile.used && styles.tileTextUsed]}>{U(tile.ch)}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.actionRow}>
          <Button title={t('shuffle')} small color={colors.surfaceSoft} textColor={colors.textMuted} onPress={shuffleRack} style={styles.flexBtn} disabled={finished} />
          <Button title={t('clear')} small color={colors.surfaceSoft} textColor={colors.textMuted} onPress={clearWord} style={styles.flexBtn} disabled={finished} />
        </View>

        {!finished && (
          <Button title={t('submit')} color={colors.success} onPress={submit} style={{ marginTop: 12 }} disabled={word.length < 2} />
        )}

        {!finished && (
          <Button title={series ? t('endRound') : t('finishShow')} color={colors.blue} textColor={colors.textDark} onPress={endRound} style={{ marginTop: 10 }} />
        )}

        {revealed && (
          <Card style={{ marginTop: 16, backgroundColor: colors.bgAlt }}>
            <Text style={styles.solTitle}>
              {revealed.words.length > 1 ? t('longestWords', { n: revealed.length }) : t('longestWord', { n: revealed.length })}
            </Text>
            {revealed.words.length === 0 ? (
              <Text style={styles.solStep}>{t('noWord')}</Text>
            ) : (
              <Text style={styles.solWords}>{revealed.words.map(U).join(' · ')}</Text>
            )}
          </Card>
        )}

        {finished && series && (
          <Button
            title={series.step >= series.total ? t('seeResults') : t('next')}
            color={colors.lavenderDeep}
            onPress={complete}
            style={{ marginTop: 16 }}
          />
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, paddingHorizontal: 18, paddingTop: 6 },
  scroll: { paddingBottom: 40 },
  counter: { fontSize: 14, fontWeight: '800', color: colors.lavenderDeep, marginBottom: 10 },
  lead: { fontSize: 26, fontWeight: '900', color: colors.textDark, marginTop: 4 },
  sub: { fontSize: 14, color: colors.textMuted, marginTop: 6, marginBottom: 22, lineHeight: 20 },
  pickPreview: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, justifyContent: 'center' },
  slot: { width: 52, height: 60, borderRadius: radius.sm, backgroundColor: colors.surfaceSoft, alignItems: 'center', justifyContent: 'center' },
  slotFilled: { backgroundColor: colors.lavender },
  slotText: { fontSize: 26, fontWeight: '900', color: '#fff' },
  counterMid: { textAlign: 'center', fontSize: 15, color: colors.textMuted, fontWeight: '700', marginVertical: 18 },
  pickRow: { flexDirection: 'row', gap: 12 },
  flexBtn: { flex: 1 },
  timerBar: { alignSelf: 'center', backgroundColor: colors.surface, paddingHorizontal: 20, paddingVertical: 8, borderRadius: radius.pill, marginTop: 8, ...shadowSoft },
  timerDanger: { backgroundColor: colors.dangerSoft },
  timerText: { fontSize: 18, fontWeight: '800', color: colors.textDark },
  sectionLabel: { fontSize: 12, fontWeight: '800', color: colors.textSoft, letterSpacing: 1.2, marginTop: 18, marginBottom: 10 },
  wordArea: { minHeight: 64, borderRadius: radius.md, backgroundColor: colors.surface, padding: 10, flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center', ...shadowSoft },
  wordPlaceholder: { color: colors.textSoft, fontSize: 15, paddingHorizontal: 8 },
  wordTile: { width: 46, height: 46, borderRadius: radius.sm, backgroundColor: colors.lavenderDeep, alignItems: 'center', justifyContent: 'center' },
  wordTileText: { fontSize: 22, fontWeight: '800', color: '#fff' },
  banner: { borderRadius: radius.md, padding: 14, marginTop: 12, alignItems: 'center' },
  bannerText: { fontSize: 15, fontWeight: '800', textAlign: 'center' },
  rackGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 9, justifyContent: 'center' },
  tile: { width: 58, height: 64, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', ...shadowSoft },
  tileVowel: { backgroundColor: colors.peach },
  tileCons: { backgroundColor: colors.tile },
  tileUsed: { opacity: 0.35 },
  tileText: { fontSize: 28, fontWeight: '900', color: colors.tileText },
  tileTextUsed: { color: colors.textSoft },
  actionRow: { flexDirection: 'row', gap: 12, marginTop: 16 },
  solTitle: { fontSize: 16, fontWeight: '800', color: colors.textDark, marginBottom: 10 },
  solStep: { fontSize: 15, color: colors.textMuted },
  solWords: { fontSize: 20, fontWeight: '800', color: colors.lavenderDeep, lineHeight: 28 },
});
