package cc.bkhk.monoceros.extension.action.entity

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object EntityActionLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = EntityActionExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
