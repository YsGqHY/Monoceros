package cc.bkhk.monoceros.impl.config

import cc.bkhk.monoceros.impl.util.FileWatcherThrottle
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.warning
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 文件资源加载基类
 *
 * 所有资源加载模块（脚本、分发器、命令、调度）共享此基类。
 * 提供目录扫描、SHA-256 哈希计算、变更检测与回调通知能力。
 * 支持通过 FileWatcher 实现热重载（配置开关）。
 */
abstract class ConfigService(
    /** 相对于插件数据目录的子目录名 */
    private val directoryName: String,
    /** 支持的文件扩展名 */
    private val extensions: Set<String> = setOf("yml", "yaml"),
    /** FileWatcher 节流间隔（毫秒），设为 0 或负数则不启用 watcher */
    private val watcherThrottleMs: Long = 500L,
) {

    /** 上一次扫描的文件哈希快照 */
    private val lastHashes = ConcurrentHashMap<String, ConfigFileHash>()

    /** 文件监听器实例，调用 startWatcher 后创建 */
    private var watcher: FileWatcherThrottle? = null

    /** 获取资源目录 */
    fun directory(): File = File(getDataFolder(), directoryName)

    /**
     * 扫描目录并检测变更，通过回调通知
     *
     * @return 本次扫描的文件数量
     */
    fun scan(callback: ConfigServiceCallback): Int {
        val dir = directory()
        if (!dir.exists()) {
            dir.mkdirs()
            return 0
        }

        val currentFiles = collectFiles(dir)
        val currentIds = mutableSetOf<String>()

        for ((fileId, file) in currentFiles) {
            currentIds.add(fileId)
            val relativePath = file.relativeTo(dir).path.replace('\\', '/')
            val sha256 = computeSha256(file)
            val hash = ConfigFileHash(fileId, relativePath, sha256)
            val oldHash = lastHashes[fileId]

            when {
                // 新文件
                oldHash == null -> {
                    lastHashes[fileId] = hash
                    callback.onCreated(fileId, hash)
                }
                // 内容变更
                oldHash.sha256 != sha256 -> {
                    lastHashes[fileId] = hash
                    callback.onModified(fileId, hash)
                }
                // 未变更，不通知
            }
        }

        // 检测已删除的文件
        val removedIds = lastHashes.keys - currentIds
        for (id in removedIds) {
            lastHashes.remove(id)
            callback.onDeleted(id)
        }

        return currentFiles.size
    }

    /** 清空哈希快照 */
    fun clearHashes() {
        lastHashes.clear()
    }

    /**
     * 启动文件监听
     *
     * 监听资源目录的文件变更，变更后自动触发 [scan] 并通过回调通知。
     * 若 watcherThrottleMs <= 0 则不启用。
     */
    fun startWatcher(callback: ConfigServiceCallback) {
        if (watcherThrottleMs <= 0) return
        stopWatcher()
        watcher = FileWatcherThrottle(
            directory = directory(),
            throttleMs = watcherThrottleMs,
            onChange = { scan(callback) },
        ).also { it.start() }
    }

    /**
     * 停止文件监听
     */
    fun stopWatcher() {
        watcher?.stop()
        watcher = null
    }

    /** 文件监听是否正在运行 */
    fun isWatching(): Boolean = watcher?.isRunning() == true

    /**
     * 收集目录下所有符合扩展名的文件
     *
     * 以 # 开头的文件会被忽略。
     * 文件 ID 由相对路径推导：example/default.yml -> example.default
     */
    private fun collectFiles(dir: File): List<Pair<String, File>> {
        val result = mutableListOf<Pair<String, File>>()
        dir.walkTopDown().forEach { file ->
            if (file.isFile && !file.name.startsWith("#") && file.extension in extensions) {
                val relativePath = file.relativeTo(dir).path.replace('\\', '/')
                val fileId = relativePath
                    .substringBeforeLast('.')
                    .replace('/', '.')
                result.add(fileId to file)
            }
        }
        return result
    }

    /** 计算文件 SHA-256 哈希 */
    private fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            warning("Failed to compute SHA-256 for ${file.path}: ${e.message}")
            ""
        }
    }
}
