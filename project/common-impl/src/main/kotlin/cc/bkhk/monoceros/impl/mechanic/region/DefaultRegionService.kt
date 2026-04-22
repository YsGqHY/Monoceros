package cc.bkhk.monoceros.impl.mechanic.region

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.mechanic.region.*
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DefaultRegionService : RegionService {
    private val regions = ConcurrentHashMap<String, RegionDefinition>()
    private val playerRegions = ConcurrentHashMap<UUID, MutableSet<String>>()
    @Volatile private var tickTaskRunning = false

    override fun register(definition: RegionDefinition) {
        regions[definition.id] = definition
        DiagnosticLogger.debug("Region", "注册区域: ${definition.id}")
    }
    override fun unregister(id: String): RegionDefinition? = regions.remove(id)
    override fun get(id: String): RegionDefinition? = regions[id]
    override fun all(): Collection<RegionDefinition> = regions.values.toList()
    override fun getRegionsAt(location: Location): List<RegionDefinition> = regions.values.filter { it.shape.contains(location) }
    override fun isInRegion(playerId: UUID, regionId: String): Boolean = playerRegions[playerId]?.contains(regionId) == true
    override fun getPlayersInRegion(regionId: String): Set<UUID> = playerRegions.entries.filter { it.value.contains(regionId) }.map { it.key }.toSet()

    /** 启动区域检测轮询 */
    fun startTicking() {
        if (tickTaskRunning) return
        tickTaskRunning = true
        submit(period = 20L) {
            if (!tickTaskRunning) { cancel(); return@submit }
            tick()
        }
    }

    fun stopTicking() { tickTaskRunning = false }

    private fun tick() {
        for (player in Bukkit.getOnlinePlayers()) {
            val currentRegions = getRegionsAt(player.location).map { it.id }.toSet()
            val previousRegions = playerRegions.getOrPut(player.uniqueId) { ConcurrentHashMap.newKeySet() }
            val entered = currentRegions - previousRegions
            val left = previousRegions - currentRegions
            val staying = currentRegions.intersect(previousRegions)

            for (regionId in entered) {
                previousRegions.add(regionId)
                val region = regions[regionId] ?: continue
                region.onEnterScript?.let { invokeScript(it, player, region) }
                applyEffects(player, region)
            }
            for (regionId in left) {
                previousRegions.remove(regionId)
                val region = regions[regionId] ?: continue
                region.onLeaveScript?.let { invokeScript(it, player, region) }
                removeEffects(player, region)
            }
            for (regionId in staying) {
                val region = regions[regionId] ?: continue
                region.onStayScript?.let { invokeScript(it, player, region) }
                applyEffects(player, region)
            }
        }
    }

    private fun invokeScript(scriptId: String, player: Player, region: RegionDefinition) {
        try {
            val vars = HashMap(region.variables)
            vars["player"] = player
            vars["regionId"] = region.id
            Monoceros.api().scripts().invoke(scriptId, adaptPlayer(player), vars)
        } catch (e: Exception) {
            DiagnosticLogger.warn("Region", "区域脚本执行异常: $scriptId", e)
        }
    }

    private fun applyEffects(player: Player, region: RegionDefinition) {
        for (effect in region.effects) {
            val type = PotionEffectType.getByName(effect.type.uppercase()) ?: continue
            player.addPotionEffect(PotionEffect(type, 40, effect.amplifier, effect.ambient, false), true)
        }
    }

    private fun removeEffects(player: Player, region: RegionDefinition) {
        for (effect in region.effects) {
            val type = PotionEffectType.getByName(effect.type.uppercase()) ?: continue
            player.removePotionEffect(type)
        }
    }

    fun clearPlayer(playerId: UUID) { playerRegions.remove(playerId) }
}
