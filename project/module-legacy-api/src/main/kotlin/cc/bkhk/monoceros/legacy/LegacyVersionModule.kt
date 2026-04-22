package cc.bkhk.monoceros.legacy

import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.api.version.VersionModuleProvider
import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.service.MaterialMappingService
import cc.bkhk.monoceros.api.version.service.TextProcessingService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.impl.version.DefaultVersionAdapterResolver
import cc.bkhk.monoceros.legacy.material.LegacyMaterialMapping
import cc.bkhk.monoceros.legacy.text.LegacyTextProcessing
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

object LegacyVersionModule : VersionModuleProvider {
    override val moduleId: String = "module-legacy-api"
    override fun supports(profile: VersionProfile): Boolean = profile.legacyMode

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        val resolver = PlatformFactory.getAPIOrNull<VersionAdapterResolver>() as? DefaultVersionAdapterResolver ?: return
        resolver.register(VersionModuleProvider::class, this)

        // 注册旧版服务实现
        resolver.register(MaterialMappingService::class, LegacyMaterialMapping)
        resolver.register(TextProcessingService::class, LegacyTextProcessing)

        DiagnosticLogger.info("Version", "注册旧版兼容模块: $moduleId (MaterialMapping, TextProcessing)")
    }
}
