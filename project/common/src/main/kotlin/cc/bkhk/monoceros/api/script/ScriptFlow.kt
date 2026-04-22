package cc.bkhk.monoceros.api.script

import taboolib.common.platform.ProxyCommandSender

/**
 * 有序脚本链
 *
 * 支持前置/后置处理、共享变量流转、失败处理和主动终止。
 * 脚本间通过共享变量表传递数据。
 */
interface ScriptFlow {

    /** 添加脚本到执行链 */
    fun add(scriptId: String): ScriptFlow

    /** 添加脚本到执行链（带自定义变量） */
    fun add(scriptId: String, extraVariables: Map<String, Any?>): ScriptFlow

    /** 设置最后添加的脚本的前置处理 */
    fun preprocess(handler: (MutableMap<String, Any?>) -> Unit): ScriptFlow

    /** 设置最后添加的脚本的后置处理 */
    fun postprocess(handler: (Any?, MutableMap<String, Any?>) -> Unit): ScriptFlow

    /** 设置全局异常处理 */
    fun onFailure(handler: (Exception) -> Unit): ScriptFlow

    /** 终止脚本流 */
    fun terminate()

    /** 是否已终止 */
    fun isTerminated(): Boolean

    /** 启动执行，返回最终结果 */
    fun execute(): Any?

    /** 获取共享变量表 */
    fun variables(): MutableMap<String, Any?>
}
