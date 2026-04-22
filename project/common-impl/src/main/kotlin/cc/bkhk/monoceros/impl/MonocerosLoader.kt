package cc.bkhk.monoceros.impl

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.api.applicative.ApplicativeRegistry
import cc.bkhk.monoceros.api.extension.ExtensionRegistry
import cc.bkhk.monoceros.impl.applicative.DefaultApplicativeRegistry
import cc.bkhk.monoceros.impl.extension.DefaultExtensionRegistry
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.info
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Monoceros 生命周期加载器
 *
 * 在 LOAD 阶段注册 API 到全局单例，使用 AtomicBoolean 保证单次注册。
 * [DefaultMonocerosAPI] 内部通过 PlatformFactory 动态查找真实实现，
 * 查不到时回退到静态 Noop 实例，无需在此处组装 Noop。
 */
object MonocerosLoader {

    private val registered = AtomicBoolean(false)

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        if (registered.compareAndSet(false, true)) {
            Monoceros.register(DefaultMonocerosAPI())

            // 注册扩展注册中心到 PlatformFactory
            PlatformFactory.registerAPI<ExtensionRegistry>(DefaultExtensionRegistry())

            // 注册 Applicative 注册中心到 PlatformFactory
            PlatformFactory.registerAPI<ApplicativeRegistry>(DefaultApplicativeRegistry)

            info("[Monoceros] API registration completed at LOAD phase.")
        }
    }

    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        info("[Monoceros] Enable phase - loading configurations...")
    }

    @Awake(LifeCycle.ACTIVE)
    fun onActive() {
        info("[Monoceros] Active phase - all systems operational.")
    }

    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        // 禁用所有扩展
        PlatformFactory.getAPIOrNull<ExtensionRegistry>()?.disableAll()
        info("[Monoceros] Disable phase - cleaning up resources...")
    }
}
