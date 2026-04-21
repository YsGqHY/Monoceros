package cc.bkhk.monoceros.api.registry

/**
 * 泛型注册表接口
 *
 * 所有注册中心（脚本类型、分发器、调度任务、命令、动作、属性）共用同一接口。
 * 实现层使用线程安全容器，但接口层不暴露并发细节。
 */
interface Registry<T> {

    fun register(id: String, value: T): T

    fun unregister(id: String): T?

    fun get(id: String): T?

    fun contains(id: String): Boolean

    fun keys(): Set<String>

    fun values(): Collection<T>

    fun clear()
}
