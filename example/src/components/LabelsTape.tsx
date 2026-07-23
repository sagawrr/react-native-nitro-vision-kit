import { StyleSheet, Text, View } from 'react-native'
import { body, display, ink, mute, signal } from '../palette'
import type { RunResult } from '../types'
import { formatPercent } from '../utils/format'

export function LabelsTape({ result }: { result: RunResult }) {
  const hasLabels = result.classifications.length > 0
  const hasOcr = result.ocrText != null && result.ocrText.length > 0
  const showOcrEmpty =
    !hasOcr && (result.mode === 'ocr' || result.mode === 'analyze')
  if (!hasLabels && !hasOcr && !showOcrEmpty) return null

  return (
    <View style={styles.wrap}>
      {hasLabels ? (
        <View style={styles.section}>
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
      ) : null}

      {hasOcr || showOcrEmpty ? (
        <View style={styles.section}>
          <Text style={styles.heading}>Found text</Text>
          <Text style={hasOcr ? styles.ocr : styles.ocrEmpty}>
            {hasOcr ? result.ocrText : 'Nothing readable in this frame.'}
          </Text>
        </View>
      ) : null}
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: {
    marginTop: 36,
    paddingHorizontal: 24,
    gap: 28,
  },
  section: {
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
  ocr: {
    color: ink,
    fontFamily: body,
    fontSize: 16,
    lineHeight: 24,
  },
  ocrEmpty: {
    color: mute,
    fontFamily: body,
    fontSize: 16,
    lineHeight: 24,
  },
})
