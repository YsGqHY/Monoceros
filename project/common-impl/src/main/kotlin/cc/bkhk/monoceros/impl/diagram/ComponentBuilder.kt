package cc.bkhk.monoceros.impl.diagram

import taboolib.module.chat.ComponentText
import taboolib.module.chat.Components

/**
 * ComponentText 拼接构建器
 *
 * 将连续纯文本合并为单个 [ComponentText]，减少组件数量。
 * 遇到 [ComponentText] 时先 flush 纯文本缓冲。
 */
class ComponentBuilder {

    private val parts = mutableListOf<ComponentText>()
    private val textBuffer = StringBuilder()

    /** 追加纯文本 */
    fun append(text: String): ComponentBuilder {
        textBuffer.append(text)
        return this
    }

    /** 追加富文本组件 */
    fun append(component: ComponentText): ComponentBuilder {
        flush()
        parts.add(component)
        return this
    }

    /** 追加换行 */
    fun newLine(): ComponentBuilder {
        textBuffer.append("\n")
        return this
    }

    /** 构建最终 ComponentText */
    fun build(): ComponentText {
        flush()
        if (parts.isEmpty()) return Components.text("")
        var result = parts[0]
        for (i in 1 until parts.size) {
            result = result.append(parts[i])
        }
        return result
    }

    private fun flush() {
        if (textBuffer.isNotEmpty()) {
            parts.add(Components.text(textBuffer.toString()))
            textBuffer.clear()
        }
    }
}
