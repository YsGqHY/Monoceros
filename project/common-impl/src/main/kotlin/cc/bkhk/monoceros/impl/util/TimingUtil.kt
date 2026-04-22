package cc.bkhk.monoceros.impl.util

/**
 * 耗时计量工具
 *
 * 基于 [System.nanoTime] 的高精度计时，返回毫秒级耗时。
 */
object TimingUtil {

    /**
     * 开始计时
     *
     * @return 纳秒级起始时间戳
     */
    fun startTiming(): Long = System.nanoTime()

    /**
     * 停止计时并返回耗时（毫秒）
     *
     * @param startTime [startTiming] 返回的起始时间戳
     * @return 格式化后的毫秒耗时（保留两位小数）
     */
    fun stopTiming(startTime: Long): Double {
        val elapsed = (System.nanoTime() - startTime) / 1_000_000.0
        return (elapsed * 100).toLong() / 100.0
    }

    /**
     * 格式化耗时为可读字符串
     *
     * @param ms 毫秒值
     * @return 如 "1.23ms"、"2.50s"、"1m 30.00s"
     */
    fun formatMs(ms: Double): String = when {
        ms < 1_000 -> "${format2(ms)}ms"
        ms < 60_000 -> "${format2(ms / 1_000)}s"
        else -> {
            val minutes = (ms / 60_000).toLong()
            val seconds = (ms % 60_000) / 1_000
            "${minutes}m ${format2(seconds)}s"
        }
    }

    private fun format2(value: Double): String = "%.2f".format(value)
}

/**
 * 便捷计时函数
 */
fun timing(): Long = System.nanoTime()

/**
 * 便捷停止计时函数
 */
fun timing(start: Long): Double = TimingUtil.stopTiming(start)
