package dev.pablocoding.contadorderasgueosdeacordes.data.repository

import android.os.CountDownTimer
import dev.pablocoding.contadorderasgueosdeacordes.data.audio.AudioDetector
import dev.pablocoding.contadorderasgueosdeacordes.data.datasource.PreferencesDataSource
import dev.pablocoding.contadorderasgueosdeacordes.di.ApplicationScope
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Session
import dev.pablocoding.contadorderasgueosdeacordes.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val audioDetector: AudioDetector,
    @param:ApplicationScope private val appScope: CoroutineScope
) : SessionRepository {

    private val _sessionFlow = MutableStateFlow(Session())
    override val sessionFlow: StateFlow<Session> = _sessionFlow.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var audioJob: Job? = null

    override suspend fun startSession(durationSeconds: Int) {
        countDownTimer?.cancel()
        audioJob?.cancel()

        val sensitivity = preferencesDataSource.getSensitivity()
        val debounceMs  = preferencesDataSource.getDebounce().toLong()

        _sessionFlow.value = Session(
            durationSeconds = durationSeconds,
            transitionCount = 0,
            isRunning = true,
            isFinished = false,
            remainingSeconds = durationSeconds
        )

        countDownTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _sessionFlow.update { it.copy(remainingSeconds = (millisUntilFinished / 1000).toInt() + 1) }
            }
            override fun onFinish() {
                audioJob?.cancel()
                audioJob = null
                _sessionFlow.update { it.copy(isRunning = false, isFinished = true, remainingSeconds = 0) }
            }
        }.start()

        // Start listening on the long-lived ApplicationScope so it is not tied to any ViewModel
        audioJob = appScope.launch {
            audioDetector.detectStrums(sensitivity, debounceMs)
                .catch { e -> e.printStackTrace() }
                .collect {
                    if (_sessionFlow.value.isRunning) {
                        _sessionFlow.update { it.copy(transitionCount = it.transitionCount + 1) }
                    }
                }
        }
    }

    override suspend fun stopSession() {
        countDownTimer?.cancel()
        countDownTimer = null
        audioJob?.cancel()
        audioJob = null
        _sessionFlow.update { it.copy(isRunning = false, isFinished = false) }
    }

    override suspend fun registerTransition() {
        if (_sessionFlow.value.isRunning) {
            _sessionFlow.update { it.copy(transitionCount = it.transitionCount + 1) }
        }
    }

    override suspend fun getPreferredDuration(): Int = preferencesDataSource.getDuration()
    override suspend fun savePreferredDuration(seconds: Int) = preferencesDataSource.saveDuration(seconds)

    override suspend fun getPreferredSensitivity(): Float = preferencesDataSource.getSensitivity()
    override suspend fun savePreferredSensitivity(value: Float) = preferencesDataSource.saveSensitivity(value)

    override suspend fun getPreferredDebounce(): Int = preferencesDataSource.getDebounce()
    override suspend fun savePreferredDebounce(ms: Int) = preferencesDataSource.saveDebounce(ms)
}
