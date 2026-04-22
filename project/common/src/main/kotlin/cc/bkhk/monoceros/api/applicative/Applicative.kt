package cc.bkhk.monoceros.api.applicative

/**
 * 统一值转换接口
 *
 * 将任意类型的值转换为目标类型 [T]。
 * 所有转换器通过 [ApplicativeRegistry] 注册和查找。
 */
interface Applicative<T> {

    /** 转换器名称（通常为类型简名的小写形式） */
    val name: String

    /** 名称别名 */
    val aliases: Array<String> get() = emptyArray()

    /**
     * 强制转换
     *
     * @throws NullPointerException 当 [instance] 为 null 时
     * @throws IllegalArgumentException 当类型不匹配或值不合法时
     */
    fun convert(instance: Any?): T

    /**
     * 安全转换，失败返回 null
     */
    fun convertOrNull(instance: Any?): T?
}
