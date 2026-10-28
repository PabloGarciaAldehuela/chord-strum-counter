package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

/**
 * Keeps the screen on while this composable is in composition and [enabled].
 *
 * Every screen shares the activity's root view, so a plain flag set/clear races during navigation:
 * the outgoing screen is disposed after the incoming one has started and would clear the flag the
 * new screen just set. Requests are counted per root view instead; the screen stays on while any
 * screen still asks for it.
 */
@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val root = view.rootView
        acquire(root)
        onDispose { release(root) }
    }
}

private val requests = WeakHashMap<View, Int>()

private fun acquire(root: View) {
    requests[root] = (requests[root] ?: 0) + 1
    root.keepScreenOn = true
}

private fun release(root: View) {
    val remaining = (requests[root] ?: 1) - 1
    if (remaining > 0) {
        requests[root] = remaining
    } else {
        requests.remove(root)
        root.keepScreenOn = false
    }
}
