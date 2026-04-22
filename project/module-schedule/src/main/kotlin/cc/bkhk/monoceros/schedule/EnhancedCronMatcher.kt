package cc.bkhk.monoceros.schedule

import java.util.Calendar

/**
 * 增强 Cron 匹配器
 *
 * 支持标准 6 字段 cron 格式：秒 分 时 日 月 星期
 *
 * 增强语法：
 * - `*` / `?` — 通配
 * - `N` — 单值
 * - `N,M,...` — 多值
 * - `N-M` / `N to M` — 范围
 * - `N/M` / `N at M` — 步进（从 N 开始每 M 步）
 * - `L` — 日字段：月最后一天
 * - `LW` — 日字段：月最后一个工作日
 * - `NW` — 日字段：离 N 号最近的工作日
 * - `N#M` / `N on M` — 星期字段：第 M 周的星期 N
 * - `NL` — 星期字段：月最后一个星期 N
 *
 * 保留 [SimpleCronMatcher] 作为无增强语法的降级方案。
 */
class EnhancedCronMatcher(expression: String) {

    private val fields: List<CronField>
    private var lastMatchedSecond: Long = -1

    init {
        // 预处理：将 "to" 替换为 "-"，将 "at" 替换为 "/"，将 "on" 替换为 "#"
        val normalized = expression.trim()
            .replace(Regex("\\s+to\\s+"), "-")
            .replace(Regex("\\s+at\\s+"), "/")
            .replace(Regex("\\s+on\\s+"), "#")
        val parts = normalized.split("\\s+".toRegex())
        require(parts.size == 6) { "Cron 表达式需要 6 个字段: $expression" }
        fields = parts.mapIndexed { index, part -> parseCronField(part, index) }
    }

    /**
     * 检查当前时间是否匹配 cron 表达式
     *
     * 同一秒内只匹配一次，避免重复触发。
     */
    fun matches(): Boolean {
        val now = Calendar.getInstance()
        val currentSecond = now.timeInMillis / 1000

        if (currentSecond == lastMatchedSecond) return false

        val matched = fields[0].matches(now, FieldType.SECOND) &&
            fields[1].matches(now, FieldType.MINUTE) &&
            fields[2].matches(now, FieldType.HOUR) &&
            fields[3].matches(now, FieldType.DAY) &&
            fields[4].matches(now, FieldType.MONTH) &&
            fields[5].matches(now, FieldType.WEEK)

        if (matched) {
            lastMatchedSecond = currentSecond
        }
        return matched
    }

    private enum class FieldType { SECOND, MINUTE, HOUR, DAY, MONTH, WEEK }

    /** 解析单个 cron 字段 */
    private fun parseCronField(field: String, index: Int): CronField {
        if (field == "*" || field == "?") return CronField.Any

        // 日字段增强语法
        if (index == 3) {
            if (field.uppercase() == "L") return CronField.LastDay
            if (field.uppercase() == "LW") return CronField.LastWeekday
            if (field.uppercase().endsWith("W") && field.length > 1) {
                val day = field.dropLast(1).toIntOrNull()
                if (day != null) return CronField.NearestWeekday(day)
            }
        }

        // 星期字段增强语法
        if (index == 5) {
            if (field.uppercase().endsWith("L") && field.length > 1) {
                val dow = field.dropLast(1).toIntOrNull()
                if (dow != null) return CronField.LastDayOfWeek(dow)
            }
            if (field.contains('#')) {
                val parts = field.split('#')
                if (parts.size == 2) {
                    val dow = parts[0].toIntOrNull()
                    val nth = parts[1].toIntOrNull()
                    if (dow != null && nth != null) return CronField.NthDayOfWeek(dow, nth)
                }
            }
        }

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
        abstract fun matches(cal: Calendar, type: FieldType): Boolean

        /** 获取当前字段的日历值 */
        protected fun calValue(cal: Calendar, type: FieldType): Int = when (type) {
            FieldType.SECOND -> cal.get(Calendar.SECOND)
            FieldType.MINUTE -> cal.get(Calendar.MINUTE)
            FieldType.HOUR -> cal.get(Calendar.HOUR_OF_DAY)
            FieldType.DAY -> cal.get(Calendar.DAY_OF_MONTH)
            FieldType.MONTH -> cal.get(Calendar.MONTH) + 1
            FieldType.WEEK -> cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=周日..6=周六
        }

        object Any : CronField() {
            override fun matches(cal: Calendar, type: FieldType) = true
        }

        data class Values(val values: Set<Int>) : CronField() {
            override fun matches(cal: Calendar, type: FieldType) = calValue(cal, type) in values
        }

        data class Range(val start: Int, val end: Int) : CronField() {
            override fun matches(cal: Calendar, type: FieldType) = calValue(cal, type) in start..end
        }

        data class Step(val start: Int, val step: Int) : CronField() {
            override fun matches(cal: Calendar, type: FieldType): Boolean {
                val value = calValue(cal, type)
                return value >= start && (value - start) % step == 0
            }
        }

        /** 月最后一天 */
        object LastDay : CronField() {
            override fun matches(cal: Calendar, type: FieldType): Boolean {
                return cal.get(Calendar.DAY_OF_MONTH) == cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }

        /** 月最后一个工作日 */
        object LastWeekday : CronField() {
            override fun matches(cal: Calendar, type: FieldType): Boolean {
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val temp = cal.clone() as Calendar
                temp.set(Calendar.DAY_OF_MONTH, lastDay)
                // 如果最后一天是周六，回退到周五
                while (temp.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || temp.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    temp.add(Calendar.DAY_OF_MONTH, -1)
                }
                return cal.get(Calendar.DAY_OF_MONTH) == temp.get(Calendar.DAY_OF_MONTH)
            }
        }

        /** 离 N 号最近的工作日 */
        data class NearestWeekday(val day: Int) : CronField() {
            override fun matches(cal: Calendar, type: FieldType): Boolean {
                val temp = cal.clone() as Calendar
                temp.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(temp.getActualMaximum(Calendar.DAY_OF_MONTH)))
                val dow = temp.get(Calendar.DAY_OF_WEEK)
                val adjusted = when (dow) {
                    Calendar.SATURDAY -> if (day > 1) day - 1 else day + 2
                    Calendar.SUNDAY -> if (day < temp.getActualMaximum(Calendar.DAY_OF_MONTH)) day + 1 else day - 2
                    else -> day
                }
                return cal.get(Calendar.DAY_OF_MONTH) == adjusted
            }
        }

        /** 月最后一个星期 N（0=周日..6=周六） */
        data class LastDayOfWeek(val dayOfWeek: Int) : CronField() {
            override fun matches(cal: Calendar, type: FieldType): Boolean {
                val currentDow = cal.get(Calendar.DAY_OF_WEEK) - 1
                if (currentDow != dayOfWeek) return false
                // 检查是否是本月最后一个该星期
                val temp = cal.clone() as Calendar
                temp.add(Calendar.WEEK_OF_YEAR, 1)
                return temp.get(Calendar.MONTH) != cal.get(Calendar.MONTH)
            }
        }

        /** 第 M 周的星期 N（0=周日..6=周六） */
        data class NthDayOfWeek(val dayOfWeek: Int, val nth: Int) : CronField() {
            override fun matches(cal: Calendar, type: FieldType): Boolean {
                val currentDow = cal.get(Calendar.DAY_OF_WEEK) - 1
                if (currentDow != dayOfWeek) return false
                // 计算当前是本月第几个该星期
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val weekNum = (dayOfMonth - 1) / 7 + 1
                return weekNum == nth
            }
        }
    }
}
