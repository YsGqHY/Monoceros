package cc.bkhk.monoceros.dispatcher.pipeline

import cc.bkhk.monoceros.api.dispatcher.pipeline.Pipeline
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineContext
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * 反射 Player 提取管道
 *
 * 通过反射自动从事件对象中提取 Player 字段。
 * 作为兜底管道使用（priority = 0），当其他 Pipeline 未设置 principal 时生效。
 *
 * @param fieldName 指定字段名，null 时自动检索事件类中的 Player 类型字段
 * @param playerRequired 若为 true，未找到 Player 时过滤事件
 */
class ReflexPlayerPipeline(
    private val fieldName: String? = null,
    private val playerRequired: Boolean = false,
) : Pipeline {

    override val priority: Int = 0

    /** 缓存：事件类 -> Player 字段 */
    private val fieldCache = HashMap<Class<*>, java.lang.reflect.Field?>()

    override fun initPrincipal(context: PipelineContext) {
        // 如果已有 principal，跳过
        if (context.principal != null) return

        val player = extractPlayer(context.event)
        if (player != null) {
            context.principal = player
            context.principalId = "PLAYER@${player.uniqueId}"
            context.player = player
        } else if (playerRequired) {
            context.isFiltered = true
        }
    }

    private fun extractPlayer(event: Event): Player? {
        val clazz = event.javaClass
        val field = fieldCache.getOrPut(clazz) { resolveField(clazz) }
        if (field == null) return null
        return try {
            field.get(event) as? Player
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveField(clazz: Class<*>): java.lang.reflect.Field? {
        // 指定字段名
        if (fieldName != null) {
            return try {
                clazz.getDeclaredField(fieldName).apply { isAccessible = true }
            } catch (_: Exception) {
                null
            }
        }
        // 自动检索：遍历所有字段（含父类），找到 Player 类型字段
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            for (f in current.declaredFields) {
                if (Player::class.java.isAssignableFrom(f.type)) {
                    f.isAccessible = true
                    return f
                }
            }
            current = current.superclass
        }
        return null
    }
}
