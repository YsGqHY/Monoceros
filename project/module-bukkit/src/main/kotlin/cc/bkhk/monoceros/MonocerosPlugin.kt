package cc.bkhk.monoceros

import cc.bkhk.monoceros.impl.DefaultMonocerosBooster
import taboolib.common.LifeCycle
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.disablePlugin
import taboolib.common.platform.function.registerLifeCycleTask

/**
 * Monoceros Bukkit 平台入口
 *
 * 仅负责 INIT 阶段引导，不承担任何业务逻辑。
 */
object MonocerosPlugin : Plugin() {

    init {
        registerLifeCycleTask(LifeCycle.INIT) {
            try {
                DefaultMonocerosBooster.startup()
            } catch (e: Exception) {
                e.printStackTrace()
                disablePlugin()
            }
        }
    }
}
