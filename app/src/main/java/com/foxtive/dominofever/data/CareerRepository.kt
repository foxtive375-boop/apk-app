package com.foxtive.dominofever.data

import android.content.Context
import com.foxtive.dominofever.tournament.CareerProgress

class CareerRepository(context: Context) {
    private val prefs = context.getSharedPreferences("domino_career", Context.MODE_PRIVATE)

    fun load(): CareerProgress {
        val currentStage = prefs.getInt(KEY_STAGE, 0)
        val rating = prefs.getInt(KEY_RATING, 0)
        val finished = prefs.getBoolean(KEY_FINISHED, false)
        return CareerProgress(
            currentStageIndex = currentStage,
            rating = rating,
            finished = finished
        )
    }

    fun save(progress: CareerProgress) {
        prefs.edit()
            .putInt(KEY_STAGE, progress.currentStageIndex)
            .putInt(KEY_RATING, progress.rating)
            .putBoolean(KEY_FINISHED, progress.finished)
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_STAGE = "stage"
        const val KEY_RATING = "rating"
        const val KEY_FINISHED = "finished"
    }
}
