import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { Text, View, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { colors, radius, shadow, shadowSoft } from '../theme';
import { Header, Button, Card } from '../components/ui';
import { useCountdown } from '../components/useCountdown';
import { generatePuzzle, applyOp, OP_SYMBOL } from '../game/numbers';

const ROUND_SECONDS = 45;
const OPS = ['+', '-', '*', '/'];

function scoreFor(diff) {
  if (diff === 0) return 10;
  if (diff <= 5) return 7;
  if (diff <= 10) return 5;
  return 0;
}

export default function NumbersScreen({ mode, onBack, onComplete, duel }) {
  const timed = mode === 'yarisma';
  const [puzzle, setPuzzle] = useState(() => generatePuzzle());
  const [tiles, setTiles] = useState(() => puzzle.numbers.map((v, i) => ({ id: i, value: v, used: false })));
  const [selA, setSelA] = useState(null);
  const [selOp, setSelOp] = useState(null);
  const [history, setHistory] = useState([]);
  const [finished, setFinished] = useState(false);
  const [revealed, setRevealed] = useState(false);
  const [toast, setToast] = useState(null);
  const nextId = useMemo(() => ({ v: 6 }), [puzzle]);

  const bestDiff = useMemo(() => {
    let d = Infinity;
    for (const t of tiles) d = Math.min(d, Math.abs(t.value - puzzle.target));
    return d;
  }, [tiles, puzzle.target]);

  const solved = bestDiff === 0;

  const { remaining, reset } = useCountdown(ROUND_SECONDS, timed && !finished && !solved, () => endRound());

  function flash(msg) {
    setToast(msg);
    setTimeout(() => setToast(null), 1300);
  }

  const newPuzzle = useCallback(() => {
    const p = generatePuzzle();
    setPuzzle(p);
    setTiles(p.numbers.map((v, i) => ({ id: i, value: v, used: false })));
    nextId.v = 6;
    setSelA(null);
    setSelOp(null);
    setHistory([]);
    setFinished(false);
    setRevealed(false);
    reset(ROUND_SECONDS);
  }, [nextId, reset]);

  function endRound() {
    setFinished(true);
    setRevealed(true);
  }

  function handleFinishPress() {
    if (duel) {
      onComplete && onComplete(scoreFor(bestDiff));
    } else {
      newPuzzle();
    }
  }

  const tileById = (id) => tiles.find((t) => t.id === id);

  function onTileTap(tile) {
    if (finished || tile.used) return;
    if (selA === null) {
      setSelA(tile.id);
      return;
    }
    if (tile.id === selA) {
      setSelA(null);
      setSelOp(null);
      return;
    }
    if (selOp === null) {
      // operand A'yı değiştir
      setSelA(tile.id);
      return;
    }
    // ikinci operand: işlemi uygula
    const a = tileById(selA).value;
    const b = tile.value;
    const res = applyOp(a, b, selOp);
    if (res === null) {
      flash(selOp === '/' ? 'Tam bölünmüyor' : 'Sonuç negatif olamaz');
      return;
    }
    const newTile = { id: nextId.v++, value: res, used: false };
    setHistory((h) => [...h, { aId: selA, bId: tile.id, newId: newTile.id }]);
    setTiles((prev) =>
      prev
        .map((t) => (t.id === selA || t.id === tile.id ? { ...t, used: true } : t))
        .concat(newTile)
    );
    setSelA(null);
    setSelOp(null);
    if (res === puzzle.target) {
      // çözüldü
      if (timed) flash('🎉 Hedefe ulaştın!');
    }
  }

  function undo() {
    if (finished || history.length === 0) return;
    const last = history[history.length - 1];
    setHistory((h) => h.slice(0, -1));
    setTiles((prev) =>
      prev
        .filter((t) => t.id !== last.newId)
        .map((t) => (t.id === last.aId || t.id === last.bId ? { ...t, used: false } : t))
    );
    setSelA(null);
    setSelOp(null);
  }

  function resetMoves() {
    if (finished) return;
    setTiles(puzzle.numbers.map((v, i) => ({ id: i, value: v, used: false })));
    nextId.v = 6;
    setSelA(null);
    setSelOp(null);
    setHistory([]);
  }

  const previewA = selA !== null ? tileById(selA)?.value : null;

  return (
    <View style={styles.root}>
      <Header title={duel ? `Düello • İşlem (${duel.step}/${duel.total})` : 'Bir İşlem'} onBack={onBack} />

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View style={styles.topRow}>
          <View style={styles.targetBox}>
            <Text style={styles.targetLabel}>HEDEF</Text>
            <Text style={styles.targetValue}>{puzzle.target}</Text>
          </View>
          {timed && (
            <View style={[styles.timerBox, remaining <= 10 && styles.timerDanger]}>
              <Text style={[styles.timerValue, remaining <= 10 && styles.timerValueDanger]}>{remaining}</Text>
              <Text style={styles.timerLabel}>saniye</Text>
            </View>
          )}
        </View>

        {solved && (
          <View style={[styles.banner, { backgroundColor: colors.successSoft }]}>
            <Text style={[styles.bannerText, { color: '#2E8B6B' }]}>✓ Hedefe ulaştın! +{scoreFor(0)} puan</Text>
          </View>
        )}

        <Text style={styles.sectionLabel}>SAYILAR</Text>
        <View style={styles.tileGrid}>
          {tiles.map((t) => {
            const isSel = t.id === selA;
            const isTarget = t.value === puzzle.target && !t.used;
            return (
              <TouchableOpacity
                key={t.id}
                activeOpacity={0.8}
                disabled={t.used || finished}
                onPress={() => onTileTap(t)}
                style={[
                  styles.tile,
                  t.used && styles.tileUsed,
                  isSel && styles.tileSelected,
                  isTarget && styles.tileTarget,
                ]}
              >
                <Text style={[styles.tileText, t.used && styles.tileTextUsed, isSel && styles.tileTextSelected]}>
                  {t.value}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <Text style={styles.sectionLabel}>İŞLEM</Text>
        <View style={styles.opRow}>
          {OPS.map((op) => (
            <TouchableOpacity
              key={op}
              activeOpacity={0.8}
              disabled={selA === null || finished}
              onPress={() => setSelOp(op)}
              style={[styles.opBtn, selOp === op && styles.opBtnActive, selA === null && styles.opBtnDisabled]}
            >
              <Text style={[styles.opText, selOp === op && styles.opTextActive]}>{OP_SYMBOL[op]}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.previewBox}>
          <Text style={styles.previewText}>
            {previewA !== null ? previewA : '·'} {selOp ? OP_SYMBOL[selOp] : ''} {selOp ? '?' : ''}
          </Text>
        </View>

        {toast && (
          <View style={styles.toast}>
            <Text style={styles.toastText}>{toast}</Text>
          </View>
        )}

        <View style={styles.actionRow}>
          <Button title="↶ Geri Al" small color={colors.peach} textColor={colors.textDark} onPress={undo} style={styles.flexBtn} disabled={finished || history.length === 0} />
          <Button title="Sıfırla" small color={colors.surfaceSoft} textColor={colors.textMuted} onPress={resetMoves} style={styles.flexBtn} disabled={finished} />
        </View>

        {!finished && !solved && (
          <Button
            title={duel ? 'Turu Bitir →' : 'Çözümü Göster'}
            color={colors.blue}
            textColor={colors.textDark}
            onPress={endRound}
            style={{ marginTop: 4 }}
          />
        )}

        {revealed && (
          <Card style={{ marginTop: 16, backgroundColor: colors.bgAlt }}>
            <Text style={styles.solTitle}>Örnek Çözüm</Text>
            {puzzle.solution.map((s, i) => (
              <Text key={i} style={styles.solStep}>
                {i + 1}.  {s}
              </Text>
            ))}
            <Text style={styles.solNote}>Hedef: {puzzle.target}</Text>
          </Card>
        )}

        {(finished || solved) && (
          <Button
            title={duel ? 'Sonuçlara Geç →' : 'Yeni Soru'}
            color={colors.lavenderDeep}
            onPress={handleFinishPress}
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
  topRow: { flexDirection: 'row', gap: 12, marginBottom: 8 },
  targetBox: { flex: 1, backgroundColor: colors.lavenderDeep, borderRadius: radius.lg, padding: 16, ...shadow },
  targetLabel: { color: '#EDE7FF', fontSize: 12, fontWeight: '800', letterSpacing: 1.5 },
  targetValue: { color: '#fff', fontSize: 46, fontWeight: '900', marginTop: 2 },
  timerBox: { width: 96, backgroundColor: colors.surface, borderRadius: radius.lg, padding: 12, alignItems: 'center', justifyContent: 'center', ...shadow },
  timerDanger: { backgroundColor: colors.dangerSoft },
  timerValue: { fontSize: 34, fontWeight: '900', color: colors.textDark },
  timerValueDanger: { color: colors.danger },
  timerLabel: { fontSize: 11, color: colors.textMuted, fontWeight: '600' },
  banner: { borderRadius: radius.md, padding: 14, marginTop: 10, alignItems: 'center' },
  bannerText: { fontSize: 16, fontWeight: '800' },
  sectionLabel: { fontSize: 12, fontWeight: '800', color: colors.textSoft, letterSpacing: 1.2, marginTop: 18, marginBottom: 10 },
  tileGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  tile: {
    width: 64, height: 64, borderRadius: radius.md, backgroundColor: colors.tile,
    alignItems: 'center', justifyContent: 'center', ...shadowSoft,
  },
  tileUsed: { backgroundColor: colors.surfaceSoft, opacity: 0.45 },
  tileSelected: { backgroundColor: colors.lavenderDeep },
  tileTarget: { borderWidth: 2, borderColor: colors.mint },
  tileText: { fontSize: 24, fontWeight: '800', color: colors.tileText },
  tileTextUsed: { color: colors.textSoft },
  tileTextSelected: { color: '#fff' },
  opRow: { flexDirection: 'row', gap: 10 },
  opBtn: { flex: 1, height: 56, borderRadius: radius.md, backgroundColor: colors.surface, alignItems: 'center', justifyContent: 'center', ...shadowSoft },
  opBtnActive: { backgroundColor: colors.blue },
  opBtnDisabled: { opacity: 0.5 },
  opText: { fontSize: 28, fontWeight: '800', color: colors.textDark },
  opTextActive: { color: '#fff' },
  previewBox: { marginTop: 14, alignItems: 'center', minHeight: 32 },
  previewText: { fontSize: 26, fontWeight: '800', color: colors.textMuted, letterSpacing: 2 },
  toast: { backgroundColor: colors.dangerSoft, borderRadius: radius.md, padding: 10, marginTop: 6, alignItems: 'center' },
  toastText: { color: colors.danger, fontWeight: '700' },
  actionRow: { flexDirection: 'row', gap: 12, marginTop: 16 },
  flexBtn: { flex: 1 },
  solTitle: { fontSize: 16, fontWeight: '800', color: colors.textDark, marginBottom: 10 },
  solStep: { fontSize: 16, color: colors.textMuted, marginBottom: 6, fontWeight: '600' },
  solNote: { fontSize: 14, color: colors.lavenderDeep, fontWeight: '700', marginTop: 6 },
});
