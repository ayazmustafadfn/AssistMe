import React, { useState, useEffect, useCallback } from 'react';
import { Text, View, StyleSheet, ScrollView } from 'react-native';
import { colors, radius, shadow, shadowSoft } from '../theme';
import { Header, Button, Card } from '../components/ui';
import { useLang } from '../i18n';
import { loadHistory, loadHighScores, clearAll } from '../storage';

const GAME_EMOJI = { numbers: '🔢', letters: '🔤', duel: '🏆' };

function gameName(game, t) {
  if (game === 'numbers') return t('gNumbersTitle');
  if (game === 'letters') return t('gLettersTitle');
  return t('gDuelTitle');
}

export default function ScoresScreen({ onBack }) {
  const { t, lang } = useLang();
  const [history, setHistory] = useState([]);
  const [highs, setHighs] = useState({});

  const refresh = useCallback(async () => {
    setHistory(await loadHistory());
    setHighs(await loadHighScores());
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const highList = Object.entries(highs)
    .map(([key, val]) => {
      const [game, total] = key.split('-');
      return { game, total: Number(total), ...val };
    })
    .sort((a, b) => b.score - a.score);

  async function onClear() {
    await clearAll();
    refresh();
  }

  function fmtDate(iso) {
    try {
      return new Date(iso).toLocaleDateString(lang === 'tr' ? 'tr-TR' : 'en-US', {
        day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
      });
    } catch (e) {
      return '';
    }
  }

  return (
    <View style={styles.root}>
      <Header title={t('scoresTitle')} onBack={onBack} />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <Text style={styles.sectionLabel}>{t('bestScores')}</Text>
        {highList.length === 0 ? (
          <Card style={styles.empty}>
            <Text style={styles.emptyText}>{t('noGames')}</Text>
          </Card>
        ) : (
          highList.map((h, i) => (
            <View key={i} style={[styles.bestRow, shadowSoft]}>
              <Text style={styles.bestEmoji}>{GAME_EMOJI[h.game]}</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.bestName}>{gameName(h.game, t)}</Text>
                <Text style={styles.bestSub}>{h.total} {t('qShort')} · {h.lang ? h.lang.toUpperCase() : ''}</Text>
              </View>
              <Text style={styles.bestScore}>{h.score}<Text style={styles.bestMax}> / {h.max}</Text></Text>
            </View>
          ))
        )}

        {history.length > 0 && (
          <>
            <Text style={styles.sectionLabel}>{t('history')}</Text>
            {history.map((g) => (
              <View key={g.id} style={styles.histRow}>
                <Text style={styles.histEmoji}>{GAME_EMOJI[g.game]}</Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.histName}>
                    {gameName(g.game, t)} · {g.total} {t('qShort')}
                  </Text>
                  <Text style={styles.histDate}>{fmtDate(g.date)} · {g.mode === 'yarisma' ? t('modeTimed') : t('modeRelaxed')}</Text>
                </View>
                <Text style={styles.histScore}>{g.score}<Text style={styles.histMax}>/{g.max}</Text></Text>
              </View>
            ))}
            <Button title={t('clearHistory')} color={colors.dangerSoft} textColor={colors.danger} onPress={onClear} style={{ marginTop: 18 }} />
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, paddingHorizontal: 18, paddingTop: 6 },
  scroll: { paddingBottom: 40 },
  sectionLabel: { fontSize: 12, fontWeight: '800', color: colors.textSoft, letterSpacing: 1.2, marginTop: 18, marginBottom: 10 },
  empty: { alignItems: 'center', backgroundColor: colors.surface },
  emptyText: { color: colors.textMuted, fontSize: 15 },
  bestRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.surface, borderRadius: radius.md, padding: 14, marginBottom: 10 },
  bestEmoji: { fontSize: 26, marginRight: 14 },
  bestName: { fontSize: 17, fontWeight: '800', color: colors.textDark },
  bestSub: { fontSize: 12.5, color: colors.textMuted, marginTop: 2 },
  bestScore: { fontSize: 22, fontWeight: '900', color: colors.lavenderDeep },
  bestMax: { fontSize: 14, color: colors.textSoft, fontWeight: '700' },
  histRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.bgAlt, borderRadius: radius.md, padding: 12, marginBottom: 8 },
  histEmoji: { fontSize: 20, marginRight: 12 },
  histName: { fontSize: 15, fontWeight: '700', color: colors.textDark },
  histDate: { fontSize: 12, color: colors.textMuted, marginTop: 2 },
  histScore: { fontSize: 18, fontWeight: '800', color: colors.textDark },
  histMax: { fontSize: 13, color: colors.textSoft, fontWeight: '600' },
});
