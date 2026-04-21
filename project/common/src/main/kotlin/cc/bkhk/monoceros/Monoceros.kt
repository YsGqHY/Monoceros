package cc.bkhk.monoceros

import cc.bkhk.monoceros.api.MonocerosAPI

/**
 * Monoceros 全局单例入口
 *
 * 所有外部调用通过 [api] 获取服务，注册在 common-impl 的 LOAD 阶段完成。
 */
object Monoceros {

    private var api: MonocerosAPI? = null

    fun api(): MonocerosAPI =
        api ?: throw IllegalStateException("MonocerosAPI has not been registered")

    fun apiOrNull(): MonocerosAPI? = api

    fun register(api: MonocerosAPI) {
        this.api = api
    }
}
