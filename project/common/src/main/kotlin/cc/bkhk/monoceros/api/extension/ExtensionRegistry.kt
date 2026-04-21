package cc.bkhk.monoceros.api.extension

/**
 * 扩展注册中心
 *
 * 管理所有已加载的扩展实例。
 */
interface ExtensionRegistry {

    /** 注册扩展 */
    fun register(extension: Extension)

    /** 注销扩展 */
    fun unregister(id: String): Extension?

    /** 获取扩展 */
    fun get(id: String): Extension?

    /** 获取所有已注册扩展 */
    fun all(): Collection<Extension>

    /** 启用所有扩展 */
    fun enableAll()

    /** 禁用所有扩展 */
    fun disableAll()
}
