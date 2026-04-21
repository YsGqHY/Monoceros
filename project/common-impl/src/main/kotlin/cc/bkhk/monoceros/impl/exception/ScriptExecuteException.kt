package cc.bkhk.monoceros.impl.exception

/**
 * 脚本执行异常
 */
class ScriptExecuteException(scriptId: String, cause: Throwable) :
    RuntimeException("脚本执行失败: $scriptId", cause)
