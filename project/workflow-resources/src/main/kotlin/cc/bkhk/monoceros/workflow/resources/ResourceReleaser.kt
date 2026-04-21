package cc.bkhk.monoceros.workflow.resources

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import java.io.File

/**
 * 默认资源释放器
 *
 * 在 ENABLE 阶段检查并释放默认配置、模板、语言文件和示例资源。
 * 仅在目标文件不存在时释放，不覆盖用户已修改的文件。
 */
object ResourceReleaser {

    private const val MODULE = "Resources"

    /** 需要释放的资源文件列表 */
    private val resources = listOf(
        "config.yml",
        "lang/zh_CN.yml",
        "lang/en_US.yml",
        "dispatcher/player-join.yml",
        "schedule/broadcast.yml",
        "workflow/action/combat-hit.yml",
        "script/shared/audit-before.fs",
        "script/dispatcher/player-join.fs",
        "script/command/debug-run.fs",
    )

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        var released = 0
        for (path in resources) {
            val target = File(getDataFolder(), path)
            if (!target.exists()) {
                try {
                    releaseResourceFile(path)
                    released++
                } catch (e: Exception) {
                    DiagnosticLogger.warn(MODULE, "资源释放失败: $path", e)
                }
            }
        }
        if (released > 0) {
            DiagnosticLogger.info(MODULE, "释放默认资源: $released 个")
        }
    }
}
