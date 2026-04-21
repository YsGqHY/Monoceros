package cc.bkhk.monoceros.extension.property.entity

import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

object EntityPropertyLoader {
    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        val ext = EntityPropertyExtension()
        ext.onEnable()
        DiagnosticLogger.info("Extension", "已加载: ${ext.name}")
    }
}
