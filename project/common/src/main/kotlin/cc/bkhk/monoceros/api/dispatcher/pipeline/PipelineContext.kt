package cc.bkhk.monoceros.api.dispatcher.pipeline

import org.bukkit.entity.Player
import org.bukkit.event.Event
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console

/**
 * Pipeline 运行时上下文
 *
 * 在事件分发过程中，Pipeline 链依次处理此上下文，
 * 提取主体、注入变量、执行过滤和后置处理。
 */
class PipelineContext(
    /** 原始 Bukkit 事件 */
    val event: Event,
) {
    /** 事件主体（Player/Entity/Block 等），由 Pipeline 在 initPrincipal 阶段设置 */
    var principal: Any? = null

    /**
     * 主体唯一标识
     *
     * 格式：`PLAYER@uuid` / `ENTITY@uuid` / `BLOCK@world,x,y,z`
     * 用于冷却/节流等按主体隔离的场景。
     */
    var principalId: String = ""

    /** 从 principal 提取的玩家引用 */
    var player: Player? = null

    /** 事件是否被取消 */
    var isCancelled: Boolean = false

    /** 事件是否被过滤（不执行后续脚本/工作流） */
    var isFiltered: Boolean = false

    /** 事件是否被冷却/节流拦截 */
    var isFilterBaffled: Boolean = false

    /** 脚本/工作流执行结果 */
    var result: Any? = null

    /** 变量表 */
    val variables: MutableMap<String, Any?> = LinkedHashMap()

    /** 获取 ProxyCommandSender */
    fun sender(): ProxyCommandSender {
        return player?.let { adaptPlayer(it) } ?: console()
    }
}
