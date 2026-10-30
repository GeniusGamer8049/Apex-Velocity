package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProfile
import com.example.model.TireCompound
import com.example.model.Vehicle
import com.example.model.VehicleUpgrades
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.ApexGold
import com.example.ui.theme.ApexGreen
import com.example.ui.theme.ApexOrange
import com.example.ui.theme.ApexRed
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun GarageScreen(
    profile: PlayerProfile,
    onSelectVehicle: (String) -> Unit,
    onPurchaseVehicle: (String) -> Unit,
    onUpgradeVehicle: (String, String) -> Unit,
    onEquipTireCompound: (String, TireCompound) -> Unit,
    onUpdateCustomization: (String, String, String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentVehicle = profile.vehicles.find { it.id == profile.selectedVehicleId } ?: profile.vehicles.first()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Garage Header with Image Banner & Currency
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_garage_hero_1793368517617),
                    contentDescription = "Garage Showroom",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CarbonBlack.copy(alpha = 0.90f))
                            )
                        )
                )

                // Top Balance Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Currency pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, ApexGreen, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$ ${String.format("%,d", profile.credits)}",
                            color = ApexGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Reputation pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CarbonBlack.copy(alpha = 0.85f))
                            .border(1.dp, ApexGold, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Reputation", tint = ApexGold, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${profile.reputation} REP",
                                color = ApexGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Title and selected car
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentVehicle.name,
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${currentVehicle.brand} • ${currentVehicle.category} • ${currentVehicle.driveTrain.displayName}",
                        color = ApexCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Fleet Selector Cards
        item {
            Text(
                text = "SELECT VEHICLE FLEET",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(profile.vehicles) { vehicle ->
                    val isSelected = vehicle.id == currentVehicle.id
                    VehicleCard(
                        vehicle = vehicle,
                        isSelected = isSelected,
                        playerCredits = profile.credits,
                        playerRep = profile.reputation,
                        onSelect = { onSelectVehicle(vehicle.id) },
                        onPurchase = { onPurchaseVehicle(vehicle.id) }
                    )
                }
            }
        }

        // Section Tabs: Performance Upgrades vs Customization Paint
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = ApexCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ApexCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("PERFORMANCE UPGRADES", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("TIRES & STYLING", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        if (selectedTab == 0) {
            // Performance Upgrades Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpgradeItem(
                        title = "Engine & Turbo Tuning",
                        category = "engine",
                        stage = currentVehicle.upgrades.engineStage,
                        currentVal = "${currentVehicle.effectiveBhp} BHP",
                        nextVal = "+${(currentVehicle.baseBhp * 0.16f).toInt()} BHP",
                        playerCredits = profile.credits,
                        onUpgrade = { onUpgradeVehicle(currentVehicle.id, "engine") }
                    )
                    UpgradeItem(
                        title = "Racing Suspension & Camber",
                        category = "suspension",
                        stage = currentVehicle.upgrades.suspensionStage,
                        currentVal = "Stage ${currentVehicle.upgrades.suspensionStage}",
                        nextVal = "-15% Body Roll, +10% Turn-in",
                        playerCredits = profile.credits,
                        onUpgrade = { onUpgradeVehicle(currentVehicle.id, "suspension") }
                    )
                    UpgradeItem(
                        title = "Transmission & Gearbox",
                        category = "transmission",
                        stage = currentVehicle.upgrades.transmissionStage,
                        currentVal = "Stage ${currentVehicle.upgrades.transmissionStage}",
                        nextVal = "Faster Shift Times, +Top Speed",
                        playerCredits = profile.credits,
                        onUpgrade = { onUpgradeVehicle(currentVehicle.id, "transmission") }
                    )
                    UpgradeItem(
                        title = "Carbon-Ceramic Brakes",
                        category = "brakes",
                        stage = currentVehicle.upgrades.brakesStage,
                        currentVal = "Stage ${currentVehicle.upgrades.brakesStage}",
                        nextVal = "+18% Stopping Force",
                        playerCredits = profile.credits,
                        onUpgrade = { onUpgradeVehicle(currentVehicle.id, "brakes") }
                    )
                    UpgradeItem(
                        title = "Aerodynamics & GT Wing",
                        category = "aero",
                        stage = currentVehicle.upgrades.aeroStage,
                        currentVal = "Stage ${currentVehicle.upgrades.aeroStage}",
                        nextVal = "+High Speed Downforce",
                        playerCredits = profile.credits,
                        onUpgrade = { onUpgradeVehicle(currentVehicle.id, "aero") }
                    )
                    UpgradeItem(
                        title = "Weight Reduction Diet",
                        category = "weight",
                        stage = currentVehicle.upgrades.weightStage,
                        currentVal = "${currentVehicle.effectiveWeightKg} kg",
                        nextVal = "-${(currentVehicle.baseWeightKg * 0.05f).toInt()} kg",
                        playerCredits = profile.credits,
                        onUpgrade = { onUpgradeVehicle(currentVehicle.id, "weight") }
                    )
                }
            }
        } else {
            // Tire Compound Selection & Styling
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "TIRE COMPOUND STRATEGY (CRUCIAL FOR WEATHER)",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TireCompound.values().forEach { compound ->
                        val isEquipped = currentVehicle.upgrades.tireCompound == compound
                        val cost = VehicleUpgrades.tireCost(compound)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isEquipped) 2.dp else 1.dp,
                                    color = if (isEquipped) ApexCyan else SurfaceBorder,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = compound.displayName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (isEquipped) {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ApexCyan).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                Text("EQUIPPED", color = CarbonBlack, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = compound.description, color = TextMuted, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("Dry Grip: ${(compound.dryGripMultiplier * 100).toInt()}%", color = ApexGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("Wet Grip: ${(compound.wetGripMultiplier * 100).toInt()}%", color = Color(0xFF29B6F6), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                if (!isEquipped) {
                                    Button(
                                        onClick = { onEquipTireCompound(currentVehicle.id, compound) },
                                        enabled = cost == 0 || profile.credits >= cost,
                                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan, contentColor = CarbonBlack),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (cost > 0) "EQUIP ($$cost)" else "EQUIP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Body Paint Colors
                    Text(
                        text = "BODY PAINT FINISH",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val colors = listOf(
                        "#00E5FF", // Apex Cyan
                        "#FF6D00", // Apex Orange
                        "#E53935", // Red
                        "#76FF03", // Acid Lime
                        "#1E88E5", // Racing Blue
                        "#FFD600", // Yellow
                        "#9C27B0", // Deep Purple
                        "#FFFFFF", // Pure White
                        "#212121"  // Matte Black
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { hex ->
                            val color = parseHex(hex)
                            val isSelected = currentVehicle.bodyColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) TextWhite else SurfaceBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateCustomization(currentVehicle.id, hex, currentVehicle.underglowColorHex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = if (hex == "#FFFFFF") CarbonBlack else Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Neon Underglow Toggle
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NEON UNDERGLOW",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val newUnderglow = if (currentVehicle.underglowColorHex == null) "#00E5FF" else null
                                onUpdateCustomization(currentVehicle.id, currentVehicle.bodyColorHex, newUnderglow)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentVehicle.underglowColorHex != null) ApexCyan else DarkSurfaceElevated,
                                contentColor = if (currentVehicle.underglowColorHex != null) CarbonBlack else TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (currentVehicle.underglowColorHex != null) "UNDERGLOW: ON" else "UNDERGLOW: OFF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    playerCredits: Int,
    playerRep: Int,
    onSelect: () -> Unit,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ApexCyan else SurfaceBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                if (vehicle.isUnlocked) onSelect()
            },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = vehicle.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (isSelected) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ApexCyan).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("ACTIVE", color = CarbonBlack, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(text = vehicle.category, color = TextMuted, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(10.dp))

            // Specs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("POWER", color = TextMuted, fontSize = 9.sp)
                    Text("${vehicle.effectiveBhp} BHP", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("TOP SPEED", color = TextMuted, fontSize = 9.sp)
                    Text("${vehicle.effectiveTopSpeedKmh} km/h", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("DRIVE", color = TextMuted, fontSize = 9.sp)
                    Text(vehicle.driveTrain.name, color = ApexOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!vehicle.isUnlocked) {
                val canBuy = playerCredits >= vehicle.price && playerRep >= vehicle.repRequired
                Button(
                    onClick = onPurchase,
                    enabled = canBuy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ApexGreen, contentColor = CarbonBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("BUY $$vehicle.price", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!isSelected) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SELECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun UpgradeItem(
    title: String,
    category: String,
    stage: Int,
    currentVal: String,
    nextVal: String,
    playerCredits: Int,
    onUpgrade: () -> Unit
) {
    val isMax = stage >= 5
    val nextStage = stage + 1
    val cost = VehicleUpgrades.costForUpgrade(category, nextStage)
    val canAfford = playerCredits >= cost && !isMax

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = currentVal, color = ApexCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (!isMax) {
                        Text(text = "➜ $nextVal", color = ApexGreen, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Stage indicators 1..5
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(if (i <= stage) ApexCyan else Color(0xFF263238))
                        )
                    }
                }
            }

            if (!isMax) {
                Button(
                    onClick = onUpgrade,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) ApexCyan else DarkSurfaceElevated,
                        contentColor = if (canAfford) CarbonBlack else TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STAGE $nextStage", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("$$cost", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ApexGold.copy(alpha = 0.2f))
                        .border(1.dp, ApexGold, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("MAX STAGE", color = ApexGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun parseHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        ApexCyan
    }
}
