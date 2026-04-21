package cc.bkhk.monoceros.extension.action.event

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object EventActionLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = EventActionExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
