package cc.bkhk.monoceros.impl.script.relocate

import org.objectweb.asm.commons.Remapper

/**
 * ASM 包名重映射器
 *
 * 将 relocate 后的包名（before）转回原始包名（after），
 * 用于外部 FluxonPlugin 存在时的类转译。
 */
class RelocateTranslation(
    private val before: String,
    private val after: String,
    private val classToRename: String? = null
) : Remapper() {

    override fun map(internalName: String): String {
        // 如果是要重命名的类本身，在后面加 T
        if (classToRename != null && internalName == classToRename) {
            return "${internalName}T"
        }
        return applyRule(internalName, before, after)
    }

    /**
     * 应用单条重定向规则
     *
     * @param name 类名或包名
     * @param from 重定向前
     * @param to   重定向后
     * @return 重定向后的名称
     */
    private fun applyRule(name: String, from: String, to: String): String {
        // 完整匹配
        if (name == from) {
            return to
        }
        // 前缀匹配
        if (name.startsWith("$from/")) {
            return to + name.substring(from.length)
        }
        return name
    }
}
