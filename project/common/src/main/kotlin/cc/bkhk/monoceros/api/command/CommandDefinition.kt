package cc.bkhk.monoceros.api.command

/**
 * 命令定义
 */
data class CommandDefinition(
    val id: String,
    val aliases: List<String>,
    val permission: String? = null,
    val permissionMessage: String? = null,
    val root: CommandNode,
)

/**
 * 命令节点
 */
sealed interface CommandNode {
    val name: String
    val children: List<CommandNode>
}

/**
 * 字面量节点
 */
data class LiteralNode(
    override val name: String,
    val route: CommandRoute? = null,
    override val children: List<CommandNode> = emptyList(),
) : CommandNode

/**
 * 参数节点
 */
data class ArgumentNode(
    override val name: String,
    val argument: ArgumentSpec,
    val route: CommandRoute? = null,
    override val children: List<CommandNode> = emptyList(),
) : CommandNode

/**
 * 参数规格
 */
data class ArgumentSpec(
    val type: ArgumentType,
    val required: Boolean = true,
    val suggest: String? = null,
    val restrict: RestrictionSpec? = null,
)

/**
 * 参数类型
 */
enum class ArgumentType {
    STRING,
    INT,
    DOUBLE,
    BOOLEAN,
    PLAYER,
    OFFLINE_PLAYER,
    WORLD,
    MATERIAL,
    SCRIPT_ID,
}

/**
 * 参数限制规格
 */
data class RestrictionSpec(
    val mode: String,
    val value: String,
)
