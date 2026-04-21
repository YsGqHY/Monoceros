package cc.bkhk.monoceros.impl.config

/**
 * 文件变更回调接口
 */
interface ConfigServiceCallback {

    /** 文件新增 */
    fun onCreated(fileId: String, hash: ConfigFileHash)

    /** 文件修改 */
    fun onModified(fileId: String, hash: ConfigFileHash)

    /** 文件删除 */
    fun onDeleted(fileId: String)
}
