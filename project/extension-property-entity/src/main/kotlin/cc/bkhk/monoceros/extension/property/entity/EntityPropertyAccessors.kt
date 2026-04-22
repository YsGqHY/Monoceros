package cc.bkhk.monoceros.extension.property.entity

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.PropertyAccessor
import org.bukkit.entity.Damageable
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.reflect.KClass

/** Entity 属性访问器 */
object EntityPropertyAccessor : PropertyAccessor<Entity> {
    override val targetType: KClass<Entity> = Entity::class

    @Suppress("DEPRECATION")
    override fun read(target: Entity, key: String, context: Map<String, Any?>): Any? = when (key) {
        "uuid" -> target.uniqueId
        "type" -> target.type
        "world" -> target.world
        "location" -> target.location
        "velocity" -> target.velocity
        "passengers" -> target.passengers
        "vehicle" -> target.vehicle
        "glowing" -> target.isGlowing
        "silent" -> target.isSilent
        "customName" -> target.customName
        "name" -> target.name
        "height" -> target.height
        "width" -> target.width
        "boundingBox" -> try { target.boundingBox } catch (_: Exception) { null }
        "freezeTicks" -> try { target.freezeTicks } catch (_: Exception) { 0 }
        "portalCooldown" -> target.portalCooldown
        "facing" -> target.facing.name
        "pose" -> try { target.pose.name } catch (_: Exception) { null }
        "entityId" -> target.entityId
        "ticksLived" -> target.ticksLived
        "isOnGround" -> target.isOnGround
        "isInWater" -> try { target.isInWater } catch (_: Exception) { null }
        "passenger" -> target.passenger
        else -> null
    }

    override fun write(target: Entity, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "glowing" -> target.isGlowing = value as? Boolean ?: error("Entity.glowing 需要 Boolean 类型")
            "silent" -> target.isSilent = value as? Boolean ?: error("Entity.silent 需要 Boolean 类型")
            "customName" -> target.customName = value?.toString()
            "velocity" -> if (value is org.bukkit.util.Vector) target.velocity = value else error("Entity.velocity 需要 Vector 类型")
            "portalCooldown" -> target.portalCooldown = ((value as? Number) ?: error("Entity.portalCooldown 需要 Number 类型")).toInt().coerceAtLeast(0)
            "ticksLived" -> target.ticksLived = ((value as? Number) ?: error("Entity.ticksLived 需要 Number 类型")).toInt().coerceAtLeast(1)
            "isOnGround" -> try { target.isOnGround } catch (_: Exception) { /* 旧版本不支持写入 isOnGround */ }
            else -> error("Entity 属性 '$key' 不可写")
        }
    }
}

/** Damageable 属性访问器 */
object DamageablePropertyAccessor : PropertyAccessor<Damageable> {
    override val targetType: KClass<Damageable> = Damageable::class

    @Suppress("DEPRECATION")
    override fun read(target: Damageable, key: String, context: Map<String, Any?>): Any? = when (key) {
        "health" -> target.health
        "maxHealth" -> target.maxHealth
        "absorptionAmount" -> try { (target as? LivingEntity)?.absorptionAmount } catch (_: Exception) { null }
        else -> null
    }

    @Suppress("DEPRECATION")
    override fun write(target: Damageable, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "health" -> target.health = ((value as? Number) ?: error("Damageable.health 需要 Number 类型")).toDouble().coerceIn(0.0, target.maxHealth)
            "maxHealth" -> target.maxHealth = ((value as? Number) ?: error("Damageable.maxHealth 需要 Number 类型")).toDouble().coerceAtLeast(1.0)
            "absorptionAmount" -> {
                val living = target as? LivingEntity ?: error("absorptionAmount 仅适用于 LivingEntity")
                living.absorptionAmount = ((value as? Number) ?: error("Damageable.absorptionAmount 需要 Number 类型")).toDouble().coerceAtLeast(0.0)
            }
            else -> error("Damageable 属性 '$key' 不可写")
        }
    }
}

/** LivingEntity 属性访问器 */
object LivingEntityPropertyAccessor : PropertyAccessor<LivingEntity> {
    override val targetType: KClass<LivingEntity> = LivingEntity::class

    override fun read(target: LivingEntity, key: String, context: Map<String, Any?>): Any? = when (key) {
        "equipment" -> target.equipment
        "eyeLocation" -> target.eyeLocation
        "potionEffects" -> target.activePotionEffects
        "noDamageTicks" -> target.noDamageTicks
        "remainingAir" -> target.remainingAir
        "maximumAir" -> target.maximumAir
        "arrowsInBody" -> try { target.arrowsInBody } catch (_: Exception) { 0 }
        "canPickupItems" -> target.canPickupItems
        "isGliding" -> try { target.isGliding } catch (_: Exception) { false }
        "isSwimming" -> try { target.isSwimming } catch (_: Exception) { false }
        "isInvisible" -> try { target.isInvisible } catch (_: Exception) { false }
        "hasAI" -> try { target.hasAI() } catch (_: Exception) { true }
        "isCollidable" -> try { target.isCollidable } catch (_: Exception) { true }
        else -> DamageablePropertyAccessor.read(target, key, context) ?: EntityPropertyAccessor.read(target, key, context)
    }

