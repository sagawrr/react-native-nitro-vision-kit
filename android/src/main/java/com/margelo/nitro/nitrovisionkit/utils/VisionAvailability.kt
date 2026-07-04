package com.margelo.nitro.nitrovisionkit

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

internal object VisionAvailability {
  const val supportsImageClassification: Boolean = true

  fun supportsBackgroundRemoval(context: Context): Boolean =
    playServicesResult(context) == ConnectionResult.SUCCESS

  fun backgroundRemovalUnavailableReason(context: Context): String {
    val result = playServicesResult(context)
    if (result == ConnectionResult.SUCCESS) {
      return "Background removal is unavailable on this device."
    }
    return GoogleApiAvailability.getInstance().getErrorString(result)
      ?: "Background removal requires Google Play Services."
  }

  fun requireBackgroundRemoval(context: Context) {
    if (!supportsBackgroundRemoval(context)) {
      throw RuntimeException(backgroundRemovalUnavailableReason(context))
    }
  }

  private fun playServicesResult(context: Context): Int =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
}
