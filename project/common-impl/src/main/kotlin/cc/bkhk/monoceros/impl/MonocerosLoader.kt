package cc.bkhk.monoceros.impl

import cc.bkhk.monoceros.Monoceros
import cc.bkhk.monoceros.impl.noop.NoopActionWorkflowService
import cc.bkhk.monoceros.impl.noop.NoopCommandService
import cc.bkhk.monoceros.impl.noop.NoopDispatcherService
import cc.bkhk.monoceros.impl.noop.NoopPacketService
import cc.bkhk.monoceros.impl.noop.NoopPropertyWorkflowService
import cc.bkhk.monoceros.impl.noop.NoopScheduleService
import cc.bkhk.monoceros.impl.noop.NoopScriptHandler
import cc.bkhk.monoceros.impl.noop.NoopVolatilityService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Monoceros 生命周期加载器
 *
 * 在 LOAD 阶段注册 API 到全局单例，使用 AtomicBoolean 保证单次注册。
 * 当前阶段使用 Noop 占位实现，各功能模块就绪后会替换为真实实现。
 */
object MonocerosLoader {

    private val registered = AtomicBoolean(false)

    @Awake(LifeCycle.LOAD)
    private fun onLoad() {
        if (registered.compareAndSet(false, true)) {
            // 使用 Noop 占位实现组装 API，后续由各功能模块替换
            val api = DefaultMonocerosAPI(
                scriptHandler = NoopScriptHandler(),
                dispatcherService = NoopDispatcherService(),
                scheduleService = NoopScheduleService(),
                commandService = NoopCommandService(),
                packetService = NoopPacketService(),
                volatilityService = NoopVolatilityService(),
                actionWorkflowService = NoopActionWorkflowService(),
                propertyWorkflowService = NoopPropertyWorkflowService(),
            )
            Monoceros.register(api)
            info("[Monoceros] API registration completed at LOAD phase.")
        }
    }

    @Awake(LifeCycle.ENABLE)
    private fun onEnable() {
        info("[Monoceros] Enable phase - loading configurations...")
    }

    @Awake(LifeCycle.ACTIVE)
    private fun onActive() {
        info("[Monoceros] Active phase - all systems operational.")
    }

    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        info("[Monoceros] Disable phase - cleaning up resources...")
    }
}
