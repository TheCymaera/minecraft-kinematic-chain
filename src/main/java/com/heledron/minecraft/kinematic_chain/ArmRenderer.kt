package com.heledron.minecraft.kinematic_chain

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.Vector

object ArmRenderer {
	fun drawArm(arm: Arm) {
		for (segment in arm.segments) {
			drawLine(segment.base, segment.span, segment.thickness)
		}
	}

	private fun drawLine(origin: Location, line: Vector, thickness: Double) {
		val segments = (line.length() * 10).toInt()
		for (loc in getPointsOnLine(origin, line, segments)) {
			loc.world.spawnParticle(Particle.WATER_BUBBLE, loc, Math.max(1, (thickness * 20).toInt()), thickness / 2, thickness / 2, thickness / 2, 0.0)
		}
	}

	private fun getPointsOnLine(location: Location, line: Vector, segments: Int): Array<Location> {
		val outs = Array(segments) { location }
		val stride = line.clone().multiply(1 / segments.toDouble())

		val currentPoint = location.clone()
		for (i in 0 until segments) {
			outs[i] = currentPoint.clone()
			currentPoint.add(stride)
		}
		return outs
	}
}