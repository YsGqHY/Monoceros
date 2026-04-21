package cc.bkhk.monoceros.api.script

import java.nio.file.Path

/**
 * 脚本文件监听器
 *
 * 监听 script/ 目录的文件变更，触发增量重载。
 */
interface ScriptFileWatcher {

    /** 启动监听 */
    fun start()

    /** 停止监听 */
    fun stop()

    /** 将指定路径加入重载队列 */
    fun queueReload(path: Path)
}
