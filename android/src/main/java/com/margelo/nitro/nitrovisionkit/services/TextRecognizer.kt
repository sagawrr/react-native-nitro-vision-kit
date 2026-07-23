package com.margelo.nitro.nitrovisionkit

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect as AndroidRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

internal object TextRecognizer {
  /**
   * Latin-script languages covered by the ML Kit Latin model (v1).
   * @see https://developers.google.com/ml-kit/vision/text-recognition/v2/languages
   */
  val latinSupportedLanguages: Array<String> = arrayOf(
    "af", "sq", "ca", "hr", "cs", "da", "nl", "en", "et", "fil", "fi", "fr",
    "de", "hu", "is", "id", "it", "lv", "lt", "ms", "no", "pl", "pt", "ro",
    "sr-Latn", "sk", "sl", "es", "sv", "tr", "vi",
  )

  private val recognizer by lazy {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  }

  suspend fun recognize(
    bitmap: Bitmap,
    region: Rect?,
    minTextHeightFraction: Double?,
  ): TextRecognitionOutput {
    val cropped = if (region != null) bitmap.cropToRegion(region) else null
    val source = cropped ?: bitmap
    val mlInput = source.toMlKitInput(maxEdge = VisionKitLimits.TEXT_MAX_EDGE)
    val ownsMlInput = mlInput !== source
    val mlWidth = mlInput.width
    val mlHeight = mlInput.height
    try {
      val visionText = recognizer.process(InputImage.fromBitmap(mlInput, 0)).await()
      return mapResult(
        visionText = visionText,
        mlWidth = mlWidth,
        mlHeight = mlHeight,
        sourceWidth = source.width,
        sourceHeight = source.height,
        fullWidth = bitmap.width,
        fullHeight = bitmap.height,
        region = region,
        minTextHeightFraction = minTextHeightFraction,
      )
    } finally {
      if (ownsMlInput) {
        mlInput.recycle()
      }
      if (cropped != null && cropped !== bitmap) {
        cropped.recycle()
      }
    }
  }

  private fun mapResult(
    visionText: Text,
    mlWidth: Int,
    mlHeight: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    fullWidth: Int,
    fullHeight: Int,
    region: Rect?,
    minTextHeightFraction: Double?,
  ): TextRecognitionOutput {
    val offsetX = if (region != null) region.x * fullWidth else 0.0
    val offsetY = if (region != null) region.y * fullHeight else 0.0
    val scaleX = if (mlWidth > 0) sourceWidth.toDouble() / mlWidth else 1.0
    val scaleY = if (mlHeight > 0) sourceHeight.toDouble() / mlHeight else 1.0
    val minHeight = minTextHeightFraction?.coerceIn(0.0, 1.0)

    val blocks = ArrayList<RecognizedTextBlock>(visionText.textBlocks.size)
    val blockTexts = ArrayList<String>(visionText.textBlocks.size)

    for (block in visionText.textBlocks) {
      val lineModels = ArrayList<RecognizedTextLine>(block.lines.size)
      for (line in block.lines) {
        val bounds = normalizeBox(
          box = line.boundingBox,
          scaleX = scaleX,
          scaleY = scaleY,
          offsetX = offsetX,
          offsetY = offsetY,
          fullWidth = fullWidth,
          fullHeight = fullHeight,
        ) ?: continue
        if (minHeight != null && bounds.height < minHeight) continue
        val confidence = line.confidence.takeIf { it > 0f }?.toDouble()
        lineModels.add(
          RecognizedTextLine(
            text = line.text,
            bounds = bounds,
            confidence = confidence,
            language = sanitizeLanguage(line.recognizedLanguage),
            angleDegrees = line.angle.toDouble(),
            cornerPoints = normalizeCorners(
              points = line.cornerPoints,
              scaleX = scaleX,
              scaleY = scaleY,
              offsetX = offsetX,
              offsetY = offsetY,
              fullWidth = fullWidth,
              fullHeight = fullHeight,
            ),
            candidates = null,
          ),
        )
      }
      if (lineModels.isEmpty()) continue
      val blockBounds = normalizeBox(
        box = block.boundingBox,
        scaleX = scaleX,
        scaleY = scaleY,
        offsetX = offsetX,
        offsetY = offsetY,
        fullWidth = fullWidth,
        fullHeight = fullHeight,
      ) ?: unionBounds(lineModels.map { it.bounds })
      blocks.add(
        RecognizedTextBlock(
          text = block.text,
          bounds = blockBounds,
          lines = lineModels.toTypedArray(),
          language = sanitizeLanguage(block.recognizedLanguage),
          cornerPoints = normalizeCorners(
            points = block.cornerPoints,
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = offsetX,
            offsetY = offsetY,
            fullWidth = fullWidth,
            fullHeight = fullHeight,
          ),
        ),
      )
      blockTexts.add(block.text)
    }

    return TextRecognitionOutput(
      text = if (blockTexts.isNotEmpty()) blockTexts.joinToString("\n") else visionText.text,
      blocks = blocks.toTypedArray(),
    )
  }

  private fun sanitizeLanguage(tag: String?): String? {
    if (tag.isNullOrEmpty() || tag == "und") return null
    return tag
  }

  private fun normalizeBox(
    box: AndroidRect?,
    scaleX: Double,
    scaleY: Double,
    offsetX: Double,
    offsetY: Double,
    fullWidth: Int,
    fullHeight: Int,
  ): Rect? {
    if (box == null || fullWidth <= 0 || fullHeight <= 0) return null
    return Rect(
      x = ((offsetX + box.left * scaleX) / fullWidth).coerceIn(0.0, 1.0),
      y = ((offsetY + box.top * scaleY) / fullHeight).coerceIn(0.0, 1.0),
      width = (box.width() * scaleX / fullWidth).coerceIn(0.0, 1.0),
      height = (box.height() * scaleY / fullHeight).coerceIn(0.0, 1.0),
    )
  }

  private fun normalizeCorners(
    points: Array<Point>?,
    scaleX: Double,
    scaleY: Double,
    offsetX: Double,
    offsetY: Double,
    fullWidth: Int,
    fullHeight: Int,
  ): Array<NormalizedPoint>? {
    if (points == null || points.isEmpty() || fullWidth <= 0 || fullHeight <= 0) return null
    return Array(points.size) { i ->
      val p = points[i]
      NormalizedPoint(
        x = ((offsetX + p.x * scaleX) / fullWidth).coerceIn(0.0, 1.0),
        y = ((offsetY + p.y * scaleY) / fullHeight).coerceIn(0.0, 1.0),
      )
    }
  }

  private fun unionBounds(rects: List<Rect>): Rect {
    if (rects.isEmpty()) return Rect(0.0, 0.0, 0.0, 0.0)
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for (r in rects) {
      minX = minOf(minX, r.x)
      minY = minOf(minY, r.y)
      maxX = maxOf(maxX, r.x + r.width)
      maxY = maxOf(maxY, r.y + r.height)
    }
    return Rect(
      x = minX,
      y = minY,
      width = (maxX - minX).coerceAtLeast(0.0),
      height = (maxY - minY).coerceAtLeast(0.0),
    )
  }
}
