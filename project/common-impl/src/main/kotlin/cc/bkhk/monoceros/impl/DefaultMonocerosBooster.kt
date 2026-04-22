package cc.bkhk.monoceros.impl

import cc.bkhk.monoceros.impl.version.DefaultVersionAdapterResolver
import taboolib.common.platform.function.console
import taboolib.module.chat.colored

/**
 * Monoceros 引导器
 *
 * 负责异常安全的启动流程、版本与模块诊断信息输出。
 * 由 module-bukkit 在 INIT 阶段调用。
 */
object DefaultMonocerosBooster {

    /**
     * 渐变色段映射表（从左到右）
     *
     * 控制台只支持基础 16 色（§0~§f），RGB 格式（§x...）不会被渲染。
     * 因此统一使用传统颜色码做分段渐变，模拟 logo 的浅青 → 蓝紫过渡：
     * &b（aqua）→ &b → &3（dark_aqua）→ &3 → &9（blue）→ &1（dark_blue）
     */
    private val gradientSegments = listOf("&b", "&b", "&3", "&3", "&9", "&1")

    /** ASCII Art 行 */
    private val bannerLines = arrayOf(
        "  __  __                                          ",
        " |  \\/  | ___  _ __   ___   ___ ___ _ __ ___  ___ ",
        " | |\\/| |/ _ \\| '_ \\ / _ \\ / __/ _ \\ '__/ _ \\/ __|",
        " | |  | | (_) | | | | (_) | (_|  __/ | | (_) \\__ \\",
        " |_|  |_|\\___/|_| |_|\\___/ \\___\\___|_|  \\___/|___/",
    )

    /**
     * 启动引导
     *
     * 若启动失败，异常向上传播由调用方（MonocerosPlugin）处理。
     * 颜色渐变参考 Monoceros logo 配色（浅青 → 蓝紫）。
     */
    fun startup() {
        val profile = DefaultVersionAdapterResolver.currentProfile()
        val c = console()
        c.sendMessage("")
        for (line in bannerLines) {
            c.sendMessage(gradientLine(line))
        }
        c.sendMessage("")
        c.sendMessage("&7 Monoceros is initializing... &8(${profile.profileId})".colored())
        c.sendMessage("")
    }

    /**
     * 对单行文本做水平分段渐变上色
     *
     * 将每行按字符位置等分为 [gradientSegments].size 段，
     * 每段使用对应的传统颜色码前缀，颜色相同时不重复插入。
     */
    private fun gradientLine(line: String): String {
        if (line.isBlank()) return line
        val segCount = gradientSegments.size
        val segLen = (line.length + segCount - 1) / segCount
        val sb = StringBuilder(line.length * 3)
        var lastColor = ""
        for ((i, ch) in line.withIndex()) {
            val seg = (i / segLen).coerceAtMost(segCount - 1)
            val color = gradientSegments[seg]
            if (color != lastColor) {
                sb.append(color)
                lastColor = color
            }
            sb.append(ch)
        }
        return sb.toString().colored()
    }
}
