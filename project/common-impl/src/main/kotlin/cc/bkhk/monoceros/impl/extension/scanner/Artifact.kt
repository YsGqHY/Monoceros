package cc.bkhk.monoceros.impl.extension.scanner

/**
 * 扩展包工件模型
 *
 * 解析 JAR 文件名中的扩展名与版本号。
 * 文件名格式约定：`{name}-{version}.jar`，如 `my-extension-1.2.3.jar`
 */
data class Artifact(
    /** 扩展名 */
    val name: String,
    /** 版本号字符串 */
    val version: String,
    /** 主版本号 */
    val majorVersion: Int,
    /** 次版本号 */
    val minorVersion: Int,
    /** 修订版本号 */
    val patchVersion: Int,
    /** JAR 文件路径 */
    val filePath: String,
) : Comparable<Artifact> {

    override fun compareTo(other: Artifact): Int {
        val major = majorVersion.compareTo(other.majorVersion)
        if (major != 0) return major
        val minor = minorVersion.compareTo(other.minorVersion)
        if (minor != 0) return minor
        return patchVersion.compareTo(other.patchVersion)
    }

    companion object {

        /** 版本号正则：匹配末尾的 -x.y.z 或 -x.y */
        private val VERSION_PATTERN = Regex("^(.+?)-(\\d+)\\.(\\d+)(?:\\.(\\d+))?\\.jar$")

        /**
         * 从文件名解析 Artifact
         *
         * @return 解析成功返回 Artifact，失败返回 null
         */
        fun parse(fileName: String, filePath: String): Artifact? {
            val match = VERSION_PATTERN.matchEntire(fileName) ?: return null
            val name = match.groupValues[1]
            val major = match.groupValues[2].toIntOrNull() ?: 0
            val minor = match.groupValues[3].toIntOrNull() ?: 0
            val patch = match.groupValues[4].toIntOrNull() ?: 0
            val version = "$major.$minor.$patch"
            return Artifact(name, version, major, minor, patch, filePath)
        }
    }
}
