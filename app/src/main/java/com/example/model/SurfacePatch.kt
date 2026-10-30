package com.example.model

enum class SurfaceHazardType(val displayName: String, val gripFactor: Float, val inducesAquaplane: Boolean) {
    PUDDLE("Standing Water Puddle", 0.35f, true),
    ICE_PATCH("Black Ice Patch", 0.20f, false),
    OIL_SLICK("Oil Spill", 0.28f, false),
    KERB_RUMBLE("Apex Rumble Kerb", 0.92f, false),
    OFFROAD_GRASS("Off-track Grass", 0.35f, false),
    GRAVEL_TRAP("Gravel Runoff Trap", 0.22f, false)
}

data class SurfacePatch(
    val id: String,
    val type: SurfaceHazardType,
    val x: Float,
    val y: Float,
    val radius: Float,
    val length: Float = radius * 2f,
    val angle: Float = 0f
) {
    fun contains(px: Float, py: Float): Boolean {
        val dx = px - x
        val dy = py - y
        return (dx * dx + dy * dy) <= (radius * radius)
    }
}
