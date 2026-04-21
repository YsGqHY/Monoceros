package cc.bkhk.monoceros.extension.property.common

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object CommonPropertyLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = CommonPropertyExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
