import React from 'react';
import { Text, View, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { colors, radius, shadow, shadowSoft } from '../theme';
import { WORD_COUNT } from '../game/dictionary';

function ModeToggle({ mode, setMode }) {
  const options = [
    { key: 'yarisma', label: 'Yarışma', icon: '⏱' },
    { key: 'rahat', label: 'Rahat', icon: '🌿' },
  ];
  return (
    <View style={styles.toggle}>
      {options.map((o) => {
        const active = mode === o.key;
        return (
          <TouchableOpacity
            key={o.key}
            activeOpacity={0.8}
            onPress={() => setMode(o.key)}
            style={[styles.toggleItem, active && styles.toggleItemActive]}
          >
            <Text style={[styles.toggleText, active && styles.toggleTextActive]}>
              {o.icon}  {o.label}
            </Text>
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

export default function HomeScreen({ mode, setMode, onSelect }) {
  return (
    <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
      <View style={styles.heroBadge}>
        <Text style={styles.heroBadgeText}>TÜRKÇE KELİME & ZEKÂ OYUNU</Text>
      </View>
      <Text style={styles.title}>Bir Kelime</Text>
      <Text style={styles.titleAccent}>Bir İşlem</Text>
      <Text style={styles.tagline}>Harflerle kelime kur, sayılarla hedefi yakala.</Text>

      <Text style={styles.sectionLabel}>OYUN MODU</Text>
      <ModeToggle mode={mode} setMode={setMode} />

      <Text style={styles.sectionLabel}>TUR SEÇ</Text>
      <GameCard
        title="Bir İşlem"
        subtitle="6 sayı • hedefe ulaş"
        emoji="🔢"
        color={colors.blue}
        onPress={() => onSelect('numbers')}
      />
      <GameCard
        title="Bir Kelime"
        subtitle="9 harf • en uzun kelime"
        emoji="🔤"
        color={colors.peach}
        onPress={() => onSelect('letters')}
      />
      <GameCard
        title="Düello"
        subtitle="Önce kelime, sonra işlem"
        emoji="🏆"
        color={colors.mint}
        onPress={() => onSelect('duel')}
      />

      <View style={styles.footer}>
        <Text style={styles.footerText}>
          {mode === 'yarisma'
            ? 'Yarışma modu: her tur süreli, doğru cevaba puan.'
            : 'Rahat mod: süre yok, dilediğin kadar düşün.'}
        </Text>
        <Text style={styles.footerSmall}>{WORD_COUNT.toLocaleString('tr-TR')} kelimelik Türkçe sözlük</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 22, paddingTop: 12, paddingBottom: 40 },
  heroBadge: {
    alignSelf: 'flex-start',
    backgroundColor: colors.surfaceSoft,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: radius.pill,
    marginBottom: 10,
  },
  heroBadgeText: { fontSize: 11, fontWeight: '800', color: colors.lavenderDeep, letterSpacing: 1 },
  title: { fontSize: 40, fontWeight: '900', color: colors.textDark, lineHeight: 44 },
  titleAccent: { fontSize: 40, fontWeight: '900', color: colors.lavenderDeep, lineHeight: 44, marginBottom: 8 },
  tagline: { fontSize: 15, color: colors.textMuted, marginBottom: 22, lineHeight: 21 },
  sectionLabel: { fontSize: 12, fontWeight: '800', color: colors.textSoft, letterSpacing: 1.2, marginTop: 14, marginBottom: 10 },
  toggle: { flexDirection: 'row', backgroundColor: colors.surfaceSoft, borderRadius: radius.pill, padding: 5 },
  toggleItem: { flex: 1, paddingVertical: 12, borderRadius: radius.pill, alignItems: 'center' },
  toggleItemActive: { backgroundColor: colors.surface, ...shadowSoft },
  toggleText: { fontSize: 15, fontWeight: '700', color: colors.textMuted },
  toggleTextActive: { color: colors.lavenderDeep },
  gameCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    padding: 16,
    marginBottom: 14,
  },
  emojiWrap: { width: 56, height: 56, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginRight: 16 },
  emoji: { fontSize: 28 },
  gameTitle: { fontSize: 19, fontWeight: '800', color: colors.textDark },
  gameSub: { fontSize: 13.5, color: colors.textMuted, marginTop: 2 },
  chevron: { fontSize: 30, color: colors.textSoft, fontWeight: '300' },
  footer: { marginTop: 18, alignItems: 'center' },
  footerText: { fontSize: 13, color: colors.textMuted, textAlign: 'center', lineHeight: 19 },
  footerSmall: { fontSize: 12, color: colors.textSoft, marginTop: 8 },
});
