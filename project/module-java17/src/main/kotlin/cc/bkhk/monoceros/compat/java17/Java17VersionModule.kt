package cc.bkhk.monoceros.compat.java17

import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.api.version.VersionModuleProvider
import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.impl.version.DefaultVersionAdapterResolver
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * Java 17 版本模块
 *
 * 当运行环境为 Java 17+ 时激活。
 * 提供需要 Java 17 API 的服务实现（如 sealed class 支持、record 类型处理等）。
 */
object Java17VersionModule : VersionModuleProvider {
    override val moduleId: String = "module-java17"
    override fun supports(profile: VersionProfile): Boolean = profile.javaVersion >= 17

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        val resolver = PlatformFactory.getAPIOrNull<VersionAdapterResolver>() as? DefaultVersionAdapterResolver ?: return
        resolver.register(VersionModuleProvider::class, this)
        DiagnosticLogger.info("Version", "注册 Java17 模块: $moduleId (Java ${Runtime.version().feature()})")
    }
}
