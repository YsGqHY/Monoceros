package cc.bkhk.monoceros.modern

import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.api.version.VersionModuleProvider
import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.service.ItemMetaBridgeService
import cc.bkhk.monoceros.api.version.service.MaterialMappingService
import cc.bkhk.monoceros.api.version.service.TextProcessingService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import cc.bkhk.monoceros.impl.version.DefaultVersionAdapterResolver
import cc.bkhk.monoceros.modern.component.ModernItemMetaBridge
import org.bukkit.Material
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

object ModernVersionModule : VersionModuleProvider {
    override val moduleId: String = "module-modern"
    override fun supports(profile: VersionProfile): Boolean = profile.modernMode

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        val resolver = PlatformFactory.getAPIOrNull<VersionAdapterResolver>() as? DefaultVersionAdapterResolver ?: return
        resolver.register(VersionModuleProvider::class, this)

        // 注册现代版本服务实现
        resolver.register(ItemMetaBridgeService::class, ModernItemMetaBridge)
        resolver.register(MaterialMappingService::class, ModernMaterialMapping)
        resolver.register(TextProcessingService::class, ModernTextProcessing)

        DiagnosticLogger.info("Version", "注册现代版本模块: $moduleId (ItemMetaBridge, MaterialMapping, TextProcessing)")
    }
}

/** 现代版本 Material 映射（直接使用 Bukkit API） */
object ModernMaterialMapping : MaterialMappingService {
    override fun supports(profile: VersionProfile): Boolean = profile.modernMode
    override fun matchMaterial(name: String): Material? = Material.matchMaterial(name)
    override fun canonicalName(material: Material): String = material.name
}

/** 现代版本文本处理（支持 & 和 § 颜色代码） */
object ModernTextProcessing : TextProcessingService {
    private val COLOR_PATTERN = Regex("&([0-9a-fk-orA-FK-OR])")
    private val STRIP_PATTERN = Regex("[§&][0-9a-fk-orA-FK-OR]")

    override fun supports(profile: VersionProfile): Boolean = profile.modernMode
    override fun colorize(text: String): String = COLOR_PATTERN.replace(text, "\u00a7$1")
    override fun stripColor(text: String): String = STRIP_PATTERN.replace(text, "")
}
