package com.accidentalproductions.tetristats.data

data class RangeScalingFactor(
    val low: Double,
    val mid: Double,
    val high: Double
)

typealias ScalingFactor = Any // Can be Double or RangeScalingFactor

object ScalingFactors {
    val FACTORS = mapOf(
        "NES Tetris" to mapOf(
            "Game Boy Tetris" to 0.75,
            "Tetris DX" to 0.75,
            "Tetris DS" to RangeScalingFactor(3.0, 3.3, 4.5),
            "Tetris Effect" to RangeScalingFactor(2.5, 3.8, 4.5),
            "Rosy Retrospection DX" to RangeScalingFactor(4.0, 1.5, 1.8),
            "Apotris" to RangeScalingFactor(1.8, 3.8, 4.4)
        ),
        "Game Boy Tetris" to mapOf(
            "NES Tetris" to 1.33,
            "Tetris DX" to 1.1,
            "Tetris DS" to RangeScalingFactor(4.0, 2.0, 2.0),
            "Tetris Effect" to RangeScalingFactor(4.0, 2.3, 2.3),
            "Rosy Retrospection DX" to 1.1,
            "Apotris" to RangeScalingFactor(1.33, 1.33, 2.33)
        ),
        "Tetris DX" to mapOf(
            "NES Tetris" to 1.33,
            "Game Boy Tetris" to 0.91,
            "Tetris DS" to RangeScalingFactor(4.0, 2.0, 2.0),
            "Tetris Effect" to RangeScalingFactor(4.0, 2.3, 2.3),
            "Rosy Retrospection DX" to 1.1,
            "Apotris" to RangeScalingFactor(1.33, 1.33, 2.33)
        ),
        "Tetris DS" to mapOf(
            "NES Tetris" to RangeScalingFactor(0.33, 0.3, 0.22),
            "Game Boy Tetris" to RangeScalingFactor(0.25, 0.5, 0.5),
            "Tetris DX" to RangeScalingFactor(0.25, 0.5, 0.5),
            "Tetris Effect" to RangeScalingFactor(0.83, 0.91, 1.0),
            "Rosy Retrospection DX" to RangeScalingFactor(0.25, 0.91, 0.67),
            "Apotris" to RangeScalingFactor(0.33, 0.67, 0.9)
        ),
        "Tetris Effect" to mapOf(
            "NES Tetris" to RangeScalingFactor(0.4, 0.26, 0.22),
            "Game Boy Tetris" to RangeScalingFactor(0.25, 0.43, 0.43),
            "Tetris DX" to RangeScalingFactor(0.25, 0.43, 0.43),
            "Tetris DS" to RangeScalingFactor(1.2, 1.1, 1.0),
            "Rosy Retrospection DX" to RangeScalingFactor(0.25, 0.43, 0.57),
            "Apotris" to RangeScalingFactor(0.33, 0.67, 0.85)
        ),
        "Rosy Retrospection DX" to mapOf(
            "NES Tetris" to RangeScalingFactor(0.25, 0.67, 0.57),
            "Game Boy Tetris" to 0.91,
            "Tetris DX" to 0.91,
            "Tetris DS" to RangeScalingFactor(4.0, 1.5, 1.8),
            "Tetris Effect" to RangeScalingFactor(4.0, 2.3, 1.8),
            "Apotris" to RangeScalingFactor(1.1, 0.67, 0.5)
        ),
        "Apotris" to mapOf(
            "NES Tetris" to RangeScalingFactor(0.56, 0.26, 0.23),
            "Game Boy Tetris" to RangeScalingFactor(0.75, 0.75, 0.5),
            "Tetris DX" to RangeScalingFactor(0.75, 0.75, 0.5),
            "Tetris DS" to RangeScalingFactor(3.0, 1.5, 1.0),
            "Tetris Effect" to RangeScalingFactor(3.0, 1.7, 1.2),
            "Rosy Retrospection DX" to RangeScalingFactor(1.1, 0.67, 0.5)
        )
    )

    fun getScalingFactor(fromGame: String, toGame: String, score: Int): Double {
        val factor = FACTORS[fromGame]?.get(toGame) ?: return 1.0
        return when (factor) {
            is Double -> factor
            is RangeScalingFactor -> {
                when {
                    score < 100000 -> factor.low
                    score < 500000 -> factor.mid
                    else -> factor.high
                }
            }
            else -> 1.0
        }
    }

    fun convertScore(fromGame: String, toGame: String, score: Int): Int {
        val factor = getScalingFactor(fromGame, toGame, score)
        return (score * factor).toInt()
    }
} 