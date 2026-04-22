package cc.bkhk.monoceros.impl.diagram

import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components
import java.util.function.BiFunction

/**
 * 树形结构图
 *
 * 将树形数据渲染为 ASCII 树形结构的 [ComponentText]。
 *
 * 输出示例：
 * ```
 * Root
 * ├── Child1
 * │   ├── Grandchild1
 * │   └── Grandchild2
 * └── Child2
 * ```
 *
 * @param T 节点类型
 */
class TreeDiagram<T>(
    val tabBranch: String = "├── ",
    val tabBranchEnd: String = "└── ",
    val tabIndicator: String = "│   ",
    val tabEmpty: String = "    ",
) {

    /** 节点绘制回调 (depth, node) -> ComponentText */
    private var drawFunc: BiFunction<Int, T, ComponentText>? = null

    /** 缩进回调 (depth, node) -> 额外缩进空格数 */
    private var indentFunc: BiFunction<Int, T, Int>? = null

    /** 子节点遍历回调 (depth, node) -> children */
    private var traversalFunc: BiFunction<Int, T, List<T>>? = null

    fun onDraw(func: BiFunction<Int, T, ComponentText>): TreeDiagram<T> {
        drawFunc = func
        return this
    }

    fun onIndent(func: BiFunction<Int, T, Int>): TreeDiagram<T> {
        indentFunc = func
        return this
    }

    fun onTraversal(func: BiFunction<Int, T, List<T>>): TreeDiagram<T> {
        traversalFunc = func
        return this
    }

    /** 构建完整树，返回 ComponentText */
    fun build(root: T): ComponentText {
        val lines = buildList(root)
        if (lines.isEmpty()) return Components.text("")
        val builder = ComponentBuilder()
        lines.forEachIndexed { index, line ->
            if (index > 0) builder.newLine()
            builder.append(line)
        }
        return builder.build()
    }

    /** 构建为行列表 */
    fun buildList(root: T): List<ComponentText> {
        val result = mutableListOf<ComponentText>()
        renderNode(root, 0, "", true, result)
        return result
    }

    private fun renderNode(
        node: T,
        depth: Int,
        prefix: String,
        isRoot: Boolean,
        result: MutableList<ComponentText>,
    ) {
        // 绘制当前节点
        val content = drawFunc?.apply(depth, node) ?: Components.text(node.toString())
        val indent = indentFunc?.apply(depth, node) ?: 0
        val indentStr = " ".repeat(indent)

        if (isRoot) {
            result.add(Components.text("$indentStr").append(content))
        } else {
            result.add(Components.text("$indentStr$prefix").append(content))
        }

        // 遍历子节点
        val children = traversalFunc?.apply(depth, node) ?: emptyList()
        children.forEachIndexed { index, child ->
            val isLast = index == children.size - 1
            val childPrefix = if (isLast) tabBranchEnd else tabBranch
            val continuationPrefix = if (isRoot) "" else {
                prefix.replace(tabBranch, tabIndicator).replace(tabBranchEnd, tabEmpty)
            }
            renderNode(child, depth + 1, continuationPrefix + childPrefix, false, result)
        }
    }
}
