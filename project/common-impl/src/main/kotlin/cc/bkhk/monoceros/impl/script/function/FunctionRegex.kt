package cc.bkhk.monoceros.impl.script.function

import cc.bkhk.monoceros.impl.script.FluxonChecker
import cc.bkhk.monoceros.impl.script.relocate.FluxonRelocate
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 正则表达式函数，封装 java.util.regex 为 Fluxon 脚本函数。
 *
 * 虽然 Fluxon String 已有 matches/replaceAll/findAll 基础能力，
 * 但缺少独立 Regex 对象（捕获组、命名组、编译缓存等）。
 *
 * ```fluxon
 * // 快捷函数
 * m = regexMatch("Hello 123 World 456", "\\d+")
 * print(&m :: value())   // "123"
 *
 * all = regexMatchAll("Hello 123 World 456", "\\d+")
 * &all :: each(|| print(&it :: value()))  // "123", "456"
 *
 * result = regexReplace("Hello World", "(\\w+)", "[$1]")
 * print(&result)  // "[Hello] [World]"
 *
 * // 编译正则对象，可复用
 * re = regex("(\\d{4})-(\\d{2})-(\\d{2})")
 * m = &re :: match("日期: 2024-01-15")
 * if &m != null {
 *     print(&m :: group(1))  // "2024"
 *     print(&m :: group(2))  // "01"
 *     print(&m :: group(3))  // "15"
 * }
 *
 * // 分割
 * parts = &re :: split("aaa---bbb---ccc")
 * ```
 */
@FluxonRelocate
object FunctionRegex {

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        if (!FluxonChecker.isReady()) return
        with(FluxonRuntime.getInstance()) {
            exportRegistry.registerClass(RegexApi::class.java)

            // regex(pattern) -> RegexWrapper
            registerFunction("regex", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                ctx.setReturnRef(RegexApi.compile(ctx.getString(0)))
            }
            exportRegisteredFunction("regex")

            // regexMatch(text, pattern) -> MatchWrapper?
            registerFunction("regexMatch", FunctionSignature.returnsObject().params(Type.STRING, Type.STRING)) { ctx ->
                ctx.setReturnRef(RegexApi.match(ctx.getString(0), ctx.getString(1)))
            }
            exportRegisteredFunction("regexMatch")

            // regexMatchAll(text, pattern) -> List<MatchWrapper>
            registerFunction("regexMatchAll", FunctionSignature.returnsObject().params(Type.STRING, Type.STRING)) { ctx ->
                ctx.setReturnRef(RegexApi.matchAll(ctx.getString(0), ctx.getString(1)))
            }
            exportRegisteredFunction("regexMatchAll")

            // regexReplace(text, pattern, replacement) -> String
            registerFunction("regexReplace", FunctionSignature.returns(Type.STRING).params(Type.STRING, Type.STRING, Type.STRING)) { ctx ->
                ctx.setReturnRef(RegexApi.replaceAll(ctx.getString(0), ctx.getString(1), ctx.getString(2)))
            }
            exportRegisteredFunction("regexReplace")

            // RegexWrapper 扩展函数
            registerExtension(RegexWrapper::class.java)
                .sharedFunction("test", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.test(ctx.getString(0)))
                }
                .sharedFunction("match", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.match(ctx.getString(0)))
                }
                .sharedFunction("matchAll", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.matchAll(ctx.getString(0)))
                }
                .sharedFunction("replace", FunctionSignature.returns(Type.STRING).params(Type.STRING, Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.replace(ctx.getString(0), ctx.getString(1)))
                }
                .sharedFunction("replaceAll", FunctionSignature.returns(Type.STRING).params(Type.STRING, Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.replaceAll(ctx.getString(0), ctx.getString(1)))
                }
                .sharedFunction("split", FunctionSignature.returnsObject().params(Type.STRING)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.split(ctx.getString(0)))
                }
                .sharedFunction("pattern", FunctionSignature.returns(Type.STRING).noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.pattern())
                }

            // MatchWrapper 扩展函数
            registerExtension(MatchWrapper::class.java)
                .sharedFunction("value", FunctionSignature.returns(Type.STRING).noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.value)
                }
                .sharedFunction("group", FunctionSignature.returns(Type.STRING).params(Type.I)) { ctx ->
                    ctx.setReturnRef(ctx.target!!.group(ctx.getInt(0)))
                }
                .sharedFunction("groupCount", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.groupCount)
                }
                .sharedFunction("start", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.start)
                }
                .sharedFunction("end", FunctionSignature.returns(Type.I).noParams()) { ctx ->
                    ctx.setReturnInt(ctx.target!!.end)
                }
                .sharedFunction("groups", FunctionSignature.returnsObject().noParams()) { ctx ->
                    ctx.setReturnRef(ctx.target!!.groups())
                }
        }
    }

    /** 正则表达式包装类 */
    class RegexWrapper(private val compiled: Pattern) {

        /** 测试是否匹配 */
        fun test(text: String): Boolean = compiled.matcher(text).find()

        /** 首个匹配 */
        fun match(text: String): MatchWrapper? {
            val matcher = compiled.matcher(text)
            return if (matcher.find()) MatchWrapper.from(matcher) else null
        }

        /** 全局匹配 */
        fun matchAll(text: String): List<MatchWrapper> {
            val matcher = compiled.matcher(text)
            val results = mutableListOf<MatchWrapper>()
            while (matcher.find()) {
                results.add(MatchWrapper.from(matcher))
            }
            return results
        }

        /** 替换首个匹配 */
        fun replace(text: String, replacement: String): String {
            return compiled.matcher(text).replaceFirst(replacement)
        }

        /** 替换全部匹配 */
        fun replaceAll(text: String, replacement: String): String {
            return compiled.matcher(text).replaceAll(replacement)
        }

        /** 按正则分割 */
        fun split(text: String): List<String> {
            return compiled.split(text).toList()
        }

        /** 获取正则表达式字符串 */
        fun pattern(): String = compiled.pattern()
    }

    /** 匹配结果包装类 */
    class MatchWrapper(
        val value: String,
        val start: Int,
        val end: Int,
        val groupCount: Int,
        private val groupValues: List<String?>
    ) {
        /** 按索引获取捕获组（0 为完整匹配） */
        fun group(index: Int): String? {
            return if (index in groupValues.indices) groupValues[index] else null
        }

        /** 所有捕获组列表 */
        fun groups(): List<String?> = groupValues

        companion object {
            fun from(matcher: Matcher): MatchWrapper {
                val groups = (0..matcher.groupCount()).map {
                    try { matcher.group(it) } catch (_: Throwable) { null }
                }
                return MatchWrapper(
                    value = matcher.group(),
                    start = matcher.start(),
                    end = matcher.end(),
                    groupCount = matcher.groupCount(),
                    groupValues = groups
                )
            }
        }
    }

    object RegexApi {

        /** 编译正则表达式 */
        @Export(shared = true)
        fun compile(pattern: String): RegexWrapper {
            return RegexWrapper(Pattern.compile(pattern))
        }

        /** 快捷匹配首个 */
        @Export(shared = true)
        fun match(text: String, pattern: String): MatchWrapper? {
            return compile(pattern).match(text)
        }

        /** 快捷全局匹配 */
        @Export(shared = true)
        fun matchAll(text: String, pattern: String): List<MatchWrapper> {
            return compile(pattern).matchAll(text)
        }

        /** 快捷替换全部 */
        @Export(shared = true)
        fun replaceAll(text: String, pattern: String, replacement: String): String {
            return compile(pattern).replaceAll(text, replacement)
        }
    }
}
