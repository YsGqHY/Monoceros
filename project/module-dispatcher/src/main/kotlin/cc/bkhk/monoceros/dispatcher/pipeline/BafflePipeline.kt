package cc.bkhk.monoceros.dispatcher.pipeline

import cc.bkhk.monoceros.api.dispatcher.pipeline.Pipeline
import cc.bkhk.monoceros.api.dispatcher.pipeline.PipelineContext
import cc.bkhk.monoceros.impl.util.TimeUtil
import taboolib.common5.Baffle
import java.util.concurrent.TimeUnit

/**
 * 冷却/节流管道
 *
 * 支持两种模式：
 * - 计数冷却：每 N 次触发一次
 * - 时间冷却：间隔 N 毫秒触发一次
 *
 * @param mode 冷却模式："count" 或 "time"
 * @param value 冷却值（计数次数或时间字符串如 "3s"）
 * @param global 是否全局共享冷却（false 时按 principalId 隔离）
 * @param cancelOnBaffle 冷却触发时是否取消事件（false 时仅过滤）
 */
class BafflePipeline(
    private val mode: String,
    private val value: String,
    private val global: Boolean = false,
    private val cancelOnBaffle: Boolean = false,
) : Pipeline {

    override val priority: Int = 128

    private val baffle: Baffle = when (mode.lowercase()) {
        "count" -> Baffle.of(value.toIntOrNull() ?: 1)
        "time" -> {
            val ms = TimeUtil.parseMsOrDefault(value, 1000L)
            Baffle.of(ms, TimeUnit.MILLISECONDS)
        }
        else -> Baffle.of(1)
    }

    override fun filter(context: PipelineContext) {
        val key = if (global) "__global__" else context.principalId
        if (key.isEmpty()) return
        if (!baffle.hasNext(key)) {
            context.isFilterBaffled = true
            context.isFiltered = true
            if (cancelOnBaffle) {
                context.isCancelled = true
            }
        }
    }

    override fun afterFilter(context: PipelineContext) {
        // 过滤通过后更新冷却数据
        val key = if (global) "__global__" else context.principalId
        if (key.isEmpty()) return
        baffle.reset(key)
    }
}
