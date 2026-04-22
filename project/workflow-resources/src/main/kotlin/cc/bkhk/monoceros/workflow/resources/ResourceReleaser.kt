package cc.bkhk.monoceros.workflow.resources

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.platform.function.releaseResourceFolder
import java.io.File

/**
 * 默认资源释放器
 *
 * 在 ENABLE 阶段检查并释放默认配置、模板、语言文件和示例资源。
 * 仅在根文件夹不存在时通过 [releaseResourceFolder] 释放该文件夹下全部资源；
 * 若根文件夹已存在（即使为空），则跳过整组。
 * 根目录下的散落文件（如 config.yml）以文件自身是否存在为准。
 */
object ResourceReleaser {

    private const val MODULE = "Resources"

    /** 需要按文件夹释放的资源目录 */
    private val folders = listOf(
        "lang",
        "dispatcher",
        "schedule",
        "workflow",
        "script",
        "wireshark",
    )

    /** 根目录下的散落资源文件 */
    private val rootFiles = listOf(
        "config.yml",
    )

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        val dataFolder = getDataFolder()
        var released = 0
        // 按根文件夹释放：文件夹不存在时才释放
        for (folder in folders) {
            if (File(dataFolder, folder).exists()) continue
            try {
                releaseResourceFolder(folder)
                released++
                DiagnosticLogger.info(MODULE, "释放资源文件夹: $folder")
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "资源文件夹释放失败: $folder", e)
            }
        }
        // 根目录散落文件：文件不存在时才释放
        for (path in rootFiles) {
            if (File(dataFolder, path).exists()) continue
            try {
                releaseResourceFile(path)
                released++
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "资源释放失败: $path", e)
            }
        }
        if (released > 0) {
            DiagnosticLogger.info(MODULE, "资源释放完成")
        }
    }
}
