package cc.bkhk.monoceros.api.version

/**
 * 版本模块提供者标记接口
 */
interface VersionModuleProvider : VersionedService {
    val moduleId: String
}
