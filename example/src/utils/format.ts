export function formatPercent(value: number): string {
  return `${Math.round(value * 100)}%`
}

export function toFileUri(path: string): string {
  if (
    path.startsWith('file://') ||
    path.startsWith('content://') ||
    path.startsWith('http://') ||
    path.startsWith('https://') ||
    path.startsWith('ph://') ||
    path.startsWith('assets-library://')
  ) {
    return path
  }
  return `file://${path}`
}
