package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerSettings
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReadingMode
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerTolerance
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TuningCategory

/** "E A D G B E" style summary of a tuning's notes (low to high). */
fun tuningNotesSummary(tuning: Tuning, naming: NoteNaming): String =
    if (tuning.isChromatic) "Any note"
    else tuning.strings.joinToString(" ") { PitchMath.noteName(it, tuning.preferFlats, naming) }

// ── Tuning selection ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningSelectionSheet(
    tunings: List<Tuning>,
    selectedId: String,
    naming: NoteNaming,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎸 Choose Tuning",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            TuningCategory.entries.forEach { category ->
                val inCategory = tunings.filter { it.category == category }
                if (inCategory.isEmpty()) return@forEach
                Spacer(modifier = Modifier.height(14.dp))
                SheetSectionLabel(category.label.uppercase())
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inCategory.forEach { tuning ->
                        TuningRow(
                            tuning = tuning,
                            naming = naming,
                            isSelected = tuning.id == selectedId,
                            onClick = {
                                onSelect(tuning.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TuningRow(
    tuning: Tuning,
    naming: NoteNaming,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (isSelected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tuning.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tuningNotesSummary(tuning, naming),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = scheme.primary
                )
            }
        }
    }
}

// ── Tuner settings ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerSettingsSheet(
    settings: TunerSettings,
    onReferencePitchChange: (Int) -> Unit,
    onToleranceChange: (TunerTolerance) -> Unit,
    onReadingModeChange: (TunerReadingMode) -> Unit,
    onNoteNamingChange: (NoteNaming) -> Unit,
    onInTuneSoundChange: (Boolean) -> Unit,
    onLeftHandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚙️ Tuner Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))
            SheetSectionLabel("REFERENCE PITCH")
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(
                    icon = { Icon(Icons.Default.Remove, contentDescription = "Lower reference pitch") },
                    enabled = settings.referencePitchHz > TunerSettings.MIN_REFERENCE_PITCH_HZ,
                    onClick = { onReferencePitchChange(settings.referencePitchHz - 1) }
                )
                Text(
                    text = "A4 = ${settings.referencePitchHz} Hz",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    color = scheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                StepButton(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Raise reference pitch") },
                    enabled = settings.referencePitchHz < TunerSettings.MAX_REFERENCE_PITCH_HZ,
                    onClick = { onReferencePitchChange(settings.referencePitchHz + 1) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Concert pitch is 440 Hz. Change it only to match another instrument.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (settings.referencePitchHz != TunerSettings.DEFAULT_REFERENCE_PITCH_HZ) {
                    TextButton(onClick = { onReferencePitchChange(TunerSettings.DEFAULT_REFERENCE_PITCH_HZ) }) {
                        Text("Reset")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SheetSectionLabel("IN-TUNE TOLERANCE")
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                options = TunerTolerance.entries,
                selected = settings.tolerance,
                label = { it.label },
                onSelect = onToleranceChange
            )

            Spacer(modifier = Modifier.height(20.dp))
            SheetSectionLabel("READING")
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                options = TunerReadingMode.entries,
                selected = settings.readingMode,
                label = { it.label },
                onSelect = onReadingModeChange
            )
            Text(
                text = when (settings.readingMode) {
                    TunerReadingMode.LIVE ->
                        "The needle follows each note from the pluck. Strings start slightly sharp and sink a few cents as they ring."
                    TunerReadingMode.STEADY ->
                        "One steady value for the ringing note, after the pluck's sharp start. Strings tuned this way sit a few cents higher."
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            SheetSectionLabel("NOTE NAMES")
            Spacer(modifier = Modifier.height(10.dp))
            ChoiceRow(
                options = NoteNaming.entries,
                selected = settings.noteNaming,
                label = { it.label },
                onSelect = onNoteNamingChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            SwitchRow(
                title = "In-tune sound",
                subtitle = "Play a chime when a string reaches pitch",
                checked = settings.inTuneSound,
                onCheckedChange = onInTuneSoundChange
            )
            SwitchRow(
                title = "Left-handed",
                subtitle = "Mirror the headstock",
                checked = settings.leftHanded,
                onCheckedChange = onLeftHandedChange
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepButton(icon: @Composable () -> Unit, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) { icon() }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option == selected
            FilledTonalButton(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                colors = if (isSelected) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                Text(
                    text = label(option),
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
