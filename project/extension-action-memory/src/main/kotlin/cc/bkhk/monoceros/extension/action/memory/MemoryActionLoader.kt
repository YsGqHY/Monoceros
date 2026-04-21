package cc.bkhk.monoceros.extension.action.memory

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object MemoryActionLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = MemoryActionExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
