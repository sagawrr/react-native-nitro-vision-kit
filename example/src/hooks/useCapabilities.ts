import { useEffect, useState } from 'react'
import { VisionKit } from 'react-native-nitro-vision-kit'

export function useCapabilities() {
  const [error, setError] = useState<string | null>(null)
  const [canSegment, setCanSegment] = useState(false)
  const [canClassify, setCanClassify] = useState(false)

  useEffect(() => {
    try {
      const capabilities = VisionKit.capabilities
      setCanSegment(capabilities.supportsBackgroundRemoval)
      setCanClassify(capabilities.supportsImageClassification)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Could not read VisionKit capabilities on this device.',
      )
    }
  }, [])

  return {
    error,
    canSegment,
    canClassify,
  }
}
