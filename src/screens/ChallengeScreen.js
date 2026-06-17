import React, { useState } from 'react';
import { Text, View, StyleSheet, TextInput, ScrollView } from 'react-native';
import { colors, radius, shadow } from '../theme';
import { Header, Button, Card } from '../components/ui';
import { useLang } from '../i18n';
import { decodeChallenge } from '../game/challenge';

export default function ChallengeScreen({ onCreate, onPlayCode, onBack }) {
  const { t } = useLang();
  const [code, setCode] = useState('');
  const [error, setError] = useState(false);

  function tryPlay() {
    const decoded = decodeChallenge(code);
    if (!decoded) {
      setError(true);
      return;
    }
    setError(false);
    onPlayCode(decoded);
  }

  const games = [
    { key: 'numbers', label: `🔢  ${t('gNumbersTitle')}`, color: colors.blue },
    { key: 'letters', label: `🔤  ${t('gLettersTitle')}`, color: colors.peach },
    { key: 'duel', label: `🏆  ${t('gDuelTitle')}`, color: colors.mint },
  ];

  return (
    <View style={styles.root}>
      <Header title={t('challenge')} onBack={onBack} />
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <Card style={{ marginTop: 6 }}>
          <Text style={styles.cardTitle}>📨  {t('chCreate')}</Text>
          <Text style={styles.cardHint}>{t('chCreateHint')}</Text>
          <Text style={styles.miniLabel}>{t('chPickGame')}</Text>
          {games.map((g) => (
            <Button
              key={g.key}
              title={g.label}
              color={g.color}
              textColor={colors.textDark}
              onPress={() => onCreate(g.key)}
              style={{ marginTop: 10 }}
            />
          ))}
        </Card>

        <View style={styles.orWrap}>
          <View style={styles.orLine} />
          <Text style={styles.orText}>•</Text>
          <View style={styles.orLine} />
        </View>

        <Card>
          <Text style={styles.cardTitle}>🎯  {t('chEnter')}</Text>
          <Text style={styles.cardHint}>{t('chEnterHint')}</Text>
          <TextInput
            value={code}
            onChangeText={(v) => {
              setCode(v);
              setError(false);
            }}
            placeholder={t('chCodePlaceholder')}
            placeholderTextColor={colors.textSoft}
            autoCapitalize="characters"
            autoCorrect={false}
            style={[styles.input, error && styles.inputError]}
            maxLength={24}
          />
          {error && <Text style={styles.errorText}>{t('chInvalid')}</Text>}
          <Button
            title={t('chStart')}
            color={colors.lavenderDeep}
            onPress={tryPlay}
            style={{ marginTop: 12 }}
            disabled={code.trim().length < 8}
          />
        </Card>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, paddingHorizontal: 18, paddingTop: 6 },
  scroll: { paddingBottom: 40 },
  cardTitle: { fontSize: 19, fontWeight: '800', color: colors.textDark },
  cardHint: { fontSize: 13.5, color: colors.textMuted, marginTop: 6, lineHeight: 19 },
  miniLabel: { fontSize: 11, fontWeight: '800', color: colors.textSoft, letterSpacing: 1.2, marginTop: 16 },
  orWrap: { flexDirection: 'row', alignItems: 'center', marginVertical: 20 },
  orLine: { flex: 1, height: 1, backgroundColor: colors.surfaceSoft },
  orText: { color: colors.textSoft, marginHorizontal: 12, fontSize: 18 },
  input: {
    marginTop: 14,
    backgroundColor: colors.surfaceSoft,
    borderRadius: radius.md,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 20,
    fontWeight: '800',
    letterSpacing: 2,
    color: colors.textDark,
    textAlign: 'center',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  inputError: { borderColor: colors.danger },
  errorText: { color: colors.danger, fontWeight: '700', marginTop: 8, textAlign: 'center' },
});
