package cc.bkhk.monoceros.impl.version

import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * 版本服务注册器
 */
object VersionServiceLoader {

    private const val MODULE = "Version"

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        PlatformFactory.registerAPI<VersionAdapterResolver>(DefaultVersionAdapterResolver)
        DiagnosticLogger.info(MODULE, "版本适配解析器已注册到 PlatformFactory")
    }
}
