package com.heledron.minecraft.kinematic_chain

import org.bukkit.Location
import org.bukkit.util.Vector

class Arm(var location: Location, var segments: Array<Segment>) {
	fun connectSegments() {
		val current = location.clone()
		for (segment in segments) {
			segment.base = current.clone()
			current.add(segment.span)
		}
	}

	fun follow(target: Location?) {
		var target = target
		for (i in segments.indices.reversed()) {
			val segment = segments[i]
			segment.follow(target)
			target = segment.base
		}
		val resolve = segments[0].base.toVector().subtract(location.toVector())
		for (segment in segments) segment.base.subtract(resolve)
	}

	fun followIncrementally(target: Location, length: Double) {
		val tip = tipLocation()
		if (target.toVector().distance(tip.toVector()) > length) {
			val change = target.toVector().subtract(tip.toVector()).normalize().multiply(length)
			follow(tip.add(change))
		} else {
			follow(target)
		}
	}

	fun tipLocation(): Location {
		val lastSegment = segments[segments.size - 1]
		return lastSegment.base.clone().add(lastSegment.span)
	}

	class Segment(base: Location, span: Vector, thickness: Double) {
		var base = base
		var span = span
		var thickness = thickness

		fun follow(target: Location?) {
			span.copy(target!!.toVector().subtract(base.toVector()).normalize().multiply(span.length()))
			base = target.clone().subtract(span)
		}
	}
}