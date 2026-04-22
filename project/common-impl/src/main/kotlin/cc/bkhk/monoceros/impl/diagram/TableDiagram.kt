package cc.bkhk.monoceros.impl.diagram

import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components

/**
 * 表格结构图
 *
 * 将表格数据渲染为 Unicode 边框表格的 [ComponentText]。
 *
 * 输出示例：
 * ```
 * ┌──────┬──────┐
 * │  ID  │ Name │
 * ├──────┼──────┤
 * │  1   │ Test │
 * └──────┴──────┘
 * ```
 */
class TableDiagram(private val padding: Int = 2) {

    enum class Align { LEFT, CENTER, RIGHT }

    private data class Header(val content: String, val component: ComponentText?, val align: Align)
    private data class Row(val cells: List<String>, val components: List<ComponentText?>)

    private val headers = mutableListOf<Header>()
    private val rows = mutableListOf<Row>()

    /** 添加表头列（纯文本） */
    fun addHeader(content: String, align: Align = Align.CENTER): TableDiagram {
        headers.add(Header(content, null, align))
        return this
    }

    /** 添加表头列（富文本） */
    fun addHeader(component: ComponentText, content: String, align: Align = Align.CENTER): TableDiagram {
        headers.add(Header(content, component, align))
        return this
    }

    /** 添加纯文本行 */
    fun addRow(vararg cells: String): TableDiagram {
        rows.add(Row(cells.toList(), cells.map { null }))
        return this
    }

    /** 添加纯文本行 */
    fun addRow(cells: List<String>): TableDiagram {
        rows.add(Row(cells, cells.map { null }))
        return this
    }

    /** 添加富文本行 */
    fun addComponentRow(cells: List<String>, components: List<ComponentText?>): TableDiagram {
        rows.add(Row(cells, components))
        return this
    }

    /** 构建表格 */
    fun build(): ComponentText {
        if (headers.isEmpty()) return Components.text("")

        val colCount = headers.size
        // 计算每列宽度
        val colWidths = IntArray(colCount) { col ->
            var maxWidth = fixedLength(headers[col].content) + padding * 2
            for (row in rows) {
                val cellWidth = if (col < row.cells.size) fixedLength(row.cells[col]) + padding * 2 else padding * 2
                if (cellWidth > maxWidth) maxWidth = cellWidth
            }
            // 确保偶数宽度
            if (maxWidth % 2 != 0) maxWidth + 1 else maxWidth
        }

        val builder = ComponentBuilder()

        // 顶部边框
        builder.append(buildBorder("┌", "┬", "┐", colWidths)).newLine()

        // 表头行
        builder.append(buildRow(headers.map { it.content }, headers.map { it.component }, headers.map { it.align }, colWidths)).newLine()

        // 表头分隔线
        builder.append(buildBorder("├", "┼", "┤", colWidths))

        // 数据行
        for ((index, row) in rows.withIndex()) {
            builder.newLine()
            val aligns = headers.map { it.align }
            val cells = (0 until colCount).map { if (it < row.cells.size) row.cells[it] else "" }
            val components = (0 until colCount).map { if (it < row.components.size) row.components[it] else null }
            builder.append(buildRow(cells, components, aligns, colWidths))

            // 行间分隔线（最后一行用底部边框）
            if (index < rows.size - 1) {
                builder.newLine()
                builder.append(buildBorder("├", "┼", "┤", colWidths))
            }
        }

        // 底部边框
        builder.newLine()
        builder.append(buildBorder("└", "┴", "┘", colWidths))

        return builder.build()
    }

    /** 构建边框行 */
    private fun buildBorder(left: String, mid: String, right: String, colWidths: IntArray): String {
        val sb = StringBuilder(left)
        colWidths.forEachIndexed { index, width ->
            sb.append("─".repeat(width))
            sb.append(if (index < colWidths.size - 1) mid else right)
        }
        return sb.toString()
    }

    /** 构建数据行 */
    private fun buildRow(
        cells: List<String>,
        components: List<ComponentText?>,
        aligns: List<Align>,
        colWidths: IntArray,
    ): ComponentText {
        val cb = ComponentBuilder()
        cb.append("│")
        for (col in colWidths.indices) {
            val content = if (col < cells.size) cells[col] else ""
            val component = if (col < components.size) components[col] else null
            val align = if (col < aligns.size) aligns[col] else Align.LEFT
            val width = colWidths[col]
            val contentLen = fixedLength(content)
            val totalPad = width - contentLen

            val (leftPad, rightPad) = when (align) {
                Align.LEFT -> padding to (totalPad - padding).coerceAtLeast(0)
                Align.RIGHT -> (totalPad - padding).coerceAtLeast(0) to padding
                Align.CENTER -> {
                    val left = totalPad / 2
                    left to (totalPad - left)
                }
            }

            if (component != null) {
                cb.append(" ".repeat(leftPad))
                cb.append(component)
                cb.append(" ".repeat(rightPad))
            } else {
                cb.append(" ".repeat(leftPad) + content + " ".repeat(rightPad))
            }
            cb.append("│")
        }
        return cb.build()
    }

    companion object {
        /**
         * 计算字符串的显示宽度
         *
         * ASCII 字母、数字、常见符号算 1，其他字符（含中文）算 2。
         */
        fun fixedLength(str: String): Int {
            var length = 0
            for (ch in str) {
                length += if (ch.code in 0x20..0x7E) 1 else 2
            }
            return length
        }
    }
}
