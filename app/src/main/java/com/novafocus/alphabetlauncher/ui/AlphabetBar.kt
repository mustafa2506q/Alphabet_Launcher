package com.novafocus.alphabetlauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp

private val ALPHABET = ('A'..'Z').toList()

// How far a letter reaches toward the finger (dp), and how quickly that
// pull fades out for letters further away.
private const val MAX_PULL_DP = 56f
private const val FALLOFF_ROWS = 2.4f

// Gaussian falloff: the touched letter gets the full pull, letters further
// away get less, in a smooth curve rather than a sharp V shape.
internal fun pullForDistance(rowsAway: Float, maxPullPx: Float): Float {
    val normalized = rowsAway / FALLOFF_ROWS
    return maxPullPx * exp(-(normalized * normalized))
}

// The vertical A-Z strip on the right edge. Tracks the finger directly while
// pressed, bends nearby letters toward it, and springs back on release.
// Doesn't know anything about apps/PackageManager - it only ever reports a
// selected letter and the raw touch Y, keeping animation logic and app data
// cleanly separate.
@Composable
fun AlphabetBar(
    onLetterSelected: (Char?) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTouchYChanged: (Float?) -> Unit = {},
    lettersWithApps: Set<Char>? = null, // null = don't dim anything
) {
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableStateOf<Float?>(null) }

    // 0 = resting straight, 1 = fully bent toward the finger. Snaps to 1
    // instantly on touch-down (no lag while dragging), springs back on release.
    val curveAmount by animateFloatAsState(
        targetValue = if (touchY != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "curveAmount",
    )

    Box(
        modifier = modifier
            .width(40.dp)
            .fillMaxHeight()
            .onGloballyPositioned { barHeightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                trackAlphabetDrag { y ->
                    touchY = y
                    onLetterSelected(y?.let { letterForTouchY(it, barHeightPx) })
                    onPressChanged(y != null)
                    onTouchYChanged(y)
                }
            },
    ) {
        val maxPullPx = with(LocalDensity.current) { MAX_PULL_DP.dp.toPx() }
        val rowHeight = if (barHeightPx > 0f) barHeightPx / ALPHABET.size else 0f

        ALPHABET.forEachIndexed { index, letter ->
            val letterCenterY = (index + 0.5f) * rowHeight
            val rowsAway = if (touchY != null && rowHeight > 0f) {
                abs(letterCenterY - touchY!!) / rowHeight
            } else {
                Float.MAX_VALUE
            }
            val pullPx = pullForDistance(rowsAway, maxPullPx) * curveAmount

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offsetForLetter(index, rowHeight, pullPx),
            ) {
                val hasApps = lettersWithApps == null || letter in lettersWithApps
                Text(
                    text = letter.toString(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (hasApps) 1f else 0.3f),
                    fontWeight = if (rowsAway < 0.5f) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

internal fun letterForTouchY(touchY: Float, barHeightPx: Float): Char? {
    if (barHeightPx <= 0f) return null
    val rowHeight = barHeightPx / ALPHABET.size
    val index = (touchY / rowHeight).toInt().coerceIn(0, ALPHABET.lastIndex)
    return ALPHABET[index]
}

// Finger down -> report Y every move -> null on lift/cancel.
private suspend fun PointerInputScope.trackAlphabetDrag(onTouchYChanged: (Float?) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onTouchYChanged(down.position.y)
        var pointer = down
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointer.id } ?: break
            if (change.changedToUp()) {
                onTouchYChanged(null)
                break
            }
            onTouchYChanged(change.position.y)
            change.consume()
            pointer = change
        }
    }
}

// Places a letter at its resting row, shifted left by pullPx toward the finger.
private fun Modifier.offsetForLetter(index: Int, rowHeight: Float, pullPx: Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val yPx = if (rowHeight > 0f) index * rowHeight else 0f
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = -pullPx.toInt(), y = yPx.toInt())
        }
    }
