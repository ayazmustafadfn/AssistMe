import React from 'react';
import { Text, TouchableOpacity, View, StyleSheet } from 'react-native';
import { colors, radius, shadow, shadowSoft } from '../theme';

export function Button({ title, onPress, color = colors.lavenderDeep, textColor = '#fff', style, disabled, small }) {
  return (
    <TouchableOpacity
      activeOpacity={0.85}
      onPress={onPress}
      disabled={disabled}
      style={[
        styles.btn,
        small && styles.btnSmall,
        { backgroundColor: disabled ? colors.textSoft : color },
        shadowSoft,
        style,
      ]}
    >
      <Text style={[styles.btnText, small && styles.btnTextSmall, { color: textColor }]}>{title}</Text>
    </TouchableOpacity>
  );
}

export function Header({ title, onBack }) {
  return (
    <View style={styles.header}>
      {onBack ? (
        <TouchableOpacity onPress={onBack} style={styles.back} activeOpacity={0.7}>
          <Text style={styles.backText}>‹</Text>
        </TouchableOpacity>
      ) : (
        <View style={styles.back} />
      )}
      <Text style={styles.headerTitle} numberOfLines={1}>
        {title}
      </Text>
      <View style={styles.back} />
    </View>
  );
}

export function Card({ children, style }) {
  return <View style={[styles.card, shadow, style]}>{children}</View>;
}

const styles = StyleSheet.create({
  btn: {
    paddingVertical: 16,
    paddingHorizontal: 22,
    borderRadius: radius.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnSmall: { paddingVertical: 10, paddingHorizontal: 16 },
  btnText: { fontSize: 18, fontWeight: '800', letterSpacing: 0.3 },
  btnTextSmall: { fontSize: 15, fontWeight: '700' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 6,
    marginBottom: 8,
  },
  back: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface,
    ...shadowSoft,
  },
  backText: { fontSize: 30, color: colors.lavenderDeep, marginTop: -4, fontWeight: '700' },
  headerTitle: { flex: 1, textAlign: 'center', fontSize: 20, fontWeight: '800', color: colors.textDark },
  card: { backgroundColor: colors.surface, borderRadius: radius.lg, padding: 20 },
});
