package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.chat.colored
import taboolib.module.chat.uncolored

/**
 * 颜色/富文本函数，封装 TabooLib 的 colored/uncolored 为 Fluxon 脚本函数。
 *
 * ```fluxon
 * // 顶层函数
 * msg = colored("&a绿色 &b青色 &c红色")
 * plain = uncolored(&msg)
 *
 * // 扩展函数
 * msg = "&e[提示] &f你好" :: colored()
 * plain = &msg :: uncolored()
 * ```
 */
@FluxonRelocate
object FunctionText {

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            exportRegistry.registerClass(TextApi::class.java)

            // colored(text) -> String
            registerFunction("colored", FunctionSignature.returns(Type.STRING).params(Type.STRING)) { ctx ->
                ctx.setReturnRef(TextApi.colored(ctx.getString(0)))
            }
            exportRegisteredFunction("colored")

            // uncolored(text) -> String
            registerFunction("uncolored", FunctionSignature.returns(Type.STRING).params(Type.STRING)) { ctx ->
                ctx.setReturnRef(TextApi.uncolored(ctx.getString(0)))
            }
            exportRegisteredFunction("uncolored")

            // String 扩展函数
            registerExtension(String::class.java)
                .sharedFunction("colored", FunctionSignature.returns(Type.STRING).noParams()) { ctx ->
                    ctx.setReturnRef(TextApi.colored(ctx.target!!))
                }
                .sharedFunction("uncolored", FunctionSignature.returns(Type.STRING).noParams()) { ctx ->
                    ctx.setReturnRef(TextApi.uncolored(ctx.target!!))
                }
        }
    }

    object TextApi {

        /** 颜色代码转换（&a → §a） */
        @Export(shared = true)
        fun colored(text: String): String {
            return text.colored()
        }

        /** 去除所有颜色代码 */
        @Export(shared = true)
        fun uncolored(text: String): String {
            return text.uncolored()
        }
    }
}
