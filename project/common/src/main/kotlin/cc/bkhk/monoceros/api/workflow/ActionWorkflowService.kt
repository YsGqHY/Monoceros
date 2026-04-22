package cc.bkhk.monoceros.api.workflow

import taboolib.common.platform.ProxyCommandSender

/**
 * 动作工作流定义
 */
data class ActionWorkflowDefinition(
    val id: String,
    val nodes: List<ActionNodeDefinition>,
    val failurePolicy: ActionFailurePolicy = ActionFailurePolicy.STOP,
)

/**
 * 动作节点定义
 */
data class ActionNodeDefinition(
    val id: String,
    val type: String,
    val config: Map<String, Any?> = emptyMap(),
)

/**
 * 动作失败策略
 */
enum class ActionFailurePolicy {
    STOP,
    CONTINUE,
    SKIP_NODE,
}

/**
 * 动作运行时上下文
 */
data class ActionContext(
    val workflowId: String,
    val sender: ProxyCommandSender?,
    val variables: MutableMap<String, Any?> = LinkedHashMap(),
    val trace: MutableList<String> = mutableListOf(),
)

/**
 * 动作节点执行结果
 *
 * 节点可以返回普通值（被包装为 Continue），也可以返回特殊控制流标记。
 */
sealed class ActionResult {
    /** 继续执行后续节点，携带结果值 */
    data class Continue(val value: Any?) : ActionResult()

    /** 延迟指定 tick 后继续执行后续节点 */
    data class Delay(val ticks: Long) : ActionResult()

    /** 条件分支：accepted=true 继续执行，accepted=false 执行 elseWorkflow 或中断 */
    data class Branch(val accepted: Boolean, val thenWorkflow: String? = null, val elseWorkflow: String? = null) : ActionResult()

    /** 中断当前工作流 */
    object Break : ActionResult()
}

/**
 * 动作节点接口
 */
interface ActionNode {
    val type: String
    fun execute(context: ActionContext, definition: ActionNodeDefinition): Any?
}

/**
 * 动作工作流服务
 */
interface ActionWorkflowService {
    fun registerNode(node: ActionNode): ActionNode
    fun unregisterNode(type: String): ActionNode?
    fun execute(id: String, sender: ProxyCommandSender?, variables: Map<String, Any?> = emptyMap()): Any?
    fun reloadAll(): Int
}
