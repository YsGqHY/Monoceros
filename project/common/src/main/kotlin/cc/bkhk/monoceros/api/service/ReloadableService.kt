package cc.bkhk.monoceros.api.service

/**
 * 可重载服务接口
 *
 * 各功能模块在 LOAD 阶段将自身注册到 [cc.bkhk.monoceros.impl.service.ReloadService]，
 * 由统一入口驱动全量或单服务重载。
 */
interface ReloadableService {

    /** 服务唯一标识，用于命令补全与单服务重载 */
    val serviceId: String

    /** 重载优先级，越大越先重载；脚本应最先重载，工作流最后 */
    val priority: Int get() = 0

    /**
     * 执行重载
     *
     * @return 本次加载的资源数量
     */
    fun reload(): Int
}
