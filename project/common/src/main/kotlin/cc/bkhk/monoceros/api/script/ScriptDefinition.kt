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
)
