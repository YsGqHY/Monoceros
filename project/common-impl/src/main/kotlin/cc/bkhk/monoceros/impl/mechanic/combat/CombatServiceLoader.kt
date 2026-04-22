package cc.bkhk.monoceros.impl.mechanic.combat

import cc.bkhk.monoceros.api.mechanic.combat.*
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

object CombatServiceLoader {
    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        val cooldown = DefaultCooldownManager()
        PlatformFactory.registerAPI<CooldownManager>(cooldown)
        PlatformFactory.registerAPI<ComboTracker>(DefaultComboTracker())
        PlatformFactory.registerAPI<StatusManager>(DefaultStatusManager())
        PlatformFactory.registerAPI<SkillExecutor>(DefaultSkillExecutor(cooldown))
        DiagnosticLogger.info("Combat", "战斗机制服务已注册")
    }
}
