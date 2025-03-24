package com.accidentalproductions.tetristats.util

import com.accidentalproductions.tetristats.data.RangeScalingFactor

data class GameScoreSample(
    val game: String,
    val score: Int,
    val level: Int,
    val skillLevel: String, // "beginner", "intermediate", "advanced"
    val notes: String // Any special conditions or mechanics used
)

class ScalingFactorAnalyzer {
    private val samples = mutableListOf<GameScoreSample>()
    private val baseGame = "NES Tetris"

    val sampleCount: Int
        get() = samples.size

    fun addSample(sample: GameScoreSample) {
        samples.add(sample)
    }

    fun addSamples(newSamples: List<GameScoreSample>) {
        samples.addAll(newSamples)
    }

    fun clearSamples() {
        samples.clear()
    }

    fun analyzeScoringCurves(): Map<String, RangeScalingFactor> {
        val groupedSamples = samples.groupBy { it.game }
        val conversionFactors = mutableMapOf<String, RangeScalingFactor>()

        groupedSamples.forEach { (game, scores) ->
            if (game != baseGame) {
                val lowScores = scores.filter { it.score < 100000 }
                val midScores = scores.filter { it.score in 100000..500000 }
                val highScores = scores.filter { it.score > 500000 }

                val lowFactor = calculateAverageFactor(lowScores, baseGame)
                val midFactor = calculateAverageFactor(midScores, baseGame)
                val highFactor = calculateAverageFactor(highScores, baseGame)

                conversionFactors[game] = RangeScalingFactor(lowFactor, midFactor, highFactor)
            }
        }

        return conversionFactors
    }

    private fun calculateAverageFactor(samples: List<GameScoreSample>, baseGame: String): Double {
        if (samples.isEmpty()) return 1.0

        val baseGameSamples = this.samples.filter { it.game == baseGame }
        if (baseGameSamples.isEmpty()) return 1.0

        // Find matching base game samples by skill level
        val factors = samples.map { sample ->
            val matchingBaseSamples = baseGameSamples.filter { 
                it.skillLevel == sample.skillLevel && 
                it.level == sample.level
            }
            
            if (matchingBaseSamples.isNotEmpty()) {
                matchingBaseSamples.map { it.score.toDouble() / sample.score }
            } else {
                // If no exact match, find closest level
                val closestBaseSample = baseGameSamples.minByOrNull { 
                    kotlin.math.abs(it.level - sample.level) 
                }
                if (closestBaseSample != null) {
                    listOf(closestBaseSample.score.toDouble() / sample.score)
                } else {
                    emptyList()
                }
            }
        }.flatten()

        return if (factors.isNotEmpty()) {
            factors.average()
        } else {
            1.0
        }
    }

    fun generateScalingFactorCode(): String {
        val factors = analyzeScoringCurves()
        val code = StringBuilder()

        code.appendLine("val FACTORS = mapOf(")
        
        // Add base game entries
        val games = samples.map { it.game }.distinct()
        games.forEach { game ->
            code.appendLine("    \"$game\" to mapOf(")
            
            games.filter { it != game }.forEach { otherGame ->
                val factor = factors[otherGame] ?: RangeScalingFactor(1.0, 1.0, 1.0)
                code.appendLine("        \"$otherGame\" to RangeScalingFactor(${factor.low}, ${factor.mid}, ${factor.high}),")
            }
            
            code.appendLine("    ),")
        }

        code.appendLine(")")
        return code.toString()
    }

    fun validateConversion(fromGame: String, toGame: String, score: Int): Double {
        val factor = analyzeScoringCurves()[toGame] ?: return 1.0
        return when {
            score < 100000 -> factor.low
            score < 500000 -> factor.mid
            else -> factor.high
        }
    }

    fun printAnalysisReport() {
        println("=== Scaling Factor Analysis Report ===")
        println("Base Game: $baseGame")
        println("Total Samples: ${samples.size}")
        println("\nSamples per Game:")
        samples.groupBy { it.game }.forEach { (game, samples) ->
            println("$game: ${samples.size} samples")
            println("  Skill Levels: ${samples.map { it.skillLevel }.distinct()}")
            println("  Level Range: ${samples.minOf { it.level }} - ${samples.maxOf { it.level }}")
            println("  Score Range: ${samples.minOf { it.score }} - ${samples.maxOf { it.score }}")
        }

        println("\nCalculated Scaling Factors:")
        analyzeScoringCurves().forEach { (game, factor) ->
            println("$game:")
            println("  Low (<100k): ${factor.low}")
            println("  Mid (100k-500k): ${factor.mid}")
            println("  High (>500k): ${factor.high}")
        }
    }
} 