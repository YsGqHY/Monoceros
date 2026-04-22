package cc.bkhk.monoceros.extension.action.target

import cc.bkhk.monoceros.api.extension.ExtensionRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 目标域扩展加载器
 */
object TargetActionLoader {
    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        val registry = PlatformFactory.getAPIOrNull<ExtensionRegistry>()
        if (registry != null) {
            registry.register(TargetActionExtension())
        } else {
            val ext = TargetActionExtension()
            ext.onEnable()
            DiagnosticLogger.warn("Extension", "ExtensionRegistry 不可用，直接启用: ${ext.name}")
        }
    }
}
