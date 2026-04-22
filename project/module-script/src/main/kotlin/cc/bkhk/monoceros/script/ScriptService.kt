package cc.bkhk.monoceros.script

import cc.bkhk.monoceros.api.script.MonocerosScriptSource
import cc.bkhk.monoceros.api.script.ScriptDefinition
import cc.bkhk.monoceros.api.script.ScriptDefinitionLoader
import cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry
import cc.bkhk.monoceros.api.script.ScriptLoadResult
import cc.bkhk.monoceros.impl.config.ConfigService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * 脚本资源加载服务
 *
 * 基于 ConfigService 扫描 script/ 目录，解析 .fs 和 .yml/.yaml 文件为脚本定义。
 * 维护三张索引表实现文件到脚本 ID 的双向映射。
 * 采用"最后一次成功版本保留"策略做错误隔离。
 */
class ScriptService(
    private val registry: ScriptDefinitionRegistry,
) : ConfigService("script", setOf("fs", "yml", "yaml")), ScriptDefinitionLoader {

    companion object {
        private const val MODULE = "ScriptService"
    }

    /** 文件路径 -> 该文件关联的脚本 ID 集合 */
    private val fileToIds = ConcurrentHashMap<String, MutableSet<String>>()

    /** 脚本 ID -> 文件路径 */
    private val idToFile = ConcurrentHashMap<String, String>()

    override fun loadAll(): ScriptLoadResult {
        val startMs = System.currentTimeMillis()
        var loaded = 0
        var failed = 0

        val dir = directory()
        if (!dir.exists()) {
            dir.mkdirs()
            return ScriptLoadResult(0, 0, 0, System.currentTimeMillis() - startMs)
        }

        // 收集新的定义，先在临时集合中校验冲突
        val newDefinitions = mutableMapOf<String, ScriptDefinition>()
        val newFileToIds = mutableMapOf<String, MutableSet<String>>()
        val newIdToFile = mutableMapOf<String, String>()

        collectScriptFiles(dir).forEach { file ->
            try {
                val definitions = parseFile(file, dir)
                for (def in definitions) {
                    if (newDefinitions.containsKey(def.id)) {
                        DiagnosticLogger.warn(MODULE, "脚本 ID 冲突: ${def.id}，来源: ${file.path}")
                        failed++
                        continue
                    }
                    newDefinitions[def.id] = def
                    val filePath = file.relativeTo(dir).path.replace('\\', '/')
                    newFileToIds.getOrPut(filePath) { mutableSetOf() }.add(def.id)
                    newIdToFile[def.id] = filePath
                    loaded++
                }
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "脚本文件解析失败: ${file.path}", e)
                failed++
            }
        }

        // 原子替换注册表
        val oldIds = registry.keys()
        // removed 应为旧注册表中存在但新加载中不再存在的脚本数量
        val removedCount = (oldIds - newDefinitions.keys).size

        registry.clear()
        fileToIds.clear()
        idToFile.clear()

        newDefinitions.forEach { (id, def) -> registry.register(id, def) }
        newFileToIds.forEach { (path, ids) -> fileToIds[path] = ids.toMutableSet() }
        newIdToFile.forEach { (id, path) -> idToFile[id] = path }

        val costMs = System.currentTimeMillis() - startMs
        DiagnosticLogger.summary(MODULE, loaded, failed)
        return ScriptLoadResult(loaded, failed, removedCount, costMs)
    }

    override fun reload(path: Path): ScriptLoadResult {
        val startMs = System.currentTimeMillis()
        val dir = directory()
        val file = path.toFile()
        val relativePath = file.relativeTo(dir).path.replace('\\', '/')

        // 获取旧映射
        val oldIds = fileToIds[relativePath] ?: emptySet()
        var loaded = 0
        var failed = 0

        if (!file.exists()) {
            // 文件已删除
            remove(path)
            return ScriptLoadResult(0, 0, oldIds.size, System.currentTimeMillis() - startMs)
        }

        try {
            val definitions = parseFile(file, dir)
            val newIds = mutableSetOf<String>()

            for (def in definitions) {
                // 检查 ID 冲突（排除自身旧 ID）
                val existingFile = idToFile[def.id]
                if (existingFile != null && existingFile != relativePath) {
                    DiagnosticLogger.warn(MODULE, "脚本 ID 冲突: ${def.id}，已存在于: $existingFile")
                    failed++
                    continue
                }
                registry.register(def.id, def)
                newIds.add(def.id)
                idToFile[def.id] = relativePath
                loaded++
            }

            // 移除旧文件中不再存在的 ID
            val removedIds = oldIds - newIds
            removedIds.forEach { id ->
                registry.unregister(id)
                idToFile.remove(id)
            }

            fileToIds[relativePath] = newIds.toMutableSet()

            val costMs = System.currentTimeMillis() - startMs
            return ScriptLoadResult(loaded, failed, removedIds.size, costMs)
        } catch (e: Exception) {
            // 解析失败，保留旧定义
            DiagnosticLogger.warn(MODULE, "脚本文件重载失败，保留旧定义: ${file.path}", e)
            return ScriptLoadResult(0, 1, 0, System.currentTimeMillis() - startMs)
        }
    }

    override fun remove(path: Path) {
        val dir = directory()
        val relativePath = path.toFile().relativeTo(dir).path.replace('\\', '/')
        val ids = fileToIds.remove(relativePath) ?: return
        ids.forEach { id ->
            registry.unregister(id)
            idToFile.remove(id)
        }
        DiagnosticLogger.info(MODULE, "移除脚本文件: $relativePath (${ids.size} 个定义)")
    }

    override fun findByPrefix(prefix: String): Collection<ScriptDefinition> {
        return registry.keys()
            .filter { it.startsWith(prefix) }
            .mapNotNull { registry.get(it) }
    }

    /** 收集目录下所有脚本文件 */
    private fun collectScriptFiles(dir: File): List<File> {
        return dir.walkTopDown()
            .filter { it.isFile && !it.name.startsWith("#") && it.extension in setOf("fs", "yml", "yaml") }
            .toList()
    }

    /**
     * 解析单个脚本文件
     *
     * .fs 文件：纯脚本，ID 由路径推导
     * .yml/.yaml 文件：带元数据定义
     */
    private fun parseFile(file: File, baseDir: File): List<ScriptDefinition> {
        return when (file.extension) {
            "fs" -> listOf(parseFsFile(file, baseDir))
            "yml", "yaml" -> parseYamlFile(file, baseDir)
            else -> emptyList()
        }
    }

    /** 解析纯脚本文件 */
    private fun parseFsFile(file: File, baseDir: File): ScriptDefinition {
        val relativePath = file.relativeTo(baseDir).path.replace('\\', '/')
        val id = relativePath.substringBeforeLast('.').replace('/', '.')
        val content = file.readText(Charsets.UTF_8)
        return ScriptDefinition(
            id = id,
            source = MonocerosScriptSource(content = content),
            file = file.toPath(),
        )
    }

    /** 解析 YAML 定义文件 */
    private fun parseYamlFile(file: File, baseDir: File): List<ScriptDefinition> {
        val config = Configuration.loadFromFile(file)
        val relativePath = file.relativeTo(baseDir).path.replace('\\', '/')
        val defaultId = relativePath.substringBeforeLast('.').replace('/', '.')

        // 单脚本定义（顶层包含 script 字段）
        if (config.contains("script")) {
            return listOf(parseYamlSection(config, defaultId, file))
        }

        // 多脚本定义（每个顶层 key 是一个脚本）
        val definitions = mutableListOf<ScriptDefinition>()
        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue
            if (!section.contains("script")) continue
            val sectionId = section.getString("id") ?: "$defaultId.$key"
            definitions.add(parseYamlSection(section, sectionId, file))
        }
        return definitions
    }

    /** 从 YAML 配置节解析单个脚本定义 */
    private fun parseYamlSection(section: ConfigurationSection, defaultId: String, file: File): ScriptDefinition {
        val id = section.getString("id") ?: defaultId
        val type = section.getString("type") ?: MonocerosScriptSource.DEFAULT_TYPE
        val content = section.getString("script") ?: ""
        val enabled = section.getBoolean("enabled", true)
        val preheat = section.getBoolean("preheat", false)
        val async = section.getBoolean("async", true)
        val tags = section.getStringList("tags").toSet()

        val metadata = mutableMapOf<String, Any?>()
        val metaSection = section.getConfigurationSection("metadata")
        if (metaSection != null) {
            for (key in metaSection.getKeys(false)) {
                metadata[key] = metaSection.get(key)
            }
        }

        // 高级脚本定义封装
        val parameters = mutableMapOf<String, String>()
        section.getConfigurationSection("parameters")?.let { params ->
            for (key in params.getKeys(false)) {
                params.getString(key)?.let { parameters[key] = it }
            }
        }
        val condition = section.getString("condition")
        val deny = section.getString("deny")
        val functions = mutableMapOf<String, String>()
        section.getConfigurationSection("functions")?.let { funcs ->
            for (key in funcs.getKeys(false)) {
                funcs.getString(key)?.let { functions[key] = it }
            }
        }
        val timeoutMs = section.getLong("timeout", 0)
        val onTimeout = section.getString("on-timeout")
        val onException = section.getString("on-exception")
        val returnConversion = section.getString("return-conversion")

        return ScriptDefinition(
            id = id,
            source = MonocerosScriptSource(type = type, content = content, origin = file.path),
            file = file.toPath(),
            enabled = enabled,
            preheat = preheat,
            asyncAllowed = async,
            tags = tags,
            metadata = metadata,
            parameters = parameters,
            condition = condition,
            deny = deny,
            functions = functions,
            timeoutMs = timeoutMs,
            onTimeout = onTimeout,
            onException = onException,
            returnConversion = returnConversion,
        )
    }
}
