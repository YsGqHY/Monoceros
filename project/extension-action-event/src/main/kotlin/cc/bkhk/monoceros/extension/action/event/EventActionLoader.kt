package cc.bkhk.monoceros.extension.action.event

import cc.bkhk.monoceros.api.extension.ExtensionRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

object EventActionLoader {
    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        val registry = PlatformFactory.getAPIOrNull<ExtensionRegistry>()
        if (registry != null) {
            registry.register(EventActionExtension())
        } else {
            val ext = EventActionExtension()
            ext.onEnable()
            DiagnosticLogger.warn("Extension", "ExtensionRegistry 不可用，直接启用: ${ext.name}")
        }
    }
}
