package com.example.model

object VehicleRegistry {
    fun createDefaultVehicles(): List<Vehicle> {
        return listOf(
            Vehicle(
                id = "apex_gt86",
                name = "Apex GT-86 Coupe",
                brand = "Apex Motors",
                category = "Sport Compact",
                baseBhp = 205,
                baseWeightKg = 1240,
                baseMaxRpm = 7500,
                baseTopSpeedKmh = 225,
                driveTrain = DriveTrain.RWD,
                price = 0,
                repRequired = 0,
                isUnlocked = true,
                bodyColorHex = "#00E5FF", // Cyan
                liveryId = 0
            ),
            Vehicle(
                id = "silvia_s15",
                name = "Silvia S15 Spec-R",
                brand = "Nissan Spirit",
                category = "Drift Tuner",
                baseBhp = 250,
                baseWeightKg = 1210,
                baseMaxRpm = 7800,
                baseTopSpeedKmh = 245,
                driveTrain = DriveTrain.RWD,
                price = 6800,
                repRequired = 100,
                isUnlocked = false,
                bodyColorHex = "#FF6D00", // Apex Orange
                liveryId = 1
            ),
            Vehicle(
                id = "evo_rally",
                name = "Evolution IX GSR",
                brand = "Mitsubishi Rally",
                category = "AWD Track & Wet",
                baseBhp = 320,
                baseWeightKg = 1380,
                baseMaxRpm = 7600,
                baseTopSpeedKmh = 260,
                driveTrain = DriveTrain.AWD,
                price = 15000,
                repRequired = 300,
                isUnlocked = false,
                bodyColorHex = "#E53935", // Red
                liveryId = 2
            ),
            Vehicle(
                id = "viper_gt",
                name = "Viper GTS V10",
                brand = "Detroit Muscle",
                category = "Grand Tourer",
                baseBhp = 505,
                baseWeightKg = 1520,
                baseMaxRpm = 6800,
                baseTopSpeedKmh = 310,
                driveTrain = DriveTrain.RWD,
                price = 32000,
                repRequired = 650,
                isUnlocked = false,
                bodyColorHex = "#1E88E5", // Blue with white stripes
                liveryId = 3
            ),
            Vehicle(
                id = "veneno_hyper",
                name = "Veneno Hyper GT",
                brand = "Apex Exotic",
                category = "Hypercar",
                baseBhp = 750,
                baseWeightKg = 1340,
                baseMaxRpm = 8500,
                baseTopSpeedKmh = 355,
                driveTrain = DriveTrain.AWD,
                price = 72000,
                repRequired = 1100,
                isUnlocked = false,
                bodyColorHex = "#76FF03", // Acid Neon Lime
                liveryId = 4
            ),
            Vehicle(
                id = "apex_f1",
                name = "Apex Formula Open-Wheel",
                brand = "Scuderia Apex",
                category = "Formula Single-Seater",
                baseBhp = 950,
                baseWeightKg = 795,
                baseMaxRpm = 12000,
                baseTopSpeedKmh = 370,
                driveTrain = DriveTrain.RWD,
                price = 145000,
                repRequired = 1800,
                isUnlocked = false,
                bodyColorHex = "#D50000", // Scuderia Red
                liveryId = 5
            )
        )
    }
}
