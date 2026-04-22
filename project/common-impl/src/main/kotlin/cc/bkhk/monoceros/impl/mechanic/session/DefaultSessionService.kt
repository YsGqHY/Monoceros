package cc.bkhk.monoceros.impl.mechanic.session

import cc.bkhk.monoceros.api.mechanic.session.*
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.SubscribeEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DefaultPlayerSession(override val playerId: UUID) : PlayerSession {
    override val createdAt: Long = System.currentTimeMillis()
    private val data = ConcurrentHashMap<String, Any?>()
    private val mechanics = ConcurrentHashMap.newKeySet<String>()

    override fun get(key: String): Any? = data[key]
    override fun set(key: String, value: Any?) { data[key] = value }
    override fun remove(key: String): Any? = data.remove(key)
    override fun has(key: String): Boolean = data.containsKey(key)
    override fun snapshot(): Map<String, Any?> = HashMap(data)
    override fun restore(snapshot: Map<String, Any?>) { data.clear(); data.putAll(snapshot) }
    override fun clear() { data.clear(); mechanics.clear() }
    override fun getActiveMechanics(): Set<String> = mechanics.toSet()
    override fun joinMechanic(mechanicId: String) { mechanics.add(mechanicId) }
    override fun leaveMechanic(mechanicId: String) { mechanics.remove(mechanicId) }
}

class DefaultSessionService : SessionService {
    internal val sessions = ConcurrentHashMap<UUID, DefaultPlayerSession>()

    override fun getOrCreate(playerId: UUID): PlayerSession = sessions.computeIfAbsent(playerId) { DefaultPlayerSession(it) }
    override fun get(playerId: UUID): PlayerSession? = sessions[playerId]
    override fun destroy(playerId: UUID) { sessions.remove(playerId)?.clear() }
    override fun snapshot(playerId: UUID): Map<String, Any?>? = sessions[playerId]?.snapshot()
    override fun restore(playerId: UUID, snapshot: Map<String, Any?>) { getOrCreate(playerId).restore(snapshot) }
    override fun getPlayersInMechanic(mechanicId: String): Set<UUID> =
        sessions.entries.filter { it.value.getActiveMechanics().contains(mechanicId) }.map { it.key }.toSet()
    override fun activeCount(): Int = sessions.size
}

object SessionServiceLoader {
    private lateinit var service: DefaultSessionService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultSessionService()
        PlatformFactory.registerAPI<SessionService>(service)
        DiagnosticLogger.info("Session", "会话机制服务已注册")
    }
    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        service.sessions.keys.toList().forEach { service.destroy(it) }
    }

    @SubscribeEvent
    fun onQuit(event: PlayerQuitEvent) { service.destroy(event.player.uniqueId) }
}
