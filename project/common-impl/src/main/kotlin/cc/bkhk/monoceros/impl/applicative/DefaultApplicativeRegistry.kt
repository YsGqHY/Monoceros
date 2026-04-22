package cc.bkhk.monoceros.impl.applicative

import cc.bkhk.monoceros.api.applicative.Applicative
import cc.bkhk.monoceros.api.applicative.ApplicativeRegistry
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.library.reflex.ReflexClass
import java.util.concurrent.ConcurrentHashMap

/**
 * Applicative 注册中心默认实现
 *
 * 继承 TabooLib [ClassVisitor]，在 LOAD 阶段自动扫描所有 [AbstractApplicative] 子类并注册。
 */
@Awake(LifeCycle.LOAD)
object DefaultApplicativeRegistry : ClassVisitor(-4), ApplicativeRegistry {

    private const val MODULE = "Applicative"

    /** Class -> Applicative 映射 */
    private val classRegistry = ConcurrentHashMap<Class<*>, Applicative<*>>()

    /** 名称 -> Applicative 映射（包含别名） */
    private val nameRegistry = ConcurrentHashMap<String, Applicative<*>>()

    /** 基本类型到包装类型的映射 */
    private val primitiveMapping = mapOf(
        java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
        java.lang.Integer.TYPE to java.lang.Integer::class.java,
        java.lang.Long.TYPE to java.lang.Long::class.java,
        java.lang.Float.TYPE to java.lang.Float::class.java,
        java.lang.Double.TYPE to java.lang.Double::class.java,
        java.lang.Byte.TYPE to java.lang.Byte::class.java,
        java.lang.Short.TYPE to java.lang.Short::class.java,
        java.lang.Character.TYPE to java.lang.Character::class.java,
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(clazz: Class<T>): Applicative<T>? {
        return classRegistry[clazz] as? Applicative<T>
            ?: classRegistry[primitiveMapping[clazz]] as? Applicative<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String): Applicative<T>? {
        return nameRegistry[name.lowercase()] as? Applicative<T>
    }

    override fun register(clazz: Class<*>, applicative: Applicative<*>) {
        classRegistry[clazz] = applicative
        nameRegistry[applicative.name.lowercase()] = applicative
        for (alias in applicative.aliases) {
            nameRegistry[alias.lowercase()] = applicative
        }
        // 注册基本类型映射
        primitiveMapping[clazz]?.let { classRegistry[it] = applicative }
    }

    override fun all(): Collection<Applicative<*>> = classRegistry.values.toSet()

    /**
     * ClassVisitor 回调：自动扫描并注册 [AbstractApplicative] 子类
     */
    override fun visitStart(clazz: ReflexClass) {
        val javaClass = clazz.toClass() ?: return
        if (!AbstractApplicative::class.java.isAssignableFrom(javaClass)) return
        if (javaClass == AbstractApplicative::class.java) return

        try {
            val instance = try {
                // 优先尝试 Kotlin object 单例（通过 INSTANCE 静态字段）
                val instanceField = javaClass.getDeclaredField("INSTANCE")
                instanceField.isAccessible = true
                instanceField.get(null)
            } catch (_: Exception) {
                null
            } ?: try {
                // 尝试无参构造
                javaClass.getDeclaredConstructor().newInstance()
            } catch (_: Exception) {
                null
            } ?: return

            val applicative = instance as AbstractApplicative<*>
            register(applicative.clazz, applicative)
            DiagnosticLogger.debug(MODULE, "自动注册转换器: ${applicative.name} (${applicative.clazz.simpleName})")
        } catch (e: Exception) {
            DiagnosticLogger.warn(MODULE, "转换器自动注册失败: ${javaClass.simpleName}", e)
        }
    }

    override fun getLifeCycle(): LifeCycle = LifeCycle.LOAD
}
