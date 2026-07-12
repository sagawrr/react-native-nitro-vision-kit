import { StyleSheet, Text, View } from 'react-native'
import { body, display, ink, mute, signal } from '../palette'
import type { RunResult } from '../types'
import { formatPercent } from '../utils/format'

export function LabelsTape({ result }: { result: RunResult }) {
  if (result.classifications.length === 0) return null

  return (
    <View style={styles.wrap}>
      <Text style={styles.heading}>In this photo</Text>
      {result.classifications.map(item => (
        <View key={`${item.index}-${item.label}`} style={styles.row}>
          <Text style={styles.label} numberOfLines={1}>
            {item.label}
          </Text>
          <Text style={styles.pct}>{formatPercent(item.confidence)}</Text>
        </View>
      ))}
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: {
    marginTop: 36,
    paddingHorizontal: 24,
    gap: 16,
  },
  heading: {
    color: mute,
    fontFamily: body,
    fontSize: 13,
    letterSpacing: 0.2,
    marginBottom: 4,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    gap: 16,
  },
  label: {
    flex: 1,
    color: ink,
    fontFamily: display,
    fontSize: 18,
    fontWeight: '700',
    letterSpacing: -0.4,
    textTransform: 'capitalize',
  },
  pct: {
    color: signal,
    fontFamily: body,
    fontSize: 14,
    fontVariant: ['tabular-nums'],
    fontWeight: '600',
  },
})
