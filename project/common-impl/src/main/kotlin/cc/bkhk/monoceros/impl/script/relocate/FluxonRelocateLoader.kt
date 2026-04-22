package cc.bkhk.monoceros.impl.script.relocate

import cc.bkhk.monoceros.impl.script.DefaultScriptHandler
import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.handler.Fluxon
import cc.bkhk.monoceros.impl.script.handler.FluxonHandler
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.inject.ClassVisitorHandler
import taboolib.common.io.runningClassMapInJar
import taboolib.common.platform.Awake
import taboolib.library.reflex.ReflexClass

/**
 * Fluxon Relocate 加载器
 *
 * 在 CONST 阶段判断 Fluxon 来源，决定是否需要对标记了 @FluxonRelocate 的类做 ASM 转译。
 * 当外部 FluxonPlugin 存在时，将 relocate 后的包名转回原始包名以使用外部类加载器。
 */
object FluxonRelocateLoader {

    private var propertySetted = false
    var needToTranslate = false

    @Awake(LifeCycle.CONST)
    fun init() {
        if (!propertySetted) {
            when {
                Bukkit.getServer().pluginManager.getPlugin("FluxonPlugin") != null -> {
                    propertySetted = true
                    needToTranslate = true
                }

                FluxonChecker.isReady() -> {
                    DefaultScriptHandler.fluxonHandler = Fluxon
                    propertySetted = true
                }

                else -> {
                    propertySetted = true
                    return
                }
            }
        }
        if (needToTranslate) {
            for ((_, clazz) in runningClassMapInJar) {
                if (clazz.structure.isAnnotationPresent(FluxonRelocate::class.java)) {
                    val newClazz = ReflexClass.of(AsmClassTranslation.createNewClass(clazz.name!!))
                    ClassVisitorHandler.injectAll(newClazz)
                    // 判断是否为 Fluxon handler 实现类
                    if (clazz.name == "cc.bkhk.monoceros.impl.script.handler.Fluxon") {
                        ClassVisitor.findInstance(newClazz).let { DefaultScriptHandler.fluxonHandler = it as FluxonHandler }
                    }
                }
            }
        }
    }

    /**
     * 在 ACTIVE 阶段从共享注册表导入 Monoceros 导出的函数到 FluxonPlugin 的 Runtime。
     */
    @Awake(LifeCycle.ACTIVE)
    fun importSharedFunctions() {
        if (!needToTranslate) return
        try {
            val fluxonPlugin = Bukkit.getServer().pluginManager.getPlugin("FluxonPlugin") ?: return
            val cl = fluxonPlugin.javaClass.classLoader
            val fluxonRuntimeClass = Class.forName("org.tabooproject.fluxon.runtime.FluxonRuntime", true, cl)
            val runtime = fluxonRuntimeClass.getMethod("getInstance").invoke(null)
            fluxonRuntimeClass.getMethod("importAllSharedFunctions", String::class.java).invoke(runtime, "Monoceros")
        } catch (_: Throwable) {
            // FluxonPlugin 的 Runtime 不可用时静默忽略
        }
    }
}
