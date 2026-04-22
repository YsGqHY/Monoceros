package cc.bkhk.monoceros.api.applicative

/**
 * 转换器注册中心接口
 *
 * 管理所有 [Applicative] 转换器的注册与查找。
 */
interface ApplicativeRegistry {

    /** 按 Class 查找转换器 */
    fun <T> get(clazz: Class<T>): Applicative<T>?

    /** 按名称查找转换器 */
    fun <T> get(name: String): Applicative<T>?

    /** 注册转换器 */
    fun register(clazz: Class<*>, applicative: Applicative<*>)

    /** 获取所有已注册的转换器 */
    fun all(): Collection<Applicative<*>>
}
