package cc.bkhk.monoceros.schedule

import java.util.Calendar

/**
 * 简易 Cron 匹配器
 *
 * 支持标准 6 字段 cron 格式：秒 分 时 日 月 星期
 * 支持 * 通配符和具体数值，不支持复杂表达式（范围、步进等）。
 * 用于 Cron 调度任务的秒级轮询匹配。
 */
class SimpleCronMatcher(expression: String) {

    private val fields: List<CronField>
    private var lastMatchedSecond: Long = -1

    init {
        val parts = expression.trim().split("\\s+".toRegex())
        require(parts.size == 6) { "Cron 表达式需要 6 个字段: $expression" }
        fields = parts.map { parseCronField(it) }
    }

    /**
     * 检查当前时间是否匹配 cron 表达式
     *
     * 同一秒内只匹配一次，避免重复触发。
     */
    fun matches(): Boolean {
        val now = Calendar.getInstance()
        val currentSecond = now.timeInMillis / 1000

        // 同一秒内不重复匹配
        if (currentSecond == lastMatchedSecond) return false

        val matched = fields[0].matches(now.get(Calendar.SECOND)) &&
            fields[1].matches(now.get(Calendar.MINUTE)) &&
            fields[2].matches(now.get(Calendar.HOUR_OF_DAY)) &&
            fields[3].matches(now.get(Calendar.DAY_OF_MONTH)) &&
            fields[4].matches(now.get(Calendar.MONTH) + 1) &&
            // Calendar.DAY_OF_WEEK: 1=周日..7=周六 -> cron 标准: 0=周日..6=周六
            fields[5].matches(now.get(Calendar.DAY_OF_WEEK) - 1)

        if (matched) {
            lastMatchedSecond = currentSecond
        }
        return matched
    }

    /** 解析单个 cron 字段 */
    private fun parseCronField(field: String): CronField {
        if (field == "*" || field == "?") return CronField.Any
        // 逗号分隔的多值
        if (field.contains(',')) {
            val values = field.split(',').map { it.trim().toInt() }.toSet()
            return CronField.Values(values)
        }
        // 范围
        if (field.contains('-')) {
            val (start, end) = field.split('-').map { it.trim().toInt() }
            return CronField.Range(start, end)
        }
        // 步进
        if (field.contains('/')) {
            val (base, step) = field.split('/').map { it.trim() }
            val start = if (base == "*") 0 else base.toInt()
            return CronField.Step(start, step.toInt())
        }
        // 单值
        return CronField.Values(setOf(field.toInt()))
    }

    /** Cron 字段匹配 */
    private sealed class CronField {
        abstract fun matches(value: Int): Boolean

        object Any : CronField() {
            override fun matches(value: Int) = true
        }

        data class Values(val values: Set<Int>) : CronField() {
            override fun matches(value: Int) = value in values
        }

        data class Range(val start: Int, val end: Int) : CronField() {
            override fun matches(value: Int) = value in start..end
        }

        data class Step(val start: Int, val step: Int) : CronField() {
            override fun matches(value: Int) = value >= start && (value - start) % step == 0
        }
    }
}
