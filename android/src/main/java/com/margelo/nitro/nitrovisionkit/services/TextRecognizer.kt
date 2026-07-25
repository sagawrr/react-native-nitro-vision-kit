package com.margelo.nitro.nitrovisionkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect as AndroidRect
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer as MlKitTextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal object TextRecognizer {
  /**
   * Languages this build can request via `languages` (ML Kit v2 supported tags).
   * @see https://developers.google.com/ml-kit/vision/text-recognition/v2/languages
   */
  val supportedLanguages: Array<String> = arrayOf(
    "af", "sq", "ca", "hr", "cs", "da", "nl", "en", "et", "fil", "fi", "fr",
    "de", "hu", "is", "id", "it", "lv", "lt", "ms", "no", "pl", "pt", "ro",
    "sr-Latn", "sk", "sl", "es", "sv", "tr", "vi",
    "zh", "zh-Hans", "zh-Hant", "ja", "ko",
    "hi", "mr", "ne",
  )

  private val clientsLock = Any()
  private val clients = HashMap<TextRecognitionScript, MlKitTextRecognizer>()

  /** ModuleInstall API for this script. Creates/caches the ML Kit client — call off the JS thread. */
  fun optionalApi(script: TextRecognitionScript): OptionalModuleApi = client(script)

  suspend fun recognize(
    context: Context,
    bitmap: Bitmap,
    languages: Array<String>?,
    region: Rect?,
    minTextHeightFraction: Double?,
  ): TextRecognitionOutput {
    val scripts = TextRecognitionScript.fromLanguages(languages)
    val apis = scripts.map { optionalApi(it) }.toTypedArray()
    MlKitModuleInstaller.ensure(context, "text recognition", *apis)

    val cropped = if (region != null) bitmap.cropToRegion(region) else null
    val source = cropped ?: bitmap
    val mlInput = source.toMlKitInput(maxEdge = VisionKitLimits.TEXT_MAX_EDGE)
    val ownsMlInput = mlInput !== source
    val mlWidth = mlInput.width
    val mlHeight = mlInput.height
    try {
      val inputImage = InputImage.fromBitmap(mlInput, 0)
      val mapped = try {
        if (scripts.size == 1) {
          listOf(
            mapResult(
              visionText = client(scripts[0]).process(inputImage).await(),
              mlWidth = mlWidth,
              mlHeight = mlHeight,
              sourceWidth = source.width,
              sourceHeight = source.height,
              fullWidth = bitmap.width,
              fullHeight = bitmap.height,
              region = region,
              minTextHeightFraction = minTextHeightFraction,
            ),
          )
        } else {
          coroutineScope {
            scripts.map { script ->
              async {
                mapResult(
                  visionText = client(script).process(inputImage).await(),
                  mlWidth = mlWidth,
                  mlHeight = mlHeight,
                  sourceWidth = source.width,
                  sourceHeight = source.height,
                  fullWidth = bitmap.width,
                  fullHeight = bitmap.height,
                  region = region,
                  minTextHeightFraction = minTextHeightFraction,
                )
              }
            }.awaitAll()
          }
        }
      } catch (error: Exception) {
        throw RuntimeException(MlKitModuleInstaller.friendlyError(error, "text recognition"), error)
      }
      return mergeOutputs(mapped)
    } finally {
      if (ownsMlInput) {
        mlInput.recycle()
      }
      if (cropped != null && cropped !== bitmap) {
        cropped.recycle()
      }
    }
  }

  private fun client(script: TextRecognitionScript): MlKitTextRecognizer {
    synchronized(clientsLock) {
      clients[script]?.let { return it }
      val created = when (script) {
        TextRecognitionScript.LATIN ->
          TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        TextRecognitionScript.CHINESE ->
          TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        TextRecognitionScript.JAPANESE ->
          TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        TextRecognitionScript.KOREAN ->
          TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        TextRecognitionScript.DEVANAGARI ->
          TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
      }
      clients[script] = created
      return created
    }
  }

  private fun mergeOutputs(outputs: List<TextRecognitionOutput>): TextRecognitionOutput {
    if (outputs.isEmpty()) {
      return TextRecognitionOutput(text = "", blocks = emptyArray())
    }
    if (outputs.size == 1) return outputs[0]

    val merged = ArrayList<RecognizedTextBlock>()
    for (output in outputs) {
      for (block in output.blocks) {
        if (!isDuplicate(merged, block)) {
          merged.add(block)
        }
      }
    }
    merged.sortWith(
      compareBy(
        { it.bounds.y },
        { it.bounds.x },
      ),
    )
    return TextRecognitionOutput(
      text = merged.joinToString("\n") { it.text },
      blocks = merged.toTypedArray(),
    )
  }

  /** Drop near-duplicate blocks from overlapping multi-script runs (IoU + text). */
  private fun isDuplicate(
    existing: List<RecognizedTextBlock>,
    candidate: RecognizedTextBlock,
  ): Boolean {
    for (block in existing) {
      if (iou(block.bounds, candidate.bounds) < 0.5) continue
      if (block.text == candidate.text) return true
      if (normalizedText(block.text) == normalizedText(candidate.text)) return true
    }
    return false
  }

  private fun normalizedText(value: String): String =
    value.filter { !it.isWhitespace() }

  private fun iou(a: Rect, b: Rect): Double {
    val ax2 = a.x + a.width
    val ay2 = a.y + a.height
    val bx2 = b.x + b.width
    val by2 = b.y + b.height
    val ix1 = maxOf(a.x, b.x)
    val iy1 = maxOf(a.y, b.y)
    val ix2 = minOf(ax2, bx2)
    val iy2 = minOf(ay2, by2)
    val iw = (ix2 - ix1).coerceAtLeast(0.0)
    val ih = (iy2 - iy1).coerceAtLeast(0.0)
    val intersection = iw * ih
    if (intersection <= 0.0) return 0.0
    val union = a.width * a.height + b.width * b.height - intersection
    return if (union > 0.0) intersection / union else 0.0
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
