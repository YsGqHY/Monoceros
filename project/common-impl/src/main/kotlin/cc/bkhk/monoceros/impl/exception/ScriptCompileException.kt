package cc.bkhk.monoceros.impl.exception

/**
 * 脚本编译异常
 */
class ScriptCompileException(scriptId: String, cause: Throwable) :
    RuntimeException("脚本编译失败: $scriptId", cause)
