package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ControlScheme
import com.example.model.TireCompound
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.components.RacePauseMenu

@Composable
fun PauseMenuDialog(
    currentCompound: TireCompound,
    trackWetness: Float,
    isMuted: Boolean,
    controlScheme: ControlScheme,
    onResume: () -> Unit,
    onPitStopTireSwap: (TireCompound) -> Unit,
    onToggleAudio: () -> Unit,
    onToggleControls: () -> Unit,
    onOpenTelemetry: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    RacePauseMenu(
        onResume = onResume,
        onRestartRace = onRestart,
        onQuitToMainMenu = onExit,
        currentCompound = currentCompound,
        trackWetness = trackWetness,
        isMuted = isMuted,
        controlScheme = controlScheme,
        onPitStopTireSwap = onPitStopTireSwap,
        onToggleAudio = onToggleAudio,
        onToggleControls = onToggleControls,
        onOpenTelemetry = onOpenTelemetry
    )
}
