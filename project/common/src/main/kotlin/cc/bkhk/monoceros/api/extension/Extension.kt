package cc.bkhk.monoceros.api.extension

/**
 * 扩展接口
 *
 * 所有扩展模块（内建或外部 JAR）实现此接口。
 * 扩展在 ENABLE 阶段加载，DISABLE 阶段卸载。
 */
interface Extension {

    /** 扩展唯一 ID */
    val id: String

    /** 扩展名称 */
    val name: String

    /** 扩展版本 */
    val version: String

    /** 加载扩展 */
    fun onEnable()

    /** 卸载扩展 */
    fun onDisable()
}

/**
 * 内建扩展
 *
 * 随 Monoceros 一起编译的扩展，无需额外 JAR 加载。
 */
abstract class NativeExtension : Extension {
    override fun onDisable() {}
}

/**
 * 外部扩展
 *
 * 从外部 JAR 文件动态加载的扩展。
 */
abstract class ExternalExtension : Extension {

    /** 扩展 JAR 文件路径 */
    abstract val jarPath: String

    override fun onDisable() {}
}
