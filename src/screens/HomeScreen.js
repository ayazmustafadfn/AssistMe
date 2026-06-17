import React from 'react';
import { Text, View, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { colors, radius, shadow, shadowSoft } from '../theme';
import { useLang } from '../i18n';
import { wordCount } from '../game/dictionary';

function Segmented({ options, value, onChange }) {
  return (
    <View style={styles.toggle}>
      {options.map((o) => {
        const active = value === o.key;
        return (
          <TouchableOpacity key={o.key} activeOpacity={0.8} onPress={() => onChange(o.key)} style={[styles.toggleItem, active && styles.toggleItemActive]}>
            <Text style={[styles.toggleText, active && styles.toggleTextActive]}>{o.label}</Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

function GameCard({ title, subtitle, emoji, color, onPress }) {
  return (
    <TouchableOpacity activeOpacity={0.9} onPress={onPress} style={[styles.gameCard, shadow]}>
      <View style={[styles.emojiWrap, { backgroundColor: color }]}>
        <Text style={styles.emoji}>{emoji}</Text>
      </View>
      <View style={{ flex: 1 }}>
        <Text style={styles.gameTitle}>{title}</Text>
        <Text style={styles.gameSub}>{subtitle}</Text>
      </View>
      <Text style={styles.chevron}>›</Text>
    </TouchableOpacity>
  );
}

export default function HomeScreen({ mode, setMode, length, setLength, onSelect }) {
  const { t, lang, setLang } = useLang();

  return (
    <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
      <View style={styles.topBar}>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{t('badge')}</Text>
        </View>
        <View style={styles.langSwitch}>
          {['tr', 'en'].map((l) => (
            <TouchableOpacity key={l} onPress={() => setLang(l)} style={[styles.langBtn, lang === l && styles.langBtnActive]} activeOpacity={0.8}>
              <Text style={[styles.langText, lang === l && styles.langTextActive]}>{l.toUpperCase()}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      <Text style={styles.title}>{t('appTitle1')}</Text>
      <Text style={styles.titleAccent}>{t('appTitle2')}</Text>
      <Text style={styles.tagline}>{t('tagline')}</Text>

      <Text style={styles.sectionLabel}>{t('secMode')}</Text>
      <Segmented
        value={mode}
        onChange={setMode}
        options={[
          { key: 'yarisma', label: `⏱ ${t('modeTimed')}` },
          { key: 'rahat', label: `🌿 ${t('modeRelaxed')}` },
        ]}
      />

      <Text style={styles.sectionLabel}>{t('secLength')}</Text>
      <Segmented
        value={length}
        onChange={setLength}
        options={[
          { key: 5, label: `5 ${t('qShort')}` },
          { key: 10, label: `10 ${t('qShort')}` },
        ]}
      />

      <Text style={styles.sectionLabel}>{t('secSelect')}</Text>
      <GameCard title={t('gNumbersTitle')} subtitle={t('gNumbersSub')} emoji="🔢" color={colors.blue} onPress={() => onSelect('numbers')} />
      <GameCard title={t('gLettersTitle')} subtitle={t('gLettersSub')} emoji="🔤" color={colors.peach} onPress={() => onSelect('letters')} />
      <GameCard title={t('gDuelTitle')} subtitle={t('gDuelSub')} emoji="🏆" color={colors.mint} onPress={() => onSelect('duel')} />

      <TouchableOpacity activeOpacity={0.85} onPress={() => onSelect('scores')} style={[styles.scoresBtn, shadowSoft]}>
        <Text style={styles.scoresText}>📊  {t('scores')}</Text>
        <Text style={styles.chevron}>›</Text>
      </TouchableOpacity>

      <View style={styles.footer}>
        <Text style={styles.footerText}>{mode === 'yarisma' ? t('footTimed') : t('footRelaxed')}</Text>
        <Text style={styles.footerSmall}>{t('footDict', { n: wordCount(lang).toLocaleString(lang === 'tr' ? 'tr-TR' : 'en-US') })}</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 22, paddingTop: 10, paddingBottom: 40 },
  topBar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 },
  badge: { backgroundColor: colors.surfaceSoft, paddingHorizontal: 12, paddingVertical: 6, borderRadius: radius.pill },
  badgeText: { fontSize: 11, fontWeight: '800', color: colors.lavenderDeep, letterSpacing: 1 },
  langSwitch: { flexDirection: 'row', backgroundColor: colors.surfaceSoft, borderRadius: radius.pill, padding: 4 },
  langBtn: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: radius.pill },
  langBtnActive: { backgroundColor: colors.lavenderDeep },
  langText: { fontSize: 13, fontWeight: '800', color: colors.textMuted },
  langTextActive: { color: '#fff' },
  title: { fontSize: 40, fontWeight: '900', color: colors.textDark, lineHeight: 44 },
  titleAccent: { fontSize: 40, fontWeight: '900', color: colors.lavenderDeep, lineHeight: 44, marginBottom: 8 },
  tagline: { fontSize: 15, color: colors.textMuted, marginBottom: 8, lineHeight: 21 },
  sectionLabel: { fontSize: 12, fontWeight: '800', color: colors.textSoft, letterSpacing: 1.2, marginTop: 16, marginBottom: 10 },
  toggle: { flexDirection: 'row', backgroundColor: colors.surfaceSoft, borderRadius: radius.pill, padding: 5 },
  toggleItem: { flex: 1, paddingVertical: 12, borderRadius: radius.pill, alignItems: 'center' },
  toggleItemActive: { backgroundColor: colors.surface, ...shadowSoft },
  toggleText: { fontSize: 15, fontWeight: '700', color: colors.textMuted },
  toggleTextActive: { color: colors.lavenderDeep },
  gameCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.surface, borderRadius: radius.lg, padding: 16, marginBottom: 14 },
  emojiWrap: { width: 56, height: 56, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginRight: 16 },
  emoji: { fontSize: 28 },
  gameTitle: { fontSize: 19, fontWeight: '800', color: colors.textDark },
  gameSub: { fontSize: 13.5, color: colors.textMuted, marginTop: 2 },
  chevron: { fontSize: 30, color: colors.textSoft, fontWeight: '300' },
  scoresBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.surfaceSoft, borderRadius: radius.lg, paddingVertical: 16, paddingHorizontal: 20, marginTop: 2 },
  scoresText: { fontSize: 17, fontWeight: '800', color: colors.lavenderDeep },
  footer: { marginTop: 18, alignItems: 'center' },
  footerText: { fontSize: 13, color: colors.textMuted, textAlign: 'center', lineHeight: 19 },
  footerSmall: { fontSize: 12, color: colors.textSoft, marginTop: 8 },
});
