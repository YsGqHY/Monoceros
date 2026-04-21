package cc.bkhk.monoceros.impl.util

import taboolib.common.platform.function.warning
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 文件监听与 reload 节流工具
 *
 * 基于 Java NIO WatchService 监听目录变更，通过节流机制合并短时间内的多次变更事件，
 * 避免频繁触发重载。递归监听所有子目录。
 *
 * @param directory 监听的目录
 * @param throttleMs 节流间隔（毫秒），同一目录在此时间内的多次变更只触发一次回调
 * @param onChange 变更回调，在节流窗口结束后触发
 */
class FileWatcherThrottle(
    private val directory: File,
    private val throttleMs: Long = 500L,
    private val onChange: () -> Unit,
) {

    private val running = AtomicBoolean(false)
    private var watchService: WatchService? = null
    private var watchThread: Thread? = null
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Monoceros-FileWatcher-Throttle").apply { isDaemon = true }
    }
    private var pendingFuture: ScheduledFuture<*>? = null

    /**
     * 启动文件监听
     *
     * 若目录不存在会自动创建。重复调用无效。
     * 递归注册所有子目录到 WatchService。
     */
    fun start() {
        if (!running.compareAndSet(false, true)) return

        if (!directory.exists()) {
            directory.mkdirs()
        }

        try {
            val ws = FileSystems.getDefault().newWatchService()
            watchService = ws

            // 递归注册目录及所有子目录
            registerDirectoryRecursive(directory.toPath(), ws)

            watchThread = Thread({
                while (running.get()) {
                    try {
                        val key = ws.poll(1, TimeUnit.SECONDS) ?: continue
                        // 消费所有事件
                        val events = key.pollEvents()
                        key.reset()

                        // 检查是否有新目录创建，需要注册监听
                        for (event in events) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                val context = event.context()
                                if (context is Path) {
                                    val watchablePath = key.watchable()
                                    if (watchablePath is Path) {
                                        val newPath = watchablePath.resolve(context)
                                        if (newPath.toFile().isDirectory) {
                                            try {
                                                registerDirectoryRecursive(newPath, ws)
                                            } catch (_: Exception) {
                                                // 新目录注册失败不影响主流程
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        scheduleThrottled()
                    } catch (_: InterruptedException) {
                        break
                    } catch (e: Exception) {
                        if (running.get()) {
                            warning("[Monoceros] FileWatcher 异常: ${e.message}")
                        }
                    }
                }
            }, "Monoceros-FileWatcher-${directory.name}").apply {
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            running.set(false)
            warning("[Monoceros] FileWatcher 启动失败: ${directory.path} - ${e.message}")
        }
    }

    /**
     * 停止文件监听
     */
    fun stop() {
        if (!running.compareAndSet(true, false)) return

        pendingFuture?.cancel(false)
        pendingFuture = null
        watchThread?.interrupt()
        watchThread = null

        try {
            watchService?.close()
        } catch (_: Exception) {
        }
        watchService = null

        scheduler.shutdownNow()
    }

    /** 递归注册目录及其所有子目录到 WatchService */
    private fun registerDirectoryRecursive(path: Path, ws: WatchService) {
        path.register(
            ws,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE,
        )
        path.toFile().listFiles()?.forEach { child ->
            if (child.isDirectory && !child.name.startsWith(".")) {
                registerDirectoryRecursive(child.toPath(), ws)
            }
        }
    }

    /** 节流调度：取消上一次待执行的回调，重新计时 */
    private fun scheduleThrottled() {
        synchronized(this) {
            pendingFuture?.cancel(false)
            pendingFuture = scheduler.schedule({
                try {
                    onChange()
                } catch (e: Exception) {
                    warning("[Monoceros] FileWatcher 回调异常: ${e.message}")
                }
            }, throttleMs, TimeUnit.MILLISECONDS)
        }
    }

    /** 是否正在运行 */
    fun isRunning(): Boolean = running.get()
}
