import { useEffect, useRef, useState } from 'react'
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { FrameRail } from './components/FrameRail'
import { HeroStage } from './components/HeroStage'
import { LabelsTape } from './components/LabelsTape'
import { ModeLine } from './components/ModeLine'
import { useCapabilities } from './hooks/useCapabilities'
import { useVisionRun } from './hooks/useVisionRun'
import { aqua, body, danger, display, ink, mute, paper, signal } from './palette'
import { pickPhotoFromLibrary } from './pickPhoto'
import {
  SAMPLE_PHOTOS,
  photoFromAsset,
  type PhotoAsset,
  type SamplePhoto,
} from './samplePhotos'
import {
  firstAvailableMode,
  getMode,
  modeAvailable,
  type Mode,
  type StageView,
} from './types'
import { exportImage } from './utils/persistMedia'

type ExportStatus =
  | { kind: 'idle' }
  | { kind: 'saving' }
  | { kind: 'saved'; forUri: string }
  | { kind: 'failed'; message: string }

export function PlaygroundScreen() {
  const insets = useSafeAreaInsets()
  const { height } = useWindowDimensions()
  const { canSegment, canClassify, error: capError } = useCapabilities()
  const {
    run,
    loading,
    error: runError,
    result,
    clearResult,
    clearError,
  } = useVisionRun()

  const [mode, setModeState] = useState<Mode>('cutout')
  const [stageView, setStageView] = useState<StageView>('original')
  const [frames, setFrames] = useState<SamplePhoto[]>(() => [...SAMPLE_PHOTOS])
  const [asset, setAsset] = useState<PhotoAsset | null>(
    () => SAMPLE_PHOTOS[0]?.asset ?? null,
  )
  const [sampleId, setSampleId] = useState<string | null>(
    () => SAMPLE_PHOTOS[0]?.id ?? null,
  )
  const [picking, setPicking] = useState(false)
  const [exportStatus, setExportStatus] = useState<ExportStatus>({ kind: 'idle' })
  const [error, setError] = useState<string | null>(null)
  const exportLock = useRef(false)
  const exportGen = useRef(0)
  const cutoutUriRef = useRef<string | null>(null)

  const modeInfo = getMode(mode)
  const busy = picking || loading || exportStatus.kind === 'saving'
  const photoUri = result?.originalUri ?? asset?.uri
  const cutoutUri = result?.cutoutUri ?? null
  cutoutUriRef.current = cutoutUri
  const alreadySaved =
    exportStatus.kind === 'saved' && exportStatus.forUri === cutoutUri
  const runStatus =
    mode === 'classify' ? 'Reading…' : mode === 'analyze' ? 'Working…' : 'Lifting…'
  const stageHeight = Math.min(Math.round(height * 0.46), 400)

  useEffect(() => {
    if (capError) setError(capError)
  }, [capError])

  useEffect(() => {
    if (runError) setError(runError)
  }, [runError])

  useEffect(() => {
    if (!modeAvailable(mode, canSegment, canClassify)) {
      setModeState(firstAvailableMode(canSegment, canClassify))
    }
  }, [canClassify, canSegment, mode])

  useEffect(() => {
    if (!cutoutUri) {
      setExportStatus({ kind: 'idle' })
      return
    }
    setExportStatus(prev =>
      prev.kind === 'saved' && prev.forUri === cutoutUri ? prev : { kind: 'idle' },
    )
  }, [cutoutUri])

  function dismissError() {
    setError(null)
    clearError()
  }

  function invalidateExport() {
    exportGen.current += 1
    setExportStatus({ kind: 'idle' })
  }

  function resetPhoto(next: PhotoAsset, id: string) {
    invalidateExport()
    setAsset(next)
    setSampleId(id)
    setStageView('original')
    clearResult()
    dismissError()
  }

  async function addFrame() {
    if (busy) return
    setPicking(true)
    dismissError()
    try {
      const picked = await pickPhotoFromLibrary()
      if (!picked) return
      const photo = photoFromAsset(picked.asset)
      setFrames(prev => [...prev, photo])
      resetPhoto(photo.asset, photo.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setPicking(false)
    }
  }

  async function handleRun() {
    if (!asset?.uri || busy) return
    dismissError()
    setExportStatus({ kind: 'idle' })
    const next = await run({
      mode,
      photoUri: asset.uri,
      cacheKey: asset.fileName ?? sampleId,
      canSegment,
      canClassify,
    })
    if (next?.cutoutUri) setStageView('result')
  }

  async function handleExport() {
    if (!cutoutUri || exportLock.current || alreadySaved || busy) return
    exportLock.current = true
    exportGen.current += 1
    const myExport = exportGen.current
    const exportedUri = cutoutUri
    setExportStatus({ kind: 'saving' })
    try {
      await exportImage(exportedUri)
      if (exportGen.current === myExport && cutoutUriRef.current === exportedUri) {
        setExportStatus({ kind: 'saved', forUri: exportedUri })
      }
    } catch (err) {
      if (exportGen.current === myExport && cutoutUriRef.current === exportedUri) {
        setExportStatus({
          kind: 'failed',
          message:
            err instanceof Error
              ? err.message
              : 'Couldn’t save. Tap to try again.',
        })
      }
    } finally {
      exportLock.current = false
    }
  }

  function setMode(id: Mode) {
    invalidateExport()
    setModeState(id)
    setStageView('original')
    clearResult()
  }

  const exportLabel =
    exportStatus.kind === 'saving'
      ? 'Saving…'
      : alreadySaved
        ? 'Kept in Photos'
        : exportStatus.kind === 'failed'
          ? 'Try again'
          : 'Keep cutout'

  const exportDisabled = busy || alreadySaved
  const showResultLedge = cutoutUri != null && !loading

  return (
    <View style={[styles.root, { paddingTop: insets.top }]}>
      <ScrollView
        contentContainerStyle={{
          paddingBottom: insets.bottom + 100,
        }}
        showsVerticalScrollIndicator={false}
        bounces={false}>
        <View style={styles.masthead}>
          <Text style={styles.brand}>Nitro Vision</Text>
          <Text style={styles.tagline}>On your device. Nothing leaves.</Text>
        </View>

        <HeroStage
          stageHeight={stageHeight}
          photoUri={photoUri}
          cutoutUri={cutoutUri}
          processing={loading}
          statusLabel={runStatus}
          stageView={stageView}
        />

        {showResultLedge ? (
          <View style={styles.resultLedge}>
            <View style={styles.compareRow}>
              {(['original', 'result'] as const).map(id => {
                const on = stageView === id
                return (
                  <Pressable
                    key={id}
                    onPress={() => setStageView(id)}
                    hitSlop={8}
                    style={styles.compareHit}>
                    <Text style={[styles.compareText, on && styles.compareOn]}>
                      {id === 'original' ? 'Original' : 'Cutout'}
                    </Text>
                  </Pressable>
                )
              })}
            </View>

            <Pressable
              accessibilityLabel="Keep cutout in Photos"
              disabled={exportDisabled}
              onPress={handleExport}
              hitSlop={8}
              style={exportDisabled ? styles.dim : undefined}>
              <Text
                style={[
                  styles.keepText,
                  alreadySaved && styles.keepSaved,
                  exportStatus.kind === 'failed' && styles.keepFailed,
                ]}>
                {exportLabel}
              </Text>
            </Pressable>
          </View>
        ) : null}

        {exportStatus.kind === 'failed' ? (
          <Pressable onPress={handleExport} style={styles.failPad}>
            <Text style={styles.fail}>{exportStatus.message}</Text>
          </Pressable>
        ) : null}

        <ModeLine
          selectedId={mode}
          onSelect={setMode}
          canSegment={canSegment}
          canClassify={canClassify}
          disabled={busy}
        />

        <FrameRail
          photos={frames}
          selectedId={sampleId}
          onSelect={photo => {
            if (!busy) resetPhoto(photo.asset, photo.id)
          }}
          onAdd={addFrame}
          adding={picking}
          disabled={busy}
        />

        {error ? (
          <Pressable onPress={dismissError} style={styles.errorPad}>
            <Text style={styles.error}>{error}</Text>
          </Pressable>
        ) : null}

        {result ? <LabelsTape result={result} /> : null}
      </ScrollView>

      <View
        style={[styles.dock, { paddingBottom: Math.max(insets.bottom, 16) }]}>
        <Pressable
          accessibilityLabel={modeInfo.runLabel}
          disabled={busy || !asset}
          onPress={handleRun}
          style={({ pressed }) => [
            styles.run,
            (busy || !asset) && styles.dim,
            pressed && !busy && styles.runPressed,
          ]}>
          <Text style={styles.runText}>
            {loading ? runStatus : modeInfo.runLabel}
          </Text>
        </Pressable>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: paper,
  },
  masthead: {
    paddingHorizontal: 24,
    paddingTop: 20,
    paddingBottom: 24,
    gap: 8,
  },
  brand: {
    color: ink,
    fontFamily: display,
    fontSize: 32,
    fontWeight: '800',
    letterSpacing: -1.2,
  },
  tagline: {
    color: mute,
    fontFamily: body,
    fontSize: 15,
    lineHeight: 20,
  },
  resultLedge: {
    paddingHorizontal: 24,
    paddingTop: 20,
    paddingBottom: 4,
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    gap: 24,
  },
  compareRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 20,
    flexShrink: 1,
  },
  compareHit: {
    paddingVertical: 4,
  },
  compareText: {
    color: mute,
    fontFamily: body,
    fontSize: 15,
    fontWeight: '600',
  },
  compareOn: {
    color: ink,
    fontWeight: '700',
  },
  keepText: {
    color: signal,
    fontFamily: display,
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: -0.3,
  },
  keepSaved: {
    color: aqua,
  },
  keepFailed: {
    color: danger,
  },
  failPad: {
    paddingHorizontal: 24,
    paddingTop: 10,
  },
  fail: {
    color: danger,
    fontFamily: body,
    fontSize: 14,
    lineHeight: 20,
    textAlign: 'right',
  },
  errorPad: {
    paddingHorizontal: 24,
    paddingTop: 28,
  },
  error: {
    color: danger,
    fontFamily: body,
    fontSize: 14,
    lineHeight: 20,
  },
  dock: {
    backgroundColor: paper,
    paddingHorizontal: 24,
    paddingTop: 16,
  },
  run: {
    paddingVertical: 8,
    borderBottomWidth: 2,
    borderBottomColor: signal,
  },
  runPressed: { opacity: 0.65 },
  runText: {
    color: ink,
    fontFamily: display,
    fontSize: 20,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
  dim: { opacity: 0.35 },
})
