package cc.bkhk.monoceros.extension.action.item

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object ItemActionLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = ItemActionExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
