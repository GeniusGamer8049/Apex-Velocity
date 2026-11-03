package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "apex_velocity_settings")

/**
 * Data class representing persisted audio settings.
 */
data class AudioSettings(
    val masterVolume: Float = 1.0f,
    val isMasterMuted: Boolean = false,
    val soundEffectsEnabled: Boolean = true,
    val soundEffectsVolume: Float = 1.0f,
    val engineSoundEnabled: Boolean = true
)

/**
 * DataStore repository responsible for persisting audio and game preferences.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_MASTER_VOLUME = floatPreferencesKey("master_volume")
        val KEY_MASTER_MUTED = booleanPreferencesKey("master_muted")
        val KEY_SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val KEY_SOUND_EFFECTS_VOLUME = floatPreferencesKey("sound_effects_volume")
        val KEY_ENGINE_SOUND_ENABLED = booleanPreferencesKey("engine_sound_enabled")

        @Volatile
        private var INSTANCE: SettingsDataStore? = null

        fun getInstance(context: Context): SettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsDataStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Continuous reactive Flow emitting current audio settings from DataStore.
     */
    val audioSettingsFlow: Flow<AudioSettings> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AudioSettings(
                masterVolume = preferences[KEY_MASTER_VOLUME] ?: 1.0f,
                isMasterMuted = preferences[KEY_MASTER_MUTED] ?: false,
                soundEffectsEnabled = preferences[KEY_SOUND_EFFECTS_ENABLED] ?: true,
                soundEffectsVolume = preferences[KEY_SOUND_EFFECTS_VOLUME] ?: 1.0f,
                engineSoundEnabled = preferences[KEY_ENGINE_SOUND_ENABLED] ?: true
            )
        }

    suspend fun setMasterVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_MASTER_VOLUME] = clamped
        }
    }

    suspend fun setMasterMuted(isMuted: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_MASTER_MUTED] = isMuted
        }
    }

    suspend fun toggleMasterMuted() {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[KEY_MASTER_MUTED] ?: false
            preferences[KEY_MASTER_MUTED] = !current
        }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_SOUND_EFFECTS_ENABLED] = enabled
        }
    }

    suspend fun toggleSoundEffects() {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[KEY_SOUND_EFFECTS_ENABLED] ?: true
            preferences[KEY_SOUND_EFFECTS_ENABLED] = !current
        }
    }

    suspend fun setSoundEffectsVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_SOUND_EFFECTS_VOLUME] = clamped
        }
    }

    suspend fun setEngineSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_ENGINE_SOUND_ENABLED] = enabled
        }
    }

    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_MASTER_VOLUME] = 1.0f
            preferences[KEY_MASTER_MUTED] = false
            preferences[KEY_SOUND_EFFECTS_ENABLED] = true
            preferences[KEY_SOUND_EFFECTS_VOLUME] = 1.0f
            preferences[KEY_ENGINE_SOUND_ENABLED] = true
        }
    }
}
