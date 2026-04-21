package cc.bkhk.monoceros.impl

import taboolib.common.platform.function.info

/**
 * Monoceros 引导器
 *
 * 负责异常安全的启动流程、版本与模块诊断信息输出。
 * 由 module-bukkit 在 INIT 阶段调用。
 */
object DefaultMonocerosBooster {

    /**
     * 启动引导
     *
     * 若启动失败，异常向上传播由调用方（MonocerosPlugin）处理。
     */
    fun startup() {
        info("")
        info("  __  __                                          ")
        info(" |  \\/  | ___  _ __   ___   ___ ___ _ __ ___  ___ ")
        info(" | |\\/| |/ _ \\| '_ \\ / _ \\ / __/ _ \\ '__/ _ \\/ __|")
        info(" | |  | | (_) | | | | (_) | (_|  __/ | | (_) \\__ \\")
        info(" |_|  |_|\\___/|_| |_|\\___/ \\___\\___|_|  \\___/|___/")
        info("")
        info(" Monoceros is initializing...")
        info("")
    }
}
