package cc.bkhk.monoceros.impl.extension.scanner

import cc.bkhk.monoceros.api.extension.Extension
import cc.bkhk.monoceros.api.extension.ExternalExtension
import cc.bkhk.monoceros.api.extension.ExtensionRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.getDataFolder
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.jar.JarFile

/**
 * 外部扩展包扫描器
 *
 * 在 ENABLE 阶段扫描 `plugins/Monoceros/extension/` 目录下的 JAR 文件，
 * 处理更新、归档，并加载扩展到 [ExtensionRegistry]。
 */
object ExtensionScanner {

    private const val MODULE = "ExtensionScanner"

    /** 扩展目录 */
    private val extensionDir by lazy { File(getDataFolder(), "extension") }

    /** 更新目录 */
    private val updaterDir by lazy { File(extensionDir, "updater") }

    /** 归档目录 */
    private val archiveDir by lazy { File(extensionDir, "archive") }

    /** 已加载的外部扩展 ClassLoader */
    private val loadedClassLoaders = mutableListOf<URLClassLoader>()

    @Awake(LifeCycle.ENABLE)
    fun scan() {
        ensureDirectories()
        processUpdates()
        loadExtensions()
    }

    @Awake(LifeCycle.DISABLE)
    fun cleanup() {
        // 关闭所有外部扩展的 ClassLoader
        loadedClassLoaders.forEach { loader ->
            try {
                loader.close()
            } catch (_: Exception) {
            }
        }
        loadedClassLoaders.clear()
    }

    /** 确保目录存在 */
    private fun ensureDirectories() {
        extensionDir.mkdirs()
        updaterDir.mkdirs()
        archiveDir.mkdirs()
    }

    /**
     * 处理更新：updater/ 目录中的新版本替换旧版本
     *
     * 流程：
     * 1. 扫描 updater/ 目录下的 JAR 文件
     * 2. 解析为 Artifact
     * 3. 在 extension/ 目录中查找同名旧版本
     * 4. 如果新版本更高，将旧版本移入 archive/，新版本移入 extension/
     */
    private fun processUpdates() {
        val updateFiles = updaterDir.listFiles { f -> f.isFile && f.extension == "jar" } ?: return
        if (updateFiles.isEmpty()) return

        val existingArtifacts = scanArtifacts(extensionDir)

        for (updateFile in updateFiles) {
            val newArtifact = Artifact.parse(updateFile.name, updateFile.absolutePath)
            if (newArtifact == null) {
                DiagnosticLogger.warn(MODULE, "无法解析更新文件: ${updateFile.name}")
                continue
            }

            val existing = existingArtifacts[newArtifact.name]
            if (existing != null && newArtifact > existing) {
                // 归档旧版本
                val oldFile = File(existing.filePath)
                val archiveFile = File(archiveDir, oldFile.name)
                try {
                    oldFile.renameTo(archiveFile)
                    DiagnosticLogger.info(MODULE, "归档旧版本: ${oldFile.name} -> archive/")
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "归档失败: ${oldFile.name}", e)
                    continue
                }
            }

            // 移入扩展目录
            val targetFile = File(extensionDir, updateFile.name)
            try {
                updateFile.renameTo(targetFile)
                DiagnosticLogger.info(MODULE, "更新扩展: ${updateFile.name}")
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "更新失败: ${updateFile.name}", e)
            }
        }
    }

    /** 扫描目录下的 JAR 文件并解析为 Artifact 映射（name -> Artifact） */
    private fun scanArtifacts(dir: File): Map<String, Artifact> {
        val result = mutableMapOf<String, Artifact>()
        val files = dir.listFiles { f -> f.isFile && f.extension == "jar" } ?: return result
        for (file in files) {
            val artifact = Artifact.parse(file.name, file.absolutePath)
            if (artifact != null) {
                val existing = result[artifact.name]
                if (existing == null || artifact > existing) {
                    result[artifact.name] = artifact
                }
            }
        }
        return result
    }

    /** 加载扩展目录下的所有 JAR */
    private fun loadExtensions() {
        val jarFiles = extensionDir.listFiles { f -> f.isFile && f.extension == "jar" } ?: return
        if (jarFiles.isEmpty()) return

        val registry = PlatformFactory.getAPIOrNull<ExtensionRegistry>()
        if (registry == null) {
            DiagnosticLogger.warn(MODULE, "ExtensionRegistry 不可用，跳过外部扩展加载")
            return
        }

        var loaded = 0
        for (jarFile in jarFiles) {
            try {
                val extensions = loadJar(jarFile)
                for (ext in extensions) {
                    registry.register(ext)
                    loaded++
                }
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "扩展加载失败: ${jarFile.name}", e)
            }
        }

        if (loaded > 0) {
            DiagnosticLogger.info(MODULE, "外部扩展加载完成: $loaded 个")
        }
    }

    /**
     * 加载单个 JAR 文件中的扩展
     *
     * 通过 ServiceLoader 机制发现 Extension 实现类。
     * JAR 中需要包含 META-INF/services/cc.bkhk.monoceros.api.extension.Extension 文件。
     */
    private fun loadJar(file: File): List<Extension> {
        val url = file.toURI().toURL()
        val classLoader = URLClassLoader(arrayOf(url), javaClass.classLoader)
        loadedClassLoaders.add(classLoader)

        val extensions = mutableListOf<Extension>()

        // 方式 1：ServiceLoader 发现
        try {
            val serviceLoader = ServiceLoader.load(Extension::class.java, classLoader)
            for (ext in serviceLoader) {
                extensions.add(ext)
                DiagnosticLogger.info(MODULE, "发现扩展 (ServiceLoader): ${ext.id} v${ext.version}")
            }
        } catch (_: Exception) {
            // ServiceLoader 未配置，尝试其他方式
        }

        // 方式 2：扫描 JAR 中实现 Extension 接口的类
        if (extensions.isEmpty()) {
            try {
                val jar = JarFile(file)
                jar.entries().asSequence()
                    .filter { it.name.endsWith(".class") && !it.name.contains('$') }
                    .forEach { entry ->
                        val className = entry.name.replace('/', '.').removeSuffix(".class")
                        try {
                            val clazz = classLoader.loadClass(className)
                            if (Extension::class.java.isAssignableFrom(clazz) && !clazz.isInterface && !java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) {
                                val ext = clazz.getDeclaredConstructor().newInstance() as Extension
                                extensions.add(ext)
                                DiagnosticLogger.info(MODULE, "发现扩展 (类扫描): ${ext.id} v${ext.version}")
                            }
                        } catch (_: Exception) {
                            // 忽略无法加载的类
                        }
                    }
                jar.close()
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "JAR 类扫描失败: ${file.name}", e)
            }
        }

        return extensions
    }
}
