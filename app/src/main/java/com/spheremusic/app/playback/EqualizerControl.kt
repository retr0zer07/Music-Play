package com.spheremusic.app.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val enabled: Boolean = false,
    val frequencies: List<Int> = emptyList(),
    val levels: List<Int> = emptyList(),
    val minimum: Int = -1500,
    val maximum: Int = 1500,
    val preset: String = "Normal",
    val error: Boolean = false,
)

enum class EqualizerPreset(val label: String, val curve: List<Int>) {
    NORMAL("Normal", listOf(0, 0, 0, 0, 0)),
    ROCK("Rock", listOf(500, 250, -150, 250, 450)),
    POP("Pop", listOf(-100, 200, 400, 200, -100)),
    CLASSICAL("Clásica", listOf(350, 150, -100, 150, 350)),
    BASS("Graves", listOf(650, 400, 100, 0, 0)),
    VOICE("Voz", listOf(-250, 0, 450, 350, -100));

    fun level(index: Int, count: Int): Int {
        if (count <= 1) return curve[2]
        val position = index.toFloat() / (count - 1) * (curve.size - 1)
        val lower = position.toInt()
        val upper = (lower + 1).coerceAtMost(curve.lastIndex)
        return (curve[lower] + (curve[upper] - curve[lower]) * (position - lower)).toInt()
    }
}

object EqualizerControl {
    private val mutableState = MutableStateFlow(EqualizerState())
    val state = mutableState.asStateFlow()
    private var effect: Equalizer? = null
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.getSharedPreferences("sphere_equalizer", Context.MODE_PRIVATE)
        mutableState.value = mutableState.value.copy(
            enabled = preferences!!.getBoolean("enabled", false),
            preset = preferences!!.getString("preset", "Normal").orEmpty(),
        )
    }

    fun attach(sessionId: Int) {
        release()
        if (sessionId == 0) return
        attempt {
            val equalizer = Equalizer(0, sessionId)
            effect = equalizer
            val count = equalizer.numberOfBands.toInt()
            require(count > 0) { "No equalizer bands" }
            val range = equalizer.bandLevelRange
            val saved = preferences
            val preset = EqualizerPreset.entries.find { it.label == mutableState.value.preset }
            mutableState.value = mutableState.value.copy(
                frequencies = List(count) { equalizer.getCenterFreq(it.toShort()) / 1000 },
                levels = List(count) {
                    (saved?.getInt("band_$it", preset?.level(it, count) ?: 0) ?: 0).coerceIn(range[0].toInt(), range[1].toInt())
                },
                minimum = range[0].toInt(), maximum = range[1].toInt(), error = false,
            )
            apply()
        }
    }

    fun setEnabled(enabled: Boolean) = change { it.copy(enabled = enabled) }
    fun setBand(index: Int, value: Int) = change { state ->
        require(index in state.levels.indices)
        state.copy(levels = state.levels.mapIndexed { i, old ->
            if (i == index) value.coerceIn(state.minimum, state.maximum) else old
        }, preset = "Personalizado")
    }
    fun setPreset(preset: EqualizerPreset) = change { state ->
        state.copy(preset = preset.label, levels = state.levels.indices.map {
            preset.level(it, state.levels.size).coerceIn(state.minimum, state.maximum)
        })
    }

    private fun change(update: (EqualizerState) -> EqualizerState) {
        attempt {
            mutableState.value = update(mutableState.value)
            apply()
            val state = mutableState.value
            preferences?.edit()?.apply {
                putBoolean("enabled", state.enabled)
                putString("preset", state.preset)
                state.levels.forEachIndexed { index, value -> putInt("band_$index", value) }
            }?.apply()
        }
    }

    private fun apply() {
        val equalizer = effect ?: return
        val state = mutableState.value
        check(equalizer.hasControl()) { "Equalizer is controlled by another application" }
        state.levels.forEachIndexed { index, value -> equalizer.setBandLevel(index.toShort(), value.toShort()) }
        check(equalizer.setEnabled(state.enabled) == 0) { "Cannot enable equalizer" }
    }

    private fun attempt(block: () -> Unit) {
        try {
            block()
        } catch (error: IllegalStateException) { failed(error)
        } catch (error: IllegalArgumentException) { failed(error)
        } catch (error: UnsupportedOperationException) { failed(error)
        } catch (error: SecurityException) { failed(error) }
    }

    private fun failed(error: Exception) {
        Log.e("SphereMusic", "Equalizer unavailable", error)
        mutableState.value = mutableState.value.copy(error = true)
    }

    fun release() {
        effect?.release()
        effect = null
        mutableState.value = mutableState.value.copy(frequencies = emptyList(), levels = emptyList())
    }
}
