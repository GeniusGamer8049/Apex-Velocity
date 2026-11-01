package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.DriverAvatar
import com.example.model.DriverAvatarRegistry
import com.example.ui.theme.ApexGold
import com.example.ui.theme.CarbonBlack

enum class AvatarSize(val dimension: Dp, val iconSize: Dp, val badgeSize: Dp) {
    SMALL(36.dp, 20.dp, 14.dp),
    MEDIUM(48.dp, 28.dp, 18.dp),
    LARGE(68.dp, 38.dp, 22.dp),
    HERO(92.dp, 52.dp, 28.dp)
}

@Composable
fun DriverAvatarView(
    avatarId: String,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.MEDIUM,
    showEquippedBadgeIndicator: Boolean = false,
    hasEquippedBadge: Boolean = false
) {
    val avatar = DriverAvatarRegistry.getAvatarById(avatarId)
    val gradientBrush = Brush.sweepGradient(
        listOf(
            avatar.primaryColor,
            avatar.secondaryColor,
            avatar.primaryColor.copy(alpha = 0.6f),
            avatar.primaryColor
        )
    )

    Box(
        modifier = modifier.size(size.dimension),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ring
        Box(
            modifier = Modifier
                .size(size.dimension)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(avatar.primaryColor.copy(alpha = 0.35f), Color.Transparent)))
                .border(2.dp, gradientBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner carbon cockpit background
            Box(
                modifier = Modifier
                    .size(size.dimension - 6.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                CarbonBlack,
                                avatar.primaryColor.copy(alpha = 0.25f),
                                CarbonBlack
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Motorsports Helmet Icon with custom tint
                Icon(
                    imageVector = Icons.Default.SportsMotorsports,
                    contentDescription = avatar.name,
                    tint = avatar.primaryColor,
                    modifier = Modifier.size(size.iconSize)
                )

                // High-tech visor accent bar in middle
                Box(
                    modifier = Modifier
                        .size(width = size.iconSize * 0.75f, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(avatar.secondaryColor.copy(alpha = 0.85f))
                        .align(Alignment.Center)
                )
            }
        }

        // Corner Equipped Badge Indicator
        if (showEquippedBadgeIndicator && hasEquippedBadge) {
            Box(
                modifier = Modifier
                    .size(size.badgeSize)
                    .clip(CircleShape)
                    .background(CarbonBlack)
                    .border(1.dp, ApexGold, CircleShape)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = "Equipped Badge",
                    tint = ApexGold,
                    modifier = Modifier.size(size.badgeSize * 0.65f)
                )
            }
        }
    }
}
