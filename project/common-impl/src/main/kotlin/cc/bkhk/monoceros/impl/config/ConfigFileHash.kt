package cc.bkhk.monoceros.impl.config

/**
 * 文件哈希记录
 */
data class ConfigFileHash(
    val fileId: String,
    val relativePath: String,
    val sha256: String,
)
