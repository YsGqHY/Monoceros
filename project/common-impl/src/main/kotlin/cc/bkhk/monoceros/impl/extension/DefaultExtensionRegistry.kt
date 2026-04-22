package cc.bkhk.monoceros.impl.extension

import cc.bkhk.monoceros.api.extension.Extension
import cc.bkhk.monoceros.api.extension.ExtensionRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * 扩展注册中心默认实现
 */
class DefaultExtensionRegistry : ExtensionRegistry {

    private companion object {
        const val MODULE = "Extension"
    }

    private val extensions = ConcurrentHashMap<String, Extension>()

    override fun register(extension: Extension) {
        extensions[extension.id] = extension
        try {
            extension.onEnable()
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "扩展启用失败: ${extension.id}", e)
        }
        DiagnosticLogger.info(MODULE, "注册扩展: ${extension.id} (${extension.name} v${extension.version})")
    }

    override fun unregister(id: String): Extension? {
        val ext = extensions.remove(id)
        if (ext != null) {
            try {
                ext.onDisable()
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "扩展卸载异常: $id", e)
            }
            DiagnosticLogger.info(MODULE, "注销扩展: $id")
        }
        return ext
    }

    override fun get(id: String): Extension? = extensions[id]

    override fun all(): Collection<Extension> = extensions.values.toList()

    override fun enableAll() {
        for (ext in extensions.values) {
            try {
                ext.onEnable()
                DiagnosticLogger.info(MODULE, "启用扩展: ${ext.id}")
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "扩展启用失败: ${ext.id}", e)
            }
        }
    }

    override fun disableAll() {
        for (ext in extensions.values) {
            try {
                ext.onDisable()
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "扩展禁用异常: ${ext.id}", e)
            }
        }
        DiagnosticLogger.info(MODULE, "已禁用全部扩展")
    }
}
