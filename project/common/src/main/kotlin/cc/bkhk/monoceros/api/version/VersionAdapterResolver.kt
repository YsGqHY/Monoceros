package cc.bkhk.monoceros.api.version

import kotlin.reflect.KClass

/**
 * 版本适配解析器
 */
interface VersionAdapterResolver {

    /** 当前环境画像 */
    fun currentProfile(): VersionProfile

    /** 当前环境特性开关 */
    fun featureFlags(): FeatureFlags

    /**
     * 解析指定类型的版本适配实现。
     *
     * 当前阶段允许返回 null，表示尚无对应 provider。
     */
    fun <T : Any> resolveOrNull(type: KClass<T>): T?
}
