package cc.bkhk.monoceros.api.script

import java.nio.file.Path

/**
 * 脚本定义加载器
 *
 * 负责从文件系统扫描、解析脚本定义并注册到 [ScriptDefinitionRegistry]。
 */
interface ScriptDefinitionLoader {

    /** 全量加载 script/ 目录下所有脚本 */
    fun loadAll(): ScriptLoadResult

    /** 单文件重载 */
    fun reload(path: Path): ScriptLoadResult

    /** 移除指定文件关联的所有脚本定义 */
    fun remove(path: Path)

    /** 按 ID 前缀查找脚本定义 */
    fun findByPrefix(prefix: String): Collection<ScriptDefinition>
}
