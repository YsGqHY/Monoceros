package cc.bkhk.monoceros

import cc.bkhk.monoceros.api.MonocerosAPI
import org.bukkit.plugin.Plugin

/**
 * Monoceros 全局单例入口
 *
 * 所有外部调用通过 [api] 获取服务，注册在 common-impl 的 LOAD 阶段完成。
 */
object Monoceros {

    private var api: MonocerosAPI? = null

    @JvmStatic
    fun api(): MonocerosAPI =
        api ?: throw IllegalStateException("MonocerosAPI has not been registered")

    @JvmStatic
    fun apiOrNull(): MonocerosAPI? = api

    /** 获取 Bukkit 插件实例，供脚本中传递给 Bukkit Scheduler 等 API */
    @JvmStatic
    fun plugin(): Plugin =
        org.bukkit.Bukkit.getPluginManager().getPlugin("Monoceros")
            ?: throw IllegalStateException("Monoceros plugin is not loaded")

    @JvmStatic
    fun register(api: MonocerosAPI) {
        this.api = api
    }
}
