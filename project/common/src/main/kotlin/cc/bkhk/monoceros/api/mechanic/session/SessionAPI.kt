package cc.bkhk.monoceros.api.mechanic.session

import java.util.UUID

/** 玩家会话 */
interface PlayerSession {
    val playerId: UUID
    val createdAt: Long
    fun get(key: String): Any?
    fun set(key: String, value: Any?)
    fun remove(key: String): Any?
    fun has(key: String): Boolean
    fun snapshot(): Map<String, Any?>
    fun restore(snapshot: Map<String, Any?>)
    fun clear()
    fun getActiveMechanics(): Set<String>
    fun joinMechanic(mechanicId: String)
    fun leaveMechanic(mechanicId: String)
}

/** 会话服务 */
interface SessionService {
    fun getOrCreate(playerId: UUID): PlayerSession
    fun get(playerId: UUID): PlayerSession?
    fun destroy(playerId: UUID)
    fun snapshot(playerId: UUID): Map<String, Any?>?
    fun restore(playerId: UUID, snapshot: Map<String, Any?>)
    fun getPlayersInMechanic(mechanicId: String): Set<UUID>
    fun activeCount(): Int
}
