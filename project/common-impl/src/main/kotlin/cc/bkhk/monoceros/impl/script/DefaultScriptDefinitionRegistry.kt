package cc.bkhk.monoceros.impl.script

import cc.bkhk.monoceros.api.script.ScriptDefinition
import cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry
import java.util.concurrent.ConcurrentHashMap

/**
 * 脚本定义注册表默认实现
 *
 * 基于 ConcurrentHashMap，线程安全。
 */
class DefaultScriptDefinitionRegistry : ScriptDefinitionRegistry {

    private val map = ConcurrentHashMap<String, ScriptDefinition>()

    override fun register(id: String, value: ScriptDefinition): ScriptDefinition {
        map[id] = value
        return value
    }

    override fun unregister(id: String): ScriptDefinition? {
        return map.remove(id)
    }

    override fun get(id: String): ScriptDefinition? {
        return map[id]
    }

    override fun contains(id: String): Boolean {
        return map.containsKey(id)
    }

    override fun keys(): Set<String> {
        return map.keys.toSet()
    }

    override fun values(): Collection<ScriptDefinition> {
        return map.values.toList()
    }

    override fun clear() {
        map.clear()
    }
}
