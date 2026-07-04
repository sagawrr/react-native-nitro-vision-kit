package com.margelo.nitro.nitrovisionkit

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> Task<T>.await(): T {
  return suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
      if (continuation.isCancelled) {
        return@addOnCompleteListener
      }
      if (task.isCanceled) {
        continuation.cancel(RuntimeException("ML Kit task was canceled."))
      } else if (task.exception != null) {
        continuation.resumeWithException(task.exception!!)
      } else {
        @Suppress("UNCHECKED_CAST")
        continuation.resume(task.result as T)
      }
    }
    continuation.invokeOnCancellation { }
  }
}