    override fun write(target: LivingEntity, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "health", "maxHealth", "absorptionAmount" -> DamageablePropertyAccessor.write(target, key, value, context)
            "noDamageTicks" -> target.noDamageTicks = ((value as? Number) ?: error("LivingEntity.noDamageTicks 需要 Number 类型")).toInt().coerceAtLeast(0)
            "remainingAir" -> target.remainingAir = ((value as? Number) ?: error("LivingEntity.remainingAir 需要 Number 类型")).toInt()
            "canPickupItems" -> target.canPickupItems = value as? Boolean ?: error("LivingEntity.canPickupItems 需要 Boolean 类型")
            "isGliding" -> try { target.isGliding = value as? Boolean ?: error("LivingEntity.isGliding 需要 Boolean 类型") } catch (_: Exception) { /* 旧版本不支持 */ }
            "isSwimming" -> try { target.isSwimming = value as? Boolean ?: error("LivingEntity.isSwimming 需要 Boolean 类型") } catch (_: Exception) { /* 旧版本不支持 */ }
            "isInvisible" -> try { target.isInvisible = value as? Boolean ?: error("LivingEntity.isInvisible 需要 Boolean 类型") } catch (_: Exception) { /* 旧版本不支持 */ }
            "hasAI" -> try { target.setAI(value as? Boolean ?: error("LivingEntity.hasAI 需要 Boolean 类型")) } catch (_: Exception) { /* 旧版本不支持 */ }
            "isCollidable" -> try { target.isCollidable = value as? Boolean ?: error("LivingEntity.isCollidable 需要 Boolean 类型") } catch (_: Exception) { /* 旧版本不支持 */ }
            else -> EntityPropertyAccessor.write(target, key, value, context)
        }
    }
}

/** Player 属性访问器 */
object PlayerPropertyAccessor : PropertyAccessor<Player> {
    override val targetType: KClass<Player> = Player::class

    @Suppress("DEPRECATION")
    override fun read(target: Player, key: String, context: Map<String, Any?>): Any? = when (key) {
        "name" -> target.name
        "displayName" -> target.displayName
        "gameMode" -> target.gameMode
        "foodLevel" -> target.foodLevel
        "exp" -> target.exp
        "level" -> target.level
        "inventory" -> target.inventory
        "isSneaking" -> target.isSneaking
        "isSprinting" -> target.isSprinting
        "bedSpawnLocation" -> target.bedSpawnLocation
        "isFlying" -> target.isFlying
        "isOp" -> target.isOp
        "ping" -> try { target.ping } catch (_: Exception) { -1 }
        "locale" -> try { target.locale } catch (_: Exception) { null }
        "compassTarget" -> target.compassTarget
        "respawnLocation" -> try { target.bedSpawnLocation } catch (_: Exception) { null }
        "healthScale" -> try { target.healthScale } catch (_: Exception) { 20.0 }
        "isHealthScaled" -> try { target.isHealthScaled } catch (_: Exception) { false }
        "walkSpeed" -> target.walkSpeed
        "flySpeed" -> target.flySpeed
        "allowFlight" -> target.allowFlight
        "totalExperience" -> target.totalExperience
        "saturation" -> target.saturation
        else -> LivingEntityPropertyAccessor.read(target, key, context)
    }

    override fun write(target: Player, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "displayName" -> target.setDisplayName(value?.toString())
            "gameMode" -> target.gameMode = if (value is org.bukkit.GameMode) value
                else org.bukkit.GameMode.valueOf((value ?: error("Player.gameMode 不能为 null")).toString().uppercase())
            "foodLevel" -> target.foodLevel = ((value as? Number) ?: error("Player.foodLevel 需要 Number 类型")).toInt().coerceIn(0, 20)
            "exp" -> target.exp = ((value as? Number) ?: error("Player.exp 需要 Number 类型")).toFloat().coerceIn(0f, 1f)
            "level" -> target.level = ((value as? Number) ?: error("Player.level 需要 Number 类型")).toInt().coerceAtLeast(0)
            "compassTarget" -> if (value is org.bukkit.Location) target.compassTarget = value else error("Player.compassTarget 需要 Location 类型")
            "walkSpeed" -> target.walkSpeed = ((value as? Number) ?: error("Player.walkSpeed 需要 Number 类型")).toFloat().coerceIn(-1f, 1f)
            "flySpeed" -> target.flySpeed = ((value as? Number) ?: error("Player.flySpeed 需要 Number 类型")).toFloat().coerceIn(-1f, 1f)
            "allowFlight" -> target.allowFlight = value as? Boolean ?: error("Player.allowFlight 需要 Boolean 类型")
            "totalExperience" -> target.totalExperience = ((value as? Number) ?: error("Player.totalExperience 需要 Number 类型")).toInt().coerceAtLeast(0)
            "saturation" -> target.saturation = ((value as? Number) ?: error("Player.saturation 需要 Number 类型")).toFloat().coerceIn(0f, 20f)
            "healthScale" -> try { target.healthScale = ((value as? Number) ?: error("Player.healthScale 需要 Number 类型")).toDouble().coerceAtLeast(0.0) } catch (_: Exception) { /* 旧版本不支持 */ }
            else -> LivingEntityPropertyAccessor.write(target, key, value, context)
        }
    }
}

class EntityPropertyExtension : NativeExtension() {
    override val id = "property-entity"
    override val name = "实体属性域扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().propertyWorkflow()
        service.register(EntityPropertyAccessor)
        service.register(DamageablePropertyAccessor)
        service.register(LivingEntityPropertyAccessor)
        service.register(PlayerPropertyAccessor)
    }
}
