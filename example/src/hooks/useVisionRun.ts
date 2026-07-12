import { unlink } from '@dr.pogodin/react-native-fs'
import { useRef, useState } from 'react'
import {
  VisionKit,
  type SegmentationResult,
} from 'react-native-nitro-vision-kit'
import { RUN_DEFAULTS, type Mode, type RunResult } from '../types'
import { ensureLocalImagePath } from '../utils/ensureLocalImagePath'
import { toFileUri } from '../utils/format'

function cacheKeyFor(uri: string, hint?: string | null): string {
  if (hint) return hint
  const leaf = uri.split('?')[0]?.split('/').pop()
  return leaf && leaf.length > 0 ? leaf : `img-${Date.now()}`
}

function bare(path: string): string {
  return path.startsWith('file://') ? path.slice('file://'.length) : path
}

async function unlinkQuiet(path: string | null | undefined) {
  if (!path) return
  try {
    await unlink(bare(path))
  } catch {
    /* best-effort */
  }
}

type RunParams = {
  mode: Mode
  photoUri: string
  cacheKey?: string | null
  canSegment: boolean
  canClassify: boolean
}

export function useVisionRun() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<RunResult | null>(null)
  const gen = useRef(0)
  const tempCutoutRef = useRef<string | null>(null)

  async function dropTemp() {
    const path = tempCutoutRef.current
    tempCutoutRef.current = null
    await unlinkQuiet(path)
  }

  function clearResult() {
    gen.current += 1
    void dropTemp()
    setResult(null)
    setError(null)
    setLoading(false)
  }

  function clearError() {
    setError(null)
  }

  async function persistCutout(
    segmentation: SegmentationResult,
    myGen: number,
  ): Promise<string | null> {
    const path = await segmentation.saveToTemporaryFile('png', 100)
    if (gen.current !== myGen) {
      await unlinkQuiet(path)
      return null
    }
    tempCutoutRef.current = path
    return toFileUri(path)
  }

  async function run(params: RunParams): Promise<RunResult | null> {
    const { mode, photoUri, cacheKey, canSegment, canClassify } = params
    gen.current += 1
    const myGen = gen.current
    await dropTemp()
    setLoading(true)
    setError(null)

    let segmentation: SegmentationResult | null = null

    try {
      const localPath = await ensureLocalImagePath(
        photoUri,
        cacheKeyFor(photoUri, cacheKey),
      )
      const originalUri = toFileUri(localPath)

      if (mode === 'cutout') {
        if (!canSegment) throw new Error('Lift is not available on this device.')
        segmentation = await VisionKit.removeBackground(localPath, {
          trim: RUN_DEFAULTS.trim,
          retainMask: false,
          maxPixels: RUN_DEFAULTS.maxPixels,
        })
        const cutoutUri = await persistCutout(segmentation, myGen)
        if (!cutoutUri) return null
        const next: RunResult = {
          mode,
          originalUri,
          cutoutUri,
          classifications: [],
          meta: {
            sourceWidth: segmentation.sourceWidth,
            sourceHeight: segmentation.sourceHeight,
          },
        }
        setResult(next)
        return next
      }

      if (mode === 'classify') {
        if (!canClassify) {
          throw new Error('Read is not available on this device.')
        }
        const classifications = await VisionKit.classifyImage(localPath, {
          maxResults: RUN_DEFAULTS.maxResults,
          minConfidence: RUN_DEFAULTS.minConfidence,
        })
        if (gen.current !== myGen) return null
        const next: RunResult = {
          mode,
          originalUri,
          cutoutUri: null,
          classifications,
          meta: null,
        }
        setResult(next)
        return next
      }

      if (mode === 'analyze') {
        if (!canSegment || !canClassify) {
          throw new Error('Lift & read is not available on this device.')
        }
        const analysis = await VisionKit.analyzeImage(localPath, {
          removeBackground: {
            trim: RUN_DEFAULTS.trim,
            retainMask: false,
            maxPixels: RUN_DEFAULTS.maxPixels,
          },
          classify: {
            maxResults: RUN_DEFAULTS.maxResults,
            minConfidence: RUN_DEFAULTS.minConfidence,
          },
        })
        segmentation = analysis.segmentation ?? null
        let cutoutUri: string | null = null
        if (segmentation) {
          cutoutUri = await persistCutout(segmentation, myGen)
          if (!cutoutUri) return null
        } else if (gen.current !== myGen) {
          return null
        }
        const next: RunResult = {
          mode,
          originalUri,
          cutoutUri,
          classifications: analysis.classifications ?? [],
          meta: segmentation
            ? {
                sourceWidth: segmentation.sourceWidth,
                sourceHeight: segmentation.sourceHeight,
              }
            : null,
        }
        setResult(next)
        return next
      }

      const _never: never = mode
      throw new Error(`Unknown mode: ${_never}`)
    } catch (err) {
      await dropTemp()
      if (gen.current === myGen) {
        setError(err instanceof Error ? err.message : String(err))
        setResult(null)
      }
      return null
    } finally {
      segmentation?.dispose()
      if (gen.current === myGen) setLoading(false)
    }
  }

  return { run, loading, error, result, clearResult, clearError }
}
