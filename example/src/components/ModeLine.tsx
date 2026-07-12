import { Pressable, StyleSheet, Text, View } from 'react-native'
import { body, display, ink, mute, signal } from '../palette'
import { MODES, modeAvailable, type Mode } from '../types'

type Props = {
  selectedId: Mode
  onSelect: (id: Mode) => void
  canSegment: boolean
  canClassify: boolean
  disabled?: boolean
}

export function ModeLine({
  selectedId,
  onSelect,
  canSegment,
  canClassify,
  disabled,
}: Props) {
  const purpose = MODES.find(m => m.id === selectedId)?.purpose

  return (
    <View style={styles.wrap}>
      <View style={styles.row}>
        {MODES.map((mode, i) => {
          const on = mode.id === selectedId
          const off =
            !!disabled || !modeAvailable(mode.id, canSegment, canClassify)
          return (
            <View key={mode.id} style={styles.item}>
              {i > 0 ? <Text style={styles.sep}>  </Text> : null}
              <Pressable
                accessibilityRole="radio"
                accessibilityState={{ selected: on, disabled: off }}
                disabled={off}
                onPress={() => onSelect(mode.id)}
                hitSlop={8}
                style={off ? styles.dim : undefined}>
                <Text style={[styles.word, on && styles.wordOn]}>
                  {mode.label}
                </Text>
              </Pressable>
            </View>
          )
        })}
      </View>
      {purpose ? <Text style={styles.caption}>{purpose}</Text> : null}
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: {
    paddingHorizontal: 24,
    paddingTop: 28,
    paddingBottom: 8,
    gap: 14,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'baseline',
  },
  item: {
    flexDirection: 'row',
    alignItems: 'baseline',
  },
  sep: {
    width: 18,
  },
  word: {
    color: mute,
    fontFamily: display,
    fontSize: 20,
    fontWeight: '600',
    letterSpacing: -0.5,
  },
  wordOn: {
    color: ink,
    fontWeight: '800',
  },
  caption: {
    color: mute,
    fontFamily: body,
    fontSize: 15,
    lineHeight: 22,
    maxWidth: 280,
  },
  dim: { opacity: 0.28 },
})
