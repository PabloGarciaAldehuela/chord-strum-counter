package dev.pablocoding.contadorderasgueosdeacordes.domain.model

data class MetronomeState(
    val isPlaying: Boolean = false,
    val bpm: Int = 80,
    val beatsPerMeasure: Int = 4,
    val currentBeat: Int = 1
) {
    val tempoName: String
        get() = when {
            bpm < 60 -> "Largo"
            bpm < 66 -> "Larghetto"
            bpm < 76 -> "Adagio"
            bpm < 108 -> "Andante"
            bpm < 120 -> "Moderato"
            bpm < 168 -> "Allegro"
            bpm < 200 -> "Presto"
            else -> "Prestissimo"
        }
}
