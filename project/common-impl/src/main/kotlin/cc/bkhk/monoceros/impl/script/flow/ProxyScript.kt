package cc.bkhk.monoceros.impl.script.flow

import cc.bkhk.monoceros.Monoceros
import taboolib.common.platform.ProxyCommandSender

/**
 * 延迟解析脚本代理
 *
 * 配置中引用脚本 ID，运行时才从 ScriptHandler 解析为实际脚本并执行。
 * 支持热重载：每次执行时重新查找脚本定义。
 */
class ProxyScript(
    /** 代理的脚本定义 ID */
    val scriptId: String,
) {

    /**
     * 执行代理脚本
     *
     * @param sender 执行者
     * @param variables 变量表
     * @return 脚本执行结果
     */
    fun invoke(sender: ProxyCommandSender?, variables: Map<String, Any?> = emptyMap()): Any? {
        return Monoceros.api().scripts().invoke(scriptId, sender, variables)
    }

    /**
     * 检查代理的脚本定义是否存在
     */
    fun exists(): Boolean {
        return try {
            val registry = taboolib.common.platform.PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry>()
            registry?.get(scriptId) != null
        } catch (_: Exception) {
            false
        }
    }

    override fun toString(): String = "ProxyScript($scriptId)"
}
