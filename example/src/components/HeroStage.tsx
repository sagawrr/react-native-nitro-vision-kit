import {
  ActivityIndicator,
  Image,
  StyleSheet,
  Text,
  View,
} from 'react-native'
import { body, display, mute, paper, well } from '../palette'
import type { StageView } from '../types'

type Props = {
  stageHeight: number
  photoUri?: string
  cutoutUri?: string | null
  processing?: boolean
  stageView: StageView
  statusLabel?: string
}

export function HeroStage({
  stageHeight,
  photoUri,
  cutoutUri = null,
  processing = false,
  stageView,
  statusLabel = 'Working…',
}: Props) {
  if (!photoUri) {
    return (
      <View style={[styles.well, { height: stageHeight }]}>
        <Text style={styles.emptyTitle}>No photo yet</Text>
        <Text style={styles.emptyBody}>Choose one below to begin.</Text>
      </View>
    )
  }

  const showingCutout = stageView === 'result' && cutoutUri != null
  const displayUri = showingCutout ? cutoutUri : photoUri

  return (
    <View style={[styles.well, { height: stageHeight }]}>
      {showingCutout ? <View style={styles.checker} /> : null}
      <Image
        source={{ uri: displayUri }}
        style={StyleSheet.absoluteFill}
        resizeMode="contain"
      />

      {processing ? (
        <View style={styles.busy} pointerEvents="none">
          <ActivityIndicator color={paper} />
          <Text style={styles.busyLabel}>{statusLabel}</Text>
        </View>
      ) : null}
    </View>
  )
}

const styles = StyleSheet.create({
  well: {
    backgroundColor: well,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
  },
  checker: {
    ...StyleSheet.absoluteFill,
    backgroundColor: '#1A2220',
  },
  emptyTitle: {
    color: paper,
    fontFamily: display,
    fontSize: 24,
    fontWeight: '800',
    letterSpacing: -0.6,
    textAlign: 'center',
  },
  emptyBody: {
    color: mute,
    fontFamily: body,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
    marginTop: 12,
    paddingHorizontal: 48,
  },
  busy: {
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(14,21,18,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 14,
  },
  busyLabel: {
    color: paper,
    fontFamily: body,
    fontSize: 15,
    fontWeight: '600',
  },
})
