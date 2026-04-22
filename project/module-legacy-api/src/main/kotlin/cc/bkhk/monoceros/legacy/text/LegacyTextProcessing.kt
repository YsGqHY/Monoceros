package cc.bkhk.monoceros.legacy.text

import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.service.TextProcessingService

/**
 * 旧版文本处理实现
 *
 * 使用 & 颜色代码转换为 § 格式。
 */
object LegacyTextProcessing : TextProcessingService {

    private val COLOR_PATTERN = Regex("&([0-9a-fk-orA-FK-OR])")
    private val STRIP_PATTERN = Regex("[§&][0-9a-fk-orA-FK-OR]")

    override fun supports(profile: VersionProfile): Boolean = profile.legacyMode

    override fun colorize(text: String): String {
        return COLOR_PATTERN.replace(text, "\u00a7$1")
    }

    override fun stripColor(text: String): String {
        return STRIP_PATTERN.replace(text, "")
    }
}
