package cc.bkhk.monoceros.extension.action.entity

import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.util.Vector

private fun resolveVector(context: ActionContext, key: String = "vector"): Vector? {
    return context.variables[key] as? Vector
        ?: context.variables["lastResult"] as? Vector
}

private fun parseVector(config: Map<String, Any?>, prefix: String = ""): Vector? {
    val x = (config["${prefix}x"] as? Number)?.toDouble() ?: return null
    val y = (config["${prefix}y"] as? Number)?.toDouble() ?: return null
    val z = (config["${prefix}z"] as? Number)?.toDouble() ?: return null
    return Vector(x, y, z)
}

class VectorBuildNode : ActionNode {
    override val type = "vector.build"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val x = (definition.config["x"] as? Number)?.toDouble() ?: 0.0
        val y = (definition.config["y"] as? Number)?.toDouble() ?: 0.0
        val z = (definition.config["z"] as? Number)?.toDouble() ?: 0.0
        val vec = Vector(x, y, z)
        context.variables["vector"] = vec
        return vec
    }
}

class VectorCloneNode : ActionNode {
    override val type = "vector.clone"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val cloned = vec.clone()
        context.variables["vector"] = cloned
        return cloned
    }
}

class VectorModifyNode : ActionNode {
    override val type = "vector.modify"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        (definition.config["x"] as? Number)?.let { vec.x = it.toDouble() }
        (definition.config["y"] as? Number)?.let { vec.y = it.toDouble() }
        (definition.config["z"] as? Number)?.let { vec.z = it.toDouble() }
        return vec
    }
}

// Arithmetic
class VectorAddNode : ActionNode {
    override val type = "vector.add"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return vec
        return vec.clone().add(other).also { context.variables["vector"] = it }
    }
}

class VectorSubtractNode : ActionNode {
    override val type = "vector.subtract"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return vec
        return vec.clone().subtract(other).also { context.variables["vector"] = it }
    }
}

class VectorMultiplyNode : ActionNode {
    override val type = "vector.multiply"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val scalar = (definition.config["scalar"] as? Number)?.toDouble()
        return if (scalar != null) {
            vec.clone().multiply(scalar).also { context.variables["vector"] = it }
        } else {
            val other = parseVector(definition.config) ?: return vec
            Vector(vec.x * other.x, vec.y * other.y, vec.z * other.z).also { context.variables["vector"] = it }
        }
    }
}

class VectorDivideNode : ActionNode {
    override val type = "vector.divide"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val scalar = (definition.config["scalar"] as? Number)?.toDouble() ?: 1.0
        if (scalar == 0.0) return vec
        return Vector(vec.x / scalar, vec.y / scalar, vec.z / scalar).also { context.variables["vector"] = it }
    }
}

// Geometry
class VectorNormalizeNode : ActionNode {
    override val type = "vector.normalize"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        return vec.clone().normalize().also { context.variables["vector"] = it }
    }
}

class VectorLengthNode : ActionNode {
    override val type = "vector.length"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        return resolveVector(context)?.length()
    }
}

class VectorLengthSquaredNode : ActionNode {
    override val type = "vector.lengthSquared"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        return resolveVector(context)?.lengthSquared()
    }
}

// Relations
class VectorDotNode : ActionNode {
    override val type = "vector.dot"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return null
        return vec.dot(other)
    }
}

class VectorCrossNode : ActionNode {
    override val type = "vector.cross"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return null
        return vec.clone().crossProduct(other).also { context.variables["vector"] = it }
    }
}

class VectorAngleNode : ActionNode {
    override val type = "vector.angle"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return null
        return vec.angle(other)
    }
}

class VectorDistanceNode : ActionNode {
    override val type = "vector.distance"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return null
        return vec.distance(other)
    }
}

class VectorDistanceSquaredNode : ActionNode {
    override val type = "vector.distanceSquared"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return null
        return vec.distanceSquared(other)
    }
}

class VectorMidpointNode : ActionNode {
    override val type = "vector.midpoint"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val other = parseVector(definition.config) ?: (context.variables["other"] as? Vector) ?: return null
        return vec.clone().midpoint(other).also { context.variables["vector"] = it }
    }
}

// Rotation
class VectorRotateXNode : ActionNode {
    override val type = "vector.rotateX"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val angle = (definition.config["angle"] as? Number)?.toDouble() ?: return null
        return vec.clone().rotateAroundX(angle).also { context.variables["vector"] = it }
    }
}

class VectorRotateYNode : ActionNode {
    override val type = "vector.rotateY"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val angle = (definition.config["angle"] as? Number)?.toDouble() ?: return null
        return vec.clone().rotateAroundY(angle).also { context.variables["vector"] = it }
    }
}

class VectorRotateZNode : ActionNode {
    override val type = "vector.rotateZ"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = resolveVector(context) ?: return null
        val angle = (definition.config["angle"] as? Number)?.toDouble() ?: return null
        return vec.clone().rotateAroundZ(angle).also { context.variables["vector"] = it }
    }
}

class VectorRandomNode : ActionNode {
    override val type = "vector.random"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val vec = Vector.getRandom()
        context.variables["vector"] = vec
        return vec
    }
}
