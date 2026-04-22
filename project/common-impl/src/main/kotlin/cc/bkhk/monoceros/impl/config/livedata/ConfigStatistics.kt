package cc.bkhk.monoceros.impl.config.livedata

import cc.bkhk.monoceros.impl.util.TimingUtil
import java.io.File

/**
 * 配置加载统计
 *
 * 记录一次配置加载过程中的文件扫描、变更检测和耗时信息。
 */
class ConfigStatistics(val startTime: Long = TimingUtil.startTiming()) {

    /** 扫描到的文件 */
    val scannedFiles = LinkedHashSet<File>()

    /** 未修改的文件 */
    val unmodifiedFiles = LinkedHashSet<File>()

    /** 新增的文件 */
    val createdFiles = LinkedHashSet<File>()

    /** 修改的文件 */
    val modifiedFiles = LinkedHashSet<File>()

    /** 删除的文件 */
    val deletedFiles = LinkedHashSet<File>()

    /** 加载失败的文件 */
    val failedFiles = LinkedHashSet<File>()

    /** 详情信息 */
    val displayDetails = mutableListOf<String>()

    /** 加载耗时（毫秒） */
    val consumeTime: Double by lazy { TimingUtil.stopTiming(startTime) }

    /** 生成统计摘要 */
    fun summary(): String {
        val parts = mutableListOf<String>()
        parts.add("扫描 ${scannedFiles.size} 个文件")
        if (createdFiles.isNotEmpty()) parts.add("新增 ${createdFiles.size}")
        if (modifiedFiles.isNotEmpty()) parts.add("修改 ${modifiedFiles.size}")
        if (deletedFiles.isNotEmpty()) parts.add("删除 ${deletedFiles.size}")
        if (failedFiles.isNotEmpty()) parts.add("失败 ${failedFiles.size}")
        if (unmodifiedFiles.isNotEmpty()) parts.add("未变更 ${unmodifiedFiles.size}")
        parts.add("耗时 ${TimingUtil.formatMs(consumeTime)}")
        return parts.joinToString(", ")
    }
}
