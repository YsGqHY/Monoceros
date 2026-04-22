package cc.bkhk.monoceros.extension.action.entity

import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

private fun resolveLocation(context: ActionContext): Location? {
    return context.variables["location"] as? Location
        ?: (context.variables["player"] as? Player)?.location
        ?: (context.variables["entity"] as? Entity)?.location
        ?: context.variables["lastResult"] as? Location
}

class LocationBuildNode : ActionNode {
    override val type = "location.build"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val worldName = definition.config["world"] as? String
        val world = worldName?.let { Bukkit.getWorld(it) } ?: (context.variables["player"] as? Player)?.world
        val x = (definition.config["x"] as? Number)?.toDouble() ?: 0.0
        val y = (definition.config["y"] as? Number)?.toDouble() ?: 0.0
        val z = (definition.config["z"] as? Number)?.toDouble() ?: 0.0
        val yaw = (definition.config["yaw"] as? Number)?.toFloat() ?: 0f
        val pitch = (definition.config["pitch"] as? Number)?.toFloat() ?: 0f
        val loc = Location(world, x, y, z, yaw, pitch)
        context.variables["location"] = loc
        return loc
    }
}

class LocationCloneNode : ActionNode {
    override val type = "location.clone"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val cloned = loc.clone()
        context.variables["location"] = cloned
        return cloned
    }
}

class LocationModifyNode : ActionNode {
    override val type = "location.modify"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        (definition.config["x"] as? Number)?.let { loc.x = it.toDouble() }
        (definition.config["y"] as? Number)?.let { loc.y = it.toDouble() }
        (definition.config["z"] as? Number)?.let { loc.z = it.toDouble() }
        (definition.config["yaw"] as? Number)?.let { loc.yaw = it.toFloat() }
        (definition.config["pitch"] as? Number)?.let { loc.pitch = it.toFloat() }
        (definition.config["world"] as? String)?.let { loc.world = Bukkit.getWorld(it) }
        return loc
    }
}

class LocationAddNode : ActionNode {
    override val type = "location.add"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val x = (definition.config["x"] as? Number)?.toDouble() ?: 0.0
        val y = (definition.config["y"] as? Number)?.toDouble() ?: 0.0
        val z = (definition.config["z"] as? Number)?.toDouble() ?: 0.0
        return loc.clone().add(x, y, z).also { context.variables["location"] = it }
    }
}

class LocationSubtractNode : ActionNode {
    override val type = "location.subtract"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val x = (definition.config["x"] as? Number)?.toDouble() ?: 0.0
        val y = (definition.config["y"] as? Number)?.toDouble() ?: 0.0
        val z = (definition.config["z"] as? Number)?.toDouble() ?: 0.0
        return loc.clone().subtract(x, y, z).also { context.variables["location"] = it }
    }
}

class LocationMultiplyNode : ActionNode {
    override val type = "location.multiply"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val scalar = (definition.config["scalar"] as? Number)?.toDouble() ?: 1.0
        return Location(loc.world, loc.x * scalar, loc.y * scalar, loc.z * scalar, loc.yaw, loc.pitch)
            .also { context.variables["location"] = it }
    }
}

class LocationDivideNode : ActionNode {
    override val type = "location.divide"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val scalar = (definition.config["scalar"] as? Number)?.toDouble() ?: 1.0
        if (scalar == 0.0) return loc
        return Location(loc.world, loc.x / scalar, loc.y / scalar, loc.z / scalar, loc.yaw, loc.pitch)
            .also { context.variables["location"] = it }
    }
}

class LocationDistanceNode : ActionNode {
    override val type = "location.distance"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val other = context.variables["other"] as? Location ?: return null
        return loc.distance(other)
    }
}

class LocationDistanceSquaredNode : ActionNode {
    override val type = "location.distanceSquared"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val loc = resolveLocation(context) ?: return null
        val other = context.variables["other"] as? Location ?: return null
        return loc.distanceSquared(other)
    }
}
