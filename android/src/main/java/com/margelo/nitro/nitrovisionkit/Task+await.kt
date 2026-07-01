package com.margelo.nitro.nitrovisionkit

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Adapts a Google Play Services [Task] to a Kotlin suspend function so ML Kit
 * calls can be awaited inside `Promise.async { ... }` without hand-wired
 * success/failure listeners at every call site.
 */
internal suspend fun <T> Task<T>.await(): T {
  return suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
      if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
      if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
      if (continuation.isActive) {
        continuation.resumeWithException(RuntimeException("ML Kit task was canceled."))
      }
    }
  }
}
