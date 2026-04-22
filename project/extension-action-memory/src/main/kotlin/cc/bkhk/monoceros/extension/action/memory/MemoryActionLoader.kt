package cc.bkhk.monoceros.extension.action.memory

import cc.bkhk.monoceros.api.extension.ExtensionRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

object MemoryActionLoader {
    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        val registry = PlatformFactory.getAPIOrNull<ExtensionRegistry>()
        if (registry != null) {
            registry.register(MemoryActionExtension())
        } else {
            val ext = MemoryActionExtension()
            ext.onEnable()
            DiagnosticLogger.warn("Extension", "ExtensionRegistry 不可用，直接启用: ${ext.name}")
        }
    }
}
