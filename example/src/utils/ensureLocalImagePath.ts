import {
  CachesDirectoryPath,
  downloadFile,
  exists,
  mkdir,
  moveFile,
  unlink,
} from '@dr.pogodin/react-native-fs'

const CACHE_DIR = `${CachesDirectoryPath}/visionkit-samples`

export async function ensureLocalImagePath(
  uri: string,
  cacheKey: string,
): Promise<string> {
  if (!uri) throw new Error('Missing image uri.')
  if (
    uri.startsWith('file://') ||
    uri.startsWith('content://') ||
    uri.startsWith('/') ||
    !/^https?:\/\//i.test(uri)
  ) {
    return uri
  }

  const dest = `${CACHE_DIR}/${cacheKey.replace(/[^a-zA-Z0-9._-]/g, '_')}`
  if (await exists(dest)) return dest
  if (!(await exists(CACHE_DIR))) await mkdir(CACHE_DIR)

  const partial = `${dest}.partial`
  if (await exists(partial)) {
    try {
      await unlink(partial)
    } catch {
      /* continue */
    }
  }

  try {
    const result = await downloadFile({ fromUrl: uri, toFile: partial }).promise
    if (result.statusCode != null && result.statusCode >= 400) {
      throw new Error(
        `Could not cache sample image (${result.statusCode}). Is Metro running?`,
      )
    }
    await moveFile(partial, dest)
  } catch (err) {
    try {
      if (await exists(partial)) await unlink(partial)
    } catch {
      /* best-effort */
    }
    throw err
  }

  return dest
}
