package com.accidentalproductions.tetristats.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameVersion: String,
    val scoreValue: Int,
    val startLevel: Int? = null,
    val endLevel: Int? = null,
    val linesCleared: Int? = null,
    val dateRecorded: Long = System.currentTimeMillis(),
    val mediaUri: String? = null
)
