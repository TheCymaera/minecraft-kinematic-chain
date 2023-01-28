package com.heledron.minecraft.kinematic_chain

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.entity.Chicken
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Entity
import java.lang.Runnable
import org.bukkit.loot.LootTables
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

class Tendril internal constructor(location: Location, var mouthLocation: Location, // we need the plugin so we can schedule tasks
								   var plugin: JavaPlugin) {
	var arm: Arm
	var frozenTime = 0
	var targetEntity: Chicken? = null
	var targetEntityHeld = false
	var moveSpeed = 3.0
	var chaseSpeed = 12.0
	var chaseDistance = 3.5
	var age = 0

	init {
		arm = createArm(location)
	}

	fun update() {
		move()
		ArmRenderer.drawArm(arm)
		age++
		if (frozenTime > 0) frozenTime--
	}

	private fun createArm(location: Location): Arm {
		var rotate = 0.0
		val rotateAmount = .25
		val arm = Arm(location, arrayOf(
				Arm.Segment(location, Vector(.0, .5, .0), .3),
				Arm.Segment(location, Vector(.0, .5, .0), .3),
				Arm.Segment(location, Vector(.0, .5, .0), .2),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate -= it; rotate }), .2),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate -= it; rotate }), .2),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate -= it; rotate }), .1),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate -= it; rotate }), .1),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .1),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .1),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .1),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .1),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .00),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .00),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .00),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .00),
				Arm.Segment(location, Vector(.0, .5, .0).rotateAroundX(rotateAmount.let { rotate += it; rotate }), .00))
		)

		arm.connectSegments()

		return arm
	}

	private fun move() {
		// ambient noise
		if (age % (4 * 20) == 0) {
			_playSound(arm.location, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 1f, 0.1f)
		}

		// follow players who are holding a stick
		for (player in Bukkit.getServer().onlinePlayers) {
			// ignore players who are out of range
			if (player.location.distance(arm.location) > 10) continue
			if (player.inventory.itemInMainHand.type == Material.CARROT_ON_A_STICK) {
				val target = player.eyeLocation
				target.add(target.direction.normalize().multiply(3))
				_followTarget(target, 5.0, 1.0)
				return
			}
		}

		// find a viable chicken target
		if (targetEntity == null) {
			for (entity in arm.location.world.entities) {
				if (_isViableTarget(entity)) {
					targetEntity = entity as Chicken
					Bukkit.getServer().scheduler.runTaskLater(plugin, Runnable { _playSound(arm.location, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1f, 0f) }, 20)
					frozenTime = 30
					return
				}
			}
		}

		// stop targeting chicken if no longer viable.
		// i.e dead or out of range
		if (!_isViableTarget(targetEntity)) {
			targetEntity = null
			targetEntityHeld = false
			frozenTime = 20
			return
		}
		val targetLocation = targetEntity!!.location.add(0.0, targetEntity!!.height, 0.0)
		if (targetEntityHeld) {
			// move towards mouth
			if (frozenTime == 0) _followTarget(mouthLocation, moveSpeed, 3.0)
			targetEntity!!.teleport(arm.tipLocation().add(0.0, -targetEntity!!.height, 0.0))
		} else {
			val distance = arm.tipLocation().distance(targetLocation)
			if (distance > .3) {
				// move towards chicken
				if (frozenTime == 0) _followTarget(targetLocation, if (chaseDistance > distance) chaseSpeed else moveSpeed, 0.0)
			} else {
				// hold chicken
				targetEntityHeld = true
				frozenTime = 10
				targetEntity!!.damage(0.1)
				_playSound(targetEntity!!.location, Sound.BLOCK_SCULK_SENSOR_BREAK, 1f, 1f)
			}
		}

		// eat chicken
		if (targetLocation.block.type == Material.PURPLE_TERRACOTTA) {
			val chicken = targetEntity

			// hacky solution for custom drops without creating a loot table file.
			chicken!!.lootTable = LootTables.EMPTY.lootTable
			chicken.location.world.dropItem(chicken.location, ItemStack(Material.FEATHER))
			chicken.damage(100.0)
			_playSound(mouthLocation, Sound.BLOCK_SCULK_SENSOR_BREAK, 3f, 0f)
			_playSound(arm.location, Sound.BLOCK_SCULK_SENSOR_HIT, 1f, 0f)
			Bukkit.getServer().scheduler.runTaskLater(plugin, Runnable { _playSound(arm.location, Sound.BLOCK_SCULK_SENSOR_HIT, 1f, 0f) }, 20)
		}
	}

	private fun _playSound(location: Location, sound: Sound, volume: Float, pitch: Float) {
		for (player in Bukkit.getServer().onlinePlayers) {
			player.playSound(location, sound, volume, pitch)
		}
	}

	private fun _followTarget(target: Location, speed: Double, lift: Double) {
		var target = target
		target = target.clone()
		val distance = arm.tipLocation().distance(target)
		if (distance > 4) target.add(0.0, lift / 4, 0.0)
		if (distance > 3) target.add(0.0, lift / 4, 0.0)
		if (distance > 2) target.add(0.0, lift / 4, 0.0)
		if (distance > 1) target.add(0.0, lift / 4, 0.0)
		val expectedChange = speed / 20
		val tip = arm.tipLocation()
		arm.followIncrementally(target, expectedChange)
		val change = arm.tipLocation().distance(tip)
		if (change > expectedChange / 2) {
			for (player in Bukkit.getServer().onlinePlayers) {
				player.playSound(arm.location, Sound.BLOCK_HONEY_BLOCK_SLIDE, change.toFloat() * 3, .5f)
			}
		}
	}

	private fun _isViableTarget(entity: Entity?): Boolean {
		return entity is Chicken && entity.isValid() && entity.getLocation().distance(arm.location) < 20
	}
}