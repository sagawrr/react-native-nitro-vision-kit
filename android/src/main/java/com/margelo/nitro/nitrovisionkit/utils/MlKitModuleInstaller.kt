package com.margelo.nitro.nitrovisionkit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Unbundled Play Services ML Kit models (Lift / OCR).
 * Classify uses bundled image-labeling — no ModuleInstall.
 * @see https://developers.google.com/android/guides/module-install-apis
 */
internal object MlKitModuleInstaller {
  private const val INSTALL_TIMEOUT_MS = 120_000L
  private const val AVAILABILITY_POLL_MS = 2_000L

  fun prefetch(context: Context, vararg apis: OptionalModuleApi) {
    if (apis.isEmpty() || !hasNetwork(context)) return
    try {
      ModuleInstall.getClient(context).deferredInstall(*apis)
    } catch (_: Exception) {
    }
  }

  suspend fun ensure(
    context: Context,
    feature: String,
    vararg apis: OptionalModuleApi,
  ) {
    if (apis.isEmpty()) return
    val client = ModuleInstall.getClient(context)
    val availability = try {
      client.areModulesAvailable(*apis).await()
    } catch (error: Exception) {
      throw RuntimeException(friendlyError(error, feature), error)
    }
    if (availability.areModulesAvailable()) return

    if (!hasNetwork(context)) {
      throw RuntimeException(offlineRequired(feature))
    }

    val failure = try {
      withTimeout(INSTALL_TIMEOUT_MS) {
        awaitInstall(client, context, apis, feature)
      }
      null
    } catch (error: Exception) {
      error
    } ?: return

    // Play Services does not always deliver a terminal listener update, so a
    // finished install can still surface here as a timeout or failure.
    if (modulesReady(client, apis)) return

    throw RuntimeException(friendlyError(failure, feature), failure)
  }

  /**
   * Races the install listener against an availability poll. The poll is the
   * reliable signal; the listener only makes success and failure land sooner.
   */
  private suspend fun awaitInstall(
    client: ModuleInstallClient,
    context: Context,
    apis: Array<out OptionalModuleApi>,
    feature: String,
  ) = coroutineScope {
    val install = async { awaitUrgentInstall(context, apis, feature) }
    val poll = async {
      while (!modulesReady(client, apis)) {
        delay(AVAILABILITY_POLL_MS)
      }
    }
    try {
      select<Unit> {
        install.onAwait { }
        poll.onAwait { }
      }
    } finally {
      install.cancel()
      poll.cancel()
    }
  }

  private suspend fun modulesReady(
    client: ModuleInstallClient,
    apis: Array<out OptionalModuleApi>,
  ): Boolean = try {
    client.areModulesAvailable(*apis).await().areModulesAvailable()
  } catch (_: Exception) {
    false
  }

  private fun hasNetwork(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  private suspend fun awaitUrgentInstall(
    context: Context,
    apis: Array<out OptionalModuleApi>,
    feature: String,
  ) = suspendCancellableCoroutine { continuation ->
    val client = ModuleInstall.getClient(context)
    val listener = object : InstallStatusListener {
      override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
        when (update.installState) {
          InstallState.STATE_COMPLETED -> {
            client.unregisterListener(this)
            if (continuation.isActive) continuation.resume(Unit)
          }
          InstallState.STATE_FAILED, InstallState.STATE_CANCELED -> {
            client.unregisterListener(this)
            if (continuation.isActive) {
              continuation.resumeWithException(RuntimeException(downloadFailed(feature)))
            }
          }
          else -> Unit
        }
      }
    }

    val request = ModuleInstallRequest.newBuilder()
      .apply { apis.forEach { addApi(it) } }
      .setListener(listener)
      .build()

    continuation.invokeOnCancellation {
      try {
        client.unregisterListener(listener)
      } catch (_: Exception) {
      }
    }

    client.installModules(request)
      .addOnSuccessListener { response ->
        if (response.areModulesAlreadyInstalled()) {
          client.unregisterListener(listener)
          if (continuation.isActive) continuation.resume(Unit)
        }
      }
      .addOnFailureListener { error ->
        client.unregisterListener(listener)
        if (continuation.isActive) {
          continuation.resumeWithException(RuntimeException(friendlyError(error, feature), error))
        }
      }
  }

  fun friendlyError(error: Throwable, feature: String): String {
    val message = generateSequence(error) { it.cause }
      .mapNotNull { it.message }
      .joinToString(" ")
    val offlineHints =
      message.contains("network", ignoreCase = true) ||
        message.contains("offline", ignoreCase = true) ||
        message.contains("UNAVAILABLE", ignoreCase = true) ||
        message.contains("Unable to resolve host", ignoreCase = true)
    if (offlineHints) return offlineRequired(feature)

    val downloading =
      message.contains("optional module", ignoreCase = true) ||
        (
          message.contains("Waiting for the", ignoreCase = true) &&
            message.contains("module", ignoreCase = true)
          ) ||
        message.contains("timeout", ignoreCase = true) ||
        message.contains("Timed out", ignoreCase = true)
    if (downloading) return downloadInProgress(feature)

    val firstLine = error.message
      ?.lineSequence()
      ?.firstOrNull()
      ?.trim()
      .orEmpty()
    if (firstLine.isNotEmpty() && firstLine.length < 180 && !firstLine.contains("at ")) {
      return firstLine
    }
    return downloadFailed(feature)
  }

  private fun offlineRequired(feature: String): String =
    "On-device $feature needs a one-time download. Connect to the internet, open the app once, then it works offline."

  private fun downloadInProgress(feature: String): String =
    "Downloading on-device $feature. This can take a minute on first use — try again shortly."

  private fun downloadFailed(feature: String): String =
    "Could not download on-device $feature. Check network and Google Play Services, then try again."
}
