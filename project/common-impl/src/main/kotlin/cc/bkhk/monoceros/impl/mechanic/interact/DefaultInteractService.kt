package cc.bkhk.monoceros.impl.mechanic.interact

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.mechanic.combat.CooldownManager
import cc.bkhk.monoceros.api.mechanic.interact.*
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.adaptPlayer
import java.util.concurrent.ConcurrentHashMap

class DefaultInteractService : InteractService {
    internal val definitions = ConcurrentHashMap<String, InteractDefinition>()

    override fun register(definition: InteractDefinition) {
        definitions[definition.id] = definition
        DiagnosticLogger.info("Interact", "注册交互: ${definition.id} (${definition.type})")
    }
    override fun unregister(id: String): InteractDefinition? = definitions.remove(id)
    override fun get(id: String): InteractDefinition? = definitions[id]
    override fun all(): Collection<InteractDefinition> = definitions.values.toList()

    override fun getLookAtTarget(player: Player, maxDistance: Double): LookAtResult? {
        // 使用射线追踪查找视线目标
        return try {
            val result = player.world.rayTraceEntities(
                player.eyeLocation,
                player.eyeLocation.direction,
                maxDistance,
            ) { it != player && it is LivingEntity }
            if (result?.hitEntity != null) {
                LookAtResult(result.hitEntity, result.hitPosition.distance(player.eyeLocation.toVector()))
            } else null
        } catch (_: Exception) {
            // rayTraceEntities 在旧版本不可用，降级到 getLineOfSight
            try {
                val blocks = (player as LivingEntity).getLineOfSight(null, maxDistance.toInt())
                for (block in blocks) {
                    val nearby = block.location.world?.getNearbyEntities(block.location, 1.0, 1.0, 1.0)
                        ?.filter { it != player && it is LivingEntity }
                        ?.minByOrNull { it.location.distanceSquared(player.eyeLocation) }
                    if (nearby != null) {
                        return LookAtResult(nearby, nearby.location.distance(player.eyeLocation))
                    }
                }
                null
            } catch (_: Exception) { null }
        }
    }

    internal fun handleInteract(player: Player, type: InteractType, materialName: String?) {
        val cooldownManager = PlatformFactory.getAPIOrNull<CooldownManager>()
        for (def in definitions.values) {
            if (def.type != type) continue
            if (def.materialFilter != null && !def.materialFilter.equals(materialName, ignoreCase = true)) continue
            if (def.cooldownMs > 0 && cooldownManager != null) {
                if (cooldownManager.hasCooldown(player.uniqueId, "interact:${def.id}")) continue
                cooldownManager.setCooldown(player.uniqueId, "interact:${def.id}", def.cooldownMs)
            }
            try {
                val vars = HashMap(def.variables)
                vars["player"] = player
                vars["interactType"] = type.name
                Monoceros.api().scripts().invoke(def.script, adaptPlayer(player), vars)
            } catch (e: Exception) {
                DiagnosticLogger.warn("Interact", "交互脚本执行异常: ${def.id}", e)
            }
        }
    }
}

object InteractServiceLoader {
    private lateinit var service: DefaultInteractService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultInteractService()
        PlatformFactory.registerAPI<InteractService>(service)
        DiagnosticLogger.info("Interact", "交互机制服务已注册")
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        val type = when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
                if (event.player.isSneaking) InteractType.SNEAK_RIGHT_CLICK else InteractType.RIGHT_CLICK
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
                if (event.player.isSneaking) InteractType.SNEAK_LEFT_CLICK else InteractType.LEFT_CLICK
            else -> return
        }
        val material = event.item?.type?.name
        service.handleInteract(event.player, type, material)
    }

    @SubscribeEvent
    fun onSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            service.handleInteract(event.player, InteractType.SNEAK_TOGGLE, null)
        }
    }
}
