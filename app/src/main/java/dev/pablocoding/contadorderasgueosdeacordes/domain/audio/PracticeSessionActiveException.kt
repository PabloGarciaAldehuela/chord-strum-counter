package dev.pablocoding.contadorderasgueosdeacordes.domain.audio

/** Thrown when the tuner is started while a practice session is using the microphone. */
class PracticeSessionActiveException :
    IllegalStateException("A practice session is using the microphone")
