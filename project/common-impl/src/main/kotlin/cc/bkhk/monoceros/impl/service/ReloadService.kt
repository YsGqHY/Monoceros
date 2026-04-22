package cc.bkhk.monoceros.impl.service

import cc.bkhk.monoceros.api.service.ReloadableService
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一重载服务
 *
 * 各功能模块在 LOAD 阶段注册 [ReloadableService]，
 * 命令层通过此入口驱动全量或单服务重载。
 */
object ReloadService {

    private const val MODULE = "ReloadService"

    private val services = ConcurrentHashMap<String, ReloadableService>()

    /** 注册可重载服务 */
    fun register(service: ReloadableService) {
        services[service.serviceId] = service
        DiagnosticLogger.info(MODULE, "注册可重载服务: ${service.serviceId} (priority=${service.priority})")
    }

    /** 注销可重载服务 */
    fun unregister(id: String) {
        services.remove(id)
    }

    /** 返回已注册服务 ID 列表（按 priority 降序） */
    fun serviceIds(): List<String> {
        return services.values
            .sortedByDescending { it.priority }
            .map { it.serviceId }
    }

    /**
     * 全量重载所有服务
     *
     * 按 priority 降序逐个重载，每个服务独立 try-catch，
     * 单个服务失败不影响其他服务继续重载。
     *
     * @return 每个服务的重载结果
     */
    fun reloadAll(): Map<String, Result<Int>> {
        val sorted = services.values.sortedByDescending { it.priority }
        val results = LinkedHashMap<String, Result<Int>>(sorted.size)
        for (service in sorted) {
            results[service.serviceId] = try {
                val count = service.reload()
                DiagnosticLogger.info(MODULE, "重载服务 ${service.serviceId}: $count 个")
                Result.success(count)
            } catch (e: Exception) {
                DiagnosticLogger.warn(MODULE, "重载服务失败: ${service.serviceId}", e)
                Result.failure(e)
            }
        }
        return results
    }

    /**
     * 单服务重载
     *
     * @return 重载结果，服务不存在时返回 null
     */
    fun reload(serviceId: String): Result<Int>? {
        val service = services[serviceId] ?: return null
        return try {
            val count = service.reload()
            DiagnosticLogger.info(MODULE, "重载服务 $serviceId: $count 个")
            Result.success(count)
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "重载服务失败: $serviceId", e)
            Result.failure(e)
        }
    }
}
