package com.example.model

enum class DriveTrain(val displayName: String, val driftBias: Float, val launchGrip: Float) {
    RWD("Rear-Wheel Drive (RWD)", 1.25f, 0.95f),
    AWD("All-Wheel Drive (AWD)", 0.70f, 1.20f),
    FWD("Front-Wheel Drive (FWD)", 0.50f, 0.85f)
}

data class VehicleUpgrades(
    val engineStage: Int = 1,        // 1..5: Boosts horsepower, torque, top speed
    val tireCompound: TireCompound = TireCompound.SPORT_RADIAL,
    val tireStage: Int = 1,         // 1..5: Enhances lateral grip & tire responsiveness
    val suspensionStage: Int = 1,   // 1..5: Reduces body roll, sharpens steering response
    val transmissionStage: Int = 1, // 1..5: Reduces shift lag & optimizes acceleration
    val brakesStage: Int = 1,       // 1..5: Increases braking force and shortens stop distance
    val aeroStage: Int = 1,         // 1..5: Adds high-speed downforce and high-speed corner stability
    val weightStage: Int = 1        // 1..5: Carbon fiber diet, reduces vehicle mass
) {
    companion object {
        fun costForUpgrade(category: String, nextStage: Int): Int {
            return when (nextStage) {
                2 -> 1200
                3 -> 2800
                4 -> 5500
                5 -> 9500
                else -> 0
            }
        }
        
        fun tireCost(compound: TireCompound): Int {
            return when (compound) {
                TireCompound.SPORT_RADIAL -> 0
                TireCompound.SEMI_SLICK -> 1500
                TireCompound.SOFT_SLICK -> 2500
                TireCompound.WET_INTERMEDIATE -> 1800
                TireCompound.FULL_WET -> 2400
            }
        }
    }
}

data class Vehicle(
    val id: String,
    val name: String,
    val brand: String,
    val category: String,
    val baseBhp: Int,
    val baseWeightKg: Int,
    val baseMaxRpm: Int,
    val baseTopSpeedKmh: Int,
    val driveTrain: DriveTrain,
    val price: Int,
    val repRequired: Int,
    val isUnlocked: Boolean = false,
    val upgrades: VehicleUpgrades = VehicleUpgrades(),
    val bodyColorHex: String = "#00E5FF",
    val liveryId: Int = 0,
    val underglowColorHex: String? = null // null if off, hex if on
) {
    // Effective calculated stats factoring in upgrades
    val effectiveBhp: Int
        get() = (baseBhp * (1f + (upgrades.engineStage - 1) * 0.16f)).toInt()

    val effectiveWeightKg: Int
        get() = (baseWeightKg * (1f - (upgrades.weightStage - 1) * 0.05f)).toInt()

    val effectiveTopSpeedKmh: Int
        get() = (baseTopSpeedKmh * (1f + (upgrades.engineStage - 1) * 0.06f + (upgrades.transmissionStage - 1) * 0.02f - (upgrades.aeroStage - 1) * 0.01f)).toInt()

    val powerToWeightRatio: Float
        get() = effectiveBhp.toFloat() / (effectiveWeightKg / 1000f)

    val effectiveGripMultiplier: Float
        get() = (1f + (upgrades.tireStage - 1) * 0.09f + (upgrades.suspensionStage - 1) * 0.04f)

    val effectiveBrakingPower: Float
        get() = (1f + (upgrades.brakesStage - 1) * 0.18f)

    val effectiveDownforceFactor: Float
        get() = (upgrades.aeroStage * 0.15f)
}
