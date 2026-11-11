package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordLibrary
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Modern swipeable chord viewer bottom sheet.
 *
 * Allows smooth horizontal swiping between chord diagrams and descriptions,
 * complete with chevron navigation for stand-mounted use, finger placement guides,
 * and quick routine toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDiagramSheet(
    chords: List<Chord>,
    initialChord: Chord = chords.firstOrNull() ?: ChordLibrary.allChords.first(),
    selectedChords: List<String> = emptyList(),
    onToggleChordSelection: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val validChords = remember(chords) {
        if (chords.isEmpty()) ChordLibrary.allChords else chords
    }

    val initialIndex = remember(validChords, initialChord) {
        val idx = validChords.indexOfFirst { it.name.equals(initialChord.name, ignoreCase = true) }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { validChords.size }

    androidx.compose.runtime.LaunchedEffect(initialIndex) {
        if (pagerState.currentPage != initialIndex) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            if (sheetValue == SheetValue.Hidden) {
                ChordPagerScrollFilter.shouldAllowSheetDismiss(pagerState.isScrollInProgress)
            } else {
                true
            }
        }
    )

    // Dampens down-swipe sensitivity within the pager area to prevent accidental dismissals
    // caused by natural human thumb arc curvature during horizontal chord browsing.
    val pagerNestedScrollConnection = remember(pagerState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (pagerState.isScrollInProgress && available.y > 0f) {
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (ChordPagerScrollFilter.shouldAbsorbDownwardScroll(
                        consumedX = consumed.x,
                        availableY = available.y,
                        isScrollInProgress = pagerState.isScrollInProgress
                    )
                ) {
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (ChordPagerScrollFilter.shouldAbsorbDownwardFling(available.x, available.y)) {
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f && (abs(consumed.x) > 40f || pagerState.isScrollInProgress)) {
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Navigation & Page Counter Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Chord",
                        tint = if (pagerState.currentPage > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} of ${validChords.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < validChords.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = pagerState.currentPage < validChords.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Chord",
                            tint = if (pagerState.currentPage < validChords.size - 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Swipeable Pager ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .nestedScroll(pagerNestedScrollConnection)
            ) { page ->
                val currentChord = validChords[page]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Fretboard diagram
                    ChordDiagram(chord = currentChord)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Rich Fingering & Strings Guide Card
                    ChordDetailsGuideCard(chord = currentChord)

                    Spacer(modifier = Modifier.height(14.dp))

                    // In-Routine toggle button (if callback provided)
                    if (onToggleChordSelection != null) {
                        val isSelected = selectedChords.contains(currentChord.name)
                        if (isSelected) {
                            FilledTonalButton(
                                onClick = { onToggleChordSelection(currentChord.name) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "In Practice Routine",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onToggleChordSelection(currentChord.name) },
                                shape = RoundedCornerShape(50),
                                enabled = selectedChords.size < 6,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add to Practice Routine",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Swipe hint label
            Text(
                text = "‹ Swipe horizontally to navigate chord shapes ›",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Backward compatibility overload for displaying a single chord diagram in the sheet.
 */
@Composable
fun ChordDiagramSheet(
    chord: Chord,
    onDismiss: () -> Unit
) = ChordDiagramSheet(
    chords = listOf(chord),
    initialChord = chord,
    onDismiss = onDismiss
)

// ── Rich Fingering & Strings Guide Card ──────────────────────────────────────

private val STRING_LABELS = listOf("6 (E)", "5 (A)", "4 (D)", "3 (G)", "2 (B)", "1 (e)")

@Composable
private fun ChordDetailsGuideCard(chord: Chord) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "STRING & FINGER GUIDE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                STRING_LABELS.forEachIndexed { index, strLabel ->
                    val fret = chord.frets.getOrElse(index) { -1 }
                    val finger = chord.fingers.getOrElse(index) { 0 }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Fret badge
                        Surface(
                            shape = CircleShape,
                            color = when (fret) {
                                -1 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                0 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (fret) {
                                        -1 -> "X"
                                        0 -> "O"
                                        else -> "$fret"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (fret) {
                                        -1 -> MaterialTheme.colorScheme.onErrorContainer
                                        0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Finger number
                        Text(
                            text = when (finger) {
                                1 -> "1"
                                2 -> "2"
                                3 -> "3"
                                4 -> "4"
                                else -> "—"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Practice tip
            val tip = getChordPracticeTip(chord)
            if (tip != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun getChordPracticeTip(chord: Chord): String? = when {
    chord.name in listOf("Big G", "G5", "Cadd9", "Dsus4", "A7sus4", "A7sus4 (var)", "Em7", "Dadd11/F#", "F6/9") ->
        "Anchor fingers 3 (ring) and 4 (pinky) on the 3rd fret across transitions for maximum speed."
    chord.frets.firstOrNull() == -1 ->
        "Mute string 6 (Low E) with the tip of your thumb or leave it unstrummed."
    chord.name == "F" ->
        "Keep fingers curled cleanly so the high E string can ring out without buzz."
    chord.name == "C" ->
        "Arch your 3rd finger to avoid deadening the open 3rd string."
    else -> null
}

internal object ChordPagerScrollFilter {
    fun shouldAbsorbDownwardScroll(
        consumedX: Float,
        availableY: Float,
        isScrollInProgress: Boolean
    ): Boolean {
        if (availableY <= 0f) return false
        return isScrollInProgress || abs(consumedX) > 0.15f * availableY
    }

    fun shouldAbsorbDownwardFling(
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (velocityY <= 0f) return false
        return abs(velocityX) > 0.4f * abs(velocityY)
    }

    fun shouldAllowSheetDismiss(
        isScrollInProgress: Boolean
    ): Boolean = !isScrollInProgress
}
