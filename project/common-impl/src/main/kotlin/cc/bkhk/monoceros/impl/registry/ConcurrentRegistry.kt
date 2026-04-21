package cc.bkhk.monoceros.impl.registry

import cc.bkhk.monoceros.api.registry.Registry
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 ConcurrentHashMap 的泛型注册表默认实现
 *
 * 线程安全，支持热重载时的并发读写。
 */
class ConcurrentRegistry<T> : Registry<T> {

    private val map = ConcurrentHashMap<String, T>()

    override fun register(id: String, value: T): T {
        map[id] = value
        return value
    }

    override fun unregister(id: String): T? {
        return map.remove(id)
    }

    override fun get(id: String): T? {
        return map[id]
    }

    override fun contains(id: String): Boolean {
        return map.containsKey(id)
    }

    override fun keys(): Set<String> {
        return map.keys.toSet()
    }

    override fun values(): Collection<T> {
        return map.values.toList()
    }

    override fun clear() {
        map.clear()
    }
}
