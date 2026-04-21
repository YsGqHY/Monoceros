package cc.bkhk.monoceros.extension.action.target

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * 目标域扩展加载器
 */
object TargetActionLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = TargetActionExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
