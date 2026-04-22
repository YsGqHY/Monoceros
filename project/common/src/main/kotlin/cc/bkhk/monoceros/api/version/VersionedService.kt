package cc.bkhk.monoceros.api.version

/**
 * 带版本条件的服务提供者
 */
interface VersionedService {
    fun supports(profile: VersionProfile): Boolean
}
