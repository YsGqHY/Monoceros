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
        else -> null
    }

    override fun write(target: Entity, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "glowing" -> target.isGlowing = value as? Boolean ?: error("Entity.glowing 需要 Boolean 类型")
            "silent" -> target.isSilent = value as? Boolean ?: error("Entity.silent 需要 Boolean 类型")
            "customName" -> target.customName = value?.toString()
            "velocity" -> if (value is org.bukkit.util.Vector) target.velocity = value else error("Entity.velocity 需要 Vector 类型")
            else -> error("Entity 属性 '$key' 不可写")
        }
    }
}

/** LivingEntity 属性访问器 */
object LivingEntityPropertyAccessor : PropertyAccessor<LivingEntity> {
    override val targetType: KClass<LivingEntity> = LivingEntity::class

    override fun read(target: LivingEntity, key: String, context: Map<String, Any?>): Any? = when (key) {
        "health" -> target.health
        "maxHealth" -> target.maxHealth
        "equipment" -> target.equipment
        "eyeLocation" -> target.eyeLocation
        "potionEffects" -> target.activePotionEffects
        "noDamageTicks" -> target.noDamageTicks
        else -> EntityPropertyAccessor.read(target, key, context)
    }

    override fun write(target: LivingEntity, key: String, value: Any?, context: Map<String, Any?>) {
        when (key) {
            "health" -> target.health = ((value as? Number) ?: error("LivingEntity.health 需要 Number 类型")).toDouble().coerceIn(0.0, target.maxHealth)
            "noDamageTicks" -> target.noDamageTicks = ((value as? Number) ?: error("LivingEntity.noDamageTicks 需要 Number 类型")).toInt()
            else -> EntityPropertyAccessor.write(target, key, value, context)
        }
    }
}

/** Player 属性访问器 */
object PlayerPropertyAccessor : PropertyAccessor<Player> {
    override val targetType: KClass<Player> = Player::class

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
        service.register(LivingEntityPropertyAccessor)
        service.register(PlayerPropertyAccessor)
    }
}
