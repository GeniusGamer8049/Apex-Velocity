package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RacingSoundManager
import com.example.data.AudioSettings
import com.example.data.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing audio settings and persistence to DataStore,
 * directly controlling live synthesizer parameters in [RacingSoundManager].
 */
class SettingsViewModel(
    application: Application,
    val settingsDataStore: SettingsDataStore = SettingsDataStore.getInstance(application)
) : AndroidViewModel(application) {

    private val soundManager = RacingSoundManager.getInstance(application)

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    val audioSettings: StateFlow<AudioSettings> = settingsDataStore.audioSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AudioSettings()
        )

    init {
        // Observe settings changes and automatically sync to active sound manager
        viewModelScope.launch {
            audioSettings.collect { settings ->
                applyAudioSettings(settings)
            }
        }
    }

    private fun applyAudioSettings(settings: AudioSettings) {
        soundManager.isMuted = settings.isMasterMuted
        soundManager.masterVolume = settings.masterVolume

        val sfxMult = if (settings.soundEffectsEnabled) settings.soundEffectsVolume else 0f
        soundManager.screechVolume = 0.85f * sfxMult
        soundManager.collisionVolume = 1.0f * sfxMult
        soundManager.effectsVolume = 0.75f * sfxMult

        val engineMult = if (settings.engineSoundEnabled) 0.90f else 0f
        soundManager.engineVolume = engineMult
    }

    fun setMasterVolume(volume: Float) {
        viewModelScope.launch {
            settingsDataStore.setMasterVolume(volume)
        }
    }

    fun toggleMasterMute() {
        viewModelScope.launch {
            settingsDataStore.toggleMasterMuted()
        }
    }

    fun setMasterMuted(isMuted: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setMasterMuted(isMuted)
        }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSoundEffectsEnabled(enabled)
        }
    }

    fun toggleSoundEffects() {
        viewModelScope.launch {
            settingsDataStore.toggleSoundEffects()
        }
    }

    fun setSoundEffectsVolume(volume: Float) {
        viewModelScope.launch {
            settingsDataStore.setSoundEffectsVolume(volume)
        }
    }

    fun setEngineSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setEngineSoundEnabled(enabled)
        }
    }

    fun showOverlay() {
        _isOverlayVisible.value = true
    }

    fun hideOverlay() {
        _isOverlayVisible.value = false
    }

    fun toggleOverlay() {
        _isOverlayVisible.value = !_isOverlayVisible.value
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetToDefaults()
        }
    }

    /**
     * Plays a test sound effect to preview current volume & SFX configuration.
     */
    fun testAudio() {
        soundManager.playGearShift()
        soundManager.playCollision(0.6f)
    }
}
