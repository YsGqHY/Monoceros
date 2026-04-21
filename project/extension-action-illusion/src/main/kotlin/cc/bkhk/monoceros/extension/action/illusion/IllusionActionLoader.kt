package cc.bkhk.monoceros.extension.action.illusion

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object IllusionActionLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = IllusionActionExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
