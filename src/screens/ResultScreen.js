import React from 'react';
import { Text, View, StyleSheet } from 'react-native';
import { colors, radius, shadow } from '../theme';
import { Button } from '../components/ui';
import { useLang } from '../i18n';

export default function ResultScreen({ title, emoji = '🏆', rows, total, max, best, isRecord, onPlayAgain, onHome, onScores }) {
  const { t } = useLang();
  return (
    <View style={styles.wrap}>
      <Text style={styles.emoji}>{emoji}</Text>
      <Text style={styles.title}>{title}</Text>

      {isRecord && (
        <View style={styles.record}>
          <Text style={styles.recordText}>{t('newRecord')}</Text>
        </View>
      )}

      <View style={[styles.card, shadow]}>
        {rows.map((r, i) => (
          <View key={i} style={styles.row}>
            <Text style={styles.k}>{r.label}</Text>
            <Text style={styles.v}>{r.value}</Text>
          </View>
        ))}
        <View style={styles.divider} />
        <View style={styles.row}>
          <Text style={styles.kTotal}>{t('total')}</Text>
          <Text style={styles.vTotal}>
            {total}
            {max != null ? <Text style={styles.vMax}> / {max}</Text> : null}
          </Text>
        </View>
        {best != null && (
          <Text style={styles.best}>
            {t('bestLabel')}: {best}
          </Text>
        )}
      </View>

      <Button title={t('playAgain')} color={colors.lavenderDeep} onPress={onPlayAgain} style={styles.btn} />
      <Button title={t('scores')} color={colors.surfaceSoft} textColor={colors.lavenderDeep} onPress={onScores} style={styles.btn} />
      <Button title={t('home')} color={colors.surfaceSoft} textColor={colors.textMuted} onPress={onHome} style={styles.btn} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 28 },
  emoji: { fontSize: 60 },
  title: { fontSize: 29, fontWeight: '900', color: colors.textDark, marginTop: 6, marginBottom: 14 },
  record: { backgroundColor: colors.yellow, paddingHorizontal: 16, paddingVertical: 8, borderRadius: radius.pill, marginBottom: 14 },
  recordText: { fontSize: 15, fontWeight: '900', color: '#8A6D00' },
  card: { alignSelf: 'stretch', backgroundColor: colors.surface, borderRadius: radius.lg, padding: 22 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 9 },
  k: { fontSize: 17, color: colors.textMuted, fontWeight: '700' },
  v: { fontSize: 17, color: colors.textDark, fontWeight: '800' },
  divider: { height: 1, backgroundColor: colors.surfaceSoft, marginVertical: 8 },
  kTotal: { fontSize: 20, color: colors.textDark, fontWeight: '900' },
  vTotal: { fontSize: 26, color: colors.lavenderDeep, fontWeight: '900' },
  vMax: { fontSize: 17, color: colors.textSoft, fontWeight: '700' },
  best: { textAlign: 'right', color: colors.textMuted, fontWeight: '700', marginTop: 6 },
  btn: { alignSelf: 'stretch', marginTop: 12 },
});
