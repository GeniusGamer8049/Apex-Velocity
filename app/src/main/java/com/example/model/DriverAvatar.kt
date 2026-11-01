package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexPurple
import com.example.ui.theme.ApexRed

data class DriverAvatar(
    val id: String,
    val name: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tag: String,
    val requiredRep: Int = 0
)

object DriverAvatarRegistry {

    val allAvatars: List<DriverAvatar> = listOf(
        DriverAvatar(
            id = "avatar_cyber_neon",
            name = "Cyber Visor",
            subtitle = "Holographic telemetry HUD helmet",
            primaryColor = ApexCyan,
            secondaryColor = ApexPurple,
            tag = "CYBER",
            requiredRep = 0
        ),
        DriverAvatar(
            id = "avatar_carbon_stealth",
            name = "Carbon Shadow",
            subtitle = "Matte woven aero composite shell",
            primaryColor = Color(0xFF90A4AE),
            secondaryColor = ApexRed,
            tag = "STEALTH",
            requiredRep = 0
        ),
        DriverAvatar(
            id = "avatar_tokyo_drift",
            name = "Night Drifter",
            subtitle = "Neon magenta underglow enthusiast",
            primaryColor = Color(0xFFFF4081),
            secondaryColor = ApexPurple,
            tag = "DRIFT",
            requiredRep = 150
        ),
        DriverAvatar(
            id = "avatar_monaco_gold",
            name = "Monaco GP",
            subtitle = "Gold-leaf Grand Prix champion",
            primaryColor = ApexGold,
            secondaryColor = Color(0xFFFFF8E1),
            tag = "ELITE",
            requiredRep = 350
        ),
        DriverAvatar(
            id = "avatar_crimson_viper",
            name = "Crimson Apex",
            subtitle = "V8 twin-turbo track specialist",
            primaryColor = ApexRed,
            secondaryColor = ApexOrange,
            tag = "RACING",
            requiredRep = 600
        ),
        DriverAvatar(
            id = "avatar_tachyon_green",
            name = "Tachyon Turbo",
            subtitle = "High-velocity hybrid prototype",
            primaryColor = ApexGreen,
            secondaryColor = ApexCyan,
            tag = "HYBRID",
            requiredRep = 1000
        ),
        DriverAvatar(
            id = "avatar_titanium_phantom",
            name = "Titanium Ghost",
            subtitle = "Aerospace grade titanium structure",
            primaryColor = Color(0xFFB388FF),
            secondaryColor = Color(0xFFE0E0E0),
            tag = "TITANIUM",
            requiredRep = 1800
        ),
        DriverAvatar(
            id = "avatar_sunset_outrun",
            name = "Sunset Outrun",
            subtitle = "Synthwave retro highway cruiser",
            primaryColor = ApexOrange,
            secondaryColor = Color(0xFFFF80AB),
            tag = "SYNTHWAVE",
            requiredRep = 2500
        )
    )

    fun getAvatarById(id: String): DriverAvatar {
        return allAvatars.find { it.id == id } ?: allAvatars.first()
    }
}
