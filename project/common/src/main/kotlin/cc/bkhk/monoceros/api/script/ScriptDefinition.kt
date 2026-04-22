package cc.bkhk.monoceros.api.script

import java.nio.file.Path

/**
 * 脚本定义
 *
 * 描述一个脚本资源的静态元数据，由资源加载层解析生成。
 */
data class ScriptDefinition(
    /** 脚本唯一 ID，由文件路径或 YAML 显式声明推导 */
    val id: String,
    /** 脚本来源（类型 + 内容） */
    val source: MonocerosScriptSource,
    /** 脚本文件路径 */
    val file: Path,
    /** 是否启用 */
    val enabled: Boolean = true,
    /** 是否在加载后预热 */
    val preheat: Boolean = false,
    /** 是否允许异步执行 */
    val asyncAllowed: Boolean = true,
    /** 标签集合，用于分类与筛选 */
    val tags: Set<String> = emptySet(),
    /** 附加元数据 */
    val metadata: Map<String, Any?> = emptyMap(),

    // region 高级脚本定义封装

    /** 参数声明：参数名 -> 类型名（对应 Applicative 注册名） */
    val parameters: Map<String, String> = emptyMap(),
    /** 前置条件脚本 ID，返回 false 时拒绝执行 */
    val condition: String? = null,
    /** 条件不满足时执行的脚本 ID */
    val deny: String? = null,
    /** 自定义函数映射：函数名 -> 脚本 ID */
    val functions: Map<String, String> = emptyMap(),
    /** 执行超时（毫秒），0 或负数表示不限制 */
    val timeoutMs: Long = 0,
    /** 超时时执行的脚本 ID */
    val onTimeout: String? = null,
    /** 异常时执行的脚本 ID */
    val onException: String? = null,
    /** 返回值类型转换（对应 Applicative 注册名），null 表示不转换 */
    val returnConversion: String? = null,

    // endregion
)
