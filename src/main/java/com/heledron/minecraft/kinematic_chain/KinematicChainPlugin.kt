package com.heledron.minecraft.kinematic_chain

import org.bukkit.plugin.java.JavaPlugin
import com.heledron.minecraft.kinematic_chain.Tendril
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import java.lang.Runnable
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class KinematicChainPlugin : JavaPlugin() {
	override fun onEnable() {
		val world = server.getWorld("world")
		val location = Location(world, 222.0, 51.0, -14.0)
		val mouthLocation = Location(world, 220.0, 51.0, -13.0).add(.5, .9, .5)
		var tendril = Tendril(location, mouthLocation, this)
		server.scheduler.runTaskTimer(this, fun () { tendril.update() }, 0, 1)
		getCommand("sculkTendril")!!.setExecutor(fun (_, _, _, args): Boolean {
			if (args.size != 1) return false;

			when (args[0]) {
				"reset" -> {
					tendril = Tendril(location, mouthLocation, this)
					return true
				}
			}
			return false
		})
	}

	override fun onDisable() {}
}