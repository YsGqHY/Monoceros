package cc.bkhk.monoceros.api.version

/**
 * 当前运行环境画像
 */
data class VersionProfile(
    val minecraftVersionId: Int,
    val javaVersion: Int,
    val profileId: String,
    val legacyMode: Boolean,
    val modernMode: Boolean,
)
