package cc.bkhk.monoceros.api.version

/**
 * 由版本画像派生出的能力开关
 */
data class FeatureFlags(
    val legacyNbt: Boolean,
    val dataComponent: Boolean,
    val itemModel: Boolean,
    val packetRewriteSafe: Boolean,
)
