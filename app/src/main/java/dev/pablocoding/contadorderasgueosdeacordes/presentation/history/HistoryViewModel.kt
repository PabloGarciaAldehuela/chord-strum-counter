package dev.pablocoding.contadorderasgueosdeacordes.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetSessionHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryStats(
    val bestCount: Int,
    val averageCount: Double,
    val totalSessions: Int
)

data class HistoryUiState(
    val sessions: List<SessionResult> = emptyList(),
    val stats: HistoryStats = HistoryStats(0, 0.0, 0),
    val bestSessionId: Long? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getSessionHistory: GetSessionHistoryUseCase
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = getSessionHistory()
        .map { sessions ->
            val best = sessions.maxByOrNull { it.transitionCount }
            HistoryUiState(
                sessions = sessions,
                stats = HistoryStats(
                    bestCount = sessions.maxOfOrNull { it.transitionCount } ?: 0,
                    averageCount = if (sessions.isEmpty()) 0.0 else sessions.map { it.transitionCount }.average(),
                    totalSessions = sessions.size
                ),
                bestSessionId = best?.id
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )
}
