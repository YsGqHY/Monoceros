package cc.bkhk.monoceros.impl.version

import cc.bkhk.monoceros.api.version.FeatureFlags
import cc.bkhk.monoceros.api.version.VersionAdapterResolver
import cc.bkhk.monoceros.api.version.VersionProfile
import cc.bkhk.monoceros.api.version.VersionedService
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 默认版本解析器
 *
 * 当前阶段先完成环境画像与 provider 选择骨架，
 * 后续由 legacy/modern/java17/java21 模块继续注册具体实现。
 */
object DefaultVersionAdapterResolver : VersionAdapterResolver {

    private val providers = ConcurrentHashMap<KClass<*>, MutableList<Any>>()

    override fun currentProfile(): VersionProfile {
        val version = Bukkit.getBukkitVersion().substringBefore('-')
        val parts = version.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val versionId = major * 10_000 + minor * 100 + patch
        val javaVersion = parseJavaVersion(System.getProperty("java.version") ?: "8")
        val legacy = major == 1 && minor <= 12
        return VersionProfile(
            minecraftVersionId = versionId,
            javaVersion = javaVersion,
            profileId = "mc-$version-jdk-$javaVersion",
            legacyMode = legacy,
            modernMode = !legacy,
        )
    }

    override fun featureFlags(): FeatureFlags {
        val profile = currentProfile()
        val dataComponent = profile.minecraftVersionId >= 12005
        return FeatureFlags(
            legacyNbt = profile.legacyMode,
            dataComponent = dataComponent,
            itemModel = profile.modernMode,
            packetRewriteSafe = profile.minecraftVersionId >= 11900,
        )
    }

    override fun <T : Any> resolveOrNull(type: KClass<T>): T? {
        val profile = currentProfile()
        val candidates = providers[type].orEmpty()
        for (candidate in candidates) {
            if (candidate is VersionedService && !candidate.supports(profile)) {
                continue
            }
            @Suppress("UNCHECKED_CAST")
            return candidate as? T
        }
        return null
    }

    fun <T : Any> register(type: KClass<T>, provider: T) {
        providers.compute(type) { _, list ->
            val target = list ?: mutableListOf()
            target.removeIf { it::class == provider::class }
            target.add(provider)
            target
        }
    }

    fun registeredTypes(): List<String> {
        return providers.keys.mapNotNull { it.qualifiedName }.sorted()
    }

    private fun parseJavaVersion(raw: String): Int {
        return when {
            raw.startsWith("1.") -> raw.substringAfter("1.").substringBefore('.').toIntOrNull() ?: 8
            else -> raw.substringBefore('.').toIntOrNull() ?: 8
        }
    }
}
