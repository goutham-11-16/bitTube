package com.music.bitchord.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Whether the app is actually on screen.
 *
 * In full screen, RESUMED is required so loops pause when backgrounded.
 * When in Android Picture-in-Picture (PiP) mode, the activity is in STARTED
 * state rather than RESUMED (it is not the top focused activity), so STARTED
 * is accepted during PiP to keep video decoding and rendering alive.
 */
@Composable
fun rememberIsForeground(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle
    fun isVisible(state: Lifecycle.State): Boolean = state.isAtLeast(Lifecycle.State.STARTED)

    var foreground by remember(lifecycle) {
        mutableStateOf(isVisible(lifecycle.currentState))
    }
    DisposableEffect(lifecycle, lifecycleOwner) {
        val observer = LifecycleEventObserver { owner, _ ->
            foreground = isVisible(owner.lifecycle.currentState)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return foreground
}
