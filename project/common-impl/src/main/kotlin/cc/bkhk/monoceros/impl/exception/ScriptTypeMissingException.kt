package cc.bkhk.monoceros.impl.exception

/**
 * 脚本类型未找到异常
 */
class ScriptTypeMissingException(typeId: String) :
    RuntimeException("脚本类型未注册: $typeId")
