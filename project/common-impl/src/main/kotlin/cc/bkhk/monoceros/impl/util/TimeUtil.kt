package cc.bkhk.monoceros.impl.util

/**
 * 统一时间解析工具
 *
 * 支持将带单位的时间字符串解析为毫秒值或 tick 值。
 */
object TimeUtil {

    private val PATTERN = Regex("^(-?\\d+(?:\\.\\d+)?)\\s*(\\w+)?$")

    /**
     * 解析时间字符串为毫秒值
     *
     * 支持的单位：
     * - `ticks` / `tick` / `t` -> x * 50ms
     * - `ms` / `millis` / `milliseconds` -> 原值
     * - `seconds` / `second` / `sec` / `s` -> x * 1000
     * - `minutes` / `minute` / `min` / `m` -> x * 60000
     * - `hours` / `hour` / `h` -> x * 3600000
     *
     * 无单位时默认为毫秒。
     *
     * @throws IllegalArgumentException 格式不合法或单位不识别时
     */
    fun parseMs(value: String): Long {
        val trimmed = value.trim()
        val match = PATTERN.matchEntire(trimmed)
            ?: throw IllegalArgumentException("无效的时间格式: $value")
        val number = match.groupValues[1].toDoubleOrNull()
            ?: throw IllegalArgumentException("无效的时间数值: ${match.groupValues[1]}")
        val unit = match.groupValues[2].lowercase().ifEmpty { "ms" }
        val multiplier = resolveMultiplierMs(unit)
        return (number * multiplier).toLong()
    }

    /**
     * 解析时间字符串为 tick 值（1 tick = 50ms）
     *
     * 支持与 [parseMs] 相同的单位。
     * 无单位时默认为 tick。
     */
    fun parseTicks(value: String): Long {
        val trimmed = value.trim()
        val match = PATTERN.matchEntire(trimmed)
            ?: throw IllegalArgumentException("无效的时间格式: $value")
        val number = match.groupValues[1].toDoubleOrNull()
            ?: throw IllegalArgumentException("无效的时间数值: ${match.groupValues[1]}")
        val unit = match.groupValues[2].lowercase().ifEmpty { "t" }
        val multiplierMs = resolveMultiplierMs(unit)
        return (number * multiplierMs / 50.0).toLong()
    }

    /**
     * 安全解析毫秒值，失败返回默认值
     */
    fun parseMsOrDefault(value: String, default: Long = 0L): Long {
        return try {
            parseMs(value)
        } catch (_: Exception) {
            default
        }
    }

    /**
     * 安全解析 tick 值，失败返回默认值
     */
    fun parseTicksOrDefault(value: String, default: Long = 0L): Long {
        return try {
            parseTicks(value)
        } catch (_: Exception) {
            default
        }
    }

    private fun resolveMultiplierMs(unit: String): Double = when (unit) {
        "ticks", "tick", "t" -> 50.0
        "ms", "millis", "milliseconds", "millisecond" -> 1.0
        "seconds", "second", "sec", "s" -> 1_000.0
        "minutes", "minute", "min", "m" -> 60_000.0
        "hours", "hour", "h" -> 3_600_000.0
        else -> throw IllegalArgumentException("未知的时间单位: $unit")
    }
}
