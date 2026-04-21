package cc.bkhk.monoceros.extension.action.memory

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.extension.NativeExtension
import cc.bkhk.monoceros.api.workflow.ActionContext
import cc.bkhk.monoceros.api.workflow.ActionNode
import cc.bkhk.monoceros.api.workflow.ActionNodeDefinition
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap

/** 记忆作用域 */
enum class MemoryScope {
    GLOBAL, PLAYER, ENTITY, WORKFLOW, SCRIPT, SESSION
}

/** 记忆条目 */
data class MemoryEntry(val value: Any?, val expireAt: Long? = null) {
    fun isExpired(): Boolean = expireAt != null && System.currentTimeMillis() > expireAt
}

/** 全局记忆存储 */
object MemoryStore {
    private val stores = EnumMap<MemoryScope, ConcurrentHashMap<String, MemoryEntry>>(MemoryScope::class.java)

    init { MemoryScope.values().forEach { stores[it] = ConcurrentHashMap() } }

    fun get(scope: MemoryScope, owner: Any?, key: String): Any? {
        val store = stores[scope] ?: return null
        val compositeKey = compositeKey(owner, key)
        val entry = store[compositeKey] ?: return null
        if (entry.isExpired()) { store.remove(compositeKey, entry); return null }
        return entry.value
    }

    fun set(scope: MemoryScope, owner: Any?, key: String, value: Any?, ttlMs: Long? = null) {
        val store = stores[scope] ?: return
        val expireAt = if (ttlMs != null && ttlMs > 0) System.currentTimeMillis() + ttlMs else null
        store[compositeKey(owner, key)] = MemoryEntry(value, expireAt)
    }

    fun remove(scope: MemoryScope, owner: Any?, key: String): Any? {
        val store = stores[scope] ?: return null
        return store.remove(compositeKey(owner, key))?.value
    }

    fun clear(scope: MemoryScope, owner: Any?): Int {
        val store = stores[scope] ?: return 0
        if (owner == null) { val size = store.size; store.clear(); return size }
        val prefix = ownerKey(owner) + ":"
        val keys = store.keys.filter { it.startsWith(prefix) }
        keys.forEach { store.remove(it) }
        return keys.size
    }

    private fun compositeKey(owner: Any?, key: String): String {
        return if (owner != null) "${ownerKey(owner)}:$key" else key
    }

    private fun ownerKey(owner: Any?): String = when (owner) {
        is Player -> owner.uniqueId.toString()
        is Entity -> owner.uniqueId.toString()
        is String -> owner
        else -> owner?.toString() ?: "null"
    }
}

/** 解析作用域 */
private fun parseScope(config: Map<String, Any?>): MemoryScope {
    val name = (config["scope"] as? String)?.uppercase() ?: "GLOBAL"
    return try { MemoryScope.valueOf(name) } catch (_: Exception) { MemoryScope.GLOBAL }
}

/** 解析 owner */
private fun resolveOwner(context: ActionContext, scope: MemoryScope): Any? = when (scope) {
    MemoryScope.PLAYER -> context.variables["player"]
    MemoryScope.ENTITY -> context.variables["entity"] ?: context.variables["target"]
    MemoryScope.WORKFLOW -> context.variables["workflowId"]
    MemoryScope.SCRIPT -> context.variables["scriptId"] ?: context.variables["taskId"]
    MemoryScope.SESSION -> context.variables["sessionId"]
    MemoryScope.GLOBAL -> null
}

class MemorySetNode : ActionNode {
    override val type = "memory.set"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val scope = parseScope(definition.config)
        val key = definition.config["key"] as? String ?: return null
        val value = definition.config["value"]
        val ttl = parseTtl(definition.config["ttl"])
        MemoryStore.set(scope, resolveOwner(context, scope), key, value, ttl)
        return value
    }
}

class MemoryGetNode : ActionNode {
    override val type = "memory.get"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val scope = parseScope(definition.config)
        val key = definition.config["key"] as? String ?: return null
        return MemoryStore.get(scope, resolveOwner(context, scope), key)
    }
}

class MemoryRemoveNode : ActionNode {
    override val type = "memory.remove"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val scope = parseScope(definition.config)
        val key = definition.config["key"] as? String ?: return null
        return MemoryStore.remove(scope, resolveOwner(context, scope), key)
    }
}

class MemoryClearNode : ActionNode {
    override val type = "memory.clear"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val scope = parseScope(definition.config)
        return MemoryStore.clear(scope, resolveOwner(context, scope))
    }
}

class MemoryExpireNode : ActionNode {
    override val type = "memory.expire"
    override fun execute(context: ActionContext, definition: ActionNodeDefinition): Any? {
        val scope = parseScope(definition.config)
        val key = definition.config["key"] as? String ?: return null
        val ttl = parseTtl(definition.config["ttl"]) ?: return null
        val owner = resolveOwner(context, scope)
        val current = MemoryStore.get(scope, owner, key)
        MemoryStore.set(scope, owner, key, current, ttl)
        return ttl
    }
}

/** 解析 TTL 值，支持 3000ms / 3s / 纯数字(ms) */
private fun parseTtl(value: Any?): Long? {
    val str = value?.toString()?.trim()?.lowercase() ?: return null
    return when {
        str.endsWith("ms") -> str.dropLast(2).toLongOrNull()
        str.endsWith("s") -> (str.dropLast(1).toLongOrNull() ?: return null) * 1000
        else -> str.toLongOrNull()
    }
}

class MemoryActionExtension : NativeExtension() {
    override val id = "action-memory"
    override val name = "记忆域动作扩展"
    override val version = "1.0.0"

    override fun onEnable() {
        val service = Monoceros.api().actionWorkflow()
        service.registerNode(MemorySetNode())
        service.registerNode(MemoryGetNode())
        service.registerNode(MemoryRemoveNode())
        service.registerNode(MemoryClearNode())
        service.registerNode(MemoryExpireNode())
    }
}
