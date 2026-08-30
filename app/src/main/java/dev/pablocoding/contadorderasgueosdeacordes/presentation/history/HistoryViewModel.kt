package dev.pablocoding.contadorderasgueosdeacordes.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.SessionResult
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.UserPracticeStats
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetPracticeStatsUseCase
import dev.pablocoding.contadorderasgueosdeacordes.domain.usecase.GetSessionHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<SessionResult> = emptyList(),
    val stats: UserPracticeStats = UserPracticeStats(),
    val bestSessionId: Long? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getSessionHistory: GetSessionHistoryUseCase,
    getPracticeStats: GetPracticeStatsUseCase
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        getSessionHistory(),
        getPracticeStats()
    ) { sessions, stats ->
        val best = sessions.maxByOrNull { it.transitionCount }
        HistoryUiState(
            sessions = sessions,
            stats = stats,
            bestSessionId = best?.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )
}
