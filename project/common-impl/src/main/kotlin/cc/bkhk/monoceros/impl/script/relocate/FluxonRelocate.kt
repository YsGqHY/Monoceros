package cc.bkhk.monoceros.impl.script.relocate

/**
 * 标记需要区分自建库和中心库的类
 *
 * 当外部 FluxonPlugin 存在时，标记了此注解的类会被 ASM 转译，
 * 将 relocate 后的包名转回原始包名，以使用外部 FluxonPlugin 的类加载器。
 * 使用时请不要在非转译上下文中直接调用这些标记了 @FluxonRelocate 的类。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FluxonRelocate
